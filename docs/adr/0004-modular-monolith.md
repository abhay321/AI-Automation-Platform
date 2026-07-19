# ADR 0004: Standardize on a Modular Monolith Architecture

## Status
Accepted

## Date
2026-07-19

## Context
When starting a modern backend architecture, developers are often tempted to jump directly to Microservices. They carve out user management, workflow execution, caching, vectors, and logging into isolated micro-processes communicating via HTTP or GRPC. 

While microservices provide organizational scalability for massive enterprise teams, they introduce immense operational complexity: network latencies, distributed transactional failures (Sagas), complex local setups, massive deployment overhead, and complex debug loops. For an open-source, local-first platform meant to run smoothly on developer laptops or inside single-container Cloud Run instances, microservices are a major anti-pattern.

We need a system that offers the logical isolation of microservices (permitting future scaling) but runs cleanly in a single, high-performance runtime.

## Problem Statement
How do we structure the core platform components to ensure code segregation and scalability without paying the high operational, deployment, and performance costs of microservices?

## Decision
Standardize on a **Modular Monolith** architecture utilizing Gradle multi-project modules. 

All logical layers and functional slices run inside a single process/JVM instance but are kept segregated via strict modular compilations. Core domains are decoupled, and inter-module communications inside the process happen via synchronous function calls or asynchronous, memory-backed Event Buses.

```
                  +--------------------------------+
                  |  ROOT GRADLE (ai-platform)     |
                  +---------------+----------------+
                                  |
         +------------------------+------------------------+
         |                        |                        |
+--------v-------+       +--------v-------+       +--------v-------+
|  core-domain   |       |    platform    |       |     common     |
+--------+-------+       +--------+-------+       +----------------+
         ^                        ^
         |                        |
+--------+-------+                |
| core-application                |
+--------+-------+                |
         ^                        |
         |                        |
+--------+------------------------+-------+
|          core-infrastructure             |
+------------------------------------------+
```

## Alternatives Considered
- **Microservices**: Deemed overly complex, introducing huge startup latencies, making offline development painful, and requiring complex network mesh configurations.
- **Single-module Monolith (Flat codebase)**: Lead to quick degeneration into "spaghetti code", where any developer can directly couple logging to database controllers or route security logic through standard helpers.

## Advantages
- **Single-Process Performance**: Zero network latency, serialization overhead, or distributed state synchronization issues.
- **Ultra-Simple Local Execution**: Developers boot the entire stack with a single click in their IDE or via `./gradlew :core-infrastructure:run`.
- **Easy Deployment**: Can be compiled into a single Docker container, running efficiently on a single cheap server or Cloud Run instance.
- **Painless Evolution to Microservices**: If a specific module (e.g., the workflow runtime runner) needs dedicated scaling in the future, its strict boundary lets us pull it out into its own service with minimal refactoring.

## Disadvantages
- **Process Fate Sharing**: A memory leak or crash in one module (e.g., a plugin crash) can bring down the entire server if not sandboxed properly.
- **Technology Locking**: All modules are bound to the same JVM runtime environment and libraries defined in the version catalog.

## Consequences
- Developers must respect module dependencies defined in `settings.gradle.kts`.
- Direct module imports that bypass application/domain boundaries are blocked by compiler configurations.

## Risks
- The modular boundaries are only as good as our Gradle configuration enforcement. Developers must not inject circular dependencies between modules.

## Migration Strategy
N/A - Set up directly in Module 1.

## Future Considerations
As the platform expands to support enterprise Kubernetes clusters, the core engine execution workers can be split into distributed stateless processes, while keeping the main monorepo structure intact.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0019: Standardize on Gradle Version Catalogs](./0019-gradle-version-catalog.md)

## References
- *Designing Data-Intensive Applications* by Martin Kleppmann
- *Architecture Patterns with Python* (applying Modular Monolith guidelines to modern application structures)
