package com.llm.dto;

import jakarta.validation.constraints.NotBlank;

public record UserInput(@NotBlank(message = "prompt input is required") String prompt) {
}
