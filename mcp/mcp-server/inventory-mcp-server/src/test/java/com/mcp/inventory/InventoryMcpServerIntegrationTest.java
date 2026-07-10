package com.mcp.inventory;

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
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the SYNC webmvc server in-process on a random port and talks to it over the
 * streamable HTTP transport, exercising the read tools against the data.sql seed in H2.
 * The REST surface is covered separately in {@link InventoryControllerIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryMcpServerIntegrationTest {

    private static final String SEEDED_IPHONE_ID = "7f2c1a3e-9b4d-4c5f-8e6a-1d2b3c4d5e6f";

    @LocalServerPort
    int port;

    private McpSyncClient client;

    @BeforeAll
    void connect() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .jsonMapper(new JacksonMcpJsonMapper(JsonMapper.builder().build()))
                .build();

        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(60))
                .build();

        var initResult = client.initialize();
        assertThat(initResult.serverInfo().name()).isEqualTo("inventory-mcp-server");
    }

    @AfterAll
    void disconnect() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    void exposesTheInventoryReadTools() {
        var tools = client.listTools().tools();

        assertThat(tools).extracting(McpSchema.Tool::name).containsExactlyInAnyOrder(
                "getInventoryItemByProductId",
                "getInventoryItemsByProductType",
                "searchInventoryItemsByProductName",
                "getAllInventoryItems");
    }

    @Test
    void getsAnItemByProductId() {
        var result = client.callTool(McpSchema.CallToolRequest.builder("getInventoryItemByProductId")
                .arguments(Map.of("productId", SEEDED_IPHONE_ID))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content().toString())
                .contains("iPhone 16 Pro")
                .contains("MOB-APL-IP16P-256")
                .contains("IN_STOCK");
    }

    @Test
    void listsItemsByProductType() {
        var result = client.callTool(McpSchema.CallToolRequest.builder("getInventoryItemsByProductType")
                .arguments(Map.of("productType", "LAPTOP"))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content().toString())
                .contains("MacBook Pro 16")
                .contains("Dell XPS 15");
    }

    @Test
    void searchesItemsByProductNameCaseInsensitively() {
        var result = client.callTool(McpSchema.CallToolRequest.builder("searchInventoryItemsByProductName")
                .arguments(Map.of("productName", "watch"))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content().toString()).contains("Apple Watch Ultra 3");
    }

    @Test
    void listsAllItems() {
        var result = client.callTool(McpSchema.CallToolRequest.builder("getAllInventoryItems")
                .arguments(Map.of())
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content().toString())
                .contains("iPhone 16 Pro")
                .contains("PlayStation 5 Pro")
                .contains("LG OLED evo C4");
    }

    @Test
    void surfacesUnknownProductIdAsToolError() {
        var result = client.callTool(McpSchema.CallToolRequest.builder("getInventoryItemByProductId")
                .arguments(Map.of("productId", "no-such-id"))
                .build());

        assertThat(result.isError()).isTrue();
    }

}
