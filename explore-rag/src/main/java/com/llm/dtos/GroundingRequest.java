package com.llm.dtos;

import jakarta.validation.constraints.NotBlank;

public record GroundingRequest(@NotBlank String prompt) {
}
