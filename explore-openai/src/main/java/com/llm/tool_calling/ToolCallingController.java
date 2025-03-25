package com.llm.tool_calling;

import com.llm.dto.UserInput;
import com.llm.tool_calling.currency.CurrencyTools;
import com.llm.tool_calling.currenttime.DateTimeTools;
import com.llm.tool_calling.weather.WeatherConfigProperties;
import com.llm.tool_calling.weather.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class ToolCallingController {

    private final ChatClient chatClient;

    private final CurrencyTools currencyTools;

    private WeatherConfigProperties weatherConfigProperties;
    private WebClient.Builder webClientBuilder;

    public ToolCallingController(ChatClient.Builder builder,
                                 WeatherConfigProperties weatherConfigProperties,
                                 WebClient.Builder webClientBuilder,
                                 CurrencyTools currencyTools) {

        this.weatherConfigProperties = weatherConfigProperties;
        this.webClientBuilder = webClientBuilder;
        this.currencyTools = currencyTools;
        ToolCallback toolCallback = FunctionToolCallback
                .builder("currentWeather", new WeatherService(this.weatherConfigProperties))
                .description("Get the weather in location")
                .inputType(WeatherService.Request.class)
                .build();
        this.chatClient = builder
                .defaultSystem("You are a helpful AI Assistant that can access tools if needed to answer user questions!.")
                .defaultTools(toolCallback)
                .build();
    }

    @PostMapping("/v1/tool_calling")
    public String toolCalling(@RequestBody UserInput userInput) {
        ToolCallback[] tools = ToolCallbacks.from(
                new DateTimeTools()
                , currencyTools
        );

        return chatClient.prompt()
                .user(userInput.prompt())
                .tools(tools)
                .call()
                .content();
    }
}
