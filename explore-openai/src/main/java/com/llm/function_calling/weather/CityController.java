package com.llm.function_calling.weather;

import com.llm.dto.UserInput;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
public class CityController {

    private final ChatClient chatClient;

    public CityController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a helpful AI Assistant answering questions about cities around the world.")
                .defaultFunctions("currentWeatherFunction")
                .build();
    }

    @PostMapping("/cities")
    public String cityFaq(@RequestBody UserInput userInput) {
        return chatClient.prompt()
                .user(userInput.prompt())
                .call()
                .content();
    }
}
