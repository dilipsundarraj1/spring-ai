# Observability with Spring Boot & OpenTelemetry

## Why Observability?

In a distributed system you cannot just "look at the code" to understand what is happening at runtime. Observability answers three questions:

- **What happened?** — logs capture discrete events
- **How long did it take, and how often?** — metrics capture counts, durations, and rates
- **Why did it happen?** — traces connect the dots across service boundaries

Without it you are flying blind in production: you cannot tell whether a slow response is the LLM API, your own code, or the network — and you cannot prove it to anyone else either.

---

## OpenTelemetry — The Open Standard

OpenTelemetry (OTel) is a vendor-neutral, CNCF-graduated project that defines a single API and wire format for all three signals.

```
Your App
  │
  ├── Traces  ─────┐
  ├── Metrics ─────┤──► OTLP (OpenTelemetry Protocol) ──► any backend
  └── Logs    ─────┘
```

Before OTel, every backend (Datadog, Jaeger, Prometheus …) had its own SDK and agent. OTel decouples *instrumentation* from *backend choice*: instrument once, ship anywhere.

---

## Spring Boot — Best of Both Worlds

Spring Boot brings OTel to the JVM without boilerplate:

| Layer | What Boot wires up automatically |
|---|---|
| **Micrometer** | Metrics abstraction — counters, timers, gauges |
| **Micrometer Tracing** | Trace context propagation & span creation |
| **OTel SDK** | The bridge that turns Micrometer observations into OTLP spans & metrics |
| **OTLP exporters** | HTTP exporters for traces, metrics, and logs — configured via `application.yml` |
| **Auto-instrumentation** | HTTP requests, Spring AI chat calls, and `@Observed` methods are traced with zero code |

Add `spring-boot-starter-opentelemetry` and configure endpoints — Boot handles the rest.

---

## Architecture: App → otel-lgtm

```mermaid
flowchart LR
    subgraph App["explore-openai · port 8080"]
        REQ([HTTP Request])
        MVC["Spring MVC"]
        AI["Spring AI\nChatClient"]
        OBS["Micrometer\nObservation API"]
        SDK["OTel SDK"]
        T["Traces"]
        M["Metrics"]
        L["Logs"]

        REQ --> MVC
        MVC --> AI
        MVC --> OBS
        AI  --> OBS
        OBS --> SDK
        SDK --> T
        SDK --> M
        SDK --> L
    end

    subgraph LGTM["grafana/otel-lgtm · port 4318"]
        COL["OTel Collector"]
        TEMPO["Tempo"]
        PROM["Mimir / Prometheus"]
        LOKI["Loki"]
        GF["Grafana UI\nport 3000"]

        COL --> TEMPO
        COL --> PROM
        COL --> LOKI
        TEMPO --> GF
        PROM  --> GF
        LOKI  --> GF
    end

    T -- "OTLP/HTTP /v1/traces"  --> COL
    M -- "OTLP/HTTP /v1/metrics" --> COL
    L -- "OTLP/HTTP /v1/logs"    --> COL
```

### Component Reference

| Component | Role |
|---|---|
| **Spring MVC** | Handles inbound HTTP requests; auto-creates a span per request |
| **Spring AI ChatClient** | Executes LLM calls; auto-creates spans and records token-usage metrics |
| **Micrometer Observation API** | Unified abstraction for traces + metrics; `@Observed` and `Observation` API feed into this |
| **OTel SDK** | Translates Micrometer observations into OTLP-formatted spans, metrics, and log records |
| **OTel Collector** | Receives OTLP data, fans it out to the appropriate storage backend |
| **Tempo** | Distributed trace storage — stores and queries spans |
| **Mimir / Prometheus** | Time-series metrics storage — stores counters, histograms, and gauges |
| **Loki** | Log aggregation — stores structured log records with trace correlation |
| **Grafana UI** | Single pane of glass — query and visualise all three signals on port 3000 |

### Configuration (`application.yml`)

```yaml
management:
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
  tracing:
    sampling:
      probability: 1.0   # capture every request (tune down in production)
```

---

## What You Get for Free

Everything below requires **zero custom code** — just the starters and the `application.yml` configuration above.

### Traces

| Source | What gets traced |
|---|---|
| Spring MVC | Every inbound HTTP request — method, path, status code, latency |
| Spring AI | Every `ChatClient` call — model, prompt tokens, completion tokens, latency |
| Propagation | `traceId` / `spanId` propagated outbound via W3C `traceparent` header |

### Metrics

| Metric | Description |
|---|---|
| `http.server.request.duration` | Latency histogram per endpoint and status code |
| `jvm.memory.used` | Heap and non-heap per memory pool |
| `jvm.gc.duration` | GC pause time |
| `jvm.threads.active` | Live thread count |
| `gen_ai.client.token.usage` | Prompt + completion token counts per LLM call |
| `gen_ai.client.operation.duration` | End-to-end LLM call latency |
| `system.cpu.usage` | Process and system CPU utilisation |

### Logs

| What | Detail |
|---|---|
| All SLF4J / Logback output | Shipped to Loki via the OTLP appender |
| `traceId` & `spanId` | Injected into every log record — links a log line directly to its trace in Tempo |
| Severity mapping | Logback levels → OTel severity numbers automatically |

> **Note:** Logs require two small pieces of custom setup. Logback initialises before the Spring context, so the OTel appender exists but has no SDK reference at startup. Two files bridge that gap:
> - **`logback-spring.xml`** — declares the `OpenTelemetryAppender` and adds it alongside the console appender
> - **`InstallOpenTelemetryAppender.java`** — an `InitializingBean` that calls `OpenTelemetryAppender.install(openTelemetry)` once the Spring-managed `OpenTelemetry` bean is ready
>
> Without these, logs are written to the console only and never reach Loki.

---

## Understanding TraceId and SpanId

These two IDs are the backbone of distributed tracing. Once you grasp them, everything in Grafana Tempo clicks into place.

### The Analogy

Think of it like an **Amazon package delivery**:

- You place one order and get one **order number** — that is the `traceId`
- Behind the scenes, the order goes through multiple stages — payment processed, warehouse pick, packed, dispatched to courier, out for delivery, delivered — each stage is a **span**
- Every span has a **start time, an end time, and a status**
- You can track the entire journey with that one order number (`traceId`)
- You can drill into any individual stage (`spanId`) to see exactly what happened and how long it took

```
TraceId: a1b2c3d4e5f6...  (same across all spans below)

├── SpanId: 11aa  [Payment Processed]       0ms  → 120ms  ✅
├── SpanId: 22bb  [Warehouse Pick]          120ms → 800ms  ✅
├── SpanId: 33cc  [Packed]                  800ms → 1.2s   ✅
├── SpanId: 44dd  [Dispatched to Courier]   1.2s  → 2.1s   ✅
├── SpanId: 55ee  [Out for Delivery]        2.1s  → 8.4s   ✅
└── SpanId: 66ff  [Delivered]               8.4s  → 8.5s   ✅
```

### TraceId

- A **single unique ID** assigned at the very start of a request — when it enters Spring MVC.
- It **stays the same** for the entire lifetime of that request, across every method call, every service hop, and every log line.
- All spans belonging to the same request share this ID — it is what lets you pull up the full picture in Tempo.

### SpanId

- A **unique ID for one unit of work** within a trace — an HTTP handler, an LLM call, a custom `@Observed` method.
- Each span knows its **parent span**, forming a tree. The root span is the inbound HTTP request; child spans hang off it.
- A span records: operation name, start time, duration, status, and any attributes (e.g. `http.method`, `gen_ai.usage.input_tokens`).

### How They Fit Together

```
TraceId: a1b2c3d4e5f6...  (same across all rows below)

├── SpanId: 11aa  [HTTP GET /springai/v1/structured_outputs]  0ms → 340ms
│   ├── SpanId: 22bb  [Spring AI ChatClient call]             5ms → 330ms
│   │   └── SpanId: 33cc  [structured_outputs observation]   5ms → 330ms
```

Here is a real log line from this application. Notice the `traceId` and `spanId` embedded directly in the output:

```
2026-08-03T05:08:16.917-05:00  INFO 58957 --- [explore-openai] [nio-8080-exec-1] [ba224c7679bbf814e63ae1ebbd6ce243-30faf73441cb4fa1] com.llm.chats.ChatController : userInput message : UserInput[prompt=What is the overall format of the basketball tournament at the 2024 Olympics?]
```

Breaking it down:

| Part | Value | Meaning |
|---|---|---|
| Timestamp | `2026-08-03T05:08:16.917` | When the log was emitted |
| Thread | `nio-8080-exec-1` | The request thread |
| `traceId` | `ba224c7679bbf814e63ae1ebbd6ce243` | Identifies the entire request — paste this into Grafana Tempo to see the full trace |
| `spanId` | `30faf73441cb4fa1` | Identifies this specific unit of work within that trace |
| Logger | `com.llm.chats.ChatController` | The class that emitted the log |

### Why It Matters for Logs

Every log line emitted inside a request automatically carries the active `traceId` and `spanId`:

```
INFO  c.l.StructuredOutputsController - userInput message : ...
      traceId=a1b2c3d4e5f6  spanId=22bb
```

In Grafana you can click the `traceId` in a Loki log line and jump straight to the matching trace in Tempo — no manual searching.

---

## Custom Instrumentation

### Traces

Spring Boot auto-creates spans for every incoming HTTP request. Spring AI adds spans around each LLM call automatically. For custom business spans, use `Observation` directly:

**Programmatic span — `StructuredOutputsController.java`**

```java
// Creates a span named "structured_outputs" wrapping the LLM call
Observation.createNotStarted("structured_outputs", observationRegistry)
        .observe(() -> {
            log.info("userInput message : {} ", userInput);
            var responseSpec = chatClient.prompt(promptMessage).call();
            return responseSpec.content();
        });
```

`Observation` is the Micrometer abstraction. When the OTel bridge is on the classpath it automatically produces an OTel span — no OTel API imports required.

**Declarative span — `@Observed`**

```java
@PostMapping("/v1/structured_outputs/fewshot")
@Observed(name = "fewshot.count", contextualName = "Structured Outputs Few Shot")
public String structuredOutputsFewShot(@RequestBody @Valid UserInput userInput) { ... }
```

`@Observed` wraps the entire method in a span. Requires the AOP starter (`spring-boot-starter-aop`) to be on the classpath.

---

### Metrics

Micrometer automatically exports JVM, HTTP server, and Spring AI token-usage metrics. The `@Observed` annotation and `Observation` API also produce a timer metric (count + duration histogram) alongside the trace span — one API, two signals.

**Auto-exported metrics include:**

| Metric | Description |
|---|---|
| `http.server.requests` | Request count, latency per endpoint |
| `jvm.memory.used` | Heap and non-heap usage |
| `gen_ai.client.token.usage` | Prompt and completion token counts per LLM call |
| `fewshot.count` | Custom timer from `@Observed` |
| `structured_outputs` | Custom timer from `Observation.createNotStarted` |

---

### Logs

Logs are correlated with traces via the OTel Logback appender — the active `traceId` and `spanId` are injected into every log record automatically.

**`logback-spring.xml`** — routes all logs through the OTel appender:

```xml
<appender name="OTEL"
    class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender"/>

<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="OTEL"/>     <!-- ships logs to Loki via OTLP -->
</root>
```

**`InstallOpenTelemetryAppender.java`** — wires the Spring-managed `OpenTelemetry` SDK instance into the Logback appender at startup:

```java
@Component
class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}
```

This step is necessary because Logback initialises before the Spring context — the appender exists but has no SDK reference until `afterPropertiesSet` runs.

---

## Grafana Dashboard

This setup uses the **Spring Boot Observability** dashboard (ID `17175`), published by the Spring team and built specifically for the Loki + Tempo + Prometheus/Mimir stack.

**To import:**
1. Open Grafana at `http://localhost:3000`
2. Go to **Dashboards → Import**
3. Enter ID `17175` and click **Load**
4. Map the data sources to Prometheus, Loki, and Tempo
5. Click **Import**

**What it gives you out of the box:**

| Panel | Signal | Description |
|---|---|---|
| Request Rate | Metrics | Requests per second per endpoint |
| Error Rate | Metrics | 4xx / 5xx breakdown |
| Request Duration | Metrics | P50 / P95 / P99 latency per endpoint |
| JVM Memory | Metrics | Heap and non-heap usage over time |
| Traces | Traces | Drill into individual request traces in Tempo |
| Logs | Logs | Correlated log lines from Loki, filtered by `traceId` |

---

## Summary

| Signal | How it gets there | Where to view |
|---|---|---|
| **Traces** | Auto (HTTP + Spring AI) + `@Observed` / `Observation` API | Grafana → Tempo |
| **Metrics** | Auto (JVM + HTTP) + `@Observed` timer | Grafana → Prometheus/Mimir |
| **Logs** | Logback OTEL appender + `InstallOpenTelemetryAppender` | Grafana → Loki |

All three signals share the same `traceId`, making it easy to jump from a slow metric → the offending trace → the exact log line that explains why.
