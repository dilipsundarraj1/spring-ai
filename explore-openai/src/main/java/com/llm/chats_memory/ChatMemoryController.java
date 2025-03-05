package com.llm.chats_memory;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatMemoryController {

//    Logger log = LoggerFactory.getLogger(ChatMemoryController.class);
    Logger log = LoggerFactory.getLogger(this.getClass().getName());



    private final ChatClient chatClient;

    public ChatMemoryController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    @PostMapping("/api/memory/chats/{session_id}")
    public String chat(@RequestBody UserInput userInput,
                       @PathVariable String session_id) {
        log.info("Input userInput : {} ", userInput);
        var requestSpec = chatClient.prompt()
                .advisors(advisor -> {
                    log.info("advisor : {}", advisor);
                    advisor.param("chat_memory_conversation_id",session_id );
                })
                .user(userInput.prompt());
        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec);
        return responseSpec.content();
    }
}
