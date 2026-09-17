package com.llm.memory;

import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat memory managed by hand with the low-level ChatModel API - no advisor involved.
 * This is exactly what the MessageChatMemoryAdvisor does for you behind the scenes.
 */
@RestController
public class ChatModelMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ChatModelMemoryController.class);

    private final ChatModel chatModel;
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

    public ChatModelMemoryController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/v3/memory/chats/{conversationId}")
    public String chat(@RequestBody @Valid UserInput userInput,
                       @PathVariable String conversationId) {
        log.info("conversationId : {} , userInput : {} ", conversationId, userInput);

        // 1. save the user message
        chatMemory.add(conversationId, new UserMessage(userInput.prompt()));

        // 2. send the whole conversation (not just the latest message) to the LLM
        var chatResponse = chatModel.call(new Prompt(chatMemory.get(conversationId)));

        // 3. save the assistant reply so the next call can see it
        var assistantMessage = chatResponse.getResult().getOutput();
        chatMemory.add(conversationId, assistantMessage);

        return assistantMessage.getText();
    }
}
