# ADR 0005: Use Ktor as the Server Framework Over Spring Boot

## Status
Accepted

## Date
2026-07-19

## Context
Choosing a server-side framework for an offline-first JVM orchestrator is highly consequential. Traditional enterprise Java projects use **Spring Boot**. While Spring Boot has massive community adoption and thousands of plugins, it is exceptionally heavy. 

Spring Boot relies on heavy runtime reflection, annotation scanners, dynamic proxies, and auto-configurations. This results in high memory overhead (often 300MB+ idling), slow cold start times (often 10-30 seconds), and difficult debugging due to deep stack traces. For a local-first application where users expect the engine to start instantly and run quietly in the background on limited laptop memory, Spring's resource profile is unacceptable.

We need a lightweight, non-blocking, modern Kotlin server framework designed for low latency, low resource footprint, and native coroutines.

## Problem Statement
Which backend server framework should provide the HTTP routing, JSON serialization, and WebSockets engine for the platform?

## Decision
Use **Ktor** as the primary HTTP server and client framework. 

Ktor is an asynchronous framework built from the ground up by JetBrains using Kotlin Coroutines. It is designed to be highly modular, allowing us to include only the features we actively use (Content Negotiation, WebSockets, Status Pages, Auth) without any magic annotation scanning or runtime reflection.

## Alternatives Considered
- **Spring Boot**: Rejected due to high RAM overhead (300-500MB on boot), slow startup times, and heavy reliance on Java reflection which makes compilation to GraalVM Native Image highly fragile.
- **Vert.x**: Exceptionally performant, but has a callback-heavy reactive design that does not integrate as cleanly with Kotlin's coroutines as Ktor does.
- **Micronaut / Quarkus**: Excellent modern frameworks with fast startup times, but they still introduce a lot of boilerplate code, complex dependency configurations, and do not provide Ktor's pure, elegant DSL configurations.

## Advantages
- **Extremely Lightweight**: Starts in under 1 second and consumes less than 30MB of RAM idle, making it perfect for local-first desktop integration.
- **Coroutine Native**: Asynchronous non-blocking pipelines are handled natively with Coroutines, avoiding thread starvation under high WebSocket concurrency loads.
- **Modular Design (Features/Plugins)**: We explicitly add features we want (e.g. `install(ContentNegotiation)`). This leaves the core runtime clean and highly predictable.
- **Unified Engine**: Ktor provides both a high-performance HTTP server engine and a matching HTTP client, streamlining communication with third-party SaaS APIs.

## Disadvantages
- **No Magic**: Spring automates database mappings, security setups, and task scheduling via annotations. In Ktor, everything must be explicitly configured in Kotlin code, which requires slightly more upfront code configuration.

## Consequences
- Routing, WebSockets, and serialization pipelines will be structured inside the `core-infrastructure` delivery packages using Ktor's DSL syntax.
- Startup and logging setups must explicitly coordinate Ktor engine triggers.

## Risks
- Developers coming from Spring might struggle with the lack of declarative annotations (e.g. `@RestController`, `@Autowired`). This is mitigated by providing rich boilerplate-free utilities and thorough onboarding guides.

## Migration Strategy
N/A - Adopted as the core network runtime for the platform.

## Future Considerations
Ensure Ktor configuration blocks are isolated from the core application layer to permit potential multiplatform target setups in the future.

## Related ADRs
- [ADR 0001: Use Kotlin as the Primary Platform Language](./0001-use-kotlin.md)
- [ADR 0006: Standardize on Koin for Dependency Injection](./0006-koin-over-other-di.md)

## References
- [Ktor Official Documentation](https://ktor.io/)
- [Ktor vs Spring Boot Benchmarks](https://ktor.io/docs/comparison.html)
