package com.llm.dto;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.openai.api.OpenAiAudioApi;

import javax.validation.constraints.NotNull;

public record TTSInput(String prompt,
                       @NotNull(message = "speed cannot be null")
                       Float speed,
                       @NotNull(message = "model cannot be null")
                       OpenAiAudioApi.TtsModel model,
                       @NotNull(message = "responseFormat cannot be null")
                       OpenAiAudioApi.SpeechRequest.AudioResponseFormat responseFormat,
                       @NotNull(message = "voice cannot be null")
                        OpenAiAudioApi.SpeechRequest.Voice voice,
                       @NotBlank(message = "fileName cannot be null/blank")
                       String fileName
) {
}
