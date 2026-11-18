package com.mcp;

import com.mcp.config.CurrencyExchangeConfigProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CurrencyExchangeConfigProperties.class)
public class McpServerApplication {

    static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

}
