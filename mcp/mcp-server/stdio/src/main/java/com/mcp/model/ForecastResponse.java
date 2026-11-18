package com.mcp.model;

import java.util.List;

public record ForecastResponse(Location location, Current current, Forecast forecast) {

	public record Location(String name, String region, String country, Double lat, Double lon, String tz_id,
			String localtime) {
	}

	public record Current(Double temp_c, Double temp_f, Condition condition) {
	}

	public record Condition(String text) {
	}

	public record Forecast(List<ForecastDay> forecastday) {
	}

	public record ForecastDay(String date, Day day) {
	}

	public record Day(Double maxtemp_c, Double maxtemp_f, Double mintemp_c, Double mintemp_f, Double avgtemp_c,
			Double avgtemp_f, Condition condition) {
	}
}
