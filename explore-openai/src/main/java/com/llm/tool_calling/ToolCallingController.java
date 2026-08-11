package com.llm.tool_calling;

import com.llm.dto.UserInput;
import com.llm.tool_calling.currency.CurrencyTools;
import com.llm.tool_calling.currenttime.DateTimeTools;
import com.llm.tool_calling.weather.WeatherConfigProperties;
import com.llm.tool_calling.weather.WeatherToolsFunction;
import com.llm.tool_calling.weather.WeatherToolsFunctionV2;
import com.llm.tool_calling.weather.dtos.WeatherRequest;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ToolCallingController {
    private static final Logger log = LoggerFactory.getLogger(ToolCallingController.class);

    private final ChatClient chatClient;
    private final CurrencyTools currencyTools;
    private final OpenAiChatModel openAiChatModel;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    public ToolCallingController(ChatClient.Builder builder,
                                 WeatherConfigProperties weatherConfigProperties,
                                 OpenAiChatModel openAiChatModel,
                                 CurrencyTools currencyTools,
                                 MeterRegistry meterRegistry,
                                 ObservationRegistry observationRegistry) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;

        ToolCallback toolCallback = FunctionToolCallback
                //.builder("currentWeather", new WeatherToolsFunction(weatherConfigProperties, meterRegistry))
                .builder("currentWeather", new WeatherToolsFunctionV2(weatherConfigProperties, observationRegistry))
                .description("Get the weather in location")
                .inputType(WeatherRequest.class)
                .build();


        this.chatClient = builder
                .defaultSystem("You are a helpful AI Assistant that can access tools if needed to answer user questions!.")
                .defaultTools(toolCallback)
//                .defaultTools("currentWeatherFunction")
                .build();
        this.openAiChatModel = openAiChatModel;
        this.currencyTools = currencyTools;
    }

    @PostMapping("/v1/tool_calling")
    public String toolCalling(@RequestBody UserInput userInput,
                              @RequestHeader(value = "USER_ID", required = false) String userId) {

        var tools = ToolCallbacks.from(
                new DateTimeTools(meterRegistry),
                currencyTools
        );

        var requestSpec = chatClient.prompt()
                .user(userInput.prompt())
                .advisors(new SimpleLoggerAdvisor())
                .tools(tools);

        if (userId != null) {
            requestSpec = requestSpec.toolContext(Map.of("userId", userId));
        }

        log.info("requestSpec : {} ", requestSpec);

        var responseSpec = requestSpec.call()
                .content();
        return   responseSpec;
    }

    @PostMapping("/v2/tool_calling/custom")
    public ChatResponse toolCallingCustom(@RequestBody UserInput userInput) {

//        ToolCallback[] tools = ToolCallbacks.from(new DateTimeTools());
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        // In Spring AI 2.0, calling the ChatModel directly never executes tools
        // internally (that moved to ToolCallingAdvisor on ChatClient), so the
        // old internalToolExecutionEnabled(false) flag is no longer needed.
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
//                .toolCallbacks(tools)
                .build();
        Prompt prompt = new Prompt(userInput.prompt(), chatOptions);

        ChatResponse chatResponse = openAiChatModel.call(prompt);
        log.info(" chatResponse : {} ", chatResponse);
        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

            chatResponse = openAiChatModel.call(prompt);
        }

        return chatResponse;
    }
}
