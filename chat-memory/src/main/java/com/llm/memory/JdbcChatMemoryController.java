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
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat memory persisted in Postgres via the JdbcChatMemoryRepository - survives application restarts.
 * <p>
 * Both beans are auto-configured by spring-ai-starter-model-chat-memory-repository-jdbc:
 * a JdbcChatMemoryRepository, and a MessageWindowChatMemory (last 20 messages) that uses it.
 */
@RestController
public class JdbcChatMemoryController {

    private static final Logger log = LoggerFactory.getLogger(JdbcChatMemoryController.class);

    private final ChatMemory chatMemory;
    private final ChatClient chatClient;

    public JdbcChatMemoryController(ChatClient.Builder chatClientBuilder,
                                    ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

}
