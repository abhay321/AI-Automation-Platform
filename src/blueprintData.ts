import { BlueprintSection, FolderNode, CoreModuleDetail, Milestone, ArchitectureComparison, EventBusTradeoff } from "./types";

export const ARCHITECTURE_COMPOSITIONS: ArchitectureComparison[] = [
  {
    name: "Modular Monolith (Recommended MVP)",
    score: 95,
    pros: [
      "Single deployable artifact - extremely simple deployment and operations",
      "Low latency in-memory method invocation instead of network calls",
      "Shared transactional context across domains using simple DB transactions",
      "Significantly faster development velocity for MVP",
      "Lower operational overhead (fewer Docker containers, lower RAM requirements)"
    ],
    cons: [
      "No physical resource isolation (one heavy plugin can consume host memory)",
      "Single point of failure for the entire runtime process",
      "Scaling requires replicating the entire monolith, even if only one module is busy"
    ],
    migrationPath: "By strictly enforcing Clean Architecture and Domain-Driven Design (DDD) boundaries (no direct dependencies between modules; all cross-module communication goes through defined Java/Kotlin interfaces or an in-memory event publisher), we can easily split any core module into its own microservice or worker node later. Since Koin is used for DI, we would simply replace the in-memory service implementations with gRPC/HTTP client stubs without touching any domain logic."
  },
  {
    name: "Microservices Architecture",
    score: 65,
    pros: [
      "Independent scalability of active execution workers, agent runners, and API servers",
      "Hard process-level resource limits per service",
      "Failure in workflow runner doesn't bring down the main user dashboard",
      "Polyglot programming support (some services in Python/TypeScript, core in Kotlin)"
    ],
    cons: [
      "Substantial distributed system complexity (service discovery, distributed tracing, network failures)",
      "Eventual consistency issues across multiple databases",
      "Operational complexity requires Kubernetes, service meshes, and high cluster overhead",
      "Slower initial developer velocity"
    ],
    migrationPath: "N/A - This represents the target scaled production layout. Reaching this prematurely would drain startup or open-source community velocity. We design the codebase as an 'un-split' monolith first."
  }
];

export const EVENT_BUS_TRADEOFFS: EventBusTradeoff[] = [
  {
    name: "NATS (Recommended MVP & Scale)",
    complexity: "Low",
    throughput: "Very High (1M+ msg/sec)",
    latency: "Ultra Low (<1ms)",
    bestFor: "Cloud-native event-driven backends, high-performance pub/sub, simple clustering.",
    drawbacks: "Fewer advanced stream analytics features compared to Kafka, but perfect for lightweight event dispatching."
  },
  {
    name: "Redis Streams",
    complexity: "Very Low",
    throughput: "High",
    latency: "Low",
    bestFor: "Single-instance systems already utilizing Redis for caching. Excellent for MVP.",
    drawbacks: "Limited persistence configurations, memory-bound. Scaling to multi-datacenter clusters is complex."
  },
  {
    name: "Kafka",
    complexity: "Very High",
    throughput: "Ultra High",
    latency: "Medium (~2-5ms)",
    bestFor: "Enterprise-grade high-volume streaming, event-sourcing with decades of retention, complex transaction logs.",
    drawbacks: "Extremely heavy infrastructure footprint (JVM, ZooKeeper or KRaft metadata), steep learning curve, hard to configure locally."
  },
  {
    name: "RabbitMQ",
    complexity: "Medium",
    throughput: "Medium-High",
    latency: "Low",
    bestFor: "Classic AMQP message patterns, complex routing topologies (headers, topics, direct).",
    drawbacks: "Lacks native log-compacted streaming capabilities, memory management is tricky under high load backpressure."
  }
];

export const CORE_MODULES: CoreModuleDetail[] = [
  {
    id: "mod-workspace",
    name: "Workspace",
    description: "Acts as the top-level isolation unit. Every organization, user group, and billing policy is bound to a Workspace. It houses members, API access keys, and storage limits.",
    responsibilities: [
      "Enforce hard billing/usage boundaries across workspaces.",
      "Manage member roles (Owner, Editor, Runner, Viewer) via dynamic RBAC.",
      "Store tenant-specific custom configurations and environment profiles."
    ],
    keyDataStructures: ["WorkspaceEntity", "WorkspaceMember", "WorkspaceRole", "ApiKeyCredential"]
  },
  {
    id: "mod-project",
    name: "Project",
    description: "A logical subdirectory inside a Workspace. It aggregates related workflows, custom prompt templates, asset folders, and knowledge bases together to keep team efforts structured.",
    responsibilities: [
      "Organize workflows and configurations in a cohesive directory model.",
      "Export/Import projects as a single portable JSON file (Blueprint package).",
      "Provide shared environmental variables scoped to the project level."
    ],
    keyDataStructures: ["ProjectEntity", "ProjectMeta", "EnvironmentVariable"]
  },
  {
    id: "mod-workflow",
    name: "Workflow",
    description: "The visual graph representing an automated pipeline. It contains nodes (steps) and edges (data routes), defining how data flows and which events trigger actions.",
    responsibilities: [
      "Maintain the DAG (Directed Acyclic Graph) validation state.",
      "Store workflow version history (Git-like commits for visual workflows).",
      "Manage execution trigger definitions (Webhooks, Cron schedules, Event subscriptions)."
    ],
    keyDataStructures: ["WorkflowEntity", "WorkflowGraph", "EdgeConnection", "TriggerConfig"]
  },
  {
    id: "mod-node",
    name: "Node",
    description: "The atom of execution. Every node corresponds to a specific action (e.g., calling an LLM, writing to a database, sending an email, branching on a condition).",
    responsibilities: [
      "Expose input and output schemas dynamically via JSON Schema.",
      "Execute the target logical payload within sandboxed constraints.",
      "Define icon, UI configurations, and human-friendly labels for the canvas."
    ],
    keyDataStructures: ["NodeDefinition", "NodeParameter", "DynamicSchema"]
  },
  {
    id: "mod-execution",
    name: "Execution",
    description: "The state machine that runs a workflow. It tracks execution status, stores intermediate outputs, logs step details, and coordinates retry attempts.",
    responsibilities: [
      "Manage workflow execution lifecycles (Pending, Running, Paused, Succeeded, Failed).",
      "Persist execution logs and variable payloads at each step boundary.",
      "Handle checkpointing to allow pausing and resuming for Human-in-the-Loop approval."
    ],
    keyDataStructures: ["ExecutionState", "StepRunLog", "VariableSnapshot", "CheckpointData"]
  },
  {
    id: "mod-agent",
    name: "Agent",
    description: "An autonomous loop of LLM calls. It utilizes planning, memory, and tools to accomplish complex, open-ended objectives without predefined execution graphs.",
    responsibilities: [
      "Maintain the ReAct (Reason-Action) or Plan-and-Solve cognitive loop.",
      "Formulate steps, select and call appropriate registered tools.",
      "Evaluate self-performance and recover from tool execution errors dynamically."
    ],
    keyDataStructures: ["AgentConfig", "AgentSessionState", "CognitiveStep", "ToolDefinition"]
  },
  {
    id: "mod-plugin",
    name: "Plugin",
    description: "The core extension container. Bundles of Nodes, Providers, and Connectors created by the community are registered and managed here.",
    responsibilities: [
      "Safely download, unpack, verify, and register external JARs or JS files.",
      "Enforce sandbox capabilities (e.g., block disk write, allow selected domains).",
      "Track plugin dependency trees and manage updates from the Marketplace."
    ],
    keyDataStructures: ["PluginManifest", "PluginRegistration", "SandboxPolicy", "PermissionGrant"]
  },
  {
    id: "mod-provider",
    name: "Provider",
    description: "Normalizes access to various AI model services. It translates a single unified API into specific formats required by OpenAI, Google Gemini, Anthropic, or local Ollama instances.",
    responsibilities: [
      "Route text, image, speech, video, or embeddings calls to specified APIs.",
      "Perform intelligent rate-limiting queueing and credential injection.",
      "Normalize output tokens, costs, and response schemas."
    ],
    keyDataStructures: ["ProviderConfig", "ModelRoute", "EmbeddingPayload", "ModelCapabilities"]
  },
  {
    id: "mod-connector",
    name: "Connector",
    description: "An interface to third-party SaaS services (YouTube, Slack, Gmail, database servers, web scrapers). It abstracts API authentication and standard actions.",
    responsibilities: [
      "Manage third-party authentication tokens (API keys or OAuth2 flows).",
      "Expose standard action hooks and event webhooks for workflows.",
      "Enforce API rate limits and backoff strategies transparently."
    ],
    keyDataStructures: ["ConnectorAuth", "ConnectorAction", "WebhookEndpoint", "RateLimitBucket"]
  },
  {
    id: "mod-asset",
    name: "Asset",
    description: "Manages raw files and digital media (images, PDFs, video drafts, generated audio) processed or created during executions.",
    responsibilities: [
      "Generate presigned secure download/upload URLs for MinIO/S3 objects.",
      "Extract metadata, format, and dimensions from uploaded assets.",
      "Provide secure path-based access control tied to Workspaces and Projects."
    ],
    keyDataStructures: ["AssetMetadata", "StorageReference", "AccessPolicy"]
  },
  {
    id: "mod-knowledge",
    name: "Knowledge",
    description: "The RAG (Retrieval-Augmented Generation) ingest processor. It handles chunking documents, extracting text from multiple formats, and generating embeddings.",
    responsibilities: [
      "Parse and extract text from HTML, PDF, Markdown, DOCX, and TXT.",
      "Intelligently chunk texts using semantic, overlapping, or token-count strategies.",
      "Vectorize and upsert segments into Qdrant for semantic search lookup."
    ],
    keyDataStructures: ["KnowledgeBase", "DocumentSource", "ChunkMetadata", "VectorPayload"]
  },
  {
    id: "mod-memory",
    name: "Memory",
    description: "Provides both short-term conversational context and long-term key-value history for workflow executions and cognitive agent instances.",
    responsibilities: [
      "Summarize chat sessions dynamically to fit within LLM context windows.",
      "Retrieve relevant memories using a hybrid of vector similarity and exact match.",
      "Store user preferences or workflow state values persistently across years of runs."
    ],
    keyDataStructures: ["MemorySession", "ChatMessage", "MemoryCluster", "KeyValuePair"]
  }
];

export const REPO_STRUCTURE: FolderNode = {
  name: "ai-automation-platform",
  description: "Monorepo root containing the Core Clean Architecture modules, SDKs, Plugins, Compose Desktop App, and deployments",
  children: [
    {
      name: "core-domain",
      description: "Clean Architecture Domain module containing pure Kotlin domain entities, business models, rules, and repository interfaces (e.g., Workspace, Workflow, Node, Agent).",
      children: [
        { name: "src/commonMain/kotlin/domain/model", description: "Pure, framework-independent domain models and value objects." },
        { name: "src/commonMain/kotlin/domain/repository", description: "Abstract repository and service interfaces to be implemented by infrastructure." }
      ]
    },
    {
      name: "core-application",
      description: "Clean Architecture Application module containing application services, use cases, Command/Query handlers (CQRS), and business orchestrators.",
      children: [
        { name: "src/commonMain/kotlin/application/usecase", description: "Encapsulates application-specific business rules and coordinates data flow." },
        { name: "src/commonMain/kotlin/application/service", description: "Application-level services coordinating domain entities." }
      ]
    },
    {
      name: "core-infrastructure",
      description: "Clean Architecture Infrastructure module implementing database repositories, external API adapters, Redis clients, MinIO buckets, and Ktor server routes.",
      children: [
        { name: "src/jvmMain/kotlin/infrastructure/database", description: "Concrete database repositories mapped via Exposed ORM to PostgreSQL." },
        { name: "src/jvmMain/kotlin/infrastructure/delivery", description: "Ktor server setup, HTTP REST API routes, WebSockets endpoints, and serialization configuration." },
        { name: "src/jvmMain/kotlin/infrastructure/client", description: "Concrete external API clients and caching systems (Redis, MinIO, Qdrant)." }
      ]
    },
    {
      name: "platform",
      description: "Shared runtime services module housing enterprise-grade configuration, structured JSON logging with trace/correlation IDs, health checks, event dispatchers, and security frameworks.",
      children: [
        { name: "configuration", description: "Typesafe configuration parsing YAML files and system environment variables." },
        { name: "logging", description: "SLF4J + Logback setup with custom MDC-based correlation IDs and trace IDs formatted as JSON." },
        { name: "health", description: "Custom health indicators for database, Redis, RabbitMQ, MinIO, and Qdrant connections." },
        { name: "events", description: "Internal Event Bus and external messaging client adapters." },
        { name: "security", description: "Encryption, hashing, and token-based authentication filters." }
      ]
    },
    {
      name: "common",
      description: "Shared utilities and domain-independent code used across all modules (e.g., math, text parsers, string extensions).",
      children: [
        { name: "utils", description: "Reusable helper functions, date formatters, and extension methods." }
      ]
    },
    {
      name: "sdk",
      description: "The developer contracts (kept empty in Phase 1 except for Gradle modules and docs) that allow community extensions to be written.",
      children: [
        { name: "plugin-sdk", description: "Kotlin interfaces and annotation classes defining Custom Nodes, Manifest layouts, and Lifecycle listeners." },
        { name: "workflow-sdk", description: "Core abstract state classes, variables trackers, execution contexts, and custom node builder hooks." },
        { name: "provider-sdk", description: "Unified interfaces for LLMs, Text-to-Speech, Image generation, Embeddings, and OCR adapters." },
        { name: "connector-sdk", description: "Common REST client helpers, OAuth flow hooks, request pagination systems, and standard event listener templates." },
        { name: "agent-sdk", description: "Contract for agents, including Planning systems, reasoning logs, and structured tool selection routines." }
      ]
    },
    {
      name: "plugins",
      description: "First-party and community plugins packaged as self-contained sub-modules.",
      children: [
        { name: "ai-content-factory", description: "The flagship application built as a modular plugin! Houses blog post drafting nodes, YouTube scripting, SEO analyzers, and video metadata compilers." },
        { name: "slack-connector", description: "Plugin implementing Slack OAuth, message post actions, and raw workspace event listeners." },
        { name: "google-workspace", description: "Nodes to read/write from Google Sheets, update Google Calendar, and parse Gmail drafts." }
      ]
    },
    {
      name: "desktop-control-center",
      description: "Kotlin + Compose Multiplatform Desktop application. A native, high-performance visual node-graph builder, local executor dashboard, and real-time operations manager.",
      children: [
        { name: "src/jvmMain/kotlin/ui/canvas", description: "Native Skia-backed high-density visual node canvas supporting dragging, wire connections, and coroutine execution path animations." },
        { name: "src/jvmMain/kotlin/ui/components", description: "Custom Compose UI widgets: side panels, responsive flow forms, and telemetry log viewer." },
        { name: "src/jvmMain/kotlin/ui/state", description: "State managers coordinating reactive graph modifications and background state synchronizations." }
      ]
    },
    {
      name: "deploy",
      description: "Infrastructure as Code and setup playbooks.",
      children: [
        { name: "docker-compose.yml", description: "Profile-based docker-compose.yml file. Start services on-demand with profiles like '--profile db-only' or '--profile all'." },
        { name: "kubernetes", description: "Helm charts and deployment descriptors to scale the platform inside K8s environments." },
        { name: "local-env.sh", description: "Bootstrap scripts to install local prerequisites, download model weights, and set up test databases." }
      ]
    }
  ]
};

export const MILESTONES: Milestone[] = [
  {
    id: "ms-1",
    title: "Milestone 1: Core Engine & Graph Orchestration (The Scaffold)",
    goal: "Establish the backend domain skeleton, Directed Acyclic Graph (DAG) state manager, database schemas, and Koin injection framework.",
    deliverables: [
      "Complete Domain Entities (Workspace, Project, Workflow, Node, Edge).",
      "PostgreSQL schemas mapped via Exposed ORM.",
      "An in-memory DAG Validator confirming workflows have no loops, single entrance, and complete input mappings.",
      "A simple CLI Runner that can execute a statically configured JSON workflow of mock nodes.",
      "Koin DI registry setting up core application use-cases."
    ],
    acceptanceCriteria: [
      "The system compiles clean with zero errors using Gradle.",
      "A mock workflow can be successfully loaded from JSON, validated as a clean DAG, and run in sequence via CLI with detailed console logs.",
      "Unit test coverage for DAG loop detection is >95%."
    ],
    complexity: "Medium",
    duration: "4 Weeks",
    dependencies: []
  },
  {
    id: "ms-2",
    title: "Milestone 2: Execution State & Local Sandbox Runner",
    goal: "Build the state machine to run nodes asynchronously, log run variables, handle retries, and save execution checkpoints.",
    deliverables: [
      "Execution engine powered by Kotlin Coroutines, maintaining active threads in an non-blocking queue.",
      "Run-state persistence in PostgreSQL (ExecutionState, StepRunLog).",
      "Implement step-level retry policies with exponential backoff.",
      "Provide step-level variables evaluation, parsing handlebars-like syntax (e.g. '{{node_1.output_text}}') in node input values."
    ],
    acceptanceCriteria: [
      "A workflow run can survive a sudden engine crash; upon restart, the engine restores from the database checkpoint and resumes execution.",
      "Variables are parsed and mapped accurately from preceding nodes.",
      "Failed nodes retry exactly according to their parameters before propagating errors."
    ],
    complexity: "High",
    duration: "5 Weeks",
    dependencies: ["ms-1"]
  },
  {
    id: "ms-3",
    title: "Milestone 3: Dynamic SDKs & Initial Plugin Registry",
    goal: "Formulate the Plugin SDK and allow loading custom compiled .JAR files or TS packages into sandboxed contexts dynamically.",
    deliverables: [
      "Compile-safe Plugin SDK containing annotation processor definitions.",
      "Dynamic classloader loading plugin nodes at startup from a local folder directory.",
      "Standard Provider interfaces normalized (LLM, Embeddings, Image generation).",
      "Standard Google Gemini provider implemented using the @google/genai SDK on the server."
    ],
    acceptanceCriteria: [
      "The system can detect a new compiled JAR plugin, extract its metadata, and register its custom nodes automatically without engine restarts.",
      "Calling the unified LLM interface routes calls to Gemini and returns tokens correctly."
    ],
    complexity: "High",
    duration: "6 Weeks",
    dependencies: ["ms-2"]
  },
  {
    id: "ms-4",
    title: "Milestone 4: Visual Canvas & Desktop Control Center",
    goal: "Deliver the native Compose Multiplatform Desktop visual control center and node-graph canvas, bringing the desktop workflow building experience to life.",
    deliverables: [
      "High-performance Skia-backed custom canvas with drag-and-drop node placement.",
      "Interactive dynamic property editor using Compose UI forms mapped to JSON schema.",
      "Real-time execution telemetry overlays fed by local Ktor WebSockets or Direct JVM memory interfaces.",
      "Embedded file explorer panel managing local asset uploads to MinIO with preview thumbnails."
    ],
    acceptanceCriteria: [
      "A user can drag a trigger node, connect it to a Gemini node, wire it to an asset output, and click 'Run' inside the native desktop application window.",
      "Nodes light up in green/yellow/red in real-time as the execution progresses, rendering at a smooth 60fps.",
      "Log panel displays accurate terminal output for the active run."
    ],
    complexity: "Very High",
    duration: "6 Weeks",
    dependencies: ["ms-3"]
  },
  {
    id: "ms-5",
    title: "Milestone 5: Flagship Plugin - AI Content Factory Release",
    goal: "Deploy the full production suite of nodes, templates, and agents specifically geared towards professional automated content creation.",
    deliverables: [
      "AI Content Factory plugin containing dedicated SEO Analyzer, Blog Draft Writer, YouTube Script Compiler, and YouTube API Connector nodes.",
      "Knowledge integration, allowing users to upload raw research PDFs and ground the content writer with custom document chunks.",
      "Human-in-the-Loop approval nodes allowing editors to review, refine, and sign off draft blogs before social cross-posting.",
      "Docker-compose setup ready for the community, complete with local Ollama guidance."
    ],
    acceptanceCriteria: [
      "A professional blog post can be compiled entirely from research PDFs, run through Gemini for SEO Optimization, paused for human review, and successfully published to an external blog endpoint via single-button click.",
      "A simple 'docker compose up' boots the entire platform in <2 minutes on any developer machine."
    ],
    complexity: "High",
    duration: "5 Weeks",
    dependencies: ["ms-4"]
  }
];

export const BLUEPRINT_SECTIONS: BlueprintSection[] = [
  {
    id: "vision",
    title: "1. Core Vision & Strategic Philosophy",
    category: "Strategy",
    icon: "Sparkles",
    summary: "Why this platform exists, the core problems it resolves, and how it structurally differs from n8n, LangFlow, Dify, and other automation frameworks.",
    details: `### The Manifesto
Modern automation platforms fall into two distinct traps. First-generation tools (e.g., n8n, Zapier) are exceptional at classic web integrations (Slack, Gmail, Stripe) but handle non-deterministic AI agents, memory layers, and large language model (LLM) routing as bolted-on second-class citizens. Conversely, AI-native developer frameworks (e.g., LangFlow, Dify, Flowise) are brilliant for orchestrating RAG pipelines and prompts but lack the resilient state engines, industrial scheduling, robust error propagation, and security sandboxing required to run continuous production automation.

This **AI Automation Platform** is engineered from day one to bridge this gap. It is a unified, highly extensible, offline-first orchestration engine built on **Kotlin** with a native **Compose Multiplatform Desktop** control center. It treats AI models, vector search, cognitive loops, and enterprise APIs as native nodes within a highly resilient Directed Acyclic Graph (DAG) state machine.

### Structural Differentiation: The Architectural Grid

| Feature Category | Our AI Automation Platform | n8n / Zapier | Dify / LangFlow | Home Assistant |
| :--- | :--- | :--- | :--- | :--- |
| **Primary Focus** | Native AI agents + Resilient state machine workflows | REST API SaaS orchestrations | Prompt playground and visual chat assistants | IoT local device state-polling |
| **State Machine** | Checkpointed, pause-and-resume DAG with durable task state | Linear trigger-action pipelines, in-memory execution | Simple call trees, transient session states | Reactive state-changes on triggers |
| **AI Native SDK** | Embedded Agent, Memory, Vector RAG, and Provider interfaces | Bolted-on HTTP generic blocks | Native AI blocks, lacking complex custom logic nodes | Basic home-assistant assistant integrations |
| **Extensibility** | Strongly typed Plugin SDK (Isolated JVM JARs / TS packages) | Custom JS scripts inside workflow memory | Custom Python modules, hard to run in secure isolation | Custom python integrations |
| **Local-First / Offline** | Full Ollama/LlamaEdge support, local PostgreSQL/Redis/Qdrant | Cloud-centric, heavy SaaS reliance | Hybrid, but memory-intensive Python servers | Local hardware bound |
| **Core Architecture** | Kotlin & Compose Desktop (Clean, DDD, SOLID) | Node.js (Monolithic engine, high RAM consumption) | Python (FastAPI/Flask, high latency, hard to run safely) | Python (Monolithic loop, IoT specific) |

### The Flagship Use Case: AI Content Factory
To prove the platform's versatility, the **AI Content Factory** is built not as a core feature, but as a first-party **Plugin** using the generic building blocks of the core platform. It encapsulates specialized nodes (e.g., SEO analyzer, Youtube scripting, transcription compilers) and pre-configured workflow templates that tap into standard workspace providers, showing community developers how to build deep, vertically integrated apps on top of our system architecture.`
  },
  {
    id: "principles",
    title: "2. Design Principles & Architecture Manifesto",
    category: "Strategy",
    icon: "Shield",
    summary: "The architectural rules that govern the platform's development, ensuring maintainability after years of active open-source contribution.",
    details: `### Core Implementation Tenets

#### 1. Clean Architecture (Separation of Concerns)
The codebase is divided into concentric circles: **Domain (Core Entity Business Rules)**, **Application (Use Cases & CQRS Handlers)**, **Infrastructure (Databases, File Storage, APIs)**, and **Interfaces (REST, WS, CLI)**.
- **Dependency Rule**: Outer circles depend on inner circles. Inner circles (Domain) have absolutely zero knowledge of external frameworks, databases, or libraries. Direct database imports inside the business domain are strictly forbidden.

#### 2. Domain-Driven Design (DDD)
The system is modeled around bounded contexts. Each context is a modular pack:
- **Entities & Value Objects**: Rich domain objects that encapsulate validation and self-contained behavior (e.g., a \`Workflow\` validates that it has no floating nodes before saving).
- **Aggregates**: Cohesive clusters of associated objects treated as a single transactional unit (e.g., \`Execution\` aggregates the execution run, step logs, and active variable checkpoints).
- **Repositories**: Interface contracts defined by the Domain and implemented in the Infrastructure layer.

#### 3. SOLID Principles
- **S (Single Responsibility)**: A module, node, or class should have one, and only one, reason to change. (e.g., The workflow engine compiles graphs, while individual nodes execute logical actions).
- **O (Open/Closed)**: Core code is closed for modification but open for extension. New automation nodes, LLM providers, and SaaS connectors are registered via plugins without modifying a single line of core code.
- **L (Liskov Substitution)**: Any component implementing an SDK interface (e.g., \`LlmProvider\`) must be interchangeable without breaking the application's runtime stability.
- **I (Interface Segregation)**: Clients should not be forced to depend on interfaces they do not use (e.g., dividing SDKs into \`WorkflowSDK\`, \`ProviderSDK\`, \`ConnectorSDK\`).
- **D (Dependency Inversion)**: High-level modules do not depend on low-level modules; both depend on abstractions. We inject dependencies cleanly at runtime using **Koin**.

#### 4. Plugin-First, API-First, and Open-Source First
- **Plugin-First**: Everything is a plugin. First-party integrations are packaged identically to community integrations, forcing our team to design robust SDK interfaces.
- **Native-First & API-First**: The Compose Desktop control center is a client of our rich Ktor backend services. For MVP, it runs as a high-performance native desktop application communicating via REST/WebSockets, allowing local-first workflows to run with minimal CPU and RAM overhead.
- **Local-First & Secure-First**: Hard process-level limits and secure sandboxing isolate executions. The system operates fully disconnected from external telemetry trackers, supporting local Ollama models and private on-premise infrastructure safely.`
  },
  {
    id: "high-level-arch",
    title: "3. High-Level Architecture Diagrams",
    category: "Core Architecture",
    icon: "FileText",
    summary: "Visualizing the system layers, boundary contracts, and directional dependencies of the platform components.",
    details: `### Operational Architecture Map

\`\`\`
                                  +-----------------------+
                                  |  Compose Desktop UI / |
                                  |   CLI Control Tool    |
                                  +-----------+-----------+
                                              | HTTPS / WebSockets
                                              v
+---------------------------------------------+---------------------------------------------+
|  INTERFACE LAYER (Interfaces / Presentation)                                               |
|  - REST Controller Endpoints    - WebSockets Telemetry    - Cron / Scheduler Triggers     |
+---------------------------------------------+---------------------------------------------+
                                              | Use Cases (CQRS Commands)
                                              v
+---------------------------------------------+---------------------------------------------+
|  APPLICATION LAYER (Use Cases / Orchestration)                                            |
|  - RunWorkflowUseCase           - InstallPluginUseCase    - RetrieveMemoryUseCase         |
|  - EvaluateAgentUseCase         - SyncVectorKnowledge     - TriggerSchedulerUseCase       |
+---------------------------------------------+---------------------------------------------+
                                              | Domain Interfaces
                                              v
+---------------------------------------------+---------------------------------------------+
|  DOMAIN LAYER (Core Business Logic)                                                       |
|  - Workflow Aggregate           - Node Execution State    - Agent Cognitive Loop          |
|  - Workspace Boundaries         - Asset Access Rules      - Event Bus Pub/Sub Contracts   |
+---------------------------------------------+---------------------------------------------+
                                              ^
                                              | Implements Repo Contracts
+---------------------------------------------+---------------------------------------------+
|  INFRASTRUCTURE LAYER (Concrete Technology Adapters)                                       |
|  - PostgreSQL (Exposed ORM)     - Redis (Job & Memory)    - MinIO (Asset Object Store)    |
|  - Qdrant (Vector DB)           - NATS (Event Streaming)  - Sandbox Execution Runtime     |
+---------------------------------------------+---------------------------------------------+
                                              ^
                                              | Dynamically Loaded via SDK
+---------------------------------------------+---------------------------------------------+
|  SDK & PLUGIN LANDSCAPE (Extensions)                                                      |
|  - PluginSDK (JAR Loading)      - WorkflowSDK (State)     - ProviderSDK (LLM Routing)     |
|  - ConnectorSDK (SaaS APIs)     - AgentSDK (Reasoning)    - [Flagship Content Factory]    |
+---------------------------------------------+---------------------------------------------+
\`\`\`

### Structural Boundary Rules
1. **Core-Engine Separation**: The core-engine relies exclusively on abstract interfaces. For instance, when a node saves an asset, it calls \`AssetRepository.save()\`. It has zero knowledge of whether the file lands on local disk, MinIO, or AWS S3.
2. **Koin DI Bootstrapping**: At startup, Koin parses environment variables. If \`STORAGE_PROVIDER=minio\`, it binds the \`MinioAssetRepository\` implementation to the \`AssetRepository\` interface. All services requesting the asset store receive the MinIO connector, making transitions from local to cloud incredibly seamless.
3. **Compose Desktop Inter-op**: The Desktop Control Center leverages shared Kotlin domain models directly from the common monorepo, avoiding any serialization lag or double-declaration of data types between front-end and back-end.`
  },
  {
    id: "folder-structure",
    title: "4. Monorepo Repository Structure",
    category: "Core Architecture",
    icon: "Users",
    summary: "The physical file layout of the platform, organizing code for clean multi-language monorepo compilation.",
    details: `### Physical Layout of the Monorepo

The directory tree is designed to group logical domains cleanly while separating core runtime from third-party plugins and user deployment configurations.

- **\`core-engine/\`** - Holds the primary Kotlin backend codebase. Uses Gradle with modular packages:
  - **\`src/main/kotlin/com/platform/domain/\`** - Pure, dependency-free Kotlin model code.
  - **\`src/main/kotlin/com/platform/application/\`** - Interactor orchestrators executing CQS (Command/Query Separation).
  - **\`src/main/kotlin/com/platform/infrastructure/\`** - Concrete DB connectors, Redis setups, vector ingestion pipelines.
  - **\`src/main/kotlin/com/platform/interfaces/\`** - Controller routers delivering HTTP, WebSockets, and CLI gateways.
- **\`sdk/\`** - House the developer contracts that permit extensions to be compiled without direct core project linking:
  - **\`plugin-sdk/\`** - Shared annotations and Manifest models.
  - **\`workflow-sdk/\`** - Variable registries and Step Context interfaces.
  - **\`provider-sdk/\`** - Abstract models of multi-modal generative APIs.
  - **\`connector-sdk/\`** - Authentication and backoff handlers.
  - **\`agent-sdk/\`** - Planning loop models.
- **\`plugins/\`** - Folder for official/community plugins. Houses our flagship application:
  - **\`ai-content-factory/\`** - Contains specialized blog writer nodes, SEO parsing adapters, and social posting connectors.
- **\`desktop-control-center/\`** - Native Compose Multiplatform Desktop application. Fully written in Kotlin with Skia rendering, providing maximum performance for rendering massive DAG flows and sub-graphs smoothly.
- **\`deploy/\`** - Infrastructure templates for simple startup or production Kubernetes scaling.`
  },
  {
    id: "core-modules",
    title: "5. In-Depth Core Modules Specification",
    category: "Core Architecture",
    icon: "Sliders",
    summary: "A breakdown of the core modules that define the platform state, boundaries, and transactional capabilities.",
    details: `### Bounded Contexts Definition
Every module is isolated using package-private structures inside the Application and Infrastructure layer to prevent modular bleed.

1. **Workspace**: Encapsulates tenancy. It owns members, roles, and limits. Every table in PostgreSQL includes a \`workspace_id\` index to ensure strict query leakage prevention.
2. **Project**: Groups logical items (workflows, files, prompts). Projects act as portable units; exporting a Project returns an encrypted JSON zip package that can be imported into any other workspace instance.
3. **Workflow**: Models the Directed Acyclic Graph. Workflows support live version branch staging. Edges dictate how node outputs map to downstream node inputs.
4. **Node**: The concrete action configuration. Inputs and outputs are declared dynamically using JSON Schema formats, allowing the UI dashboard to generate input forms on-the-fly and validate connections.
5. **Execution**: Tracks runs. It acts as an immutable ledger; once a workflow step runs, its inputs, outputs, logs, start-time, and end-time are committed to the step-run logs table and can never be modified.
6. **Agent**: Operates cognitive loops. Unlike hard-coded workflow paths, the Agent receives an Objective, assesses tool registries, formulates planning chains, and acts autonomously.
7. **Plugin**: Tracks downloaded modules, manages classpath isolation, and grants sandbox permission policies.
8. **Provider**: Normalize AI interactions. Handles chat streaming, multi-modal payloads, image rendering, TTS voice-streams, and semantic vector embeddings via standard models.
9. **Connector**: Interfaces external systems with advanced backoffs, rate-limits, and token refreshment models.
10. **Asset**: Secure, deduplicated binary storage layer with direct presigned S3 url generations.
11. **Knowledge**: Segmenting, parsing, parsing OCR of documents, and pipeline vector indexing.
12. **Memory**: Long-term memory store utilizing Redis for low-latency active caching and PostgreSQL for persistent session archiving.`
  },
  {
    id: "sdk-design",
    title: "6. Platform SDK Architectures",
    category: "SDK & Extension",
    icon: "PenTool",
    summary: "Defining the development interfaces for custom Plugins, Workflows, Providers, Connectors, and Cognitive Agents.",
    details: `### Designing Stable Contracts

#### 1. Plugin SDK
To write a plugin, developers do not need to clone the core platform. They simply include our lightweight dependency in their Gradle file:
\`\`\`kotlin
dependencies {
    compileOnly("com.platform:plugin-sdk:1.0.0")
}
\`\`\`

They create custom nodes using simple annotations:
\`\`\`kotlin
@Node(
    id = "seo-analyzer",
    name = "SEO Meta Analyzer",
    category = "SEO",
    description = "Analyzes a blog draft and recommends target keyword density."
)
class SeoAnalyzerNode : ExecutableNode {
    @Input(description = "Raw HTML or Markdown content")
    lateinit var content: String

    @Input(description = "Primary target keywords")
    lateinit var keywords: List<String>

    @Output(description = "JSON analysis of density and suggestions")
    lateinit var analysisResult: SeoAnalysis

    override suspend fun execute(context: StepExecutionContext): NodeExecutionResult {
        // Business logic here, utilizing isolated, safe context
        return NodeExecutionResult.success(mapOf("analysisResult" to analysisResult))
    }
}
\`\`\`

#### 2. Workflow SDK & Execution Context
The \`StepExecutionContext\` provides secure gateways to:
- **Variables Store**: Read values from preceding node execution paths safely.
- **Project Configuration**: Access safe API secrets without exposing them directly.
- **Asset Accessor**: Obtain secure input-stream reader and writer channels to store binary files inside MinIO.`
  },
  {
    id: "plugin-system",
    title: "7. Secure Plugin & Classloading Architecture",
    category: "SDK & Extension",
    icon: "Lock",
    summary: "How plugins are dynamically loaded, isolated in secure sandboxes, and published via an open marketplace.",
    details: `### Dynamic Classloading & Sandboxing

#### Classpath Isolation
To prevent dependency conflicts (e.g., a plugin requiring an old version of Jackson while the core engine runs a modern one), the Core Engine implements custom Classloaders.
- Every installed plugin is assigned a dedicated **\`PluginClassLoader\`** which parent-defers only to core SDK classes, isolating all other internal libraries.

#### Secure Runtime Sandboxing
Since plugins can contain malicious code, they are executed in a sandboxed Java/Kotlin context with restricted JVM permissions using custom Security Policies:
- **Network Block**: Plugins cannot make arbitrary outbound HTTP connections unless their manifest declares \`permissions: ["network:domain.com"]\`.
- **Disk Isolation**: Direct access to the host file system is completely blocked. All file transactions must go through the \`AssetManager\` SDK which operates on workspace-isolated MinIO directories.
- **Process Spawning**: Spawning native system shells (\`Runtime.getRuntime().exec()\`) is strictly blocked.

#### Lifecycle Matrix
A plugin flows through an explicit, automated state machine:
1. **Unpacked / Instantiated**: Clean validation of the bundle manifest and cryptographic signature.
2. **Isolated Verification**: Classpaths parsed, permissions checked, and annotations loaded.
3. **Active**: Node configurations are exposed to the UI canvas and registered into Koin.
4. **Suspended / Disabled**: Sandbox permissions retracted; node references on active canvas display warning states.
5. **Decommissioned**: Safely garbage-collected and files removed.`
  },
  {
    id: "workflow-engine",
    title: "8. Durable Workflow State-Machine & Engine",
    category: "Engine & AI",
    icon: "RotateCcw",
    summary: "A deep dive into the resilient DAG orchestration, task scheduling, human-in-the-loop approvals, and failure rollbacks.",
    details: `### The State-Machine Core
Workflows are compiled into an optimized Directed Acyclic Graph.

#### Execution Thread Coordination
We utilize Kotlin Coroutines with custom thread pools (\`Dispatchers.Default\` configured for state parsing, and a dedicated high-throughput I/O pool for network/SaaS activities). This ensures that running millions of concurrent nodes consumes minimal memory compared to traditional thread-per-worker setups.

#### Resiliency & Checkpoint States
To handle unexpected hardware reboots or container restarts:
- Every time a node transitions from \`Pending -> Running -> Succeeded/Failed\`, the engine commits a state checkpoint to PostgreSQL inside a single ACID transaction.
- If a server crashes mid-execution, the boot-loader queries all executions marked as \`Running\`, restores their intermediate variable state from the last committed step-checkpoint, and resumes execution from the exact point of failure.

#### Advanced Flow Controls
- **Parallel Expansion**: Supports running map-reduce styles (e.g., splitting a newsletter draft into 5 language variants and executing translations in parallel coroutine slots).
- **Human-in-the-Loop Approval**: Introduces a special \`HumanApprovalNode\`. When hit, the engine halts the workflow, saves all run-state variables, and triggers an email/Slack notification with a secure review link. The workflow remains in a hydrated, dormant state until an editor clicks 'Approve' or 'Reject' on the UI dashboard.`
  },
  {
    id: "ai-system",
    title: "9. Smart Model Router & RAG Knowledge Engine",
    category: "Engine & AI",
    icon: "Sparkles",
    summary: "Orchestrating model fallback behaviors, cost calculations, dynamic semantic chunking, and cognitive agent execution loops.",
    details: `### Multi-Model Intelligence

#### The Dynamic Model Router
Never hard-code an AI endpoint. Workflows and Agents specify a semantic model tier (e.g., \`TIER_CREATIVE_WRITING\`, \`TIER_FAST_REASONING\`). The engine routes these calls dynamically:
- **Fast / Economical**: Routed to Gemini 2.5 Flash, Claude 3.5 Haiku, or a local Llama 3 model depending on available API capacity or local constraints.
- **Reasoning / Creative**: Routed to Gemini 1.5 Pro, GPT-4o, or Claude 3.5 Sonnet.
- **Failover Routine**: If Gemini returns a 429 rate limit or 503 outage, the router automatically fails over to Claude, tracking token costs and latency transparently.

#### Retrieval-Augmented Generation (RAG) Pipeline
1. **Ingest & Clean**: File Upload triggers a background job. The system extracts plaintext and cleans visual noise.
2. **Chunking Strategies**: Supports recursive character chunking (overlapping paragraphs for semantic continuity) or semantic similarity chunking using embeddings.
3. **Vectorization**: Embedded blocks are committed to **Qdrant** alongside robust metadata filtering keys (\`workspace_id\`, \`project_id\`, \`document_id\`).
4. **Hybrid Retrieval**: Search queries perform a combined Keyword BM25 and Vector Similarity search to return the most relevant context blocks directly to the LLM prompt template.`
  },
  {
    id: "storage-architecture",
    title: "10. Technology-Specific Storage Architecture",
    category: "Strategy",
    icon: "Sliders",
    summary: "Defining exact data residency rules across PostgreSQL, Redis, Qdrant, and MinIO to prevent database role leakage.",
    details: `### Defining Rigid DB Isolation

To maintain an enterprise-grade platform, different categories of data are placed strictly into databases engineered specifically for those access patterns. Mixing these duties is strictly forbidden.

| Storage Engine | Data Classification | Access Patterns | Retention & Archival Policy |
| :--- | :--- | :--- | :--- |
| **PostgreSQL (Exposed ORM)** | Structured relational state, Users, Workspace logs, Projects, DAG definitions, Execution History, Audit ledger. | Highly transactional, structured queries with strict ACID requirements. | Infinite retention for historical logs; soft deletions for workspace projects. |
| **Redis (Active Cache)** | Active task queues, distributed locking, short-term session caches, rate-limit buckets, and live workflow execution state. | Extremely low latency, high frequency key-value reads/writes. | Transient TTLs. Workflows data purged 24 hours after completion. |
| **Qdrant (Vector Store)** | Document chunk vectors, semantic embeddings, long-term memory embeddings. | Vector cosine similarity queries, semantic searches with workspace metadata filters. | Scoped to knowledge-base lifecycles. Cleared when documents are deleted. |
| **MinIO / AWS S3** | Raw media, PDF attachments, transcribed video drafts, compiled plugin JAR files, generated audio WAV files. | Direct file streaming, block read/writes, presigned URL generation. | Presigned expiration limits. Workspace-scoped bucket boundaries. |`
  },
  {
    id: "security",
    title: "11. Enterprise Security, JWT, & Cryptography",
    category: "Operations & Delivery",
    icon: "Lock",
    summary: "Enforcing Row-Level Security, secure credential encryption vaults, and dynamic API token rotations.",
    details: `### Securing the Automations

#### 1. Zero-Trust API Secrets Vault
SaaS credentials (e.g., Slack Tokens, Stripe Keys) are never stored in plaintext in the database.
- We implement an internal Cryptographic Key Management wrapper.
- All secrets are encrypted at rest using **AES-256-GCM** with a master key derived from environmental variables (\`MASTER_ENCRYPTION_KEY\`).
- Decryption occurs dynamically in memory *only* during the specific execution milliseconds of the node that requires the credential.

#### 2. Row-Level Tenant Isolation
- Every database query in the Repository layer strictly appends \`AND workspace_id = :activeWorkspaceId\`.
- Shared caching indexes in Redis are prefixed by \`workspace_id:\` to avoid any possibility of cross-tenant cross-talk.

#### 3. Execution Auditing
An immutable append-only Audit log tracks critical changes:
- Workflow modifications (who changed what block in the DAG).
- API Secret access (who authorized the Slack connector).
- System login and token generation records.`
  },
  {
    id: "observability",
    title: "12. Distributed Observability & Monitoring",
    category: "Operations & Delivery",
    icon: "Award",
    summary: "System tracking using Prometheus, Grafana, OpenTelemetry, and live workflow error dashboards.",
    details: `### Monitoring the Factory

#### Prometheus Metrics Exposed
The core-engine exposes a standard \`/metrics\` endpoint tracking:
- \`platform_active_executions_count\`: Gauge of concurrently running workflow coroutines.
- \`platform_node_execution_duration_seconds\`: Histogram tracking latency by Node Type and Workspace.
- \`platform_provider_token_cost_usd\`: Counter summarizing cumulative spending across Gemini, OpenAI, and Claude.

#### Distributed Tracing (OpenTelemetry)
Every workflow execution receives a unique trace ID (\`traceparent\`). This span is passed contextually across nodes and plugins:
- If a workflow triggers a microservice or makes an external SaaS call, the trace ID headers are propagated, allowing complete visual inspection of execution bottlenecks inside Grafana or Jaeger.

#### Core Health Dashboard
- **Plugin Health**: CPU/RAM usage of Classloaders.
- **Workflow Health**: Tracking ratios of Success vs Failures in real-time.
- **Provider Health**: Tracking rate-limit limits and API outages dynamically.`
  },
  {
    id: "installation",
    title: "13. Single-Command Local Setup & Deployment",
    category: "Operations & Delivery",
    icon: "Download",
    summary: "How one simple docker-compose up command spins up the complete, secure self-contained platform locally.",
    details: `### Zero-Friction Local Bootstrapping

We deliver a comprehensive \`docker-compose.yml\` at the monorepo root. It sets up all specialized storage engines alongside our Kotlin Ktor core services, allowing offline-first backend development instantly. To launch the native control center interface, developers simply trigger the Compose Multiplatform Desktop target from their system.

#### Compose Architecture Blueprint

\`\`\`yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ai_platform
      POSTGRES_USER: dev_user
      POSTGRES_PASSWORD: dev_secure_password
    ports:
      - "5432:5432"
    volumes:
      - pg_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"

  qdrant:
    image: qdrant/qdrant:v1.9.0
    ports:
      - "6333:6333"

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: root_dev
      MINIO_ROOT_PASSWORD: root_secure_password

  core-engine:
    build:
      context: ./core-engine
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/ai_platform
      - REDIS_HOST=redis
      - QDRANT_HOST=qdrant
      - MINIO_ENDPOINT=http://minio:9000
    depends_on:
      - postgres
      - redis
      - qdrant
      - minio
\`\`\`

#### Bootstrapping the Native Control Center
Once your backend engines are spinning cleanly in Docker, launch the Compose Multiplatform Desktop operational control center:
\`\`\`bash
# Build and execute the native Kotlin desktop window
./gradlew :desktop-control-center:run
\`\`\``
  },
  {
    id: "future-expansion",
    title: "14. Future-Proofing Multi-Form Adaptations",
    category: "Operations & Delivery",
    icon: "ChevronRight",
    summary: "Scaling the platform to Desktop, Mobile, Kubernetes, and corporate Enterprise Editions without redesigning core code.",
    details: `### Scaling From Developer Laptop to global Enterprise

This platform is architected specifically to allow scaling without structural rewrites:

#### 1. Pure Kotlin-First Alignment & Web Target (Compose HTML / Wasm)
- Because we utilize Compose Multiplatform, our UI code is 100% Kotlin. If we want to offer a web version in a future milestone, we can compile the exact same UI codebase to WebAssembly (Wasm) or Compose HTML without rewriting our visual canvas, node styling, or state logic.
- This gives us complete investment protection for our code, making us fully platform-independent while remaining Kotlin-first.

#### 2. Cloud Serverless & Kubernetes Elasticity
- For cloud operations, the core engine transitions into a distributed worker model:
  - The API Server becomes stateless, handling DAG compilation and user dashboards.
  - Active executions are dispatched as lightweight messages onto **NATS**.
  - Background worker nodes run the actual coroutines, scaling up or down dynamically depending on active queue length in Kubernetes.

#### 3. Enterprise Edition Features
Without touching the open-source core codebase, the enterprise model overlays:
- **SSO / SAML Identity Federation**: Plugging into Okta, Azure AD, or Auth0.
- **Advanced Org Billing**: Granular billing limits per project and workspace.
- **Vector Guardrails**: Real-time PI/sensitive data filtering before LLM transfers.`
  },
  {
    id: "roadmap",
    title: "15. Incremental Development Roadmap",
    category: "Operations & Delivery",
    icon: "Award",
    summary: "A step-by-step 5-milestone release plan designed to establish a solid framework before writing first-party plugins.",
    details: `### Strategic Project Delivery

The project is executed in five highly disciplined milestones, ensuring we have a compiling, testable, runnable application at each checkpoint.

- **Milestone 1: Core Scaffold & DAG Validation (Month 1)**: Focusing on database mappings, Exposed models, and the core graph loop analyzer.
- **Milestone 2: Coroutines Execution Engine (Month 2)**: Completing the state tracker, checkpoint saver, and variable mapper.
- **Milestone 3: Dynamic Classloading Plugin SDK (Month 3)**: Unlocking custom plugin execution, Sandboxing policies, and Gemini provider.
- **Milestone 4: Compose Desktop Control Center (Month 4)**: Delivering the high-performance native Compose Multiplatform node editor canvas with real-time status indicators.
- **Milestone 5: AI Content Factory Deployment (Month 5)**: Building specialized SEO and scriptwriting nodes, grounding systems via Qdrant RAG, and launching the one-click docker-compose.`
  }
];
