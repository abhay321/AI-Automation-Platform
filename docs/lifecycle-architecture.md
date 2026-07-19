# Runtime Lifecycle Framework Architecture & Specification
Version: 1.0 (Runtime Lifecycle Baseline)

This document defines the architecture, lifecycle state transitions, package topologies, module dependency specifications, and extension points for the **AI Automation Platform Runtime Lifecycle Framework**. 

This system acts as the master conductor responsible for booting, managing, and gracefully terminating the multi-module platform runtime, plugins, background schedulers, network servers, and data pools in a predictable, order-resolved sequence.

---

## 1. Package Structure

The platform runtime encapsulates the boot orchestration and lifecycle management systems in a dedicated, high-integrity package namespace:

```
platform/
└── lifecycle/
    ├── api/                 # Core lifecycle interfaces, state enums, and annotations
    ├── engine/              # Bootstrapper, Dependency DAG sorter, State Machine
    ├── registry/            # Centralized Module and Extension registration storage
    └── shutdown/            # Graceful termination coordinators, Thread pool draining, JVM hooks
```

---

## 2. Core Architectural Concepts

To scale predictably as a robust enterprise application, the platform requires a centralized coordinator that governs how services initialize, run, and terminate. Rather than letting subsystems self-initialize or rely on implicit classloading side-effects, the **Runtime Lifecycle Framework** introduces:

1. **State Machine Authority**: A strict, irreversible state flow monitored by health endpoints and platform diagnostics.
2. **Topological Dependency Ordering**: Modules declare explicit dependency requirements. The engine builds a Directed Acyclic Graph (DAG) and executes phase hooks in the mathematically correct order.
3. **Graceful Connection Draining**: Strict timeout guarantees that finish outstanding client requests, empty persistent queues, and flush buffers before dropping network sockets.

---

## 3. Visual State Transition Flow

The following diagram outlines the strict state machine of the Runtime Lifecycle Framework:

```
                 [ OFFLINE ]
                      │
                      ▼
               1. BOOTSTRAP (Load Config, Init Logging)
                      │
                      ▼
               2. INITIALIZATION (DAG Sorting, Build Pools)
                      │
                      ▼
               3. STARTUP (Bind Sockets, Enable Schedulers)
                      │
                      ▼
                 [ RUNNING ] ◄─── (Health Check: Liveness & Readiness = TRUE)
                      │
             ┌────────┴────────┐
             │ (SIGTERM/Error) │
             ▼                 ▼
   4. SHUTTING_DOWN        [ FAILED ] (Diagnostics Dump)
   (Draining Sockets)          │
             │                 ▼
             ▼            [ OFFLINE ]
   5. CLEANUP
   (Close DB/Buffers)
             │
             ▼
        [ TERMINATED ] (JVM Exit)
```

---

## 4. Lifecycle Phases & Specifications

The framework transitions sequentially through five main active execution phases:

| Phase | Intended Actions | Permitted Work | Failure Action |
| :--- | :--- | :--- | :--- |
| **BOOTSTRAP** | Prepare fundamental capabilities to run the platform. | Load `PlatformConfig`, configure environment parameters, initialize core logging, mount secrets volume, initialize encryption keys. | Abort immediately (Non-zero exit) |
| **INITIALIZATION** | Build dependencies and allocate backend resource pools. | Construct HikariCP connection pools, prepare Redis client sockets, initialize MinIO workspace directories, resolve plugin classloaders, perform database schema migrations, compile Dependency DAG. | Fail-Fast startup validation exception |
| **STARTUP** | Open network boundaries and activate processing. | Bind HTTP server socket to host/port, activate RabbitMQ listener pools, enable cron schedulers, register WebSocket routers, signal readiness to cluster routers. | Transition to SHUTTING_DOWN automatically |
| **RUNNING** | Active operational state. | Health checker answers `Readiness = TRUE` & `Liveness = TRUE`. Core transaction loops processing, plugin sandboxes active. | Monitor exceptions; flag health check degradation |
| **SHUTTING_DOWN** | Active client draining and graceful socket close. | Signal `Readiness = FALSE` to load balancers. Wait for active HTTP transactions to complete (connection draining), stop polling event streams, reject incoming commands. | Complete within timeout bounds (e.g. 15s) |
| **CLEANUP** | Resource deallocation. | Drain bounded async log buffers, close database connections, release file-system locks, terminate executor thread pools, unregister cluster nodes. | Force kill remaining threads, final flush, exit JVM |

---

## 5. Core Interfaces & API Specification

### 5.1. LifecycleState Enum
Defines the official states of the executing application.

```kotlin
package com.aiplatform.platform.lifecycle.api

enum class LifecycleState {
    OFFLINE,
    BOOTSTRAPPING,
    INITIALIZING,
    STARTING,
    RUNNING,
    SHUTTING_DOWN,
    CLEANING_UP,
    TERMINATED,
    FAILED
}
```

### 5.2. LifecycleAware Contract
Any module, service, or plugin that requires active hook notifications during startup and shutdown must implement this contract.

```kotlin
package com.aiplatform.platform.lifecycle.api

import kotlin.reflect.KClass

interface LifecycleAware {
    /**
     * Unique identifier for this lifecycle component (e.g. "DatabaseService").
     */
    val id: String

    /**
     * Declares explicit dependencies that must be fully initialized *before* this component starts up.
     */
    val dependencies: List<String> get() = emptyList()

    /**
     * Phase Executions
     */
    fun onBootstrap() {}
    fun onInitialize() {}
    fun onStartup() {}
    fun onShutdown() {}
    fun onCleanup() {}
}
```

### 5.3. Module Annotation
Provides declarative metadata for platform modules, simplifying registration.

```kotlin
package com.aiplatform.platform.lifecycle.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PlatformModule(
    val id: String,
    val dependsOn: Array<String> = [],
    val isRequired: Boolean = true,
    val description: String = ""
)
```

### 5.4. LifecycleManager Engine Contract
Controls module execution order and state changes.

```kotlin
package com.aiplatform.platform.lifecycle.api

import java.time.Duration

interface LifecycleManager {
    val currentState: LifecycleState
    
    /**
     * Registers a modular component into the dependency graph.
     */
    fun register(component: LifecycleAware)

    /**
     * Boots the complete platform runtime through BOOTSTRAP, INITIALIZE, and STARTUP phases.
     */
    fun bootstrapAndRun()

    /**
     * Gracefully terminates the running engine within the allotted grace period timeout.
     */
    fun initiateShutdown(gracePeriod: Duration = Duration.ofSeconds(30))
}
```

---

## 6. Dependency Topological Sorting (DAG Specification)

To prevent circular dependencies and guarantee that a service (such as `WorkflowEngine`) never boots before its database connectivity (`DatabaseService`) is verified, the framework includes a strict Directed Acyclic Graph (DAG) sorting algorithm.

### Sorting Algorithm Specification
1. **Adjacency Map Construction**: Build a node map where nodes are `LifecycleAware` objects, and edges represent `dependencies`.
2. **Cycle Detection**: Perform depth-first search (DFS) with node-coloring (White, Gray, Black) to detect circular dependencies (e.g., Module A depends on B, which depends on A). If a cycle is detected, fail immediately during `INITIALIZE` with a clear path diagram.
3. **Topological Order Compilation**: Compile nodes into a linear execution list where for every directed edge $U \rightarrow V$, node $U$ appears before $V$ in the list.
4. **Phase Traversal**: During Startup (`onInitialize`, `onStartup`), components are visited in **Topological Order**. During Shutdown (`onShutdown`, `onCleanup`), components are visited in the **Reverse Topological Order**.

---

## 7. Graceful Shutdown & Back-pressure Specification

Shutdown reliability is a critical differentiator for production-grade platforms. The framework enforces a strict **Connection Draining and Cleanup Protocol**:

1. **Isolation Transition**: Upon receiving SIGTERM, `ReadinessCheck` is immediately flipped to `FALSE`. Downstream proxies (e.g., Kubernetes, Nginx) have a short window (e.g., 2 seconds) to stop forwarding new HTTP queries.
2. **Traffic Draining**: Keep-Alive client TCP connections are set to close. Active HTTP threads are allowed to finish ongoing transactions.
3. **Queue Suspension**: RabbitMQ, Kafka, and background cron schedules stop polling for new tasks. Existing background tasks in the local processing queue are allowed to execute to completion up to a hard timeout limit.
4. **Thread Pool Deallocation**: Execution executors are commanded to shut down, waiting on `awaitTermination`.
5. **Data Isolation**: Finally, file locks are clean-released, transactions are rolled back/committed, and database pool links are cleanly terminated.

---

## 8. Extension Points for Plugins

The lifecycle engine provides secure hook options for external plugins to integrate with the core lifecycle sequence:

1. **LifecycleListener**: Third-party observers can monitor state changes (e.g., alerting when transition to `FAILED` occurs).
2. **PluginRegistry Hook**: Allows plugins to hook into the `onStartup` phase of the platform, enabling them to load custom web controllers or schedule background maintenance before the platform transitions to `RUNNING`.
3. **ShutdownHook**: Permits customized cleanup tasks to register dynamically during runtime operations.

---

## 9. Architectural Decision Record (ADR)

### ADR 0023: Establish topological DAG dependency ordering and graceful shutdown bounds

#### Status
Accepted

#### Date
2026-07-19

#### Context
In complex multi-module systems, implicit or ad-hoc initialization sequencing causes major instability at boot time, leading to `NullPointerException`s, race conditions, or uncoordinated database connections. Similarly, abrupt termination on SIGTERM routinely corrupts in-flight user database transactions, breaks queue message state consistency, and leaves files locked.

#### Decision
Implement a formalized, annotation-supported `LifecycleAware` architecture powered by a deterministic, topological DAG sorting mechanism. Forbid direct initialization during constructor calls. Force all modules to declare dependencies explicitly. Transition the application cleanly through defined, time-limited phases (`BOOTSTRAP` $\rightarrow$ `INITIALIZE` $\rightarrow$ `STARTUP` $\rightarrow$ `RUNNING` $\rightarrow$ `SHUTTING_DOWN` $\rightarrow$ `CLEANUP`).

#### Consequences
- Circular dependencies will prevent application startup entirely, failing fast with a detailed error diagram.
- Subsystems must cleanly separate constructor setup (fast, allocation-free) from initialization/runtime triggers.
- Unit and integration tests can isolate systems easily by executing only a subset of the lifecycle phases.
