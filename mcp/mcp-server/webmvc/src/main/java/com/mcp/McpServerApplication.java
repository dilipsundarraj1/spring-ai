package com.mcp;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class McpServerApplication {

    static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

}
