package com.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the reactive (ASYNC) server in-process on a random port — Netty, not Tomcat —
 * and talks to it over the streamable HTTP transport. The openexchangerates.org backend
 * is replaced by a WireMock stub so the full pipeline — tool call, WebClient request,
 * JSON deserialization, tool result — is covered deterministically without a real API
 * key or network access. Live calls against the real openexchangerates.org live in
 * {@link McpCurrencyConverterLiveApiTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpCurrencyConverterIntegrationTest {

	private static final String STUB_API_KEY = "test-api-key";

	private static final WireMockServer currencyApi = new WireMockServer(options().dynamicPort());

	@LocalServerPort
	int port;

	private McpSyncClient client;

	@DynamicPropertySource
	static void currencyProperties(DynamicPropertyRegistry registry) {
		currencyApi.start();
		registry.add("currency-exchange.base-url", currencyApi::baseUrl);
		registry.add("currency-exchange.api-key", () -> STUB_API_KEY);
	}

	@BeforeAll
	void stubCurrencyApiAndConnect() {
		currencyApi.stubFor(get(urlPathEqualTo("/latest.json"))
			.withQueryParam("app_id", equalTo(STUB_API_KEY))
			.withQueryParam("base", equalTo("USD"))
			.withQueryParam("symbols", equalTo("EUR,GBP,INR"))
			.willReturn(okJson(readResource("/wiremock/latest_response.json"))));

		// no symbols param -> rates for all currencies (same stubbed body)
		currencyApi.stubFor(get(urlPathEqualTo("/latest.json"))
			.withQueryParam("app_id", equalTo(STUB_API_KEY))
			.withQueryParam("base", equalTo("USD"))
			.withQueryParam("symbols", absent())
			.willReturn(okJson(readResource("/wiremock/latest_response.json"))));

		// openexchangerates.org's error shape for a base the plan doesn't allow
		currencyApi.stubFor(get(urlPathEqualTo("/latest.json"))
			.withQueryParam("base", equalTo("XXX"))
			.willReturn(aResponse().withStatus(403)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"error\":true,\"status\":403,\"message\":\"not_allowed\","
						+ "\"description\":\"Changing the API base currency is not allowed\"}")));

		var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
			.endpoint("/mcp")
			.jsonMapper(new JacksonMcpJsonMapper(JsonMapper.builder().build()))
			.build();

		client = McpClient.sync(transport)
			.requestTimeout(Duration.ofSeconds(30))
			.initializationTimeout(Duration.ofSeconds(60))
			.build();

		var initResult = client.initialize();
		assertThat(initResult.serverInfo().name()).isEqualTo("currency-converter-mcp");
	}

	@AfterAll
	void disconnect() {
		if (client != null) {
			client.closeGracefully();
		}
		currencyApi.stop();
	}

	@Test
	void exposesTheCurrencyTool() {
		var tools = client.listTools().tools();

		assertThat(tools).extracting(McpSchema.Tool::name).containsExactly("getCurrencyRates");
	}

	@Test
	void returnsCurrencyRatesFromTheStubbedApi() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getCurrencyRates")
			.arguments(Map.of("base", "USD", "symbols", "EUR,GBP,INR"))
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content().toString())
			.contains("EUR")
			.contains("GBP")
			.contains("INR")
			.contains("0.85");
	}

	@Test
	void defaultsTheBaseCurrencyToUsdWhenOmitted() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getCurrencyRates")
			.arguments(Map.of())
			.build());

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content().toString())
			.contains("USD")
			.contains("EUR");
	}

	@Test
	void surfacesCurrencyApiErrorsAsToolErrors() {
		var result = client.callTool(McpSchema.CallToolRequest.builder("getCurrencyRates")
			.arguments(Map.of("base", "XXX"))
			.build());

		assertThat(result.isError()).isTrue();
	}

	private static String readResource(String path) {
		try (var in = McpCurrencyConverterIntegrationTest.class.getResourceAsStream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
