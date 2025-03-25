package com.llm.tool_calling.currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrencyTools {
    private static final Logger log = LoggerFactory.getLogger(CurrencyTools.class);

    private final RestClient restClient;
    private final CurrencyExchangeConfigProperties currencyExchangeConfigProperties;


    public CurrencyTools(CurrencyExchangeConfigProperties  currencyExchangeConfigProperties) {
        this.currencyExchangeConfigProperties = currencyExchangeConfigProperties;
        this.restClient = RestClient.create(currencyExchangeConfigProperties.baseUrl());
    }

    @Tool(description = "Fetch the latest currency exchange rates")
    public CurrencyResponse getCurrencyRates(CurrencyRequest request) {
        log.info("RestClient CurrencyTools is invoked - getCurrencyRates: {}", request);

        try {
            var response = restClient.get()
                    .uri("/latest.json?app_id={key}&base={base}&symbols={symbols}", currencyExchangeConfigProperties.apiKey(), request.base(), request.symbols())
                    .retrieve()
                    .body(CurrencyResponse.class);
            log.info("response: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error occurred while fetching currency rates: ", e);
            throw e;
        }
    }

}