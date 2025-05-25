package com.llm.chats;

import com.llm.dto.AIResponse;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .build();
    }

    @PostMapping("/v1/chats")
    public String chat(@RequestBody @Valid UserInput userInput) {
        log.info("userInput message : {} ", userInput);
        //customChatClient(userInput);
        //http://localhost:port/api/chat
        var requestSpec = chatClient.prompt()
                .user(userInput.prompt());

        var responseSpec = requestSpec.call();
        return responseSpec.content();
    }

    @PostMapping("/v1/chats/entity")
    public AIResponse chatEntity(@RequestBody @Valid UserInput userInput) {
        log.info("userInput message : {} ", userInput);
        //customChatClient(userInput);
        var requestSpec = chatClient.prompt()
                .user(userInput.prompt());

        var aiResponse = requestSpec
                .call()
                .entity(AIResponse.class); // It uses Bean Output Converter.
        return aiResponse;
    }

    @PostMapping("/v1/chats/stream")
    public Flux<String> chatWithStream(@RequestBody UserInput userInput) {
        return chatClient.prompt()
                .user(userInput.prompt())
                .stream()
                .content()
                .doOnNext(s -> log.info("s : {}", s))
                .doOnComplete(() -> log.info("Data complete"));
    }

    @PostMapping("/v2/chats")
    public String chatV2(@RequestBody UserInput userInput) {
        log.info("userInput message : {} ", userInput);

        var userChatOptions = userInput.chatOptions();


        var temperature = (userChatOptions.temperature()==null || userChatOptions.temperature() == 0.0)
                ? 1.0f : userChatOptions.temperature();
        var maxTokens = (userChatOptions.maxTokens() == 0) ? 100 : userChatOptions.maxTokens();

        log.info("temperature : {},  maxTokens : {} ", temperature, maxTokens);

        var chatOptions = ChatOptions
                .builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        var chatClient1 = chatClient.mutate()
                .defaultOptions(chatOptions)
                .build();
        var requestSpec = chatClient1.prompt()
                .user(userInput.prompt());

        var responseSpec = requestSpec.call();
        return responseSpec.content();
    }

}
