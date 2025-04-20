package com.llm.tool_calling.currency;

import com.llm.tool_calling.currency.dtos.CurrencyRequest;
import com.llm.tool_calling.currency.dtos.CurrencyResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private final CurrencyTools currencyService;

    public CurrencyController(CurrencyTools currencyService) {
        this.currencyService = currencyService;
    }


    @GetMapping("/latest")
    public CurrencyResponse getCurrencyRates(@RequestParam String base, @RequestParam String symbols) {
        CurrencyRequest request = new CurrencyRequest(base, symbols);
        return null;
    }
}