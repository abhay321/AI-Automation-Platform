# ADR 0007: Use PostgreSQL with JSONB for Workflow State Persistence

## Status
Accepted

## Date
2026-07-19

## Context
AI automations deal with highly dynamic states. A node might accept a string schema, while another returns a massive JSON payload with deep nested lists of search results, embedding vectors, and media file references. 

Standard relational schemas are rigid. If we mapped every dynamic workflow execution state to strict SQL columns, we would end up with a sprawling, unmaintainable database schema. Conversely, using a pure Document database (like MongoDB) lacks the strict foreign key enforcement, relational transactions, ACID guarantees, and sophisticated relational joins needed to manage workspace structures, users, access tokens, and execution histories securely.

We need a hybrid database solution that supports both relational rigidity for governance data and schema-less flexibility for runtime states.

## Problem Statement
What database strategy should persist workspace metadata, user structures, security records, and dynamic DAG execution states?

## Decision
Use **PostgreSQL** as our primary relational database, and leverage **JSONB** columns to store dynamic schema-less data (such as Workflow variables, node settings configurations, execution parameters, and execution state trees). 

To interact with PostgreSQL cleanly from Kotlin without the bloat of Hibernate, we will use **JetBrains Exposed** ORM (the DAO/DSL variant).

## Alternatives Considered
- **MongoDB**: Excellent for dynamic workflow objects, but lacks strict foreign key checks, struggles with complex relational tables, and adds complex setup overhead.
- **SQLite**: Great for embedded local storage, but lacks JSONB index scaling, is not suitable for multi-user production environments, and lacks support for concurrent high-throughput operations.
- **MySQL**: Relational, but its JSON engine is less mature and less performant compared to PostgreSQL's highly advanced indexable JSONB support.

## Advantages
- **Relational Integrity with Document Flexibility**: We can enforce strict relational ties on workspaces, users, and workflows, while using JSONB columns to capture custom runtime variables and configurations.
- **Indexable JSON**: PostgreSQL allows us to index fields *inside* the JSONB column (via GIN indexes), making queries highly performant.
- **Exposed ORM Integration**: JetBrains Exposed gives us type-safe database queries and migrations written fully in Kotlin.
- **Universal Enterprise Standard**: Exceptional hosting support across all cloud and on-premise platforms.

## Disadvantages
- **PostgreSQL Setup Overhead**: Requires running a dedicated database server, unlike simple embedded files like SQLite. This is handled by providing ready-to-use Docker templates.

## Consequences
- The database schema must separate relational structural data (ID, workspaceId, dates) from dynamic configurations (using a JSON/JSONB text field mapped to Kotlin data classes via Jackson serialization).

## Risks
- Developers might over-rely on JSONB and fail to normalize fields that should be standard columns. We enforce that relational search criteria (like `workspaceId` or `status`) must remain standard columns with explicit foreign keys.

## Migration Strategy
Exposed will manage schema migrations at startup in development mode, while production deployments will use structured flyway/liquibase style steps in the future.

## Future Considerations
Exposed models will be structured under `core-infrastructure/database`, keeping the pure entities in `core-domain` completely clean of database-specific logic.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0018: Provide Profile-Based Docker Compose for Local Dev](./0018-docker-compose-development.md)

## References
- [PostgreSQL JSONB Documentation](https://www.postgresql.org/docs/current/datatype-json.html)
- [JetBrains Exposed Github](https://github.com/JetBrains/Exposed)
