# ADR 0018: Provide Profile-Based Docker Compose for Local Dev

## Status
Accepted

## Date
2026-07-19

## Context
To support our **Offline-First** and **Local-First** principles, developers must run multiple infrastructure dependencies locally: PostgreSQL, Redis, MinIO, RabbitMQ, and Qdrant. 

If we force developers to manually install and configure each of these servers on their host operating system, local onboarding becomes exceptionally slow, frustrating, and prone to version drift. We need an automated, predictable, single-command system that manages all background services cleanly on any developer OS.

However, running *all* dependencies simultaneously can consume significant RAM and CPU, which can slow down lighter development setups (for example, when a developer is only testing database queries and does not need vectors or metrics active).

## Problem Statement
How should the platform automate local infrastructure dependencies while allowing developers to start only the specific services they currently need?

## Decision
Provide a **profile-based Docker Compose configuration** (`docker-compose.yml`) at the monorepo root. 

We will organize services into targeted logical **Profiles** using Docker Compose v2 profile parameters:
- `infra`: Core metadata database and cache (PostgreSQL, Redis).
- `messaging`: Broker queues (RabbitMQ).
- `storage`: S3 file manager (MinIO).
- `vector`: RAG indexer (Qdrant).
- `monitoring`: Metrics dashboard (Prometheus, Grafana).
- `all`: Boots the entire platform stack simultaneously.

## Alternatives Considered
- **Standard Docker Compose (No profiles)**: Simpler config, but boots all services unconditionally, consuming massive RAM and CPU on smaller developer laptops.
- **Manual Local Installation Guides**: Leads to consistent "works on my machine" bugs, version drift, and slow onboarding times.

## Advantages
- **On-Demand Infrastructure**: Developers execute highly targeted startup scopes:
  ```bash
  # Start only PostgreSQL and Redis for simple CRUD testing
  docker compose --profile infra up -d
  
  # Start only the vector database for RAG testing
  docker compose --profile vector up -d
  ```
- **Frictionless Onboarding**: A newly joined open-source contributor can run the complete infrastructure using a single command: `docker compose --profile all up -d`.
- **Hermetic Environments**: No cluttering of developer laptops with persistent host background services.

## Disadvantages
- **Docker Requirement**: Developers must have Docker and Docker Compose installed on their workstation.

## Consequences
- The root `docker-compose.yml` uses strict volume mappings and healthcheck indicators for each service, ensuring containers boot in correct sequence.
- All configuration addresses in `application.yml` map cleanly to standard container ports.

## Risks
- Containers can hold stale data during development. We address this by documenting simple container teardown commands (`docker compose down -v`) in our README.

## Migration Strategy
N/A - Direct setup.

## Future Considerations
We will add local test execution scripts that automatically spin up transient containers and teardown upon completion.

## Related ADRs
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)
- [ADR 0020: Adopt Structured JSON Logging & Observability Standards](./0020-observability-strategy.md)

## References
- [Docker Compose Profiles Documentation](https://docs.docker.com/compose/profiles/)
- [Twelve-Factor App: Dev/Prod Parity](https://12factor.net/dev-prod-parity)
