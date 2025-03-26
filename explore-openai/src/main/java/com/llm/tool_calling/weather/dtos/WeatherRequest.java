package com.llm.tool_calling.weather.dtos;

import org.springframework.ai.tool.annotation.ToolParam;

public record WeatherRequest(@ToolParam(description = "The name of a city or a country")String city) {
}
