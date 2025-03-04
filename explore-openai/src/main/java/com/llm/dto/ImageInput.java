package com.llm.dto;


public record ImageInput(
        String model,
        String quality,
        String prompt,
        int height,
        int width,
        String style,
        String responseFormat
) {
}
