# Runtime Kernel Architecture & Specification
Version: 1.0 (Runtime Kernel Baseline - Frozen)

This document establishes the official architectural blueprint, boot sequence phases, dependency topology contracts, failure recovery policies, and distributed cluster readiness extension points for the **AI Automation Platform Runtime Kernel**. 

The Runtime Kernel is the foundational layer of the platform, transforming the lifecycle framework from a simple module orchestrator into an enterprise-grade execution chassis that hosts Configuration, Observability, Dependency Injection, Health checking, Schedulers, Resource pools, and the Plugin engine.

---

## 1. Monorepo Evolution & Directory Layout

To support scaling into a highly modular, reusable enterprise platform, the monorepo evolves from isolated frameworks into a layered ecosystem:

```
platform-runtime/           # Foundational execution layer
├── configuration/          # Type-safe Hoplite YAML config & secret masking
├── observability/          # Logging, Metrics, Tracing, Diagnostics
├── lifecycle/              # The Runtime Kernel (Bootstrapper, DAG, Engine)
├── dependency-injection/   # Internal lightweight service resolver
├── scheduler/              # Cron, background worker pools, task orchestrators
├── plugin-runtime/         # Isolation sandboxes & classloaders for extensions
├── event-bus/              # In-memory reactive context bus
└── health/                 # Liveness, Readiness, and Health contributor registries

workflow-engine/            # DAG workflow compile, check-point, & resume engine
├── dag/
├── execution/
└── state-machine/

ai-platform/                # AI capability engine
├── providers/              # Gemini, OpenAI, Claude, Local LLMs
├── prompts/                # Versioned prompt template registry
└── rag/                    # Embeddings, vector indices, Qdrant links

sdk/                        # Unified SDK boundaries
├── runtime-sdk/
├── plugin-sdk/
└── workflow-sdk/
```

---

## 2. Boot Phases & Orchestration Specification

The Runtime Kernel divides startup into highly granular, sequential phases. Each phase maintains a strict purpose, timeout constraints, health checks, and a rollback/degradation strategy:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                PRE_BOOT                                 │
│ Purpose: Basic environment checks, OS validations, CPU/Memory counts.   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              CONFIGURATION                              │
│ Purpose: Mount secrets volume, decrypt property keys, parse YAML.        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              OBSERVABILITY                              │
│ Purpose: Warm up log buffers, bind Micrometer registry, OpenTelemetry.   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          DEPENDENCY_INJECTION                           │
│ Purpose: Populate the IOC container, scan annotations, register beans.   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                             INFRASTRUCTURE                              │
│ Purpose: Connect database, pool links, ping Redis sockets, connect MQ.  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                SECURITY                                 │
│ Purpose: Load cryptographic providers, verify JWT signing secret.        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                 STORAGE                                 │
│ Purpose: Connect to MinIO/S3 endpoints, verify base buckets exist.      │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            PLUGIN DISCOVERY                             │
│ Purpose: Scan plugin directory, read manifests, verify versions.        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            PLUGIN VALIDATION                            │
│ Purpose: Validate JAR signatures, sandbox limits, capabilities, SDK.    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          PLUGIN INITIALIZATION                          │
│ Purpose: Create plugin classloaders, register custom controllers/hooks. │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            WORKFLOW RUNTIME                             │
│ Purpose: Initialize local DAG engine, restore checkpoints, warm-up JVM. │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                SCHEDULER                                │
│ Purpose: Resume cron tasks, load pending cluster trigger maps.         │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                   API                                   │
│ Purpose: Bind HTTP server sockets, open WebSocket routing endpoints.     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                  READY                                  │
│ Purpose: Liveness = TRUE, Readiness = TRUE. Begin serving live traffic. │
└─────────────────────────────────────────────────────────────────────────┘
```

### Detailed Boot Phase Specification Table

| Phase | Timeout (Sec) | Dependencies | Health Validation | Failure Rollback Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **PRE_BOOT** | 2 | None | OS file-descriptor limits, system RAM | Halt process immediately |
| **CONFIGURATION** | 5 | PRE_BOOT | Check if `PlatformConfig` contains valid properties | Fail-fast, print config diagnostics report |
| **OBSERVABILITY** | 5 | CONFIGURATION | Log appender diagnostic check, registry validation | Degrade to standard console fallback |
| **DEPENDENCY_INJECTION**| 5 | OBSERVABILITY | Ensure no circular dependencies are present in bean graph | Fail-fast, dump graph adjacency list |
| **INFRASTRUCTURE** | 15 | DEPENDENCY_INJECTION | Ping PostgreSQL, evaluate Hikari connection pool latency | Fail-fast or fallback to retry loop (database) |
| **SECURITY** | 3 | INFRASTRUCTURE | Cryptographic key strength evaluation (AES length >= 16) | Halt process (Security boundary block) |
| **STORAGE**| 10 | CONFIGURATION | Verify MinIO storage bucket access permissions | Degrade to local temporary directory storage |
| **PLUGIN_DISCOVERY** | 5 | DEPENDENCY_INJECTION | Manifest schema validation | Log warning, omit corrupted plugin manifests |
| **PLUGIN_VALIDATION** | 5 | PLUGIN_DISCOVERY | Security sandboxing verification | Reject and quarantine the invalid plugin |
| **PLUGIN_INITIALIZATION**| 10 | PLUGIN_VALIDATION | Classloader isolation verification | Quarantine plugin, load remaining validated plugins |
| **WORKFLOW_RUNTIME** | 10 | STORAGE, INFRASTRUCTURE | Verify workflow database tables and structures are ready | Halt startup if required, or disable execution engines |
| **SCHEDULER** | 5 | WORKFLOW_RUNTIME | Cron scheduling engine status check | Disable cron thread pool, run manually via REST |
| **API** | 10 | SCHEDULER | Test HTTP/HTTPS socket binding on requested host:port | Halt startup, release all bound sockets |
| **READY** | 1 | API | Liveness check = true, Readiness check = true | Signal degraded state to cluster load balancers |

---

## 3. Failure Recovery Matrix

When a platform module or plugin fails during initialization or running, the Kernel evaluates its **Failure Recovery Policy**:

| Trigger Scenario | Impacted Component | Default Policy | Mechanism & Action |
| :--- | :--- | :--- | :--- |
| **Port Collision** | HTTP API Router | **Fail Fast** | Immediately abort JVM startup. Port collision is unrecoverable without operator or cluster intervention. |
| **Database Link Down** | Primary DB Pool | **Retry with Timeout** | Attempt connection 5 times with exponential backoff (1s, 2s, 4s, 8s, 16s). If connection fails after 30s, abort. |
| **MinIO Offline** | Storage Provider | **Degraded Mode** | Log severity `ERROR`. Disable asset-upload capabilities. Enable read-only memory caches. Readiness is flagged as unhealthy. |
| **Redis Cache Down** | Cache / Locks | **Degraded Mode** | Fallback to local in-memory L1 cache (Caffeine). Log trace alerts. Performance logger marks lock latency increases. |
| **Plugin Classloading Error**| Custom Extension | **Skip Module** | Quarantine the bad plugin JAR. Log detailed diagnostics to system file. Keep core runtime running for other plugins. |
| **Cron Trigger Collision** | Local Scheduler | **Safe Mode** | Pause schedule loop. Prevent duplicate job runs. Send administrator notification email/webhook. |
| **Out of Memory Warning** | JVM Runtime Heap | **Maintenance Mode**| Set `Readiness = FALSE`. Signal cluster to drain requests from node. Trigger full garbage collection. Dump heap. |

---

## 4. State Machine Transition Rules

The Kernel maintains a strict, atomic state progression using volatile thread barriers to prevent concurrency races.

```
                  ┌──────────────────────────────┐
                  │           OFFLINE            │
                  └──────────────┬───────────────┘
                                 │ bootstrapAndRun()
                                 ▼
                  ┌──────────────────────────────┐
                  │        BOOTSTRAPPING         │
                  └──────────────┬───────────────┘
                                 │ Load Config & Logging
                                 ▼
                  ┌──────────────────────────────┐
                  │         INITIALIZING         │
                  └──────────────┬───────────────┘
                                 │ DAG Sorter & DI Beans
                                 ▼
                  ┌──────────────────────────────┐
                  │           STARTING           │
                  └──────────────┬───────────────┘
                                 │ Open API & MQ Sockets
                                 ▼
                  ┌──────────────────────────────┐
                  │           RUNNING            │◄──────┐ (Auto-Recovery/Safe mode)
                  └──────────────┬───────────────┘       │
                                 ├───────────────────────┘
                                 │ initiateShutdown()
                                 ▼
                  ┌──────────────────────────────┐
                  │        SHUTTING_DOWN         │ (Readiness = FALSE)
                  └──────────────┬───────────────┘
                                 │ Connection Draining
                                 ▼
                  ┌──────────────────────────────┐
                  │         CLEANING_UP          │
                  └──────────────┬───────────────┘
                                 │ Release Files, Close DB
                                 ▼
                  ┌──────────────────────────────┐
                  │          TERMINATED          │ (JVM Safe Exit)
                  └──────────────────────────────┘
```

---

## 5. Granular Resource Lifecycle Ownership

To prevent file descriptor leaks, memory bloat, and database transaction lockups, the Kernel enforces explicit allocation and cleanup ownership:

### 1. Database Pools (HikariCP)
* **Allocation**: Done in `INITIALIZING` phase. Initializes the requested minimum idle connections, verifying credentials and setting execution timeouts.
* **Cleanup**: Done in `CLEANING_UP` phase. The pool is called to close, draining remaining active JDBC connections and giving active transactions 5 seconds to commit or rollback.

### 2. Redis & Connection Sockets
* **Allocation**: Done in `INITIALIZING`. Pre-allocates cache pools.
* **Cleanup**: Done in `CLEANING_UP`. Shuts down the client connection thread pool.

### 3. RabbitMQ / Event Bus Channels
* **Allocation**: Done in `STARTING`. Binds channels to exchanges and queues, starts subscriber loop.
* **Cleanup**: Done in `SHUTTING_DOWN`. Closes client listener channels to stop accepting messages, then terminates connections in `CLEANING_UP`.

### 4. Thread Pools & Coroutines
* **Allocation**: Thread pools are managed under named JVM ThreadFactories (e.g., `platform-workflow-exec-*`). Coroutines execute within structured scopes (`SupervisorJob`).
* **Cleanup**: Executor services receive `shutdown()` in `SHUTTING_DOWN`, waiting 10 seconds for graceful execution. If uncompleted, `shutdownNow()` is called in `CLEANING_UP`.

---

## 6. Plugin Lifecycle Specification

The Plugin Engine tracks custom sandboxed plugins using a dedicated state machine:

```
[ DISCOVERED ]  ──► Manifest read successfully
      │
      ▼
[ VALIDATED ]   ──► Sandbox rules & digital signature verified
      │
      ▼
[   LOADED  ]   ──► Isolation Classloader constructed
      │
      ▼
[INITIALIZED]   ──► Dependencies resolved, configuration parameters bound
      │
      ▼
[  RUNNING  ]   ──► Active in event loops & REST routes
      │
   ┌──┴────────────────────────┐
   ▼                           ▼
[PAUSED] (Queue paused)   [STOPPING] (Draining) ──► [STOPPED] ──► [UNLOADED]
```

---

## 7. Distributed Cluster Readiness Contracts

To prepare the platform runtime for scaling to multi-node setups without introducing heavy dependencies yet, the Kernel defines zero-dependency distributed interfaces:

### Node Registration & Heartbeat
Tracks node metadata and active workload capacity.
```kotlin
package com.aiplatform.platform.lifecycle.cluster

import java.time.Instant

data class ClusterNode(
    val nodeId: String,
    val address: String,
    val port: Int,
    val joinedAt: Instant,
    val status: NodeStatus,
    val workloadCount: Int
)

enum class NodeStatus {
    ACTIVE,
    DEGRADED,
    DRAINING,
    OFFLINE
}

interface ClusterRegistry {
    fun registerNode(node: ClusterNode)
    fun deregisterNode(nodeId: String)
    fun sendHeartbeat(nodeId: String, currentWorkload: Int)
}
```

### Leader Election Contract
Handles split-brain prevention and high-availability coordinator assignment.
```kotlin
package com.aiplatform.platform.lifecycle.cluster

interface LeaderElectionListener {
    fun onElectedLeader()
    fun onSteppedDown()
}

interface LeaderElectionProvider {
    val isLeader: Boolean
    fun registerListener(listener: LeaderElectionListener)
    fun acquireLeadership()
    fun releaseLeadership()
}
```

---

## 8. Architectural Decision Record (ADR)

### ADR 0024: Consolidate the platform into a Unified Runtime Kernel

#### Status
Accepted

#### Date
2026-07-19

#### Context
Building complex AI platforms requires many interacting core elements: configuration loader, logging pipeline, sandboxed plugin runtime, reactive event bus, schedulers, and databases. If these services start up independently or in an uncontrolled order, race conditions occur. A centralized "Runtime Kernel" ensures absolute stability, order, diagnostics, and graceful termination.

#### Decision
Introduce the **Platform Runtime Kernel** as the core coordinator. The Kernel controls application boot phases, resolves topological dependencies in a DAG, implements strict failure recovery behaviors, and provides high-integrity hooks for plugins. 

#### Consequences
- The Kernel is the absolute boot authority.
- All core services (Configuration, Observability, Databases, Storage, Plugins) register as `LifecycleAware` modules.
- The state transition rules are strictly enforced and monitored.
- Transitioning to cloud clusters requires only implementing the specified cluster contracts.
