package com.llm.dto;

import org.springframework.ai.openai.api.OpenAiAudioApi;

public record TTSInput(String prompt,
                       Float speed,

                       OpenAiAudioApi.TtsModel model,

                       OpenAiAudioApi.SpeechRequest.AudioResponseFormat responseFormat,
                        OpenAiAudioApi.SpeechRequest.Voice voice,
                       String fileName
) {
}
