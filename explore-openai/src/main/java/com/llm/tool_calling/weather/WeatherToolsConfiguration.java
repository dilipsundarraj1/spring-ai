package com.llm.tool_calling.weather;

import com.llm.tool_calling.weather.dtos.WeatherRequest;
import com.llm.tool_calling.weather.dtos.WeatherResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
public class WeatherToolsConfiguration {

    private final WeatherConfigProperties props;

    public WeatherToolsConfiguration(WeatherConfigProperties props) {
        this.props = props;
    }

    @Bean
    @Description("Get the current weather conditions for the given city.")
    public Function<WeatherRequest, WeatherResponse> currentWeatherFunction() {
        return new WeatherToolsFunction(props);
    }

}
