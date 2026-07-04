# MCP Weather Server (STDIO)

A Spring AI MCP server that exposes weather tools (`getWeatherForecastByLocation` for current
conditions, `getForecastWeatherByLocation` for a multi-day forecast) over the **STDIO**
transport, backed by the [weatherapi.com](https://www.weatherapi.com) API
(an API key is required — see step 3).

## Testing with MCP Inspector

### 1. Make sure console logging is disabled

With the STDIO transport, the JSON-RPC protocol runs over **stdout**. Any console output —
the Spring Boot banner or log lines — corrupts the stream, and the Inspector will fail to
connect or show parse errors.

Note that setting `logging.file.name` adds a file appender but Spring Boot **still logs to
the console too**, so the console pattern must be explicitly blanked. `application.yml` must
contain:

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: "off"

logging:
  pattern:
    console: ""
  file:
    name: ./build/mcp-weather-stdio-server.log
```

### 2. Build the jar

The config is baked into the jar at build time, so rebuild after any `application.yml` change:

```bash
./gradlew :mcp:mcp-server:stdio:bootJar
```

The jar is written to `mcp/mcp-server/stdio/build/libs/stdio-0.0.1-SNAPSHOT.jar`.

### 3. Run the Inspector

MCP Inspector launches the server itself as a child process (that's how the stdio transport
works — you don't start the server separately), so just point it at the jar.

The server reads the weatherapi.com key from the `WEATHER_API_KEY` environment variable
(injected into `application.yml` via `${WEATHER_API_KEY}`), and the Inspector's `-e` flag
passes it to the server child process.

From the repo root (`spring-ai/`):

```bash
npx @modelcontextprotocol/inspector -e WEATHER_API_KEY=<your-weatherapi-key> java -jar mcp/mcp-server/stdio/build/libs/stdio-0.0.1-SNAPSHOT.jar
```

The jar path is relative, so the command must be run from the repo root (or adjust the
path accordingly).

It prints a URL like `http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=...` — open that in your browser.

### 4. Test in the browser UI

1. Transport type should already be **STDIO** with the command/args and the `WEATHER_API_KEY`
   environment variable pre-filled; click **Connect**.
2. Go to the **Tools** tab → **List Tools** — you should see `getWeatherForecastByLocation`
   and `getForecastWeatherByLocation`.
3. Select `getWeatherForecastByLocation`, enter a city (e.g. `Seattle`), and click **Run Tool** —
   it returns the current weather conditions as JSON.
4. Try `getForecastWeatherByLocation` with a city and an optional number of days (1-14,
   defaults to 3) — it returns the daily forecast as JSON.

If you change the code, rebuild the jar and hit **Restart** in the Inspector.

## Weather API reference

The tools are backed by the [weatherapi.com API explorer](https://www.weatherapi.com/api-explorer.aspx#forecast).

Forecast request used by `getForecastWeatherByLocation`:

```
GET http://api.weatherapi.com/v1/forecast.json?key=<your-weatherapi-key>&q=London&days=3&aqi=no&alerts=no
```

A sample forecast response is checked in at
[`src/main/resources/forecase_repsonse.json`](src/main/resources/forecase_repsonse.json) —
the `ForecastResponse` record maps the subset of these fields the tool returns
(`location`, `current`, and `forecast.forecastday[].day` temperatures/condition).

## Troubleshooting

- **Inspector can't connect / JSON parse errors**: almost always console output leaking into
  stdout. Re-check step 1, and make sure nothing in the code writes to `System.out`.
- **Server exits immediately on connect**: if `WEATHER_API_KEY` is not set, Spring fails at
  startup with a placeholder-resolution error for `${WEATHER_API_KEY}` — check it was passed
  via `-e` (or the Environment Variables section in the Inspector UI).
- **Server logs**: since the console is silenced, check the log file configured under
  `logging.file.name` (`./build/mcp-weather-stdio-server.log`, relative to the Inspector's
  working directory — so `spring-ai/build/` when launched from the repo root).
