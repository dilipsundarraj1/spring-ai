package com.llm.prompt_engineering;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TravelAssistantController {

    private static final Logger log = LoggerFactory.getLogger(TravelAssistantController.class);
    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/travel_prompt.st")
    private Resource travelPromptMessage;

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

    @PostMapping("/v2/travel_assistant")
    public String promptsv2(@RequestBody UserInput userInput) {
        log.info("userInput : {} ", userInput);

        var systemMessage = """
                You are a professional travel planner with extensive knowledge of worldwide destinations,
                including cultural attractions, accommodations, and travel logistics.
                Provide better lodging options too that supports the family.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(travelPromptMessage);
        var message = promptTemplate.createMessage(Map.of("context", userInput.context(), "input", userInput.prompt()));

        var promptMessage = new Prompt(
                new SystemMessage(systemMessage), // Sets the role
                message
                );

        log.info("promptMessage : {} ", promptMessage);
        var requestSpec = chatClient.prompt(promptMessage);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

}
