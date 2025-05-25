package com.llm.structuredoutputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.dto.flight.FlightBooking;
import com.llm.dto.UserInput;
import com.llm.dto.soccer.SoccerTeam;
import com.llm.utils.CommonUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class StructuredOutputsController {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputsController.class);

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    public StructuredOutputsController(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Value("classpath:/prompt-templates/structured_outputs/flight_details.st")
    private Resource flightBooking;

    @Value("classpath:/prompt-templates/structured_outputs/flight_details_fewshot.st")
    private Resource flightBookingFewShot;



    @PostMapping("/v1/structured_outputs")
    public String structuredOutputs(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);
        var message = new UserMessage(userInput.prompt());
        var promptMessage = new Prompt(List.of(message));

        var requestSpec = chatClient.prompt(promptMessage);

        log.info("requestSpec : {} ", requestSpec);
        var responseSpec = requestSpec.call();
        return responseSpec.content();
    }
}