# ADR 0006: Standardize on Koin for Dependency Injection

## Status
Accepted

## Date
2026-07-19

## Context
Dependency Injection (DI) is critical for coordinating services, repositories, and configurations in a Clean Architecture. In standard Java/Kotlin applications, **Dagger** or **Guice/Spring** are the defaults. Dagger relies on complex code generation (kapt/ksp) which significantly slows down build speeds and complicates multi-module builds. Guice and Spring rely on runtime reflection and classpath scanning, which makes them slow to boot, adds massive memory footprints, and conflicts with local-first, low-resource mandates.

We need a lightweight, pure Kotlin DI library that requires no code generation, uses a type-safe DSL, starts instantly, and runs seamlessly across JVM and Multiplatform environments.

## Problem Statement
What Dependency Injection framework should be standardized to wire the platform modules and services together?

## Decision
Standardize on **Koin** as our primary Dependency Injection framework. 

Koin is a pragmatic, lightweight, service-locator-based DI framework written in pure Kotlin. It requires zero compilation processing or code generation, boots instantly with zero memory overhead, and leverages a clean, type-safe DSL to declare module dependencies.

## Alternatives Considered
- **Dagger 2 / Hilt**: Extremely fast at runtime, but relies on complex annotation processors that increase compilation times and can be difficult to configure inside multi-project Gradle builds.
- **Spring DI**: Extremely heavy, slow to boot, and requires dynamic runtime class scanning which conflicts with our performance goals.
- **Manual Dependency Injection**: Writing dependency graphs by hand. While highly performant and transparent, it is too tedious to maintain as our platform expands to support dozens of SDKs and community plugins.

## Advantages
- **Instant Boot**: No annotation scanning or dynamic class analysis; Koin resolves dependency definitions instantly on startup.
- **Kotlin Multiplatform Native**: Koin supports JVM, Native, JS, and Wasm, providing future-proof alignment as our UI moves fully towards Compose Multiplatform.
- **Type-safe DSL**: Standardized, clean Kotlin builder code:
  ```kotlin
  val databaseModule = module {
      single { HikariDataSource() }
      single<WorkflowRepository> { ExposedWorkflowRepository(get()) }
  }
  ```
- **Frictionless Testing**: We can easily swap modules or mock dependencies in tests via Koin's test framework helpers.

## Disadvantages
- **Runtime Resolution**: Koin resolves dependencies on startup/runtime rather than at compilation-time. If a dependency is missing, it will crash during execution rather than fail compilation. This is mitigated by enforcing our Koin DRY validation checks inside unit testing.

## Consequences
- All components must define their classes inside Koin module blocks.
- Class dependencies should be explicitly passed via constructor parameters to ensure easy testing.

## Risks
- Missing dependency registrations are caught at runtime instead of compile-time. We mitigate this by including a standard `KoinValidationTest` in our test suite that verifies the entire dependency tree resolves cleanly on every build.

## Migration Strategy
N/A - Directly wired into the platform bootstrap layer.

## Future Considerations
Monitor Koin's compiler-safe plugins if they mature, but prefer the simple, transparent runtime DSL for ease of use.

## Related ADRs
- [ADR 0001: Use Kotlin as the Primary Platform Language](./0001-use-kotlin.md)
- [ADR 0005: Use Ktor as the Server Framework Over Spring Boot](./0005-ktor-over-spring.md)

## References
- [Koin Documentation](https://insert-koin.io/)
- [Koin Performance Benchmarks](https://insert-koin.io/docs/setup/v3/)
