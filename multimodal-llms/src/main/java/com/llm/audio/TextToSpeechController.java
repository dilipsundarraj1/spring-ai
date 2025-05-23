package com.llm.audio;

import com.llm.dto.TTSInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.llm.utils.AudioUtil.writeMP3ToFile;

@RestController
public class TextToSpeechController {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechController.class);
    public static String OUTPUT_PATH = "multimodal-llms/src/main/resources/files/audio";

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    public TextToSpeechController(OpenAiAudioSpeechModel openAiAudioSpeechModel) {
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }

    @PostMapping("/v1/tts")
    public ResponseEntity<String> images(@RequestBody TTSInput ttsInput) {
        log.info("userInput message prompt is : {} ", ttsInput);

        var speechPrompt = new SpeechPrompt(ttsInput.prompt());
        var response = openAiAudioSpeechModel.call(speechPrompt);
        log.info("response : {} ", response);
        byte[] responseAsBytes = response.getResult().getOutput();
        writeMP3ToFile(responseAsBytes, OUTPUT_PATH + "/speech.mp3");
        return ResponseEntity.ok("Audio Generated Successfully");
    }

    @PostMapping("/v2/tts")
    public ResponseEntity<String> imagesV2(@RequestBody @Valid TTSInput ttsInput) {
        log.info("userInput message prompt is : {} ", ttsInput);

        var speechOptions = OpenAiAudioSpeechOptions.builder()
                .speed(ttsInput.speed())
                .model(ttsInput.model().value)
                .responseFormat(ttsInput.responseFormat())
                .voice(ttsInput.voice())
                .build();

        log.info("speechOptions : {} ", speechOptions);

        var speechPrompt = new SpeechPrompt(ttsInput.prompt(), speechOptions);
        var response = openAiAudioSpeechModel.call(speechPrompt);
        log.info("response : {} ", response);
        byte[] responseAsBytes = response.getResult().getOutput();

        var outputFilePath = String.format("%s/%s.%s", OUTPUT_PATH, ttsInput.fileName(), ttsInput.responseFormat());
        log.info("outputFilePath : {} ", outputFilePath);


        writeMP3ToFile(responseAsBytes, OUTPUT_PATH + "/"+ttsInput.fileName()+"."+ttsInput.responseFormat().value);

        return ResponseEntity.ok("Audio Generated Successfully");
    }

}
