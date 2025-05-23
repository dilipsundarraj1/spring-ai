package com.llm.image;

import com.llm.dto.ImageInput;
import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.llm.utils.ImageUtil.decodeBase64ToImage;
import static com.llm.utils.ImageUtil.saveImageToFile;

/**
 * https://platform.openai.com/docs/api-reference/images/create
 */
@RestController
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    public OpenAiImageModel openAiImageModel;

    @Value("${spring.ai.openai.image.options.responseFormat}")
    private String responseFormat;

    public ImageController(OpenAiImageModel openAiImageModel) {
        this.openAiImageModel = openAiImageModel;
    }

    @PostMapping("/v1/images")
    public ImageResponse images(@RequestBody UserInput userInput) {
        log.info("userInput message prompt is : {} ", userInput);
        var response = openAiImageModel.call(
                new ImagePrompt(userInput.prompt())

        );
        log.info("response : {} ", response.getResult().getOutput());

        //log.info("response : {} ", response.getResult().getOutput());
        if (responseFormat.equals("b64_json")) {
            var image = decodeBase64ToImage(response.getResult().getOutput().getB64Json());
            String filePath = "output_image.png"; // specify the desired file path and format
            boolean success = saveImageToFile(image, "png", filePath);
            if (success) {
                log.info("Image successfully parsed and saved as '" + filePath + "'!");
            } else {
                log.info("Failed to save the image.");
            }
        }
        return response;
    }

    @PostMapping("/v2/images")
    public ImageResponse chat(@RequestBody @Valid ImageInput imageInput) {
        log.info("imageInput message prompt is : {} ", imageInput);
        var response = openAiImageModel.call(
                new ImagePrompt(imageInput.prompt(),
                        OpenAiImageOptions.builder()
                                // Model : dall-e-3 or dall-e-2
                                .model(imageInput.model())
                                //hd or standard
                                .quality(imageInput.quality())
                                // The number of images to generate.
                                // Must be between 1 and 10. For dall-e-3, only n=1 is supported.
                                // Must be one of 256x256, 512x512, or 1024x1024 for dall-e-2
                                // Must be one of 1024x1024, 1792x1024, or 1024x1792 for dall-e-3 models.
                                .height(imageInput.height())
                                .width(imageInput.width())
                                //This property only works for dall-e-3 model.
                                .style(imageInput.style())
                                .responseFormat(imageInput.responseFormat())
                                .build())

        );
        //log.info("response : {} ", response.getResult().getOutput());
        if (responseFormat.equals("b64_json")) {
            log.info("response : {} ", response.getResult().getOutput());
            var image = decodeBase64ToImage(response.getResult().getOutput().getB64Json());
            String filePath = "output_image.png"; // specify the desired file path and format
            boolean success = saveImageToFile(image, "png", filePath);
            if (success) {
                log.info("Image successfully parsed and saved as '" + filePath + "'!");
            } else {
                log.info("Failed to save the image.");
            }
        }
        return response;
    }
}
