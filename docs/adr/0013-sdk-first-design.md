# ADR 0013: Establish a Strict SDK-First Component Separation

## Status
Accepted

## Date
2026-07-19

## Context
When decoupling plugins and third-party extensions via a Plugin-First architecture, we face a critical challenge: **dependency leaks**. If plugins import the core engine project directly, they inherit database drivers (Exposed, Hikari), web servers (Ktor), caching layers (Redis), and internal orchestrators. 

This results in heavy plugin files, frequent compilation breaks when core engine internals are refactored, and security vulnerabilities as plugins access internal transactional engines.

We need a clear, minimal contract module that exposes only the specific API endpoints and lifecycle hooks needed to write integrations, making compilation fast and secure.

## Problem Statement
How do we isolate developer contracts from core engine execution structures to prevent dependency leaks and preserve platform backward-compatibility?

## Decision
Establish a strict **SDK-First Component Separation**. 

We will create dedicated, lightweight SDK modules under the `sdk/` directory:
- `plugin-sdk`: Core annotations, interfaces, and model configurations for declaring plugins.
- `workflow-sdk`: Definitions for state contexts, variable scopes, and execution results.
- `provider-sdk`: Standardized contracts for LLM, Chat, and Vision operations.
- `connector-sdk`: Base class wrappers for SaaS API integrations.
- `agent-sdk`: Contracts for structuring cognitive planning loops.

These SDK modules are completely decoupled from `core-application` and `core-infrastructure`. They contain only interfaces, contracts, and minimal value objects. Plugins import *only* these lightweight SDKs. The core engine compiles these SDKs, executes the dynamic plugins, and maps SDK types back to internal domain entities.

```
+------------------------------------+
|            CORE ENGINE             |
|   (orchestration, DB, Ktor, etc)   |
+-----------------+------------------+
                  |  Implements
                  v
+-----------------+------------------+
|               SDK                  |
|     (interfaces, contracts)        |
+-----------------^------------------+
                  |  Imports
                  |
+-----------------+------------------+
|             PLUGINS                |
|      (slack, workspace, etc)       |
+------------------------------------+
```

## Alternatives Considered
- **Direct Monorepo Reference**: Standardizing on importing `core-engine` into plugin projects. Highly discouraged as it leaks raw internals, violates modular encapsulation, and breaks build isolation.

## Advantages
- **Dependency Isolation**: Plugins compile against tiny, clean JAR files without database, server, or cloud vendor transitive dependencies.
- **Guaranteed Compatibility**: Changes to core infrastructure internals (e.g., refactoring PostgreSQL schemas) will never break compiled plugins as long as the SDK contracts remain stable.
- **Fast Build Pipelines**: Plugins compile in milliseconds due to minimal dependency trees.

## Disadvantages
- **Double Mapping**: Requires translating models from the SDK representation to internal Domain schemas inside the engine.

## Consequences
- SDK modules are defined inside `settings.gradle.kts` and compiled independently.
- The `sdk/` modules are kept clean of heavy third-party runtimes.

## Risks
- Over-complicating SDK interfaces can confuse developers. We mitigate this by keeping the SDK interfaces clean, minimalist, and thoroughly documented.

## Migration Strategy
N/A - Established as the core structural rule in Module 1.

## Future Considerations
Enforce binary compatibility checks on SDK modules to guarantee older compiled plugins continue to run smoothly on newer platform versions.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0012: Implement a Plugin-First Architecture](./0012-plugin-first-architecture.md)

## References
- [API Design Matters by Jaroslav Tulach](https://apidesign.org/)
- [Semantic Versioning 2.0.0 Specification](https://semver.org/)
