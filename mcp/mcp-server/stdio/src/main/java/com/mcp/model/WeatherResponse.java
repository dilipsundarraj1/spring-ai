package com.mcp.model;

public record WeatherResponse(Location location, Current current) {

	public record Location(String name, String region, String country, Long lat, Long lon) {
	}

	public record Current(String temp_f, Condition condition, String wind_mph, String humidity) {
	}

	public record Condition(String text) {
	}
}
