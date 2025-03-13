package com.llm.prompt_engineering;

import com.llm.chats.PromptController;
import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TravelAssistantController {

    private static final Logger log = LoggerFactory.getLogger(TravelAssistantController.class);
    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/01_travel_prompt.st")
    private Resource systemTemplateMessage;

    public TravelAssistantController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    @PostMapping("/v1/travel_assistant")
    public String prompts(@RequestBody UserInput userInput) {
        log.info("userInput : {} ", userInput);

        var promptMessage = new Prompt(
                List.of(
                        new UserMessage(userInput.prompt())
                )
        );

        var requestSpec = chatClient.prompt(promptMessage);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

}
