package com.llm.tool_calling;

import com.llm.dto.UserInput;
import com.llm.tool_calling.currency.CurrencyTools;
import com.llm.tool_calling.currenttime.DateTimeTools;
import com.llm.tool_calling.weather.WeatherConfigProperties;
import com.llm.tool_calling.weather.WeatherToolsFunction;
import com.llm.tool_calling.weather.dtos.WeatherRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

@RestController
public class ToolCallingController {
    private static final Logger log = LoggerFactory.getLogger(ToolCallingController.class);

    private final ChatClient chatClient;

    private final OpenAiChatModel openAiChatModel;

    public ToolCallingController(ChatClient.Builder builder,
                                 OpenAiChatModel openAiChatModel) {

        this.chatClient = builder
                .defaultSystem("You are a helpful AI Assistant that can access tools if needed to answer user questions!.")
                .build();
        this.openAiChatModel = openAiChatModel;
    }

    @PostMapping("/v1/tool_calling")
    public String toolCalling(@RequestBody UserInput userInput) {


        return chatClient.prompt()
                .user(userInput.prompt())
                .call()
                .content();
    }

    @PostMapping("/v2/tool_calling/custom")
    public ChatResponse toolCallingCustom(@RequestBody UserInput userInput) {

        ToolCallback[] tools = ToolCallbacks.from(new DateTimeTools());
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
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
