package com.llm.tool_calling.currency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "currency-exchange")
public record CurrencyExchangeConfigProperties(String apiKey, String baseUrl) {
}