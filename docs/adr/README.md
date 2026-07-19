# Architectural Decision Records (ADR)

This directory contains the Architectural Decision Records (ADRs) for the **AI Automation Platform**.

## What is an ADR?

An Architectural Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences. It serves as the official historical record of the platform's architectural evolution and provides governance for future extension designs.

## Why Do We Use ADRs?

1. **Context Preservation**: Future contributors can understand *why* a decision was made, what trade-offs were accepted, and what alternatives were rejected, avoiding the re-litigation of stable architectures.
2. **Technical Alignment**: Ensures all contributors adhere to the same engineering paradigms and design systems.
3. **Transparent Governance**: Outlines the technical philosophy of this open-source project in a clear, public, and structured format.

## How to Create a New ADR

Contributors proposing a significant change to the platform's architecture should follow this process:

1. Copy [TEMPLATE.md](./TEMPLATE.md) to a new file in this directory.
2. Number the file sequentially, following the format `NNNN-short-descriptive-title.md` (e.g., `0022-use-sqlite-for-embedded.md`).
3. Set the status to **Proposed**.
4. Submit a Pull Request containing the proposed ADR.
5. Participate in discussion and address community feedback.
6. Once the Core Maintainers accept the PR, update the status to **Accepted**.

## How Decisions Become Accepted

Decisions are evaluated on:
- Alignment with our core tenants (Local-First, Offline-First, Clean Architecture, Extensibility).
- Impact on developer experience and system performance.
- Long-term maintenance overhead.

## How to Supersede an ADR

Architectural demands shift over time. When a decision is overridden:
1. Create a new ADR explaining the new decision.
2. Mark the status of the new ADR as **Accepted**.
3. Reference the old ADR inside the new one using the `Related ADRs` section.
4. Update the status of the old ADR to **Superseded** and add a link pointing to the new ADR.

---

## Index of Architectural Decisions

| ID | Title | Status | Date |
|:---|:---|:---|:---|
| [0001](./0001-use-kotlin.md) | Use Kotlin as the Primary Platform Language | **Accepted** | 2026-07-19 |
| [0002](./0002-clean-architecture.md) | Adopt Clean Architecture Principles | **Accepted** | 2026-07-19 |
| [0003](./0003-domain-driven-design.md) | Adopt Domain-Driven Design (DDD) | **Accepted** | 2026-07-19 |
| [0004](./0004-modular-monolith.md) | Standardize on a Modular Monolith Architecture | **Accepted** | 2026-07-19 |
| [0005](./0005-ktor-over-spring.md) | Use Ktor as the Server Framework Over Spring Boot | **Accepted** | 2026-07-19 |
| [0006](./0006-koin-over-other-di.md) | Standardize on Koin for Dependency Injection | **Accepted** | 2026-07-19 |
| [0007](./0007-postgresql-with-jsonb.md) | Use PostgreSQL with JSONB for Workflow State Persistence | **Accepted** | 2026-07-19 |
| [0008](./0008-redis-for-cache.md) | Use Redis for Caching and Distributed Locking | **Accepted** | 2026-07-19 |
| [0009](./0009-qdrant-vector-database.md) | Standardize on Qdrant as the Vector Database | **Accepted** | 2026-07-19 |
| [0010](./0010-minio-object-storage.md) | Standardize on MinIO for Object Storage | **Accepted** | 2026-07-19 |
| [0011](./0011-rabbitmq-event-messaging.md) | Use RabbitMQ for External Event Messaging | **Accepted** | 2026-07-19 |
| [0012](./0012-plugin-first-architecture.md) | Implement a Plugin-First Architecture | **Accepted** | 2026-07-19 |
| [0013](./0013-sdk-first-design.md) | Establish a Strict SDK-First Component Separation | **Accepted** | 2026-07-19 |
| [0014](./0014-local-ai-first.md) | Adopt a Local-AI First Execution Policy | **Accepted** | 2026-07-19 |
| [0015](./0015-open-source-first.md) | Establish an Open-Source First Engineering Stance | **Accepted** | 2026-07-19 |
| [0016](./0016-offline-first.md) | Adopt an Offline-First Core Topology | **Accepted** | 2026-07-19 |
| [0017](./0017-compose-multiplatform-ui.md) | Use Compose Multiplatform for Desktop Control Center | **Accepted** | 2026-07-19 |
| [0018](./0018-docker-compose-development.md) | Provide Profile-Based Docker Compose for Local Dev | **Accepted** | 2026-07-19 |
| [0019](./0019-gradle-version-catalog.md) | Standardize on Gradle Version Catalogs | **Accepted** | 2026-07-19 |
| [0020](./0020-observability-strategy.md) | Adopt Structured JSON Logging & Observability Standards | **Accepted** | 2026-07-19 |
| [0021](./0021-security-foundation.md) | Standardize the Security and Encryption Foundations | **Accepted** | 2026-07-19 |
