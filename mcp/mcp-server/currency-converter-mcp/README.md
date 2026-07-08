# MCP Currency Converter Server (Streamable HTTP / WebFlux, ASYNC)

A currency-exchange MCP server built with the exact same structure as the
[`webflux`](../webflux/README.md) weather sibling: reactive end to end (Netty event loop,
`type: ASYNC`, `WebClient`, tool methods returning `Mono`), streamable HTTP transport on
`/mcp`. See the webflux README's
["How the non-blocking pipeline works"](../webflux/README.md#how-the-non-blocking-pipeline-works)
section — everything there applies here unchanged.

The tool itself is a port of `explore-openai`'s tool-calling
[`CurrencyTools`](../../../explore-openai/src/main/java/com/llm/tool_calling/currency/CurrencyTools.java):
the same `getCurrencyRates` call against [openexchangerates.org](https://openexchangerates.org),
re-exposed as an MCP tool (`@Tool` → `@McpTool`, blocking `RestClient` → non-blocking
`WebClient`).

| Tool | Parameters | Backend call |
|---|---|---|
| `getCurrencyRates` | `base` (optional, defaults to `USD`), `symbols` (optional, comma separated e.g. `EUR,GBP,INR`; all currencies when omitted) | `GET /api/latest.json` |

Configuration lives under `currency-exchange.*`:

```yaml
currency-exchange:
  api-key: ${CURRENCY_EXCHANGE_API_KEY}   # your openexchangerates.org App ID
  base-url: https://openexchangerates.org/api
```

Note: the openexchangerates.org **free plan only allows `base=USD`** — requesting another
base returns a 403, which the server surfaces to MCP clients as a tool error (wrapped in
`CurrencyApiException`, mirroring the weather server's `WeatherApiException`).

All commands run from the repo root (`spring-ai/`).

### Build and validate

```bash
# build — compiles, runs the WireMock-backed test suite, and packages the boot jar
./gradlew :mcp:mcp-server:currency-converter-mcp:build

# run only the tests (skip packaging)
./gradlew :mcp:mcp-server:currency-converter-mcp:test                                      # WireMock-backed suite (offline)
CURRENCY_EXCHANGE_API_KEY=<your-app-id> ./gradlew :mcp:mcp-server:currency-converter-mcp:test # + live openexchangerates.org smoke test
```

`build` fails if any test fails, so a green build *is* the test validation; the HTML report
lands in `mcp/mcp-server/currency-converter-mcp/build/reports/tests/test/index.html`.

### Launch the server

```bash
CURRENCY_EXCHANGE_API_KEY=<your-app-id> ./gradlew :mcp:mcp-server:currency-converter-mcp:bootRun
```

The MCP endpoint is `http://localhost:8082/mcp` (port 8082 — the weather webmvc and
webflux servers own 8080 and 8081). To run the packaged jar instead of Gradle:

```bash
CURRENCY_EXCHANGE_API_KEY=<your-app-id> java -jar mcp/mcp-server/currency-converter-mcp/build/libs/currency-converter-mcp-0.0.1-SNAPSHOT.jar
```

### Test with the MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

Choose transport `Streamable HTTP` and URL `http://localhost:8082/mcp`, then call
`getCurrencyRates` with e.g. `symbols = EUR,GBP,INR`.
