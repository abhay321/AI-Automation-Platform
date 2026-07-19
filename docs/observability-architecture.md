# Observability Platform Architecture & Blueprint
Version: 1.0 (Enterprise Observability Platform Baseline)

This document establishes the official architectural blueprint, package layout, unified data schemas, and integration pipelines for the **Unified Observability Platform** of our AI Automation Platform. 

Logging, Metrics, Distributed Tracing, Health Monitoring, Auditing, Diagnostics, Telemetry, Analytics, and future AI Monitoring are unified under this single, high-performance, non-blocking platform.

---

## 1. Package Structure

The platform runtime encapsulates all observability and instrumentation packages inside a dedicated module:

```
platform/
└── observability/
    ├── core/                # Core pipeline, context engine, common event contracts
    ├── logging/             # Structured & Async Logging (Pretty Console, compact JSON)
    ├── metrics/             # Micrometer integration, Prometheus scrapers, custom gauge bindings
    ├── tracing/             # Distributed Trace spans, OpenTelemetry-compatible context propagation
    ├── health/              # Custom health check indicator registry (DB, Redis, Disk, MQ)
    ├── audit/               # Immutable, cryptographically chained, append-only logs
    ├── diagnostics/         # Live system configuration checks and setup validation
    ├── telemetry/           # Outbound collectors, exporters, and event bridges
    └── analytics/           # Internal event stream analytics, user session tracking
```

---

## 2. Unified Observability Context

Instead of maintaining duplicate context engines for trace telemetry, logging context, and metric tags, all systems read from the central **`ObservabilityContext`**:

| Context Attribute | Key | System Propagation | Intended Use Case |
| :--- | :--- | :--- | :--- |
| **TraceId** | `traceId` | Tracing, Logging, Audit, API | Cross-system request correlation |
| **SpanId** | `spanId` | Tracing, Perf Metrics, Logging | Scope of a single nested execution block |
| **CorrelationId** | `correlationId` | All Telemetry Channels | End-to-end messaging context |
| **RequestId** | `requestId` | Logging, HTTP Router | Tracks unique inbound client queries |
| **ExecutionId** | `executionId` | All Telemetry Channels | Isolates runs of specific DAG workflows |
| **WorkflowId** | `workflowId` | Logging, Metrics, Analytics | Ties telemetry to a specific automation map |
| **AgentId** | `agentId` | Logging, AI Monitoring | Links prompts/tokens to cognitive loops |
| **PluginId** | `pluginId` | Logging, Sandboxing | Tracks plugin memory and execution footprints |
| **WorkspaceId** | `workspaceId` | All Telemetry Channels | Restricts logs and metrics to secure tenant scopes |
| **UserId** | `userId` | Audit, Analytics | Captures the actor executing the request |
| **SessionId** | `sessionId` | Logging, Analytics | Groups interactions across desktop views |
| **NodeId** | `nodeId` | Tracing, Logging, Metrics | Identifies active executing step within a DAG |
| **Region** | `region` | Analytics, Metrics | Tracks physical host execution cluster |
| **ApplicationVersion**| `appVersion` | All Telemetry Channels | Detects errors associated with version updates |

---

## 3. Unified Observability Event Schema

To support event sourcing, deep logging search, telemetry routing, and audit preservation without duplicate models, all subsystems serialize events into the **`UnifiedObservabilityEvent`**:

```json
{
  "timestamp": "2026-07-19T15:50:00.000Z",
  "schemaVersion": "1.0",
  "platformVersion": "1.0.0",
  "severity": "INFO",
  "category": "Workflow",
  "eventName": "workflow.execution.started",
  "eventVersion": 1,
  "eventType": "Audit", 
  "message": "Workflow 'Backup Database' execution triggered",
  "context": {
    "traceId": "tr-8ef3a921",
    "spanId": "span-f92a10",
    "correlationId": "corr-41fa1a2",
    "workspaceId": "ws-production",
    "workflowId": "wf-backup-db",
    "executionId": "exec-42",
    "userId": "user-admin-1"
  },
  "metrics": {
    "execution_duration_ms": 12.5,
    "nodes_count": 8
  },
  "metadata": {
    "triggeredBy": "Scheduler",
    "cronExpression": "0 0 * * *"
  },
  "exception": null,
  "security": {
    "hash": "ae27fc091c... (Audit events only)",
    "previousHash": "fb08ad21a... (Audit events only)"
  }
}
```

---

## 4. Unified Observability Pipeline

Logging, Metrics, Tracing, and Audits share the same high-throughput, non-blocking pipeline:

```
Telemetry Trigger (Log/Metric/Span/Audit)
                 │
                 ▼
     ┌───────────────────────┐
     │ 1. Context Extraction │  ◄── Reads traceId, executionId, etc from active scope
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 2. Enrichment         │  ◄── Adds system hostName, platformVersion, and custom tags
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 3. Validation         │  ◄── Enforces presence of eventName, eventVersion, and traceId
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 4. Masking Engine     │  ◄── Strips keys, PII, passwords, JWT, cards, emails, IPs
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 5. Sampling           │  ◄── Probabilistic check on TRACE/DEBUG; ignores CRITICAL/AUDIT
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 6. Classification     │  ◄── Flags event as "System Log", "Audit Record", "Metric Tick"
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 7. Routing            │  ◄── Selects destination appenders (Console, Local File, Prometheus)
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 8. Non-Blocking Buffer│  ◄── Passes event into a bounded queue (RingBuffer)
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 9. Storage / Write    │  ◄── Flushes batch to local file system / active database
     └───────────┬───────────┘
                 ▼
     ┌───────────────────────┐
     │ 10. Exporter          │  ◄── Shakes out to OpenTelemetry, Loki, ELK, Prometheus Scrapers
     └───────────────────────┘
```

---

## 5. Plugin Extensibility

Third-party integrations can inject custom observability logic securely using defined plugin contracts:

1. **TelemetryProvider**: Registers custom instrumentation engines (e.g., exposing custom performance diagnostics for specific operating systems).
2. **LogEnricher**: Appends custom environment tags (such as Kubernetes Pod ID or private company metadata) to every unified event.
3. **MetricCollector**: Binds custom gauges or counter pipelines to the platform's core Micrometer engine.
4. **TraceExporter**: Directs telemetry trace streams to custom third-party clouds or proprietary on-premises monitoring backends.
5. **HealthContributor**: Registers custom active indicators to evaluate downstream service health (e.g., verifying custom database links are live).
6. **AuditProvider**: Captures and routes audit compliance logs to physical hardware security modules (HSM) or secure cloud storage.

---

## 6. Future AI Observability & Monitoring

The platform architecture is fully prepared to integrate advanced AI Monitoring metrics, isolating specific execution characteristics under high-frequency workflows:

* **Prompt Execution Tracking**: Logs semantic structures, system prompt alterations, and template variables.
* **Token Usage Metrics**: Exposes counters tracking input, output, and reasoning tokens to calculate transactional cost profiles.
* **Model Selection Audits**: Monitors router performance, fallback actions, and provider response speeds.
* **Cost Estimation Telemetry**: Emits standard pricing events per execution run to prevent budget overruns.
* **Embedding Latency**: Logs vector generation times and dimensions.
* **Reasoning Span Mapping**: Spans track deep thinking loops natively, separating thoughts from final string outputs.

---

## 7. Observability Architecture Decision Record (ADR)

### ADR 0022: Establish a Unified Observability Platform

#### Status
Accepted

#### Date
2026-07-19

#### Context
Traditional systems isolate logging libraries (Logback), metric libraries (Micrometer), and tracing engines (OpenTelemetry) into separate, redundant code paths. This introduces massive CPU/memory overhead, leads to duplicate mapping structures, and results in mismatched trace configurations where logs, metrics, and traces use different, non-correlated identifiers. 

#### Decision
Combine Logging, Metrics, Tracing, Audit, and Diagnostics into a single unified **Observability Platform**. All systems will use the same trace correlation identifiers, share an identical context propagation engine, route events through a unified masking pipeline, and use structured Event types to guarantee perfect cross-system searchability.

#### Consequences
- The logging framework will be implemented as the core engine of this Observability Platform.
- Future metrics, distributed tracing, and health checking features will extend this architecture directly rather than introduce custom pipelines.
- Trace, Request, and Correlation IDs must propagate consistently across all async worker tasks.
