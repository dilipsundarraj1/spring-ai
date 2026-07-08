package com.mcp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	private final ChatClient chatClient;

	/**
	 * The injected {@link ToolCallbackProvider} is auto-configured by
	 * spring-ai-starter-mcp-client-webflux from the spring.ai.mcp.client.* properties:
	 * <ol>
	 * <li>Each entry under spring.ai.mcp.client.streamable-http.connections (here:
	 * "weather-server" -> {@code http://localhost:8081/mcp}) becomes an McpAsyncClient
	 * bean (type: ASYNC) that performs the MCP initialize handshake on startup. The
	 * WebFlux starter uses a non-blocking WebClient-based streamable HTTP transport.</li>
	 * <li>Because spring.ai.mcp.client.toolcallback.enabled defaults to true, the
	 * starter wraps all McpAsyncClients in a single AsyncMcpToolCallbackProvider bean,
	 * which lists the tools each server exposes (tools/list) and adapts every MCP tool
	 * into a Spring AI ToolCallback.</li>
	 * <li>Registering that provider via defaultTools(...) hands the tool definitions to
	 * the LLM; when the model picks one, the callback forwards the call to the server
	 * (tools/call) over the same streamable HTTP connection.</li>
	 * </ol>
	 */
	public ChatController(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbackProvider) {
		this.chatClient = builder
			.defaultSystem("You are a helpful assistant. Use the available tools to answer questions "
					+ "about the current weather, weather forecasts, and currency conversions.")
			.defaultTools(mcpToolCallbackProvider)
			.build();
	}

	@GetMapping("/chat")
	public Mono<String> chat(@RequestParam String question) {
		log.info("Chat question: {}", question);

		return chatClient.prompt()
			.user(question)
			.stream()
			.content()
			.collectList()
			.map(chunks -> String.join("", chunks));
	}

	@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chatStream(@RequestParam String question) {
		log.info("Chat stream question: {}", question);

		return chatClient.prompt()
			.user(question)
			.stream()
			.content();
	}

}
