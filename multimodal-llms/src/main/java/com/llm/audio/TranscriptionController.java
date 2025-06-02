package com.llm.audio;

import com.llm.dto.TranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranscriptionController {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    public TranscriptionController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    }

    @PostMapping("/v1/transcription")
    public ResponseEntity<?> transcription(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Transcription Initiated for file name : {} ", file.getOriginalFilename());
        validateInput(file);
        var audioFile = file.getResource();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile);
        var response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        return new ResponseEntity<>(new TranscriptionResponse(response.getResult().getOutput(), file.getOriginalFilename()), HttpStatus.OK);
    }

    private static ResponseEntity<String> validateInput(MultipartFile file) {
        if(file ==null){
           throw new IllegalArgumentException("Input is missing!, Please pass the required file!");
        }
        return null;
    }

    @PostMapping("/v2/transcription")
    public TranscriptionResponse transcriptionV2(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt,
            @RequestParam("model") String model,
            @RequestParam("language") String language,
            @RequestParam("response_format") String responseFormat,
            @RequestParam(value = "temperature",required = false) Float temperature

    ) {
        log.info("prompt  : {}, model : {}, responseFormat : {}, temperature : {} ", prompt, model, responseFormat, temperature);

        var responseFormatEnum = OpenAiAudioApi.TranscriptResponseFormat.valueOf(responseFormat);
        temperature = (temperature==null || temperature == 0.0f) ? 1.0f : temperature;

        log.info("responseFormatEnum  : {}, temperature : {} ",  responseFormatEnum, temperature);


        var audioFile = file.getResource();
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language(language)
                .prompt(prompt)
                .temperature(temperature)
                .responseFormat(responseFormatEnum)
                .build();

        log.info("transcriptionOptions : {} ", transcriptionOptions);


        AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        var response = openAiAudioTranscriptionModel.call(transcriptionPrompt);
        log.info("AudioTranscription Result : {} ", response.getResult());
        return new TranscriptionResponse(response.getResult().getOutput(), file.getOriginalFilename());
    }
}
