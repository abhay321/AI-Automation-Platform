# Enterprise Logging Framework Architecture & Lifecycle
Version: 1.0 (Architecture Baseline)

This document establishes the official architectural blueprint, logging lifecycle pipelines, and extension points for the **AI Automation Platform Enterprise Logging Framework**.

---

## 1. Unified Logger Hierarchy

To maintain domain separation and type-safe logging behavior across the monorepo, the platform avoids generic multi-purpose loggers. Instead, we implement a specialized **Logger Hierarchy** that inherits common behavior (context propagation, severity routing, masking) but exposes specialized metadata-driven DSL operations:

```
                  ┌───────────────────────┐
                  │    PlatformLogger     │ (Base Interface)
                  └───────────┬───────────┘
                              │
       ┌──────────────────────┼──────────────────────┐
┌──────v──────┐        ┌──────v──────┐        ┌──────v──────┐
│  AppLogger  │        │ AuditLogger │        │ SecLogger   │
└─────────────┘        └─────────────┘        └─────────────┘
       │                      │                      │
┌──────v──────┐        ┌──────v──────┐        ┌──────v──────┐
│ PerfLogger  │        │ EventLogger │        │ AIReqLogger │
└─────────────┘        └─────────────┘        └─────────────┘
       │                      │                      │
┌──────v──────┐        ┌──────v──────┐        ┌──────v──────┐
│ WorkflowLog │        │  PluginLog  │        │ ConnectLog  │
└─────────────┘        └─────────────┘        └─────────────┘
```

### Specialized Logger Definitions
1. **PlatformLogger (Core API)**: Base contract managing standard level filters (`TRACE` to `FATAL`), SLF4J mapping, dynamic level adjustment, and structured DSL builders.
2. **ApplicationLogger**: Standard backend logging for Ktor controllers, dependency injection wiring, and microservice status.
3. **AuditLogger**: Rigid, immutable, append-only logger capturing governance events (*Who, What, When, Where, Why, Result*). Designed with built-in hash chaining block verification to detect tamper events.
4. **SecurityLogger**: Specialized high-integrity logger tracking authorization checks, authentication flows, rate limiters, token validation errors, and cryptographic activities.
5. **PerformanceLogger**: Built-in support for nested timers, hierarchical spans, long-running operation warnings, and slow database query flags.
6. **EventLogger**: Specialized state tracking designed to format events for future event sourcing engines, capturing Event Name, Event Type, and schema versions.
7. **AIRequestLogger**: Logs AI token transactions, model input/output configurations, prompt templates, and generation latency without leaking customer PII.
8. **WorkflowLogger / PluginLogger / ConnectorLogger**: Domain-scoped logging used within isolation zones, securing workspace boundaries and trace context mappings.

---

## 2. The Log Event Pipeline

Every log message moves through an orderly, synchronous-to-asynchronous processing pipeline to guarantee maximum safety, performance, and formatting correctness.

```
Log Call
   │
   ▼
1. Context Enrichment ────► Captures thread-local / Coroutine Context (Trace ID, Execution ID)
   │
   ▼
2. Validation ────────────► Rejects schema infractions / Missing event version formats
   │
   ▼
3. Masking Engine ────────► Identifies & replaces PII and credentials using regular expressions
   │
   ▼
4. Log Sampling ──────────► Reduces high-frequency tracing noise under extreme transactional loads
   │
   ▼
5. Formatting ────────────► Translates LogEvent to Console Pretty Format or Compact JSON
   │
   ▼
6. Routing ───────────────► Resolves target appenders (Console, Local File, Audit Stream)
   │
   ▼
7. Async Bounded Buffer ──► Places event in non-blocking ring buffer / queue
   │
   ▼
8. Batch Appender ────────► Pulls events in batches (e.g. 50 events) to perform bulk I/O writes
   │
   ▼
9. Exporter ──────────────► Ships batches to external systems (Loki, OpenSearch, Splunk)
```

### Pipeline Phase Detail
* **Context Enrichment**: Attaches active OpenTelemetry-compatible traces and correlation contexts (derived from HTTP headers, background schedulers, or message brokers).
* **Validation**: Ensures system events contain a valid event name, schema version, and platform version.
* **Masking**: Sanitizes passwords, secrets, token parameters, credit cards, emails, and IP addresses prior to serialization.
* **Sampling**: Implements probabilistic sampling (e.g., logging only 10% of high-frequency DEBUG events) while keeping 100% of WARN, ERROR, FATAL, SECURITY, and AUDIT logs.
* **Async Buffer & Back-Pressure**: Operates with a bounded queue. In the event of a queue overflow (e.g. log output exceeds disk write speeds), the policy handles degradation:
  * **DROP_DEBUG_TRACE**: Drops non-essential logs first.
  * **BLOCK_PRODUCER**: Blocks caller threads to protect system memory.
  * **SHUTDOWN_FLUSH**: Ensures outstanding buffer logs write cleanly to appenders upon container sigterms.

---

## 3. Log Routing & Appenders

Logs can be split and routed dynamically to multiple targets based on severity, category, and audit requirements:

| Appender Target | Intended Scope | Log Format | Target System |
| :--- | :--- | :--- | :--- |
| **Console** | Local development, standard container stdout. | Colorized, indented, human-readable | Terminal, IDE, Docker Console |
| **System File** | Persistent local system logs. | Compact JSON with schema versions | `/var/log/platform/app.json` |
| **Audit Stream** | Tamper-resistant compliance logging. | Immutable, hash-verified, append-only JSON | Secure file / Audit log collector |
| **Metrics Registry** | Counter and gauge indicators. | Numeric increments, latency histograms | Prometheus, Grafana |
| **Cloud Exporter** | Centralized log ingestion clusters. | Batched JSON over HTTP/gRPC | Loki, OpenSearch, Splunk |

---

## 4. Log Schema Versioning

To ensure backward compatibility when external log routers index or parse historical records, every structured log output maintains consistent schema headers:

* `platformVersion`: Core version of the platform runtime engine (e.g. `1.0.0`).
* `schemaVersion`: Version of the logging layout envelope (e.g. `1.0`).
* `eventVersion`: Version of the specific structured event type (useful when refactoring workflow start events).
* `pluginVersion`: Declares version of the plugin originating the log.

---

## 5. Extension Points

The logging platform is open for extension, allowing plugins to customize formatting and routing policies dynamically:

1. **CustomLogEnricher**: Implement this interface to inject proprietary tenant IDs, cluster host locations, or custom environment metadata into every log frame.
2. **LogMaskingRule**: Register custom regex expressions or string patterns to detect and mask specialized proprietary token layouts or medical/financial PII.
3. **LogExporter**: Standard interface to create custom destination connectors (e.g., streaming logs to a secure internal Kafka partition or custom API endpoint).
4. **LogRoutingRule**: Controls which categories or severity bounds direct to customized file appenders.

---

## 6. Retention Policies & Security Standards

To meet compliance guidelines (SOC2, HIPAA, GDPR), log storage durations and formats are separated:

* **Application logs (INFO/DEBUG)**: Retained for 7 to 30 days. Prioritizes fast rotation.
* **Audit Logs**: Retained for 1 to 7 years. Must be append-only, tamper-evident, and cryptographically verified.
* **Security Logs**: Retained for 1 year. Critical alert triggers configured on authorization failures.
* **Workflow Runtime Logs**: Retained for 90 days. Directly viewable within the Desktop Control Center dashboard.
