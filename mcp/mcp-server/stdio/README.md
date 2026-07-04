# MCP Weather Server (STDIO)

<!-- TOC -->
* [MCP Weather Server (STDIO)](#mcp-weather-server-stdio)
  * [Why is this a STDIO server?](#why-is-this-a-stdio-server)
  * [When to use STDIO](#when-to-use-stdio)
  * [Configuration explained (`application.yml`)](#configuration-explained-applicationyml)
  * [Testing with MCP Inspector](#testing-with-mcp-inspector)
    * [1. Make sure console logging is disabled](#1-make-sure-console-logging-is-disabled)
    * [2. Build the jar](#2-build-the-jar)
    * [3. Run the Inspector](#3-run-the-inspector)
    * [4. Test in the browser UI](#4-test-in-the-browser-ui)
  * [Automated integration test](#automated-integration-test)
    * [How the server process gets launched](#how-the-server-process-gets-launched)
    * [What is covered](#what-is-covered)
  * [Weather API reference](#weather-api-reference)
  * [Troubleshooting](#troubleshooting)
<!-- TOC -->
A Spring AI MCP server that exposes weather tools (`getWeatherForecastByLocation` for current
conditions, `getForecastWeatherByLocation` for a multi-day forecast) over the **STDIO**
transport, backed by the [weatherapi.com](https://www.weatherapi.com) API
(an API key is required — see step 3).

## Why is this a STDIO server?

MCP defines two main transports: **STDIO** and **HTTP-based** (streamable HTTP / SSE, like the
sibling `webmvc`/`webflux` modules). This module uses STDIO, which means:

- **The client owns the server's lifecycle.** A client (MCP Inspector, Claude Desktop, Claude
  Code, the integration test) launches `java -jar <this jar>` as a **child process** — the server
  is never started separately and nothing listens on a port.
- **The process streams are the wire.** The client writes JSON-RPC requests to the child's
  **stdin** and reads responses from its **stdout**. stderr is left for diagnostics.
- **stdout is sacred.** Since the protocol runs over stdout, *any* other output — the Spring Boot
  banner, console logging, a stray `System.out.println` — corrupts the JSON-RPC stream and breaks
  the connection. Most of the configuration below exists to guarantee a silent stdout.

STDIO is the natural fit here: a single-user, local tool server that a desktop client spawns on
demand, with no need for networking, TLS, or auth. Expose the same tools to remote/multi-client
consumers and you'd pick the HTTP transport instead.

What makes this project render as STDIO rather than a web app is configuration, not code —
`WeatherService` would work unchanged in the `webmvc` module. The switch is flipped by
`spring.ai.mcp.server.stdio: true` plus `spring.main.web-application-type: none`, explained next.

## When to use STDIO

**Choose STDIO when:**

- The server runs **on the same machine as the client** — desktop AI apps (Claude Desktop,
  Claude Code, IDEs) spawning local tool servers on demand.
- There is **one client per server instance**. Each client gets its own private child process,
  so there's no sharing, no session management, no concurrency across clients.
- You want **zero deployment**: nothing to host, no port to open, no TLS certificate, no auth
  layer — the OS process boundary *is* the security boundary. If you can run the jar, you can
  use the server; it inherits the launching user's permissions and environment.
- The tools need access to **local resources** (files, local databases, developer credentials)
  that a remote server wouldn't have.
- Startup is cheap enough to pay per session — here a Spring Boot app in a couple of seconds.

**Choose an HTTP transport (the `webmvc`/`webflux` siblings) when:**

- Clients connect **over the network** — the server runs centrally (e.g. in Kubernetes) and is
  shared by many users or agents.
- You need **many concurrent clients** on one long-running instance, or the server holds
  expensive shared state (connection pools, caches, warmed-up models) that shouldn't be
  rebuilt per client.
- You need real **authentication/authorization**, TLS, rate limiting, observability, or
  horizontal scaling — all standard HTTP machinery.
- The server must **outlive the client**, be independently deployed and versioned, or be
  reachable by clients that can't spawn processes (e.g. web apps).

Rule of thumb: STDIO is `git`-like — a local program a client runs when needed. HTTP is
service-like — infrastructure that's always on. This weather server takes a personal API key
from the user's environment and serves a single desktop client, so STDIO fits.

**A realistic example of each:**

- **STDIO — a database assistant for developers.** Your team builds a `postgres-mcp` server
  exposing tools like `runQuery`, `explainPlan`, and `listSlowQueries`. Each developer adds it
  to Claude Code with `claude mcp add postgres -- java -jar postgres-mcp.jar`. When a developer
  asks "why is the orders query slow?", Claude Code spawns the jar, which connects to
  `localhost:5432` **with that developer's own credentials** from their environment. Every
  developer gets an isolated process, nobody had to deploy anything, and the DBA never had to
  stand up (or secure) a shared query service.
- **HTTP — the company Jira server for all agents.** The platform team deploys `jira-mcp` once
  to Kubernetes at `https://mcp.internal.acme.com/jira`, fronted by the SSO gateway. Hundreds
  of employees' assistants (and CI bots) connect to the same instance, which holds one warm
  connection pool to Jira, enforces per-user OAuth tokens, applies rate limits, and gets
  upgraded centrally without anyone reinstalling anything. Spawning a copy of this per user
  as a child process would leak admin credentials to every laptop and make upgrades impossible
  to roll out.

## Configuration explained (`application.yml`)

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: "off"
  ai:
    mcp:
      server:
        name: my-weather-server
        version: 0.0.1
        type: SYNC
        stdio: true

weather:
  api-key: ${WEATHER_API_KEY}
  api-url: http://api.weatherapi.com/v1

logging:
  pattern:
    console: ""
  file:
    name: ./build/mcp-weather-stdio-server.log
```

- **`spring.ai.mcp.server.stdio: true`** — the key switch: tells Spring AI to bind the MCP server
  to a STDIO transport (stdin/stdout of this process) instead of exposing HTTP endpoints.
- **`spring.main.web-application-type: none`** — run as a plain Java process: no embedded Tomcat,
  no port. Needed because the build depends on the `webmvc` starter, which would otherwise start
  a servlet container this transport doesn't use.
- **`spring.main.banner-mode: "off"`** — the Spring Boot banner prints to stdout, which would be
  the first thing to corrupt the protocol stream. Quoted because bare `off` in YAML parses as
  boolean `false` rather than the string.
- **`spring.ai.mcp.server.name` / `version`** — the server's identity, returned to the client in
  the `initialize` handshake (the integration test asserts `name` is `my-weather-server`). Purely
  informational; clients typically show it in their UI.
- **`spring.ai.mcp.server.type: SYNC`** — synchronous server: `@McpTool` methods are plain
  blocking calls (our `RestClient` calls block). `ASYNC` would be for reactive implementations
  returning `Mono`/`Flux`.
- **`weather.api-key` / `weather.api-url`** — custom properties bound to the
  `WeatherConfigProperties` record and used by `WeatherService` to call weatherapi.com. The key
  is not hardcoded: `${WEATHER_API_KEY}` is resolved from the environment at startup, which is
  why every launch command in this README passes that variable. Startup **fails** with a
  placeholder-resolution error if it's missing.
- **`logging.pattern.console: ""`** — blanks the console log pattern, silencing the console
  appender entirely. Required for the same stdout reason as the banner; note that configuring a
  log *file* does not turn console logging off by itself.
- **`logging.file.name`** — with the console silenced, this file is the only place logs go
  (resolved relative to the working directory of whoever launched the process).

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

The jar is written to `mcp/mcp-server/stdio/build/libs/stdio-0.0.2-SNAPSHOT.jar`
(the version comes from `build.gradle`).

### 3. Run the Inspector

MCP Inspector launches the server itself as a child process (that's how the stdio transport
works — you don't start the server separately), so just point it at the jar.

The server reads the weatherapi.com key from the `WEATHER_API_KEY` environment variable
(injected into `application.yml` via `${WEATHER_API_KEY}`), and the Inspector's `-e` flag
passes it to the server child process.

From the repo root (`spring-ai/`):

```bash
npx @modelcontextprotocol/inspector -e WEATHER_API_KEY=<your-weatherapi-key> java -jar mcp/mcp-server/stdio/build/libs/stdio-0.0.2-SNAPSHOT.jar
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

## Automated integration test

[`McpWeatherServerIntegrationTest`](src/test/java/com/mcp/McpWeatherServerIntegrationTest.java)
tests the server the same way the Inspector (or Claude) uses it: it launches the packaged jar
as a child process and talks real MCP over stdin/stdout using the MCP Java SDK client.

```bash
./gradlew :mcp:mcp-server:stdio:test                            # offline tests only
WEATHER_API_KEY=<your-key> ./gradlew :mcp:mcp-server:stdio:test # + live weatherapi.com calls
```

### How the server process gets launched

Nothing in the test starts the server explicitly — the STDIO transport owns the server's
lifecycle, exactly like a real MCP client:

1. **The Gradle `test` task depends on `bootJar`**, so the jar is always built first, and its
   path is handed to the test via the `mcp.server.jar` system property (see `build.gradle`).
2. **The test only *describes* the process** — `ServerParameters.builder("java")
   .args("-jar", <jar>).addEnvVar("WEATHER_API_KEY", ...)` is just a value object holding the
   command, arguments, and environment. Nothing runs yet.
3. **`client.initialize()` triggers the launch.** Inside the SDK, `StdioClientTransport.connect()`
   assembles the full command and calls the JDK's `ProcessBuilder.start()` — at that moment the
   child process `java -jar stdio-0.0.2-SNAPSHOT.jar` (the Spring Boot server) is born.
4. **The child's pipes become the transport.** The SDK writes JSON-RPC requests to the child's
   stdin, reads responses from its stdout, and drains stderr separately. No port, no HTTP —
   the process streams are the wire. This is why console logging must stay silenced (step 1
   above): the server's stdout belongs to the protocol.
5. **The MCP handshake completes** (`initialize` request/response — the test asserts the server
   identifies as `my-weather-server`), then the test methods issue `tools/list` and `tools/call`
   requests through the same pipes.
6. **`@AfterAll` calls `closeGracefully()`**, which shuts down the transport and terminates the
   child process.

### What is covered

| Test | Needs `WEATHER_API_KEY`? |
|---|---|
| `exposesTheWeatherTools` — `tools/list` returns exactly the two weather tools | no |
| `rejectsForecastDaysAboveTheMaximum` — `days: 20` returns an MCP error ("days must be between 1 and 14") | no |
| `rejectsForecastDaysBelowTheMinimum` — `days: 0` returns the same error | no |
| `returnsCurrentWeatherWhenRealApiKeyIsAvailable` — live `/current.json` call | yes (skipped otherwise) |
| `returnsForecastWeatherWhenRealApiKeyIsAvailable` — live `/forecast.json` call | yes (skipped otherwise) |

The validation tests need no network because the range check fires before any HTTP call, and
the server itself boots fine with the dummy key the test falls back to.

Note: if the jar is missing (only possible when running the test from an IDE without Gradle
delegation), `ProcessBuilder` still starts `java` successfully — the JVM then dies complaining
on stderr, and the client only notices as a 30-second `initialize` timeout. Run
`./gradlew bootJar` first.

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
