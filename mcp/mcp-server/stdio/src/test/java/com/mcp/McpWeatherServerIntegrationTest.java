package com.mcp;

import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Boots the packaged jar as a child process (like a real MCP client would) and talks to
 * it over the STDIO transport. Requires the boot jar to be built first — the Gradle test
 * task depends on bootJar to guarantee that.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpWeatherServerIntegrationTest {

	private static final String SERVER_JAR = System.getProperty("mcp.server.jar", "build/libs/stdio-0.0.2-SNAPSHOT.jar");

	private McpSyncClient client;

	@BeforeAll
	void connectToServer() {
		var apiKey = System.getenv().getOrDefault("WEATHER_API_KEY", "dummy-key-for-tests");

		var serverParams = ServerParameters.builder("java")
			.args("-jar", SERVER_JAR)
			.addEnvVar("WEATHER_API_KEY", apiKey)
			.build();

		var transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper(JsonMapper.builder().build()));

		client = McpClient.sync(transport)
			.requestTimeout(Duration.ofSeconds(30))
			.initializationTimeout(Duration.ofSeconds(60))
			.build();

		var initResult = client.initialize();
		assertThat(initResult.serverInfo().name()).isEqualTo("my-weather-server");
	}

	@AfterAll
	void disconnect() {
		if (client != null) {
			client.closeGracefully();
		}
	}

	@Test
	void exposesTheWeatherTools() {
		var tools = client.listTools().tools();

		assertThat(tools).extracting(McpSchema.Tool::name)
			.containsExactlyInAnyOrder("getWeatherForecastByLocation", "getForecastWeatherByLocation");
	}

	@Test
	void rejectsForecastDaysAboveTheMaximum() {
		var result = client
			.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
				.arguments(Map.of("city", "London", "days", 20))
				.build());

		assertThat(result.isError()).isTrue();
		assertThat(result.content().toString()).contains("days must be between 1 and 14");
	}

	@Test
	void rejectsForecastDaysBelowTheMinimum() {
		var result = client
			.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
				.arguments(Map.of("city", "London", "days", 0))
				.build());

		assertThat(result.isError()).isTrue();
		assertThat(result.content().toString()).contains("days must be between 1 and 14");
	}

	@Test
	void returnsCurrentWeatherWhenRealApiKeyIsAvailable() {
		assumeTrue(System.getenv("WEATHER_API_KEY") != null, "WEATHER_API_KEY not set; skipping live API call");

		var result = client
			.callTool(McpSchema.CallToolRequest.builder("getWeatherForecastByLocation")
				.arguments(Map.of("city", "London"))
				.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

	@Test
	void returnsForecastWeatherWhenRealApiKeyIsAvailable() {
		assumeTrue(System.getenv("WEATHER_API_KEY") != null, "WEATHER_API_KEY not set; skipping live API call");

		var result = client
			.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
				.arguments(Map.of("city", "London", "days", 2))
				.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

}
