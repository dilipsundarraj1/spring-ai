package com.llm.tool_calling.weather;

import com.llm.tool_calling.weather.dtos.WeatherRequest;
import com.llm.tool_calling.weather.dtos.WeatherResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/*
   Weather API
   https://www.weatherapi.com/api-explorer.aspx
 */
public class WeatherToolsFunction implements Function<WeatherRequest, WeatherResponse> {

    private static final Logger log = LoggerFactory.getLogger(WeatherToolsFunction.class);
    private final RestClient restClient;
    private final WeatherConfigProperties weatherProps;
    private final Counter invocationCounter;
    private final Counter errorCounter;

    public WeatherToolsFunction(WeatherConfigProperties props, MeterRegistry meterRegistry) {
        this.weatherProps = props;
        log.debug("Weather API URL: {}", weatherProps.apiUrl());
        this.restClient = RestClient.create(weatherProps.apiUrl());
        this.invocationCounter = Counter.builder("tool.invocations")
                .tag("tool", "weather")
                .description("Number of times the weather tool was invoked")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("tool.invocation.errors")
                .tag("tool", "weather")
                .description("Number of weather tool invocations that resulted in an error")
                .register(meterRegistry);
    }

    @Override
    public WeatherResponse apply(WeatherRequest weatherRequest) {
        invocationCounter.increment();
        log.info("Weather Request: {}", weatherRequest);

        try{
            var response = restClient
                    .get()
                    .uri("/current.json?key={key}&q={q}", weatherProps.apiKey(), weatherRequest.city())
                    .retrieve()
                    .body(WeatherResponse.class);
            log.info("Weather API Response: {}", response);
            return response;

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error occurred while fetching weather data: {} ", e.getMessage(), e);
            throw e;
        }
    }
}
