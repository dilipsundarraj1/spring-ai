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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in smoke test against the real weatherapi.com — the only place contract drift
 * (renamed/removed response fields) gets caught. The whole class is skipped unless
 * WEATHER_API_KEY is set. Day-to-day coverage lives in the WireMock-backed
 * {@link McpWeatherServerIntegrationTest}.
 */
@EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpWeatherServerLiveApiTest {

	private static final String SERVER_JAR = System.getProperty("mcp.server.jar", "build/libs/stdio-0.0.2-SNAPSHOT.jar");

	private McpSyncClient client;

	@BeforeAll
	void connectToServer() {
		var serverParams = ServerParameters.builder("java")
			.args("-jar", SERVER_JAR)
			.addEnvVar("WEATHER_API_KEY", System.getenv("WEATHER_API_KEY"))
			.build();

		var transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper(JsonMapper.builder().build()));

		client = McpClient.sync(transport)
			.requestTimeout(Duration.ofSeconds(30))
			.initializationTimeout(Duration.ofSeconds(60))
			.build();

		client.initialize();
	}

	@AfterAll
	void disconnect() {
		if (client != null) {
			client.closeGracefully();
		}
	}

	@Test
	void returnsCurrentWeatherFromTheRealApi() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getWeatherForecastByLocation")
			.arguments(Map.of("city", "London"))
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

	@Test
	void returnsForecastWeatherFromTheRealApi() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
			.arguments(Map.of("city", "London", "days", 2))
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

}
