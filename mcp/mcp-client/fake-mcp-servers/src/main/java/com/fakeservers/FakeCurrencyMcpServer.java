package com.fakeservers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process stand-in for the real currency converter MCP server. See
 * {@link FakeWeatherMcpServer} for the design notes.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class FakeCurrencyMcpServer {

	public static final List<String> invocations = new CopyOnWriteArrayList<>();

	@Bean
	FakeCurrencyTools fakeCurrencyTools() {
		return new FakeCurrencyTools();
	}

	public static class FakeCurrencyTools {

		@McpTool(description = "Fetch the latest currency exchange rates")
		public String getCurrencyRates(
				@McpToolParam(description = "The base currency code", required = false) String base,
				@McpToolParam(description = "Comma separated target currency codes",
						required = false) String symbols) {
			var baseCurrency = base != null ? base : "USD";
			invocations.add("getCurrencyRates:" + baseCurrency);
			return "STUB-RATES " + baseCurrency + ": EUR=0.9146, GBP=0.7854";
		}

	}

}
