package com.mcp;

import com.mcp.config.WeatherConfigProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WeatherConfigProperties.class)
public class McpServerApplication {

    static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

}