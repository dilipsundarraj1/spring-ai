package com.llm.memory;

import com.llm.dto.ChatMessage;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat memory kept in the JVM heap (ConcurrentHashMap) - lost when the application restarts.
 */
@RestController
public class InMemoryChatController {

    private static final Logger log = LoggerFactory.getLogger(InMemoryChatController.class);

    static final int MAX_MESSAGES = 10;

    private final ChatMemory chatMemory;
    private final ChatClient chatClient;

    public InMemoryChatController(ChatClient.Builder chatClientBuilder) {
        // Built explicitly instead of injecting the auto-configured ChatMemory bean,
        // because in this module that bean is backed by the JDBC repository.
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/v1/memory/chats/{conversationId}")
    public String chat(@RequestBody @Valid UserInput userInput,
                       @PathVariable String conversationId) {
        log.info("conversationId : {} , userInput : {} ", conversationId, userInput);
        return chatClient.prompt()
                .user(userInput.prompt())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @GetMapping("/v1/memory/chats/{conversationId}")
    public List<ChatMessage> history(@PathVariable String conversationId) {
        return chatMemory.get(conversationId)
                .stream()
                .map(ChatMessage::from)
                .toList();
    }

    @DeleteMapping("/v1/memory/chats/{conversationId}")
    public ResponseEntity<Void> clear(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
        return ResponseEntity.noContent().build();
    }
}
