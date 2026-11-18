package com.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "currency-exchange")
public record CurrencyExchangeConfigProperties(String apiKey, String baseUrl) {

}
