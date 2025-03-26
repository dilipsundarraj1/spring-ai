package com.llm.tool_calling.weather.dtos;

public record Location(String name, String region, String country, Long lat, Long lon) {
    }
