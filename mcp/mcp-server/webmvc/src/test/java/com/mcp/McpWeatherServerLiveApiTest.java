package com.mcp;

import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in smoke test against the real weatherapi.com — the only place contract drift
 * (renamed/removed response fields) gets caught. The whole class is skipped unless
 * WEATHER_API_KEY is set (the application.yml placeholder resolves it from the
 * environment). Day-to-day coverage lives in the WireMock-backed
 * {@link McpWeatherServerWebMvcIntegrationTest}.
 */
@EnabledIfEnvironmentVariable(named = "WEATHER_API_KEY", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpWeatherServerLiveApiTest {

	@LocalServerPort
	int port;

	private McpSyncClient client;

	@BeforeAll
	void connectToServer() {
		var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
			.endpoint("/mcp")
			.jsonMapper(new JacksonMcpJsonMapper(JsonMapper.builder().build()))
			.build();

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
