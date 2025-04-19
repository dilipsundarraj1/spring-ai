package com.llm;

import com.llm.tool_calling.currency.CurrencyExchangeConfigProperties;
import com.llm.tool_calling.weather.WeatherConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
//@EnableConfigurationProperties({WeatherConfigProperties.class, CurrencyExchangeConfigProperties.class})
public class ExploreOpenaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExploreOpenaiApplication.class, args);
	}

}