package com.llm.structuredoutputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.chats.ChatController;
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

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

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
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }



    @PostMapping("/v1/structured_outputs/fewshot")
    public Object structuredOutputsFewShot(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var promptTemplate = new PromptTemplate(flightBookingFewShot);
        var message = promptTemplate.createMessage(Map.of("input", userInput.prompt(), "jsonexample", CommonUtils.flightJson()));

        var promptMessage = new Prompt(List.of(message));

        var requestSpec = chatClient.prompt(promptMessage);

        log.info("requestSpec : {} ", requestSpec);
//        return requestSpec.call().content();

        return CommonUtils.fromJsonToType(requestSpec.call().content(), FlightBooking.class);

//        var responseSpec = requestSpec.call().entity(FlightBooking.class);
//        return responseSpec;
    }

    @PostMapping("/v1/structured_outputs/entity")
    public FlightBooking entity(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);


        var promptTemplate = new PromptTemplate(flightBooking);
        var message = promptTemplate.createMessage(Map.of("input", userInput.prompt()));

        var promptMessage = new Prompt(List.of(message));

        var requestSpec = chatClient.prompt(promptMessage);


        var booking = requestSpec.call().entity(FlightBooking.class);

        log.info("booking : {} ", booking);
        return booking;
    }

    @PostMapping("/v1/structured_outputs/entity/list")
    public List<SoccerTeam> entityList(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);
        var message = new UserMessage(userInput.prompt());
        var promptMessage = new Prompt(List.of(message));

        var soccerTeams = chatClient
                .prompt(promptMessage)
                .call()
                .entity(new ParameterizedTypeReference<List<SoccerTeam>>() {});

        log.info("soccerTeams : {} ", soccerTeams);
        return soccerTeams;
    }

    @PostMapping("/v1/structured_outputs/entity/map")
    public Object entityMap(@RequestBody @Valid UserInput userInput) {

        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        String format = mapOutputConverter.getFormat();

        String template = """
        Input :  {input}
        {format}
        """;
        var promptTemplate = new PromptTemplate(template);
        var message = promptTemplate.createMessage(Map.of("input", userInput.prompt(), "format", format));

        var promptMessage = new Prompt(List.of(message));

        var soccerTeamByMap = chatClient
                .prompt(promptMessage).
                call().content();

        log.info("soccerTeamByMap : {} ", soccerTeamByMap);
//        return soccerTeamByMap;

        Map<String, Object> result = mapOutputConverter.convert(soccerTeamByMap);
        return result;

    }
}