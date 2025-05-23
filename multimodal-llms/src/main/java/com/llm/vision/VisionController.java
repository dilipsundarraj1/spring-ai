package com.llm.vision;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class VisionController {
    private static final Logger log = LoggerFactory.getLogger(VisionController.class);

    private static final String UPLOAD_DIR = "explore-openai/src/main/resources/uploaded_images";

    private final ChatClient chatClient;


    public VisionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/v1/vision")
    public String vision(@RequestBody UserInput userInput) {
        log.info("userInput message : {} ", userInput);
        var imageResource = new ClassPathResource("files/vision/zebra.jpg");
        var userMessage = new UserMessage(
                userInput.prompt()); // media


        var response = chatClient.prompt(new Prompt(userMessage)).call();
        log.info("response : {} ", response);
        return response.content();
    }

    @PostMapping(value = "/v2/vision", consumes = "multipart/form-data")
    public ResponseEntity<String> visionV2(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt
    ) {

        try {
            // Get the original filename
            String fileName = file.getOriginalFilename();
            log.info("Uploaded File name is :  {} " , fileName);

            // Create UserMessage
            var userMessage = new UserMessage(
                    //"Explain what do you see in this picture?", // content
                    prompt); // media
            var response = chatClient.prompt(new Prompt(userMessage)).call();
            log.info("response : {} ", response.chatResponse());
            return ResponseEntity.ok( response.content());
        } catch (Exception e) {
            log.error("Exception is : {} ", e.getMessage(), e);
            return ResponseEntity.status(500).body("File upload failed");
        }
    }
}