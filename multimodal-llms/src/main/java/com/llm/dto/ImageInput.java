package com.llm.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public record ImageInput(
        @NotEmpty(message = "prompt cannot be empty")
        String prompt,
        @NotEmpty(message = "Model cannot be empty")
        String model,
        @NotEmpty(message = "quality cannot be empty")
        String quality,
        @Min(value = 1, message = "Height must be greater than 0")
        int height,
        @Min(value = 1, message = "Height must be greater than 0")
        int width,
        @NotEmpty(message = "style cannot be empty, vivid or natural are the allowed values")
        String style,
        @NotEmpty(message = "responseFormat cannot be empty, url or b64_json")
        String responseFormat
) {
}
