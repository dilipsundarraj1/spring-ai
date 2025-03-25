package com.llm.tool_calling.currency;

import java.util.Map;

public record CurrencyResponse(
    String disclaimer,
    String license,
    long timestamp,
    String base,
    Map<String, Double> rates
) {}