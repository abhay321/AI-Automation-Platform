# ADR 0020: Adopt Structured JSON Logging & Observability Standards

## Status
Accepted

## Date
2026-07-19

## Context
When running continuous, non-deterministic AI automations, debugging failures in production is incredibly difficult. If a node fails, we must trace exactly which workspace, workflow execution, user request, and transaction triggered the issue. 

Traditional raw flat-text logs (e.g., `10:15:30 [INFO] User logged in`) are useless under heavy concurrent loads because logs from hundreds of interleaved executions blend together. Furthermore, standard cloud log routers (like Datadog, Elastic, Google Cloud Logging, or Grafana Loki) cannot index flat-text logs efficiently.

We need an enterprise-grade, structured logging strategy that outputs machine-readable JSON, tracks end-to-end trace IDs, and maps custom operational fields automatically.

## Problem Statement
How should the platform format and coordinate logs, trace IDs, and performance metrics across asynchronous threads and coroutines to guarantee observability?

## Decision
Adopt **Structured JSON Logging** utilizing **SLF4J** and **Logback**, and integrate **MDC (Mapped Diagnostic Context)** to track distributed transaction IDs. 

Every single log entry will be formatted as a structured JSON object containing:
- `timestamp`: Standard ISO-8601 representation.
- `severity`: Standard level (INFO, WARN, ERROR, DEBUG).
- `service` / `module`: Identifies which platform submodule generated the log.
- `thread`: JVM thread ID.
- `traceId` / `correlationId`: Unified UUID matching the triggering HTTP request or cron trigger.
- `workspaceId` / `executionId` (optional): Rich metadata connecting the log directly to active execution runs.

For metrics, we will standardize on **Micrometer Core**, exporting standard counters, timers, and histograms to **Prometheus** endpoints.

## Alternatives Considered
- **Standard Flat-Text Console Logs**: Readable for local developers, but impossible for production log routers to parse, index, and alert on.
- **Direct SaaS Logging Clients (Datadog/NewRelic SDK directly)**: Creates unneeded proprietary dependencies and violates our **Offline-First** core design tenets.

## Advantages
- **Instant Searchability**: Log routers can index structured JSON fields natively, permitting instant queries like: "Show all logs of level ERROR where executionId = X".
- **Coroutine Trace Tracking**: By utilizing specialized Coroutine Context interceptors, MDC properties (like `traceId`) flow seamlessly across asynchronous thread hops during Coroutine context switches.
- **Performance metrics out-of-the-box**: Standardized Prometheus metrics make it simple to monitor platform throughput and queue lengths inside Grafana.

## Disadvantages
- **Local Dev Readability**: JSON logs can be hard for human eyes to parse inside local terminals. (Mitigated by configuring Logback to output beautiful, colorized flat-text inside the `dev` profile, while outputting pure JSON in `prod` profiles).

## Consequences
- We configure a standard Logback setup (`logback.xml`) utilizing Jackson serializers inside the `platform` module.
- We establish a `traceId` extraction interceptor on all Ktor REST/WebSocket routes.

## Risks
- Developers might manually concatenate strings inside log messages. We enforce the use of SLF4J parameterized logging templates (e.g. `logger.info("Executing node {}", nodeId)`) to preserve memory and keep formatting clean.

## Migration Strategy
N/A - Direct integration in Module 2.

## Future Considerations
Expose structured logs directly to the visual Log Panel inside the Desktop Control Center via real-time WebSocket telemetry channels.

## Related ADRs
- [ADR 0005: Use Ktor as the Server Framework Over Spring Boot](./0005-ktor-over-spring.md)
- [ADR 0018: Provide Profile-Based Docker Compose for Local Dev](./0018-docker-compose-development.md)

## References
- [Logback JSON Encoder Configuration](https://github.com/logfellow/logstash-logback-encoder)
- [MDCCoroutineContext - Coroutine MDC Integration](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-slf4j/)
