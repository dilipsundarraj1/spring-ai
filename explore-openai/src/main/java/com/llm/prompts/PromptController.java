package com.llm.prompts;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PromptController {


    private static final Logger log = LoggerFactory.getLogger(PromptController.class);
    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/java-coding-assistant.st")
    private Resource systemTemplateMessage;

    public PromptController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    @PostMapping("/prompts")
    public String prompts(@RequestBody UserInput userInput) {
        log.info("userInput : {} ", userInput);

//        var systemMessage = new SystemMessage("""
//                You are a helpful assistant, who can answer java based questions.
//                For any other questions, please respond with I don't know in a funny way!
//                """);

        var systemMessage = new SystemMessage(systemTemplateMessage);

        log.info("systemMessage : {} ", systemMessage);

        var promptMessage = new Prompt(
                List.of(systemMessage,
                        new UserMessage(userInput.prompt())));

        var requestSpec = chatClient.prompt(promptMessage);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

}
