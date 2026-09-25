package com.llm.chats;

import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * No memory at all - every call is a brand-new conversation.
 * Use this endpoint to demonstrate that LLMs are stateless.
 */
@RestController
public class StatelessChatController {

    private static final Logger log = LoggerFactory.getLogger(StatelessChatController.class);

    private final ChatClient chatClient;

    public StatelessChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/v1/chats")
    public String chat(@RequestBody @Valid UserInput userInput) {
        log.info("userInput : {} ", userInput);
        return chatClient.prompt()
                .user(userInput.prompt())
                .call()
                .content();
    }
}
