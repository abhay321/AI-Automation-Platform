# ADR 0002: Adopt Clean Architecture Principles

## Status
Accepted

## Date
2026-07-19

## Context
Early-stage automation projects usually suffer from tight coupling. A database model (ORM entity) is directly exposed to HTTP endpoints, or business workflow logic directly calls a cloud storage client (e.g., AWS S3). When infrastructure changes—such as migrating from PostgreSQL to MongoDB, or replacing Local Disk with MinIO object storage—developers must rewrite major portions of the business logic. 

In an extensible open-source system, business domain processes must remain completely decoupled from database, UI, transport, and external provider infrastructure details.

## Problem Statement
How do we organize the platform codebase so that business logic can be developed, tested, and maintained in complete isolation from database schemas, web frameworks, external SaaS integrations, and UI interfaces?

## Decision
Adopt **Uncle Bob's Clean Architecture** principles. We will strictly segregate the core code into three distinct, decoupled modules:

1. **`core-domain` (The Inner Circle)**: Pure Kotlin containing business entities, domain rules, validation, and abstract repository/service interfaces. No dependencies on databases, HTTP libraries, or framework frameworks (no Exposed, no Ktor, no Jackson).
2. **`core-application` (The Use Cases)**: Orchestrates domain entities to execute specific workflow flows, validating rules, handling commands/queries, and managing transactional boundaries. Depends strictly on `core-domain`.
3. **`core-infrastructure` (The Outer Circle)**: Concrete implementations of repository interfaces (Exposed ORM, Redis client, MinIO client, Qdrant client), HTTP controllers (Ktor routes), event listener integrations, and background schedulers.

```
+-------------------------------------------------------------+
|                      INFRASTRUCTURE                         |
|     (Ktor Controllers, Exposed ORM, Qdrant, MinIO)          |
|   +-----------------------------------------------------+   |
|   |                    APPLICATION                      |   |
|   |             (Workflow Executions, CQRS)             |   |
|   |   +---------------------------------------------+   |   |
|   |   |                   DOMAIN                    |   |   |
|   |   |           (Workflow, Node, Workspace)       |   |   |
|   |   +---------------------------------------------+   |   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
               Dependency Direction: Inner Only ->
```

## Alternatives Considered
- **Traditional Layered Architecture (Controller-Service-DAO)**: Simpler to scaffold initially, but leads to database leakage, where domain models mirror database tables, creating tight coupling and fragile test suites.
- **Feature-Slicing (Screaming Architecture alone)**: Excellent for structuring directories, but without explicit architectural boundary rules, it does not guarantee decoupling of business logic from infrastructure frameworks.

## Advantages
- **Framework Independence**: We can switch HTTP servers (e.g., move from Ktor to Spring or Vert.x) or database technologies without changing a single line of business logic.
- **Isolability in Testing**: Domain and application modules are tested using pure JUnit and MockK with zero database connections or server startups, leading to extremely fast, reliable unit tests.
- **Extremely Maintainable SDKs**: Third-party plugins can import core domain contracts directly without importing heavy database driver runtimes.

## Disadvantages
- **Class Duplication**: Requires mapping between layers (e.g., converting a database ORM row into a pure domain entity), resulting in slightly more classes and mapping code.
- **Mental Overhead**: Developers must understand correct dependency flow directions and resist the temptation to directly inject infrastructure models into domain services.

## Consequences
- Dependency flows move strictly inwards. Inner rings must never reference or depend on elements of outer rings.
- Subprojects will be created in Gradle for `core-domain`, `core-application`, and `core-infrastructure`.

## Risks
- Developers might bypass boundaries (e.g. referencing `org.jetbrains.exposed` in `core-domain`) to save development time. This will be checked using automated ArchUnit tests in Phase 2.

## Migration Strategy
N/A - Applied directly from Module 1 setup.

## Future Considerations
Review boundaries when compiling modules to multiplatform Wasm/iOS to ensure JVM-specific dependencies do not leak into multiplatform subprojects.

## Related ADRs
- [ADR 0003: Adopt Domain-Driven Design (DDD)](./0003-domain-driven-design.md)
- [ADR 0004: Standardize on a Modular Monolith Architecture](./0004-modular-monolith.md)

## References
- [The Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
