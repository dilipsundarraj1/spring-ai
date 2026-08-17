package com.llm.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranscriptionController {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    public TranscriptionController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    }

}
