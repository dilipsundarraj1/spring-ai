package com.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the packaged jar as a child process (like a real MCP client would) and talks to
 * it over the STDIO transport. The weatherapi.com backend is replaced by a WireMock stub
 * so the full pipeline — tool call, RestClient request, JSON deserialization, tool result
 * — is covered deterministically without a real API key or network access.
 *
 * Requires the boot jar to be built first — the Gradle test task depends on bootJar to
 * guarantee that. Live calls against the real weatherapi.com live in
 * {@link McpWeatherServerLiveApiTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpWeatherServerIntegrationTest {

	private static final String SERVER_JAR = System.getProperty("mcp.server.jar", "build/libs/stdio-0.0.2-SNAPSHOT.jar");

	private static final String STUB_API_KEY = "test-api-key";

	private WireMockServer weatherApi;

	private McpSyncClient client;

	@BeforeAll
	void startStubAndConnectToServer() {
		weatherApi = new WireMockServer(options().dynamicPort());
		weatherApi.start();
		stubWeatherApi();

		// the server child process is pointed at the stub instead of api.weatherapi.com
		var serverParams = ServerParameters.builder("java")
			.args("-jar", SERVER_JAR, "--weather.api-url=" + weatherApi.baseUrl())
			.addEnvVar("WEATHER_API_KEY", STUB_API_KEY)
			.build();

		var transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper(JsonMapper.builder().build()));

		client = McpClient.sync(transport)
			.requestTimeout(Duration.ofSeconds(30))
			.initializationTimeout(Duration.ofSeconds(60))
			.build();

		var initResult = client.initialize();
		assertThat(initResult.serverInfo().name()).isEqualTo("my-weather-server");
	}

	private void stubWeatherApi() {
		weatherApi.stubFor(get(urlPathEqualTo("/current.json"))
			.withQueryParam("key", equalTo(STUB_API_KEY))
			.withQueryParam("q", equalTo("London"))
			.willReturn(okJson(readResource("/wiremock/current_response.json"))));

		weatherApi.stubFor(get(urlPathEqualTo("/forecast.json"))
			.withQueryParam("key", equalTo(STUB_API_KEY))
			.withQueryParam("q", equalTo("London"))
			.withQueryParam("days", equalTo("2"))
			.willReturn(okJson(readResource("/wiremock/forecast_response.json"))));

		// weatherapi.com's error shape for an invalid key
		weatherApi.stubFor(get(urlPathEqualTo("/current.json"))
			.withQueryParam("q", equalTo("Atlantis"))
			.willReturn(aResponse().withStatus(401)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"error\":{\"code\":2006,\"message\":\"API key provided is invalid\"}}")));
	}

	@AfterAll
	void disconnect() {
		if (client != null) {
			client.closeGracefully();
		}
		if (weatherApi != null) {
			weatherApi.stop();
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
		var result = client.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
			.arguments(Map.of("city", "London", "days", 20))
			.build());

		assertThat(result.isError()).isTrue();
		assertThat(result.content().toString()).contains("days must be between 1 and 14");
	}

	@Test
	void rejectsForecastDaysBelowTheMinimum() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
			.arguments(Map.of("city", "London", "days", 0))
			.build());

		assertThat(result.isError()).isTrue();
		assertThat(result.content().toString()).contains("days must be between 1 and 14");
	}

	@Test
	void returnsCurrentWeatherFromTheStubbedApi() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getWeatherForecastByLocation")
			.arguments(Map.of("city", "London"))
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content().toString())
			.contains("London")
			.contains("United Kingdom")
			.contains("Overcast");
	}

	@Test
	void returnsForecastWeatherFromTheStubbedApi() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getForecastWeatherByLocation")
			.arguments(Map.of("city", "London", "days", 2))
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content().toString())
			.contains("2026-07-04")
			.contains("2026-07-05")
			.contains("Sunny");
	}

	@Test
	void surfacesWeatherApiErrorsAsToolErrors() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getWeatherForecastByLocation")
			.arguments(Map.of("city", "Atlantis"))
			.build());

		assertThat(result.isError()).isTrue();
	}

	private static String readResource(String path) {
		try (var in = McpWeatherServerIntegrationTest.class.getResourceAsStream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
