package com.llm.chats;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CodingAssistantController {

    private static final Logger log = LoggerFactory.getLogger(CodingAssistantController.class);


    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/coding-assistant.st")
    private Resource systemText;

    public CodingAssistantController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }



    @PostMapping("/v1/coding_assistant/{language}")
    public String promptsByLanguage(
            @PathVariable String language,
            @RequestBody UserInput userInput) {
        log.info("userInput : {} , language : {} ", userInput, language);

        var requestSpec = chatClient
                .prompt()
                .user(userInput.prompt())
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemText)
                        .param("language", language));

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

    @PostMapping("/v2/coding_assistant/{language}")
    public String promptsByLanguageV2(
            @PathVariable String language,
            @RequestBody UserInput userInput) {
        log.info("userInput : {} , language : {} ", userInput, language);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("language", language));

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
