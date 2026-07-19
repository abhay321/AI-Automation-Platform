# ADR 0012: Implement a Plugin-First Architecture

## Status
Accepted

## Date
2026-07-19

## Context
A major failing of first-generation automation systems is monorepo coupling. If a developer wants to add a new service connector (e.g., integrations with Slack, Salesforce, or custom internal systems), they must submit code directly to the core platform repository. This creates major problems:
1. Core maintainers become bottlenecks for reviewing third-party SaaS integrations.
2. The core platform size expands exponentially as thousands of custom connectors are added.
3. Upgrading or changing a single connector requires rebuilding and redeploying the entire core engine.

To ensure community scalability, the platform must decouple integrations from the core runtime engine.

## Problem Statement
How should the platform structure integrations, specialized triggers, and custom actions to allow community extensions without coupling code to the core repository?

## Decision
Implement a **Plugin-First Architecture**. 

Every integration, connector, and custom automation node will be packaged as an isolated **Plugin**. Even first-party connectors (such as our Flagship AI Content Factory, Slack, or Google Workspace integrations) will be built using the exact same public Plugin SDK. 

Plugins are compiled into self-contained packages (such as JVM JARs or isolated Gradle modules) that declare a standard `PluginManifest` defining available nodes, properties schemas (JSON schema), and dynamic event handlers. The core engine loads these plugins dynamically at startup or runtime using a sandbox-safe class loader.

## Alternatives Considered
- **Monolithic Connectors**: Embedding all connectors directly inside the core engine repository. This makes development easier initially, but leads to bloated runtimes, slow release cycles, and limits the community's ability to build private or proprietary integrations.
- **Embedded Scripting (JS/Python alone)**: Writing custom integrations inside raw text nodes. While flexible, it offers zero type-safety, has terrible performance, is highly insecure, and prevents the creation of structured, elegant visual node configurations.

## Advantages
- **Infinite Scalability**: Developers can build, compile, and distribute plugins independently of the core release cycle.
- **Strict Separation of Concerns**: Core engine maintainers focus exclusively on DAG validation, transaction checkpoints, security, and scheduling performance.
- **No Core Bloat**: Production clusters only load the exact plugins required for their active workflows, optimizing memory footprint.

## Disadvantages
- **Dynamic Class Loading Complexity**: Class isolation, class loader delegation, and dependency conflicts (e.g. plugin A using Jackson v2.15 while plugin B uses v2.17) require robust sandbox isolation.

## Consequences
- We will establish clear isolation barriers inside `sdk/plugin-sdk`.
- Core engine interactions with plugins will occur strictly via defined abstract interfaces.

## Risks
- A malicious or poorly written plugin can consume excessive CPU or memory, crashing the host engine. We mitigate this by establishing a strict lifecycle and execution policy for plugins, running them in isolated coroutine threads with memory/time thresholds in Phase 2.

## Migration Strategy
N/A - Direct integration.

## Future Considerations
Implement a secure, central Plugin Registry allowing users to download, install, and update plugins directly from the Desktop Control Center with one click.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0013: Establish a Strict SDK-First Component Separation](./0013-sdk-first-design.md)

## References
- [OSGi Alliance - Dynamic Module System for Java](https://www.osgi.org/)
- [Java Plugin Framework (JPF) Best Practices](http://jpf.sourceforge.net/)
