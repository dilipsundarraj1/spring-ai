package com.mcp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	private final ChatClient chatClient;

	/**
	 * The injected {@link ToolCallbackProvider} is auto-configured by
	 * spring-ai-starter-mcp-client from the spring.ai.mcp.client.* properties:
	 * <ol>
	 * <li>Each entry under spring.ai.mcp.client.streamable-http.connections (here:
	 * "weather-server" -> {@code http://localhost:8080/mcp}) becomes an McpSyncClient bean
	 * (type: SYNC) that performs the MCP initialize handshake on startup.</li>
	 * <li>Because spring.ai.mcp.client.toolcallback.enabled defaults to true, the
	 * starter wraps all McpSyncClients in a single SyncMcpToolCallbackProvider bean,
	 * which lists the tools each server exposes (tools/list) and adapts every MCP tool
	 * into a Spring AI ToolCallback.</li>
	 * <li>Registering that provider via defaultTools(...) hands the tool definitions to
	 * the LLM; when the model picks one, the callback forwards the call to the server
	 * (tools/call) over the same streamable HTTP connection.</li>
	 * </ol>
	 */
	public ChatController(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbackProvider) {
		this.chatClient = builder
			.defaultSystem("You are a helpful weather assistant. Use the available tools to answer questions "
					+ "about the current weather and weather forecasts.")
			.defaultTools(mcpToolCallbackProvider)
			.build();
	}

	@GetMapping("/chat")
	public String chat(@RequestParam String question) {
		log.info("Chat question: {}", question);

		return chatClient.prompt()
			.user(question)
			.call()
			.content();
	}

}
