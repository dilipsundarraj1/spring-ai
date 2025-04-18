package com.llm.chats;

import com.llm.dto.AIResponse;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private OpenAiChatModel openAiChatModel;

    public ChatController(ChatClient.Builder chatClientBuilder, OpenAiChatModel openAiChatModel) {
        this.chatClient = chatClientBuilder
                .build();
        this.openAiChatModel = openAiChatModel;
    }


    @PostMapping("/v1/chats")
    public Object chat(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);
        var requestSpec = chatClient.prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(userInput.prompt())
                //.system("You are a helpful assistant")
                ;

        log.info("requestSpec : {} ", requestSpec);

        var responseSpec = requestSpec.call();
        log.info("responseSpec1 : {} ", responseSpec);
        log.info("content : {} ", responseSpec.content());
//        return new AIResponse(responseSpec.content());
        return responseSpec.chatResponse();
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

        log.info("aiResponse : {} ", aiResponse);
        return aiResponse;
    }

    @PostMapping("/v1/chats/stream")
    public Flux<String> chatWithStream(@RequestBody UserInput userInput) {
        return chatClient.prompt()
                .user(userInput.prompt())
                .stream()
                .content()
                .doOnNext(s -> log.info("s : {}", s))
                .doOnComplete(() -> log.info("Data complete"))
//                .onErrorReturn("Error occurred in the stream");
//                .onErrorComplete(throwable -> );
                .onErrorResume(throwable -> {
                    log.error("Error occurred in the stream", throwable);
                    var error = """
                            Error occurred in the stream:
                            %s
                            """.formatted(throwable.getMessage());
                    return Flux.error(new RuntimeException(error));
                });
    }

    @PostMapping("/v2/chats")
    public String chatV2(@RequestBody UserInput userInput) {
        log.info("userInput message in chats_v2: {} ", userInput);

        var userChatOptions = userInput.chatOptions();

        var temperature = (userChatOptions.temperature() == null || userChatOptions.temperature() == 0.0)
                ? 1.0f : userChatOptions.temperature();
        var maxTokens = (userChatOptions.maxTokens() == 0) ? 100 : userChatOptions.maxTokens();

        log.info("temperature : {},  maxTokens : {} ", temperature, maxTokens);

        var chatOptions = ChatOptions
                .builder()
                .temperature(temperature)
//                .maxTokens(maxTokens)
                .build();

        var systemMessage = """
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """;

        var responseSpec = ChatClient.builder(openAiChatModel)
                .build()
                .prompt()
                .options(chatOptions)
                .user(userInput.prompt())
                .system(systemMessage)
                .call();

        log.info("responseSpec : {} ", responseSpec);
        return responseSpec.content();
    }


    @PostMapping("/v2/chats/stream")
    public  Flux<String> chatV2Stream(@RequestBody UserInput userInput) {
        log.info("userInput message in chats_v2 stream: {} ", userInput);

        var userChatOptions = userInput.chatOptions();

        var temperature = (userChatOptions.temperature() == null || userChatOptions.temperature() == 0.0)
                ? 1.0f : userChatOptions.temperature();
        var maxTokens = (userChatOptions.maxTokens() == 0) ? 100 : userChatOptions.maxTokens();

        log.info("temperature : {},  maxTokens : {} ", temperature, maxTokens);

        var chatOptions = ChatOptions
                .builder()
                .temperature(temperature)
//                .maxTokens(maxTokens)
                .build();

        var systemMessage = """
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """;

        var responseSpec = ChatClient.builder(openAiChatModel)
                .build()
                .prompt()
                .options(chatOptions)
                .user(userInput.prompt())
                .system(systemMessage)
                .stream()
                .content()
                .doOnNext(s -> log.info("s : {}", s))
                .doOnComplete(() -> log.info("Data complete"))
//                .onErrorReturn("Error occurred in the stream");
//                .onErrorComplete(throwable -> );
                .onErrorResume(throwable -> {
                    log.error("Error occurred in the stream", throwable);
                    var error = """
                            Error occurred in the stream:
                            %s
                            """.formatted(throwable.getMessage());
                    return Flux.error(new RuntimeException(error));
                });
        ;


        return responseSpec;
    }




}
