<!-- TOC -->
* [MCP Client (WebFlux)](#mcp-client-webflux)
  * [How it differs from the WebMVC client](#how-it-differs-from-the-webmvc-client)
  * [How it works](#how-it-works)
    * [From YAML to `ToolCallbackProvider`](#from-yaml-to-toolcallbackprovider)
    * [Reactive endpoints](#reactive-endpoints)
  * [Running](#running)
  * [Troubleshooting](#troubleshooting)
<!-- TOC -->

# MCP Client (WebFlux)

A Spring Boot MCP **client** that connects to two MCP servers over **Streamable HTTP** — the [MCP Weather Server (WebFlux)](../../mcp-server/webflux) and the [Currency Converter MCP Server](../../mcp-server/currency-converter-mcp) — and exposes their tools to a single OpenAI-backed `ChatClient`. It is the fully **reactive** counterpart of the [WebMVC client](../webmvc).

## How it differs from the WebMVC client

| | WebMVC client | WebFlux client (this module) |
|---|---|---|
| Boot starter | `spring-ai-starter-mcp-client` | `spring-ai-starter-mcp-client-webflux` |
| Web stack | Servlet (Tomcat) | Reactive (Netty) |
| MCP client type | `type: SYNC` → `McpSyncClient` | `type: ASYNC` → `McpAsyncClient` |
| HTTP transport | blocking `RestClient` | non-blocking `WebClient` |
| Tool provider | `SyncMcpToolCallbackProvider` | `AsyncMcpToolCallbackProvider` |
| Controller | returns `String` | returns `Mono<String>` / `Flux<String>` |
| Port | 9000 | **9001** |

Everything else — the Streamable HTTP protocol, the stateful vs. stateless session discussion, and how tools reach the LLM — is identical; see the [WebMVC client README](../webmvc/README.md) for the deep dive.

## How it works

- Uses the [`spring-ai-starter-mcp-client-webflux`](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html) Boot starter with `ASYNC` clients.
- Connects to the WebFlux weather server at `http://localhost:8081/mcp` and the currency converter at `http://localhost:8082/mcp` (see `application.yml`):

```yaml
spring:
  ai:
    mcp:
      client:
        type: ASYNC
        streamable-http:
          connections:
            weather-server:
              url: http://localhost:8081
              endpoint: /mcp
            currency-converter:
              url: http://localhost:8082
              endpoint: /mcp
```

- The starter creates one `McpAsyncClient` **per connection entry**, auto-discovers every server's tools (`getWeatherForecastByLocation` and `getForecastWeatherByLocation` from the weather server, `getCurrencyRates` from the currency converter), and merges them all into a single `ToolCallbackProvider`, which `ChatController` registers on the `ChatClient` via `defaultTools(...)`. The LLM sees one flat tool list and picks the right server's tool per question — adding another server is just another `connections:` entry, no code changes.
- On startup, `McpClientApplication` logs the tools discovered from every connected MCP server — reactively, via `McpAsyncClient.listTools()` which returns a `Mono`.

### From YAML to `ToolCallbackProvider`

Same auto-configuration flow as the WebMVC client, with the async variants swapped in:

- **Connections → `McpAsyncClient` beans**
  - Each entry under `spring.ai.mcp.client.streamable-http.connections` produces one MCP client.
  - Because `type: ASYNC`, the two entries — `weather-server` and `currency-converter` — create two `McpAsyncClient`s backed by non-blocking `WebClient` streamable HTTP transports.
  - On startup, each client performs the MCP `initialize` handshake against its own server: `http://localhost:8081/mcp` and `http://localhost:8082/mcp`.

- **Clients → one `ToolCallbackProvider` bean**
  - The starter wraps *all* `McpAsyncClient`s in a single `AsyncMcpToolCallbackProvider`.
  - The provider calls `tools/list` on each server and adapts every MCP tool into a Spring AI `ToolCallback`.

- **Provider → `ChatClient` → LLM**
  - `ChatController` registers the provider via `defaultTools(...)`; when the model picks a tool, the callback issues an MCP `tools/call` over the same streamable HTTP connection — without blocking an event-loop thread.

### Reactive endpoints

The controller never blocks:

- `GET /chat` — streams the model's answer internally and aggregates it into a single `Mono<String>` response.
- `GET /chat/stream` — returns the answer as it is generated, token by token, as a `Flux<String>` over Server-Sent Events. This is the natural fit for the reactive stack: LLM tokens flow from OpenAI through the client to the caller without buffering.

## Running

1. Start the WebFlux weather server (in another terminal):

   ```bash
   WEATHER_API_KEY=<your-weatherapi-key> ./gradlew :mcp:mcp-server:webflux:bootRun
   ```

2. Start the currency converter server (in another terminal, listens on **8082**):

   ```bash
   CURRENCY_EXCHANGE_API_KEY=<your-openexchangerates-key> ./gradlew :mcp:mcp-server:currency-converter-mcp:bootRun
   ```

3. Start this client (listens on port **9001**):

   ```bash
   OPENAI_KEY=<your-openai-key> ./gradlew :mcp:mcp-client:webflux:bootRun
   ```

4. Ask a question — the LLM decides which MCP server's tool to call:

   ```bash
   curl -G http://localhost:9001/chat --data-urlencode "question=What is the current weather in New York?"

   curl -G http://localhost:9001/chat --data-urlencode "question=How much is 100 USD in EUR?"

   # Streaming (SSE) variant — tokens arrive as they are generated
   curl -N -G http://localhost:9001/chat/stream --data-urlencode "question=Give me a 5 day forecast for Dallas"
   ```

## Troubleshooting

Same failure modes as the WebMVC client — see its [Troubleshooting](../webmvc/README.md#troubleshooting) section:

- **First request after a server restart fails** — the `STREAMABLE` protocol is stateful; the client re-handshakes automatically, just retry (or switch the server to `protocol: STATELESS`).
- **Client fails to start with `Client failed to initialize by explicit API call`** — the MCP server isn't running or the `url`/`endpoint` in `application.yml` is wrong. Start the server first.
