package com.llm.dto;

import org.springframework.ai.chat.messages.Message;

/**
 * Simplified view of a message stored in chat memory - used by the history endpoints.
 */
public record ChatMessage(String type, String text) {

    public static ChatMessage from(Message message) {
        return new ChatMessage(message.getMessageType().name(), message.getText());
    }
}
