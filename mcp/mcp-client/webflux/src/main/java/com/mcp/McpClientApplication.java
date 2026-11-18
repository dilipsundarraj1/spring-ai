package com.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpClientApplication {

	private static final Logger log = LoggerFactory.getLogger(McpClientApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

}
