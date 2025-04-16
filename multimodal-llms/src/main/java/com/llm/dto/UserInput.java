package com.llm.dto;

import jakarta.validation.constraints.NotBlank;

public record UserInput(@NotBlank String prompt,
                        String context,
                        ChatOptions chatOptions
                        ) {
}


