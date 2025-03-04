package com.llm;

import com.llm.function_calling.weather.WeatherConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WeatherConfigProperties.class)
public class ExploreOpenaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExploreOpenaiApplication.class, args);
	}

}