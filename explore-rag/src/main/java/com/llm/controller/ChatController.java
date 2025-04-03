package com.llm.controller;

import com.llm.dtos.GroundingRequest;
import com.llm.dtos.GroundingResponse;
import com.llm.service.GroundingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);


    private final GroundingService groundingService;


    public ChatController(
            GroundingService groundingService) {
        this.groundingService = groundingService;
    }

    @PostMapping("/api/v1/grounding/chat")
    public GroundingResponse getGrounding(@RequestBody GroundingRequest groundingRequest) {
        log.info("Grounding Request : {} ", groundingRequest);
        return groundingService.retrieveAnswer(groundingRequest);
    }

}
