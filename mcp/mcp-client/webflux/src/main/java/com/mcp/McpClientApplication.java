package com.mcp;

import java.util.List;

import io.modelcontextprotocol.client.McpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class McpClientApplication {

	private static final Logger log = LoggerFactory.getLogger(McpClientApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

	/**
	 * Logs the tools discovered from the connected MCP server(s) on startup.
	 */
	@Bean
	CommandLineRunner listMcpTools(List<McpAsyncClient> mcpAsyncClients) {
		return args -> Flux.fromIterable(mcpAsyncClients)
			.flatMap(client -> client.listTools()
				.doOnNext(result -> result.tools()
					.forEach(tool -> log.info("MCP server [{}] exposes tool: {} - {}",
							client.getServerInfo().name(), tool.name(), tool.description()))))
			.subscribe();
	}

}
