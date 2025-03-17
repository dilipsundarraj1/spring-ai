package com.llm.prompt_engineering;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PromptTypesController {

    private static final Logger log = LoggerFactory.getLogger(TravelAssistantController.class);
    private final ChatClient chatClient;

    public PromptTypesController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Value("classpath:/prompt-templates/few_shot.st")
    private Resource fewShotPrompt;


    @PostMapping("/v1/prompt_types/zero_shot")
    public String zeroShot(@RequestBody UserInput userInput) {
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

    //   happy
    // unhappy

    @PostMapping("/v1/prompt_types/few_shot")
    public String fewShot(@RequestBody UserInput userInput) {
        log.info("userInput : {} ", userInput);

        var fewShotExamples = """
                Prompt : "The product arrived quickly, worked perfectly, and exceeded my expectations!"
                Answer : happy
                
                Prompt : "Great quality, fast shipping, and exactly as described—highly recommend!"
                Answer : happy
                
                Prompt : "The item arrived broken and didn’t function at all—very disappointing!"
                Answer : unhappy
                
                Prompt : "Poor packaging led to a damaged product that was completely useless."
                Answer : unhappy
                
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(fewShotPrompt);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("few_shot_prompts", fewShotExamples));

        var promptMessage = new Prompt(
                List.of(
                        systemMessage,
                        new UserMessage(userInput.prompt())
                )
        );
        var requestSpec = chatClient.prompt(promptMessage);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

    @PostMapping("/v1/prompt_types/cot")
    public String cot(@RequestBody UserInput userInput) {
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
