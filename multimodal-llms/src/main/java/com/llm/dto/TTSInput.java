package com.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;

public record TTSInput(String prompt,
                       @NotNull(message = "speed cannot be null")
                       Double speed,
                       @NotNull(message = "model cannot be null")
                       String model,
                       @NotNull(message = "responseFormat cannot be null")
                       OpenAiAudioSpeechOptions.AudioResponseFormat responseFormat,
                       @NotNull(message = "voice cannot be null")
                       OpenAiAudioSpeechOptions.Voice voice,
                       @NotBlank(message = "fileName cannot be null/blank")
                       String fileName
) {
}