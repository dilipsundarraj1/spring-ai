package com.llm.tool_calling.weather;

import com.llm.tool_calling.weather.dtos.WeatherRequest;
import com.llm.tool_calling.weather.dtos.WeatherResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
public class WeatherToolsConfiguration {

    private final WeatherConfigProperties weatherProps;
    private final MeterRegistry meterRegistry;

    public WeatherToolsConfiguration(WeatherConfigProperties weatherProps, MeterRegistry meterRegistry) {
        this.weatherProps = weatherProps;
        this.meterRegistry = meterRegistry;
    }

    @Bean
    @Description("Get the current weather conditions for the given city.")
    public Function<WeatherRequest, WeatherResponse> currentWeatherFunction() {
        return new WeatherToolsFunction(weatherProps, meterRegistry);
    }
}
