package com.mcp.service;

import com.mcp.config.WeatherConfigProperties;
import com.mcp.model.ForecastResponse;
import com.mcp.model.WeatherResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/*
   Weather API
   https://www.weatherapi.com/api-explorer.aspx
 */
@Service
public class WeatherService {

	private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

	private final RestClient restClient;
	private final WeatherConfigProperties weatherProps;

	public WeatherService(WeatherConfigProperties weatherProps) {
		this.weatherProps = weatherProps;
		log.debug("Weather API URL: {}", weatherProps.apiUrl());
		this.restClient = RestClient.create(weatherProps.apiUrl());
	}

	/**
	 * Get the current weather conditions for the given city
	 * @param city The name of a city or a country
	 * @return The current weather conditions for the given city
	 * @throws RestClientException if the request fails
	 */
	@McpTool(description = "Get the current weather conditions for the given city.")
	public WeatherResponse getWeatherForecastByLocation(
			@McpToolParam(description = "The name of a city or a country") String city) {
		log.info("Weather Request city: {}", city);

		try {
			var response = restClient.get()
				.uri("/current.json?key={key}&q={q}", weatherProps.apiKey(), city)
				.retrieve()
				.body(WeatherResponse.class);
			log.info("Weather API Response: {}", response);
			return response;

		}
		catch (Exception e) {
			log.error("Error occurred while fetching weather data: {} ", e.getMessage(), e);
			throw e;
		}
	}

	private static final int MIN_FORECAST_DAYS = 1;
	private static final int MAX_FORECAST_DAYS = 14;
	private static final int DEFAULT_FORECAST_DAYS = 3;

	/**
	 * Get the weather forecast for the given city
	 * @param city The name of a city or a country
	 * @param days Number of days of forecast (1-14)
	 * @return The daily forecast for the given city
	 * @throws IllegalArgumentException if days is outside the 1-14 range
	 * @throws RestClientException if the request fails
	 */
	@McpTool(description = "Get the weather forecast for the given city for the next few days.")
	public ForecastResponse getForecastWeatherByLocation(
			@McpToolParam(description = "The name of a city or a country") String city,
			@McpToolParam(description = "Number of days of forecast, between 1 and 14. Defaults to 3.",
					required = false) Integer days) {
		var forecastDays = days != null ? days : DEFAULT_FORECAST_DAYS;
		if (forecastDays < MIN_FORECAST_DAYS || forecastDays > MAX_FORECAST_DAYS) {
			throw new IllegalArgumentException(
					"days must be between %d and %d, but was: %d".formatted(MIN_FORECAST_DAYS, MAX_FORECAST_DAYS,
							forecastDays));
		}
		log.info("Forecast Request city: {}, days: {}", city, forecastDays);

		try {
			var response = restClient.get()
				.uri("/forecast.json?key={key}&q={q}&days={days}&aqi=no&alerts=no", weatherProps.apiKey(), city,
						forecastDays)
				.retrieve()
				.body(ForecastResponse.class);
			log.info("Forecast API Response: {}", response);
			return response;

		}
		catch (Exception e) {
			log.error("Error occurred while fetching forecast data: {} ", e.getMessage(), e);
			throw e;
		}
	}

}
