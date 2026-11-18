package com.fakeservers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process stand-in for the real weather MCP server: same tool names, canned
 * responses, and a record of invocations so tests can assert the client routed a
 * tools/call here. Transport-agnostic — boot it with either the webmvc or the webflux
 * MCP server starter on the classpath (as a SYNC server, so the plain return values
 * work on both stacks). Lives outside the com.mcp package so the client applications
 * under test do not component-scan it.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class FakeWeatherMcpServer {

	public static final List<String> invocations = new CopyOnWriteArrayList<>();

	@Bean
	FakeWeatherTools fakeWeatherTools() {
		return new FakeWeatherTools();
	}

	public static class FakeWeatherTools {

		@McpTool(description = "Get the current weather for a city")
		public String getWeatherForecastByLocation(
				@McpToolParam(description = "The city name") String city) {
			invocations.add("getWeatherForecastByLocation:" + city);
			return "STUB-WEATHER " + city + ": 21C, Overcast";
		}

		@McpTool(description = "Get the weather forecast for a city")
		public String getForecastWeatherByLocation(
				@McpToolParam(description = "The city name") String city,
				@McpToolParam(description = "Number of forecast days") int days) {
			invocations.add("getForecastWeatherByLocation:" + city + ":" + days);
			return "STUB-FORECAST " + city + " for " + days + " days: Sunny";
		}

	}

}
