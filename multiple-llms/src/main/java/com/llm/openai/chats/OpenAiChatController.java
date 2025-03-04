package com.llm.openai.chats;

import com.llm.dto.AIResponse;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/openai")
public class OpenAiChatController {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatController.class);

    private final ChatClient chatClient;
    public OpenAiChatController(@Qualifier("openAIChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @PostMapping("/v1/chats")
    public String chat(@RequestBody @Valid UserInput userInput) {
        log.info("userInput message : {} ", userInput);
        //customChatClient(userInput);
        var requestSpec = chatClient.prompt()
                .user(userInput.prompt())
                //.system("You are a helpful assistant")
                ;

        log.info("requestSpec : {} ", requestSpec);

        var responseSpec = requestSpec.call();
        log.info("responseSpec1 : {} ", responseSpec);
        log.info("content : {} ", responseSpec.content());
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
                .doOnComplete(() -> log.info("Data complete"));
    }

}
