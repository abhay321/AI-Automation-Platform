# Platform Capabilities & High-Level Architecture
Version: 1.0 (Unified Platform Architecture)

Welcome to the **AI Automation Platform**. This document provides an overarching system visualization, demonstrating how individual capabilities unite into a single, cohesive, enterprise-ready platform.

Rather than thinking of the codebase in isolated, coupled modules, we model our architecture as a series of unified **Capabilities**.

---

## 1. Core Capabilities Map

The platform is divided into four distinct capability horizons, separating basic runtime infrastructure from orchestration engines and business applications:

```
  ┌────────────────────────────────────────────────────────────────────────┐
  │                              APPLICATIONS                              │
  │     AI Content Factory  •  Desktop Studio  •  Automation Builder       │
  └───────────────────────────────────┬────────────────────────────────────┘
                                      ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                              AI PLATFORM                               │
  │   Model Routing  •  Prompt Engine  •  RAG  •  Memory  •  Agent Loops   │
  └───────────────────────────────────┬────────────────────────────────────┘
                                      ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                           WORKFLOW PLATFORM                            │
  │     DAG Compiler  •  Execution  •  State Checkpoints  •  Compensation  │
  └───────────────────────────────────┬────────────────────────────────────┘
                                      ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                            PLATFORM RUNTIME                            │
  │  Kernel  •  Config  •  Observability  •  Event Bus  •  Plugin Sandbox  │
  └────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Horizontal Breakdown

### 2.1. Platform Runtime Capability
The bedrock of the system. Written in highly optimized Kotlin and designed to run inside low-overhead, containerized environments.
* **Runtime Kernel**: Owns startup phases, schedules topological dependency resolution, monitors transition thresholds, and guarantees graceful termination under SIGTERM signals.
* **Configuration (Hoplite-driven)**: Loads and parses type-safe YAML parameters, masking runtime secrets automatically.
* **Observability (Unified)**: Eradicates duplicate parsing pipelines by combining Logging, Metrics (Micrometer), Tracing (OpenTelemetry), Health indicators, and Diagnostics into a single, cohesive context engine.
* **Event Bus (Coroutine-Native)**: A high-throughput, non-blocking event dispatcher. Supports synchronous hook processing, asynchronous worker dispatching, delayed schedulers, and transactional safety via the Outbox pattern.
* **Plugin Runtime**: Provides secure Classloader isolation barriers, allowing third-party JAR plugins to run safely inside protected sandboxes.

### 2.2. Workflow Platform Capability
A transactional workflow processor designed to run complex Directed Acyclic Graphs (DAGs) resiliently.
* **DAG Compiler**: Compiles JSON/YAML definitions into runtime executions, performing cycle checks, type matching, and linting.
* **Execution Engine**: Allocates tasks dynamically to concurrent workers, tracking execution checkpoints in real-time.
* **State Checkpoints & Recovery**: Saves snapshot checkpoints, enabling cold restarts and resuming workflows from the exact point of interruption.
* **Compensation Rules**: Guarantees data consistency. If a step midway through a workflow fails (e.g., charge card succeeds but email dispatch fails), the engine automatically executes rollback routines for preceding successful nodes.

### 2.3. AI Platform Capability
The cognitive boundary. Insulates workflows and application code from changes in underlying foundation model APIs.
* **Unified Provider SDK**: A standard abstraction covering Gemini, Claude, OpenAI, and local custom inference systems.
* **Model Router**: Uses semantic profiling to route prompts. E.g., routing simple classification tasks to cheaper, faster models while reserving complex reasoning tasks for premium, high-reasoning models.
* **RAG & Memory Registers**: Standardizes chunking, vector storage bindings, and sliding-window conversation history preservation.
* **Agent Loops**: Orchestrates ReAct loops, planning modules, and real-time tool bindings.

### 2.4. Applications Ecosystem
The customer-facing experiences. Built as fully modular applications running on top of the SDK boundaries.
* **AI Content Factory**: A high-speed generation portal specialized for batch marketing, translation, and media editing.
* **Desktop Studio**: A native-grade visual dashboard for compiling, debugging, and monitoring DAG workflows live.
* **Automation Builder & Marketplace**: Web compiler interfaces to discover community-built plugins, connectors, and predefined workflows.

---

## 3. Core Architectural Rules
To maintain the integrity of this platform, all development must adhere to these frozen standards:
1. **Low-Allocation Core**: The Platform Runtime core libraries must not instantiate heavy DI frameworks during constructor scopes. Everything runs on structured coroutine-native workers.
2. **Deterministic Shutdown**: Sockets, connections, and worker threads must close in the reverse topological order of their initialization.
3. **Trace Preservation**: Distributed traces must propagate seamlessly across HTTP boundaries, async event bus hops, and background worker threads.
