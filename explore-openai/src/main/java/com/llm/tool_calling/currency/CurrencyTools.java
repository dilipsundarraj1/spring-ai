package com.llm.tool_calling.currency;

import com.llm.tool_calling.currency.dtos.CurrencyRequest;
import com.llm.tool_calling.currency.dtos.CurrencyResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrencyTools {
    private static final Logger log = LoggerFactory.getLogger(CurrencyTools.class);

    private final RestClient restClient;
    private final CurrencyExchangeConfigProperties currencyExchangeConfigProperties;
    private final Counter invocationCounter;
    private final Counter errorCounter;

    public CurrencyTools(RestClient restClient,
                         CurrencyExchangeConfigProperties currencyExchangeConfigProperties,
                         MeterRegistry meterRegistry) {
        this.restClient = RestClient.create(currencyExchangeConfigProperties.baseUrl());
        this.currencyExchangeConfigProperties = currencyExchangeConfigProperties;
        this.invocationCounter = Counter.builder("tool.invocations")
                .tag("tool", "currency")
                .description("Number of times the currency tool was invoked")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("tool.invocation.errors")
                .tag("tool", "currency")
                .description("Number of currency tool invocations that resulted in an error")
                .register(meterRegistry);
    }

//    @Tool(description = "Fetch the latest currency exchange rates")
@Tool(
        description = "Fetch the latest currency exchange rates. For multiple currency conversion use comma separated values for symbols",
returnDirect = true)
public CurrencyResponse getCurrencyRates(CurrencyRequest currencyRequest,
                                         ToolContext toolContext) {
        invocationCounter.increment();
        log.info("RestClient CurrencyTools is invoked - getCurrencyRates: {}", currencyRequest);

        if(toolContext!=null){
            var userId = toolContext.getContext().get("userId");
            log.info("userId: {}", userId);
        }

        try {
            var response = restClient
                    .get()
                    .uri("/latest.json?app_id={key}&base={base}&symbols={symbols}",
                            currencyExchangeConfigProperties.apiKey(),
                            currencyRequest.base(),
                            currencyRequest.symbols())
                    .retrieve()
                    .body(CurrencyResponse.class);

            log.info("response : {}", response);
            return response;

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error occurred while fetching the currency rates : ", e);
            throw e;
        }

    }

}