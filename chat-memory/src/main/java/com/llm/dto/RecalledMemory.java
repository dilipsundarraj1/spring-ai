package com.llm.dto;

import org.springframework.ai.document.Document;

/**
 * One message recalled from the vector store, with the similarity score that got it in.
 */
public record RecalledMemory(String type, String text, Double score) {

    public static RecalledMemory from(Document document, String messageTypeMetadataKey) {
        return new RecalledMemory(
                String.valueOf(document.getMetadata().getOrDefault(messageTypeMetadataKey, "UNKNOWN")),
                document.getText(),
                document.getScore());
    }
}
