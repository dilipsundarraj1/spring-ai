package com.llm.tool_calling.weather;

import com.llm.tool_calling.weather.dtos.WeatherRequest;
import com.llm.tool_calling.weather.dtos.WeatherResponse;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

public class WeatherToolsFunctionV2 implements Function<WeatherRequest, WeatherResponse> {

    private static final Logger log = LoggerFactory.getLogger(WeatherToolsFunctionV2.class);
    private final RestClient restClient;
    private final WeatherConfigProperties weatherProps;
    private final ObservationRegistry observationRegistry;

    public WeatherToolsFunctionV2(WeatherConfigProperties props,
                                   ObservationRegistry observationRegistry) {
        this.weatherProps = props;
        log.debug("Weather API URL: {}", weatherProps.apiUrl());
        this.restClient = RestClient.create(weatherProps.apiUrl());
        this.observationRegistry = observationRegistry;
    }

    @Override
    public WeatherResponse apply(WeatherRequest weatherRequest) {
        Observation observation = Observation
                .createNotStarted("tool.execution", observationRegistry)
                .lowCardinalityKeyValue("tool", "weather")
                .lowCardinalityKeyValue("city", weatherRequest.city())
                .start();

        log.info("Weather Request: {}", weatherRequest);

        try (Observation.Scope scope = observation.openScope()) {
            var response = restClient
                    .get()
                    .uri("/current.json?key={key}&q={q}",
                            weatherProps.apiKey(), weatherRequest.city())
                    .retrieve()
                    .body(WeatherResponse.class);
            log.info("Weather API Response: {}", response);
            return response;
        } catch (Exception e) {
            observation.error(e);
            log.error("Error occurred while fetching weather data: {}", e.getMessage(), e);
            throw e;
        } finally {
            observation.stop();
        }
    }
}
