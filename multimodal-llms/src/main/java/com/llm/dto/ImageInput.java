package com.llm.dto;


public record ImageInput(
        String prompt,
        String model,
        String quality,
        int height,
        int width,
        String style,
        String responseFormat
) {
}
