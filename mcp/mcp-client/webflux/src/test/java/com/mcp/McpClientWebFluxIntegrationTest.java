package com.mcp;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.fakeservers.FakeCurrencyMcpServer;
import com.fakeservers.FakeInventoryMcpServer;
import com.fakeservers.FakeWeatherMcpServer;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the reactive client app against three in-process fake MCP servers (weather +
 * currency + inventory, see {@link FakeWeatherMcpServer}, {@link FakeCurrencyMcpServer}
 * and {@link FakeInventoryMcpServer}) so the real MCP handshake, tools/list discovery,
 * and tools/call routing all run over real Streamable HTTP. The OpenAI chat completions API is replaced by a WireMock stub that
 * answers in the streaming (SSE) format the reactive ChatClient requests, so /chat and
 * /chat/stream are covered deterministically without an API key or network access. The
 * full LLM tool-call loop is covered by the WebMVC client's integration test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.ai.mcp.server.enabled=false")
class McpClientWebFluxIntegrationTest {

	private static final WireMockServer openAi = new WireMockServer(options().dynamicPort());

	private static ConfigurableApplicationContext weatherServer;

	private static ConfigurableApplicationContext currencyServer;

	private static ConfigurableApplicationContext inventoryServer;

	@LocalServerPort
	int port;

	@Autowired
	ToolCallbackProvider mcpToolCallbackProvider;

	@DynamicPropertySource
	static void wireFakeServersAndOpenAi(DynamicPropertyRegistry registry) {
		openAi.start();
		weatherServer = startFakeMcpServer(FakeWeatherMcpServer.class, "fake-weather-server");
		currencyServer = startFakeMcpServer(FakeCurrencyMcpServer.class, "fake-currency-server");
		inventoryServer = startFakeMcpServer(FakeInventoryMcpServer.class, "fake-inventory-server");

		registry.add("spring.ai.mcp.client.streamable-http.connections.weather-server.url",
				() -> baseUrl(weatherServer));
		registry.add("spring.ai.mcp.client.streamable-http.connections.currency-converter.url",
				() -> baseUrl(currencyServer));
		registry.add("spring.ai.mcp.client.streamable-http.connections.inventory-server.url",
				() -> baseUrl(inventoryServer));
		registry.add("spring.ai.openai.base-url", openAi::baseUrl);
		registry.add("spring.ai.openai.api-key", () -> "test-api-key");
	}

	@BeforeEach
	void reset() {
		openAi.resetAll();
		FakeWeatherMcpServer.invocations.clear();
		FakeCurrencyMcpServer.invocations.clear();
		FakeInventoryMcpServer.invocations.clear();
	}

	@AfterAll
	static void shutDown() {
		if (weatherServer != null) {
			weatherServer.close();
		}
		if (currencyServer != null) {
			currencyServer.close();
		}
		if (inventoryServer != null) {
			inventoryServer.close();
		}
		openAi.stop();
	}

	@Test
	void discoversToolsFromAllMcpServers() {
		assertThat(toolNames())
			.anyMatch(name -> name.contains("getWeatherForecastByLocation"))
			.anyMatch(name -> name.contains("getForecastWeatherByLocation"))
			.anyMatch(name -> name.contains("getCurrencyRates"))
			.anyMatch(name -> name.contains("searchInventoryItemsByProductName"))
			.anyMatch(name -> name.contains("getAllInventoryItems"));
	}

	@Test
	void routesToolCallsToTheWeatherServer() {
		var result = toolNamed("getWeatherForecastByLocation").call("{\"city\": \"London\"}");

		assertThat(result).contains("STUB-WEATHER London");
		assertThat(FakeWeatherMcpServer.invocations).containsExactly("getWeatherForecastByLocation:London");
		assertThat(FakeCurrencyMcpServer.invocations).isEmpty();
	}

	@Test
	void routesToolCallsToTheCurrencyServer() {
		var result = toolNamed("getCurrencyRates").call("{\"base\": \"USD\"}");

		assertThat(result).contains("STUB-RATES USD");
		assertThat(FakeCurrencyMcpServer.invocations).containsExactly("getCurrencyRates:USD");
		assertThat(FakeWeatherMcpServer.invocations).isEmpty();
	}

	@Test
	void routesToolCallsToTheInventoryServer() {
		var result = toolNamed("searchInventoryItemsByProductName").call("{\"productName\": \"iphone\"}");

		assertThat(result).contains("STUB-INVENTORY iphone");
		assertThat(FakeInventoryMcpServer.invocations).containsExactly("searchInventoryItemsByProductName:iphone");
		assertThat(FakeWeatherMcpServer.invocations).isEmpty();
		assertThat(FakeCurrencyMcpServer.invocations).isEmpty();
	}

	@Test
	void chatEndpointAggregatesTheStreamedAnswer() {
		stubStreamingCompletion(chunk("It is 21C", null), chunk(" and overcast.", "stop"));

		webTestClient().get()
			.uri("/chat?question={question}", "What is the weather in London?")
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class).isEqualTo("It is 21C and overcast.");
	}

	@Test
	void chatStreamEndpointEmitsServerSentEvents() {
		stubStreamingCompletion(chunk("Streamed", null), chunk("tokens", "stop"));

		var events = webTestClient().get()
			.uri("/chat/stream?question={question}", "What is the weather in London?")
			.accept(MediaType.TEXT_EVENT_STREAM)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
			.returnResult(String.class)
			.getResponseBody()
			.collectList()
			.block(Duration.ofSeconds(30));

		assertThat(String.join("", events)).isEqualTo("Streamedtokens");
	}

	private static ConfigurableApplicationContext startFakeMcpServer(Class<?> serverConfig, String serverName) {
		return new SpringApplicationBuilder(serverConfig).properties(
				// don't load this module's application.yml into the fake server context
				"spring.config.name=fake-mcp-server",
				"server.port=0",
				"spring.ai.mcp.server.name=" + serverName,
				"spring.ai.mcp.server.version=0.0.1",
				// SYNC (the default) so the shared fakes' plain return values work on
				// the reactive transport too
				"spring.ai.mcp.server.protocol=STREAMABLE",
				"spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp",
				"spring.ai.mcp.client.enabled=false",
				"spring.ai.openai.api-key=unused")
			.run();
	}

	private static String baseUrl(ConfigurableApplicationContext serverContext) {
		return "http://localhost:" + serverContext.getEnvironment().getProperty("local.server.port");
	}

	private WebTestClient webTestClient() {
		return WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.responseTimeout(Duration.ofSeconds(30))
			.build();
	}

	private List<String> toolNames() {
		return Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
			.map(callback -> callback.getToolDefinition().name())
			.toList();
	}

	private ToolCallback toolNamed(String simpleName) {
		return Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
			.filter(callback -> callback.getToolDefinition().name().contains(simpleName))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No MCP tool named " + simpleName + " was discovered"));
	}

	private static void stubStreamingCompletion(String... chunks) {
		var body = new StringBuilder();
		for (String chunk : chunks) {
			body.append("data: ").append(chunk).append("\n\n");
		}
		body.append("data: [DONE]\n\n");

		openAi.stubFor(post(urlPathMatching("(/v1)?/chat/completions"))
			.willReturn(aResponse()
				.withHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
				.withBody(body.toString())));
	}

	private static String chunk(String content, String finishReason) {
		return """
				{"id":"chatcmpl-test","object":"chat.completion.chunk","created":1751000000,"model":"gpt-5.5",\
				"choices":[{"index":0,"delta":{"role":"assistant","content":"%s"},"finish_reason":%s}]}"""
			.formatted(content, finishReason == null ? "null" : "\"" + finishReason + "\"");
	}

}
