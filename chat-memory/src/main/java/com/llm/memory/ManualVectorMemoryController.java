package com.llm.memory;

import com.llm.dto.MemoryAwareAnswer;
import com.llm.dto.RecallReport;
import com.llm.dto.RecalledMemory;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Semantic chat memory without VectorStoreChatMemoryAdvisor. The retrieval is done here, so we
 * control the two things the advisor does not expose: a similarity threshold and de-duplication. Every response reports what was recalled and at what score.
 */
@RestController
public class ManualVectorMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ManualVectorMemoryController.class);

    /**
     * Absolute relevance floor. The advisor has no equivalent: it always accepts every hit
     * (SearchRequest defaults to SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0).
     * <p>
     * Default 0.0 deliberately matches the advisor, so the difference between the two endpoints
     * starts at zero. Raise it per request (?threshold=0.85) and watch recall change - but measure
     * first: with OpenAI embeddings, unrelated short messages still score ~0.80+. See the README.
     */
    static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

    static final int DEFAULT_TOP_K = 20;

    /**
     * De-duplication has to happen before topK is applied, otherwise repeated questions have
     * already eaten the budget. We ask the store for more rows than we need, collapse them, then
     * trim back to topK.
     */
    private static final int OVER_FETCH_FACTOR = 3;

    private static final int MAX_FETCH = 200;

    /** Same metadata keys the advisor writes, so v4 and v5 share one conversation. */
    private static final String CONVERSATION_ID_METADATA = "conversationId";

    private static final String MESSAGE_TYPE_METADATA = "messageType";

    private static final String INSTRUCTIONS = """
            You are a helpful assistant.
            The RECALLED_MEMORY section holds facts this user told you earlier in this same conversation.
            Treat them as true and answer from them. If the section is empty, just answer normally - do not
            invent past conversation.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ManualVectorMemoryController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // No memory advisor at all - retrieval and storage are done explicitly below.
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/v5/memory/chats/{conversationId}")
    public MemoryAwareAnswer chat(@RequestBody @Valid UserInput userInput,
                                  @PathVariable String conversationId,
                                  @RequestParam(defaultValue = "" + DEFAULT_SIMILARITY_THRESHOLD) double threshold,
                                  @RequestParam(defaultValue = "" + DEFAULT_TOP_K) int topK,
                                  @RequestParam(defaultValue = "true") boolean dedupe) {
        log.info("conversationId : {} , threshold : {} , topK : {} , dedupe : {} , userInput : {}",
                conversationId, threshold, topK, dedupe, userInput);

        // 1. retrieve - filtered by score, de-duplicated, then trimmed to topK
        var memory = search(conversationId, userInput.prompt(), threshold, topK, dedupe);

        // 2. build the system message ourselves, with our own wording
        var systemPrompt = INSTRUCTIONS + renderMemory(memory.recalled());
        log.info("system prompt :\n{}", systemPrompt);
        log.info("user prompt : {}", userInput.prompt());

        var answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userInput.prompt())
                .call()
                .content();

        // 3. store this turn, exactly as the advisor would
        remember(conversationId, MessageType.USER, userInput.prompt());
        remember(conversationId, MessageType.ASSISTANT, answer);

        return new MemoryAwareAnswer(answer, memory);
    }

    /**
     * The retrieval step on its own - no LLM call, no tokens spent.
     */
    @GetMapping("/v5/memory/chats/{conversationId}")
    public RecallReport recall(@PathVariable String conversationId,
                               @RequestParam String query,
                               @RequestParam(defaultValue = "" + DEFAULT_SIMILARITY_THRESHOLD) double threshold,
                               @RequestParam(defaultValue = "" + DEFAULT_TOP_K) int topK,
                               @RequestParam(defaultValue = "true") boolean dedupe) {
        return search(conversationId, query, threshold, topK, dedupe);
    }

    private RecallReport search(String conversationId, String query, double threshold, int topK,
                                boolean dedupe) {
        // over-fetch so that de-duplication does not leave us short of topK
        int fetchSize = dedupe ? Math.min(topK * OVER_FETCH_FACTOR, MAX_FETCH) : topK;

        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(fetchSize)
                .similarityThreshold(threshold)          // the line the advisor never lets you write
                .filterExpression(conversationFilter(conversationId))
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        if (documents == null) {
            documents = List.of();
        }
        int fetched = documents.size();

        // results arrive ordered by score, best first
        var hits = documents.stream()
                .map(document -> RecalledMemory.from(document, MESSAGE_TYPE_METADATA))
                .toList();

        if (dedupe) {
            hits = removeDuplicates(hits);
        }
        if (hits.size() > topK) {
            hits = hits.subList(0, topK);
        }

        return new RecallReport(threshold, dedupe, topK, fetched, hits.size(), hits);
    }

    /**
     * Collapses repeated messages - the advisor stores every question, so asking the same thing
     * three times leaves three copies at similarity 1.0 that carry no information. The first
     * occurrence wins because the list is already ordered by score.
     */
    private static List<RecalledMemory> removeDuplicates(List<RecalledMemory> hits) {
        Map<String, RecalledMemory> unique = new LinkedHashMap<>();
        for (RecalledMemory hit : hits) {
            unique.putIfAbsent(normalize(hit.text()), hit);
        }
        return new ArrayList<>(unique.values());
    }

    private static String normalize(@Nullable String text) {
        return text == null ? "" : text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static String renderMemory(List<RecalledMemory> recalled) {
        if (recalled.isEmpty()) {
            return "\nRECALLED_MEMORY: (nothing relevant)\n";
        }
        var entries = recalled.stream()
                .map(memory -> "- " + memory.type().toLowerCase() + ": " + memory.text())
                .collect(Collectors.joining("\n"));
        return "\nRECALLED_MEMORY:\n" + entries + "\n";
    }

    private void remember(String conversationId, MessageType messageType, String text) {
        vectorStore.add(List.of(Document.builder()
                .text(text)
                .metadata(Map.of(CONVERSATION_ID_METADATA, conversationId, MESSAGE_TYPE_METADATA, messageType.name()))
                .build()));
    }

    private Filter.Expression conversationFilter(String conversationId) {
        return new FilterExpressionBuilder().eq(CONVERSATION_ID_METADATA, conversationId).build();
    }
}
