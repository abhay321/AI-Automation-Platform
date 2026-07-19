# ADR 0011: Use RabbitMQ for External Event Messaging

## Status
Accepted

## Date
2026-07-19

## Context
As an enterprise-grade AI automation platform, the core engine must interact with external systems asynchronously. When an automation finishes, we may need to dispatch events to webhook consumers, log execution history to secondary auditing databases, trigger external micro-services, or distribute executing tasks among multiple distributed backend workers. 

If we handle these external, slow integrations synchronously inside our main execution threads, we block critical coroutines, increase the risk of transactional failures, and severely limit platform throughput.

We need a resilient, highly stable external message broker that supports reliable queuing, message acknowledgement (ACKs), routing configurations, and robust retries.

## Problem Statement
What messaging broker should coordinate distributed operations, external webhooks, and asynchronous retry logic across the platform?

## Decision
Use **RabbitMQ** as our primary message broker for external/distributed events. 
We will encapsulate all message routing within an `EventBus` interface. In local-first or simple development modes, the platform can use an in-memory Event Bus implementation to avoid infrastructure overhead, transitioning to RabbitMQ in clustered/production modes via our DI profiles.

## Alternatives Considered
- **Apache Kafka**: Exceptional for massive streaming telemetry logs, but overkill for standard workflow orchestrations. Kafka has high RAM demands and complex local configurations, which conflict with local-first setups.
- **AWS SQS**: Managed cloud service that prevents offline execution and introduces cloud-vendor lock-in.
- **In-Memory Messaging Only**: Extremely fast but fragile; any crash results in lost event logs and prevents clustering multiple stateless engine nodes together.

## Advantages
- **Robust Message Delivery**: Native support for manual ACKs, dead-letter exchanges (DLX) for failed messages, and automatic retries.
- **Flexible Routing**: AMQP exchanges (Direct, Fanout, Topic) allow us to configure highly sophisticated event routing strategies dynamically.
- **Lightweight Resource Profile**: Consumes far less RAM compared to Kafka, running easily inside cheap development containers.
- **High Reliability**: Highly stable, battle-tested, and trusted by thousands of enterprise environments.

## Disadvantages
- **Serialization Overhead**: Requires serializing event models to/from JSON strings for transport. (Mitigated by standardizing on Jackson inside our transport layers).

## Consequences
- Event publishers and consumers will execute through abstract ports inside `core-application`, keeping the concrete RabbitMQ code fully isolated in `core-infrastructure`.

## Risks
- If RabbitMQ goes down, event dispatches could fail. We mitigate this by establishing a persistent Local Outbox pattern inside PostgreSQL: events are saved to the database in the same transaction as the state change, and are asynchronously pushed to RabbitMQ, guaranteeing **at-least-once** delivery.

## Migration Strategy
All message models will incorporate version numbers (e.g. `WorkflowExecutedV1`) to prevent deserialization failures when updating schemas.

## Future Considerations
Review performance as queues scale, ensuring we establish proper exchange partition strategies.

## Related ADRs
- [ADR 0004: Standardize on a Modular Monolith Architecture](./0004-modular-monolith.md)
- [ADR 0020: Adopt Structured JSON Logging & Observability Standards](./0020-observability-strategy.md)

## References
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Enterprise Integration Patterns: Message Channels](https://www.enterpriseintegrationpatterns.com/patterns/messaging/MessageChannel.html)
