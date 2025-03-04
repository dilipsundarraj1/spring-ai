package com.llm.context;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ContextController {

    private static final Logger log = LoggerFactory.getLogger(ContextController.class);


    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/context-template.st")
    private Resource promptTemplateWithContext;

    @Value("classpath:/files/lion-king.txt")
    private Resource context;


    public ContextController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    @PostMapping("/context")
    public String prompts(@RequestBody UserInput userInput) {
        log.info("userInput : {} ", userInput);

        log.info("promptTemplate : {} , text to provide  context : {} ", promptTemplateWithContext, context);

        var inputMap =Map.of("context", context, "question", userInput.prompt());

        var promptTemplate = new PromptTemplate(promptTemplateWithContext);

        var prompt = promptTemplate.create(inputMap);

        var requestSpec = chatClient.prompt(prompt);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

}
