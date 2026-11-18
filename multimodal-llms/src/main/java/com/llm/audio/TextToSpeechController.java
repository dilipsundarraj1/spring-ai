package com.llm.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TextToSpeechController {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechController.class);
    public static String OUTPUT_PATH = "multimodal-llms/src/main/resources/files/audio";

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    public TextToSpeechController(OpenAiAudioSpeechModel openAiAudioSpeechModel) {
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }

}
