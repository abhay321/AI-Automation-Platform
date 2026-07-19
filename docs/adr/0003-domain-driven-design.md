# ADR 0003: Adopt Domain-Driven Design (DDD)

## Status
Accepted

## Date
2026-07-19

## Context
When orchestrating complex, non-deterministic structures like AI agents, workflows, vector storage, and dynamic custom plugin variables, traditional CRUD-centric models fall apart. In a CRUD model, an object's state is mutated arbitrarily by any controller or service, making complex state machine transitions (like a workflow execution turning from `PENDING` to `RUNNING`, branching, and handling failures) incredibly difficult to trace, secure, and debug.

We need a structured, ubiquitous language and strict logical boundaries to encapsulate state mutations, enforce invariants, and model complex execution events.

## Problem Statement
How should the platform represent complex business concepts—such as workspaces, user credentials, execution workflows, runtime nodes, vector databases, and integrations—to ensure business rules are consistently enforced?

## Decision
Adopt **Domain-Driven Design (DDD)** concepts in the `core-domain` and `core-application` layers. We will organize the system around rich domain boundaries and use specific DDD constructs:

1. **Aggregate Roots**: Entities that control access and manage lifecycle invariants for child elements. For example, `Workflow` is an aggregate root that manages a collection of `Node` and `Edge` objects. Workspaces represent an Aggregate Root that scopes projects, credentials, and assets.
2. **Entities**: Objects defined by unique identifiers rather than their attributes (e.g., `Node`, `ExecutionCheckpoint`).
3. **Value Objects**: Immutable objects defined strictly by their attributes, with no identity (e.g., `CronExpression`, `MemoryContext`, `VariableMap`). Value objects encapsulate validation logic inside their instantiation constructors.
4. **Domain Events**: Records of state changes that have occurred (e.g., `WorkflowExecutionCompleted`, `NodeExecutionFailed`). These will be published to coordinate side effects (like updating dashboard widgets or pushing slack alerts).
5. **Ubiquitous Language**: Standardize terminology across product, UX, core code, plugins, and REST endpoints (e.g., using "Workspace", "Execution", "Connector", "Node", "Edge").

## Alternatives Considered
- **Anemic Domain Model (CRUD-centric)**: Domain models are simple getters/setters, and all validation and state transitions are scattered across service classes. This is highly error-prone for complex DAG engines, leading to race conditions and inconsistent workflow execution states.

## Advantages
- **Robust Invariants**: Objects are always created in a valid state. You cannot instantiate a `Node` with dangling connections, nor can you progress an `Execution` state out-of-order.
- **Traceability**: Changes inside aggregate roots are governed by explicit domain-level actions (e.g., `workflow.addNode(...)`, `workflow.run(...)`), making the code clean and self-documenting.
- **High-Performance Event Integration**: Domain events integrate smoothly with logging and real-time WebSockets telemetry channels.

## Disadvantages
- **Learning Curve**: Requires understanding domain modeling patterns, aggregate scopes, and transaction boundaries.
- **Boilerplate**: Instantiating value objects for simple string fields increases type density and mapping routines.

## Consequences
- Reusability of business concepts inside `core-domain` is guaranteed.
- The state machine execution code remains clean of networking and SQL plumbing.

## Risks
- Designing aggregates too large (e.g., placing all workspace files inside a single `Workspace` aggregate root in-memory) will lead to performance lockups. Aggregates must remain small and reference other aggregates strictly by ID.

## Migration Strategy
N/A - Directly integrated.

## Future Considerations
Ensure domain event structures are backward-compatible as we introduce versioning to workflows and nodes.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0012: Implement a Plugin-First Architecture](./0012-plugin-first-architecture.md)

## References
- *Domain-Driven Design: Tackling Complexity in the Heart of Software* by Eric Evans
- *Implementing Domain-Driven Design* by Vaughn Vernon
