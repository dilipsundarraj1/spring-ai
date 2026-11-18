package com.llm;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fake LLM that records every prompt it receives, so tests can assert which
 * messages (history included) were actually sent to the model.
 */
public class RecordingChatModel implements ChatModel {

    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage("reply-" + prompts.size()))));
    }

    /**
     * Tool-calling options, like a real chat model's - otherwise ChatClient drops the tools before the call.
     */
    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    /**
     * Messages of the most recent prompt, rendered as "TYPE: text".
     */
    public List<String> lastPromptMessages() {
        return prompts.getLast()
                .getInstructions()
                .stream()
                .map(message -> message.getMessageType() + ": " + message.getText())
                .toList();
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }

    public void reset() {
        prompts.clear();
    }
}
