# Platform Architectural Decision Records (ADR)
Version: 1.0 (ADR Registry - Frozen)

This document aggregates the foundational Architectural Decision Records (ADRs) that govern the development, runtime boundaries, and interfaces of the platform.

---

## ADR 0022: Establish a Unified Observability Platform
### Status: Accepted
### Date: 2026-07-19

#### Context
Traditional Java/Kotlin backends isolate logging libraries (Logback/SLF4J), metrics registries (Micrometer), and distributed tracing exporters (OpenTelemetry) into separate, redundant code structures. This introduces massive CPU/memory overhead, requires duplicate data mapping configurations, and results in mismatched trace identifiers, where logs, metrics, and traces use different, non-correlated keys.

#### Decision
Combine Logging, Metrics, Tracing, Audit compliance, and Diagnostics into a single, unified **Observability Platform** managed under a central `ObservabilityContext` propagation engine. All observability channels utilize a single `UnifiedObservabilityEvent` schema, guaranteeing perfect cross-system searchability and trace tracking.

#### Consequences
- The logging framework is implemented as the central processing pipeline of this Observability Platform.
- Future metrics, distributed tracing, and health checking metrics must extend this system directly.
- Trace, Request, and Correlation IDs automatically propagate across all asynchronous thread and coroutine hops.

---

## ADR 0023: Establish Topological DAG Dependency Ordering & Graceful Shutdown Bounds
### Status: Accepted
### Date: 2026-07-19

#### Context
In complex multi-module enterprise systems, implicit or ad-hoc initialization sequencing causes severe instability during system boot. This leads to `NullPointerException`s, race conditions, or uncoordinated database initialization. Similarly, abrupt process termination under SIGTERM signals corrupts in-flight database transactions, breaks queue message state consistency, and leaves files locked.

#### Decision
Implement a formalized, annotation-supported `LifecycleAware` module architecture powered by a deterministic, topological DAG sorting mechanism. Forbid direct initialization inside constructors. All modules must explicitly declare dependencies. Establish a strict, time-bounded shutdown sequence (`SHUTTING_DOWN` $\rightarrow$ `CLEANUP`) to safely drain active transactions and flush queues before process exit.

#### Consequences
- Circular dependencies will prevent application startup entirely, failing fast with a detailed error diagram.
- Modules must cleanly separate constructor setup (fast, allocation-free) from active initialization and startup execution blocks.
- Transitioning to clusters is simplified by binding state checks directly to the Kubernetes liveness/readiness API specs.

---

## ADR 0024: Consolidate Platform into a Unified Runtime Kernel
### Status: Accepted
### Date: 2026-07-19

#### Context
As the AI Automation Platform scales, multiple interconnected systems (Configurations, Observability, Event Buses, Schedulers, and Sandbox plugins) must load, run, and close in coordination. Ad-hoc lifecycle management leads to race conditions, memory leaks, and incomplete service discovery.

#### Decision
Evolve the platform lifecycle framework into a dedicated **Runtime Kernel** that serves as the supreme bootstrapper and execution conductor for all platform layers. The Kernel controls application boot phases, processes module DAGs, implements strict failure recovery policies, and manages plugin lifecycle boundaries.

#### Consequences
- The Kernel is the absolute boot authority.
- All core capabilities must register as `LifecycleAware` modules to participate in the boot phases.
- Cloud scaling and rolling restarts can be introduced cleanly by implementing the specified Cluster Readiness contracts.

---

## ADR 0025: Coroutine-Native, Type-Safe Event Bus with Transactional Outbox
### Status: Accepted
### Date: 2026-07-19

#### Context
An asynchronous automation engine must react to state updates without blocking execution. Traditional event publishers lack Kotlin Coroutine suspension integration, introduce rigid, heavy framework dependencies, or fail to propagate tracing contexts across worker pools. Furthermore, publishing events *before* local database transactions have committed leads to severe data corruption if the database subsequently rolls back.

#### Decision
Design and implement a lightweight, zero-dependency, coroutine-native `EventBus` that natively carries tracing context headers. Leverage Kotlin's `SharedFlow` and thread-safe channels for non-blocking publishing and subscription. Transactional consistency is secured using the Outbox pattern, where events are committed to a database outbox table first and released only after successful transaction commits.

#### Consequences
- Subsystems remain completely decoupled; they publish events onto the bus without knowing who consumes them.
- Subscriptions are managed as cancelable handles, preventing memory leaks when plugins load or unload dynamically.
- Tracing is preserved across asynchronous thread dispatches, as trace headers are carried over inside the `EventEnvelope`.
- Distributed backends (Kafka/RabbitMQ) can be plugged in transparently using the `EventBridge` interface.
