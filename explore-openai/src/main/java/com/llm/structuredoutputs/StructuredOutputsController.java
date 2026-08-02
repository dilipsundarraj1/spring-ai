package com.llm.structuredoutputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.dto.flight.FlightBooking;
import com.llm.dto.UserInput;
import com.llm.utils.CommonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class StructuredOutputsController {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputsController.class);

    private final ObservationRegistry observationRegistry;

    private final ChatClient chatClient;

    public StructuredOutputsController(ObservationRegistry observationRegistry, ChatClient.Builder chatClientBuilder) {
        this.observationRegistry = observationRegistry;
        this.chatClient = chatClientBuilder.build();
    }

    @Value("classpath:/prompt-templates/structured_outputs/flight_details.st")
    private Resource flightBooking;

    @Value("classpath:/prompt-templates/structured_outputs/flight_details_fewshot.st")
    private Resource flightBookingFewShot;



    @PostMapping("/v1/structured_outputs")
    public String structuredOutputs(@RequestBody @Valid UserInput userInput) {

        Observation.createNotStarted("structured_outputs", observationRegistry)
                .observe(() -> {
                    log.info("userInput message : {} ", userInput);
                    var message = new UserMessage(userInput.prompt());
                    var promptMessage = new Prompt(List.of(message));

                    var requestSpec = chatClient.prompt(promptMessage);

                    log.info("requestSpec : {} ", requestSpec);
                    var responseSpec = requestSpec.call();
                    log.info("responseSpec : {} ", responseSpec.chatResponse());
                    return responseSpec.content();
                });

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
    @Observed(name = "fewshot.count", contextualName = "Structured Outputs Few Shot")
    public String structuredOutputsFewShot(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var promptTemplate = new PromptTemplate(flightBookingFewShot);
        var message = promptTemplate.createMessage(Map.of("input", userInput.prompt(),
                "jsonexample" , CommonUtils.flightJson()));

        var promptMessage = new Prompt(List.of(message));

        var requestSpec = chatClient.prompt(promptMessage);

        log.info("requestSpec : {} ", requestSpec);
        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

    @PostMapping("/v1/structured_outputs/entity")
    public FlightBooking entity(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var promptTemplate = new PromptTemplate(flightBooking);
        var message = promptTemplate.
                createMessage(Map.of("input", userInput.prompt()));

        var promptMessage = new Prompt(List.of(message));

        var booking = chatClient
                .prompt(promptMessage)
                .call()
                .entity(FlightBooking.class);

        log.info("booking : {} ", booking);
        return booking;
    }

    @PostMapping("/v1/structured_outputs/entity/list")
    public List<String> entityList(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var message = new UserMessage(userInput.prompt());
        var promptMessage = new Prompt(List.of(message));

        var soccerTeams = chatClient
                .prompt(promptMessage)
                .call()
                //.entity(new ParameterizedTypeReference<List<SoccerTeam>>() {});
                .entity(new ListOutputConverter(new DefaultConversionService()));


        log.info("soccerTeams : {} ", soccerTeams);
        return soccerTeams;
    }

    @PostMapping("/v1/structured_outputs/entity/map")
    public Map<String, Object> entityMap(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        String format = mapOutputConverter.getFormat();
        String template = """
        Inpuut : {input}
        {format}
        """;
        var promptTemplate = new PromptTemplate(template);
        var message = promptTemplate.createMessage(
                Map.of("input", userInput.prompt(), "format", format));

        var promptMessage = new Prompt(List.of(message));

        log.info("promptMessage : {} ", promptMessage);


        var soccerTeamsByMap = chatClient
                .prompt(promptMessage)
                .call()
                        .content();


        log.info("soccerTeamsByMap : {} ", soccerTeamsByMap);

        var result = mapOutputConverter.convert(soccerTeamsByMap);
        return result;
    }
}