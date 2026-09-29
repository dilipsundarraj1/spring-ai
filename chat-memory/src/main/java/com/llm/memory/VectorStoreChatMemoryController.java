package com.llm.memory;

import com.llm.dto.ScoredMessage;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat memory stored in a vector store (pgvector). Instead of replaying the last N messages,
 * only the past messages that are semantically similar to the current question are retrieved
 * and appended to the system message.
 */
@RestController
public class VectorStoreChatMemoryController {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreChatMemoryController.class);

    /**
     * How many past messages are retrieved per question - the advisor's own default.
     * Keep this generous: the advisor stores every question too, so repeated questions sit at
     * similarity 1.0 and push the actual answers out of a small window.
     */
    static final int TOP_K = 20;

    /**
     * Metadata keys written by VectorStoreChatMemoryAdvisor on every stored message.
     * They are private constants in the advisor, so they are repeated here to query/delete by conversation.
     */
    private static final String CONVERSATION_ID_METADATA = "conversationId";

    private static final String MESSAGE_TYPE_METADATA = "messageType";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public VectorStoreChatMemoryController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        // The advisor writes into the vector store itself - no ChatMemory / ChatMemoryRepository involved.
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a helpful assistant.
                        The LONG_TERM_MEMORY section holds facts this user told you earlier in this same
                        conversation. Treat them as true and answer from them. If the answer is there, use it
                        instead of replying that you do not know or have no access to the user's data.""")
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .defaultTopK(TOP_K)
                                .build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/v4/memory/chats/{conversationId}")
    public String chat(@RequestBody @Valid UserInput userInput,
                       @PathVariable String conversationId,
                       @RequestParam(required = false) Integer topK) {
        log.info("conversationId : {} , userInput : {} ", conversationId, userInput);
        return chatClient.prompt()
                .user(userInput.prompt())
                .advisors(advisor -> {
                    advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (topK != null) {
                        // overrides defaultTopK for this call only
                        advisor.param(VectorStoreChatMemoryAdvisor.TOP_K, topK);
                    }
                })
                .call()
                .content();
    }

    /**
     * Shows which past messages the advisor would retrieve for a given question - the same
     * similarity search, run by hand.
     */
    @GetMapping("/v4/memory/chats/{conversationId}")
    public List<ScoredMessage> recall(@PathVariable String conversationId,
                                     @RequestParam String query,
                                     @RequestParam(defaultValue = "" + TOP_K) int topK) {
        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(conversationFilter(conversationId))
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        if (documents == null) {
            return List.of();
        }
        // score and distance are the same measurement inverted - see ScoredMessage
        return documents.stream()
                .map(document -> ScoredMessage.from(document, MESSAGE_TYPE_METADATA))
                .toList();
    }

    @DeleteMapping("/v4/memory/chats/{conversationId}")
    public ResponseEntity<Void> clear(@PathVariable String conversationId) {
        vectorStore.delete(conversationFilter(conversationId));
        return ResponseEntity.noContent().build();
    }

    private Filter.Expression conversationFilter(String conversationId) {
        return new FilterExpressionBuilder().eq(CONVERSATION_ID_METADATA, conversationId).build();
    }
}
