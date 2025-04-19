package com.llm.advisors;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdvisorsChatController {
    private static final Logger log = LoggerFactory.getLogger(AdvisorsChatController.class);

    private final ChatClient chatClient;

    public AdvisorsChatController(ChatClient.Builder chatClientBuilder, OpenAiChatModel openAiChatModel) {
        this.chatClient = chatClientBuilder
                .build();
    }

    @PostMapping("/v1/advisors")
    public String advisors(@RequestBody UserInput userInput) {

        var systemMessage = """
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """;

        var responseSpec = chatClient
                .prompt()
                .user(userInput.prompt())
                .system(systemMessage)
                .call();

        log.info("responseSpec : {} ", responseSpec);
        return responseSpec.content();
    }

}
