package com.llm.dto;

import org.springframework.ai.openai.OpenAiAudioSpeechOptions;

public record TTSInput(String prompt,
                       Double speed,

                       String model,

                       OpenAiAudioSpeechOptions.AudioResponseFormat responseFormat,
                       OpenAiAudioSpeechOptions.Voice voice,
                       String fileName
) {
}