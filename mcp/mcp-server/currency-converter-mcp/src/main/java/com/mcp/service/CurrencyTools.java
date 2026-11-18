package com.mcp.service;

import com.mcp.config.CurrencyExchangeConfigProperties;
import com.mcp.exception.CurrencyApiException;
import com.mcp.model.CurrencyResponse;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/*
   Open Exchange Rates API
   https://docs.openexchangerates.org/reference/latest-json

   Reactive (ASYNC) MCP port of explore-openai's tool_calling CurrencyTools: same
   getCurrencyRates tool, but exposed over MCP with @McpTool and the blocking
   RestClient replaced by a non-blocking WebClient returning a Mono.
 */
@Service
public class CurrencyTools {

	private static final Logger log = LoggerFactory.getLogger(CurrencyTools.class);

	private static final String DEFAULT_BASE_CURRENCY = "USD";

	private final WebClient webClient;
	private final CurrencyExchangeConfigProperties currencyExchangeProps;

	public CurrencyTools(CurrencyExchangeConfigProperties currencyExchangeProps) {
		this.currencyExchangeProps = currencyExchangeProps;
		log.debug("Currency Exchange API URL: {}", currencyExchangeProps.baseUrl());
		this.webClient = WebClient.create(currencyExchangeProps.baseUrl());
	}

	/**
	 * Fetch the latest currency exchange rates for the given base currency
	 * @param base The base currency code (defaults to USD)
	 * @param symbols Comma separated target currency codes; all currencies when omitted
	 * @return The latest exchange rates for the given base currency
	 * @throws CurrencyApiException if the openexchangerates.org call fails
	 */
	@McpTool(description = "Fetch the latest currency exchange rates. "
			+ "For multiple currency conversions use comma separated values for symbols.")
	public Mono<CurrencyResponse> getCurrencyRates(
			@McpToolParam(description = "The base currency code, e.g. USD. Defaults to USD.",
					required = false) String base,
			@McpToolParam(description = "Comma separated target currency codes, e.g. 'EUR,GBP,INR'. "
					+ "Omit to get rates for all currencies.", required = false) String symbols) {
		var baseCurrency = base != null && !base.isBlank() ? base : DEFAULT_BASE_CURRENCY;
		log.info("Currency Rates Request base: {}, symbols: {}", baseCurrency, symbols);

		return this.webClient.get()
			.uri(uriBuilder -> {
				uriBuilder.path("/latest.json")
					.queryParam("app_id", currencyExchangeProps.apiKey())
					.queryParam("base", baseCurrency);
				if (symbols != null && !symbols.isBlank()) {
					uriBuilder.queryParam("symbols", symbols);
				}
				return uriBuilder.build();
			})
			.retrieve()
			.bodyToMono(CurrencyResponse.class)
			.doOnNext(response -> log.info("Currency API Response: {}", response))
			.doOnError(e -> log.error("Error occurred while fetching the currency rates: {} ", e.getMessage(), e))
			.onErrorMap(e -> new CurrencyApiException(
					"Failed to fetch currency rates for base '%s': %s".formatted(baseCurrency, e.getMessage()), e));
	}

}
