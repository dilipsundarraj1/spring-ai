package com.fakeservers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process stand-in for the real inventory MCP server. See
 * {@link FakeWeatherMcpServer} for the design notes.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
public class FakeInventoryMcpServer {

	public static final List<String> invocations = new CopyOnWriteArrayList<>();

	@Bean
	FakeInventoryTools fakeInventoryTools() {
		return new FakeInventoryTools();
	}

	public static class FakeInventoryTools {

		@McpTool(description = "Search inventory items of the electronics store by product name")
		public String searchInventoryItemsByProductName(
				@McpToolParam(description = "Full or partial product name to search for") String productName) {
			invocations.add("searchInventoryItemsByProductName:" + productName);
			return "STUB-INVENTORY " + productName + ": qty=42, IN_STOCK";
		}

		@McpTool(description = "List every inventory item of the electronics store")
		public String getAllInventoryItems() {
			invocations.add("getAllInventoryItems");
			return "STUB-INVENTORY-ALL: 3 items";
		}

	}

}
