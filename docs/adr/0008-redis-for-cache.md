# ADR 0008: Use Redis for Caching and Distributed Locking

## Status
Accepted

## Date
2026-07-19

## Context
In a high-density AI automation orchestrator, we face two critical operational requirements:
1. **Low Latency Query Caching**: Reading workflow definitions, API credentials, and active workspace nodes must bypass heavy SQL databases on every operational loop.
2. **Distributed Synchronization & Locking**: In clustered environments, multiple workflow engine processes must never execute the exact same trigger node concurrently, which would lead to duplicate runs and corrupted execution states.

We need a high-performance, memory-backed caching engine that supports advanced data structures, distributed locking keys, and rapid pub/sub notifications.

## Problem Statement
How should the platform cache configuration configurations and coordinate distributed locks and shared execution contexts in concurrent/clustered environments?

## Decision
Use **Redis** as the primary caching and distributed locking coordinate system. 
We will leverage **Redisson** as the client library, which abstracts raw redis connections into type-safe JVM locks, maps, and caches.

## Alternatives Considered
- **In-Memory Cache (Guava/Caffeine)**: Exceptionally fast, but limited to a single JVM instance. If we run multiple engine processes, state caches become out-of-sync, and distributed lock coordination is impossible.
- **PostgreSQL Advisory Locks**: Avoids running a new infrastructure dependency, but adds significant transactional CPU overhead on the database and lacks performance under high locking frequencies.
- **Apache ZooKeeper**: Highly resilient distributed locks, but extremely heavy, complex to manage, and lacks simple caching key capabilities.

## Advantages
- **Extreme Speed**: Reads and writes complete in sub-millisecond speeds, protecting the database from repetitive schema fetches.
- **Robust Redisson Locks**: Provides simple `RLock` utilities that handle lock leases, renewals, and fail-safes natively without manual lua script scripting:
  ```kotlin
  val lock = redisson.getLock("execution-lock-${workflowId}")
  if (lock.tryLock(10, TimeUnit.SECONDS)) { ... }
  ```
- **Versatile Data Types**: Natively supports hashes, sorted sets (perfect for priority queues), and publish/subscribe communication.

## Disadvantages
- **Infrastructure Dependency**: Adds Redis as a mandatory service for high-performance operations, increasing local development overhead. (Mitigated by making Redis optional in local developer modes or booting it instantly via Docker).

## Consequences
- Redis client services will be initialized inside `core-infrastructure`.
- High-frequency API keys and workflow definitions will be cached using standard TTL (Time-To-Live) parameters.

## Risks
- If Redis crashes or experiences network isolation, caches could return stale data or lock-ups occur. This will be mitigated by wrapping Redis calls in resilient fallback try-catch scopes that fall back to the direct database.

## Migration Strategy
Redis holds ephemeral/cache data only. No structural data migrations are necessary; simply flushing caches is sufficient.

## Future Considerations
Ensure the cache key scheme incorporates clear namespaces (e.g. `platform:workspaces:v1:...`) to allow zero-downtime cache key upgrades in future releases.

## Related ADRs
- [ADR 0004: Standardize on a Modular Monolith Architecture](./0004-modular-monolith.md)
- [ADR 0018: Provide Profile-Based Docker Compose for Local Dev](./0018-docker-compose-development.md)

## References
- [Redisson Official Site](https://redisson.org/)
- [Redis Distributed Lock Algorithm (Redlock)](https://redis.io/docs/manual/patterns/distributed-locks/)
