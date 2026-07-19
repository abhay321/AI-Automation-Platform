# ADR 0001: Use Kotlin as the Primary Platform Language

## Status
Accepted

## Date
2026-07-19

## Context
When building a highly reliable, cross-platform, enterprise-grade AI automation orchestrator, language selection is the most foundational decision. First-generation orchestrators are split between Node.js/TypeScript (excellent developer experience, weak multi-threaded safety, high memory overhead in long-running processes) and Python (exceptional ML ecosystem, lacks strict type enforcement, poor asynchronous scaling models, high CPU latency). 

We need a technology that offers extreme memory and thread safety, first-class asynchronous concurrency models (Coroutines), static typing, strong compilation safety, high execution speed, and seamless multiplatform compilation capabilities.

## Problem Statement
What primary programming language should the AI Automation Platform leverage to ensure industrial-grade state machine execution, cross-platform UI rendering, and type-safe SDK interfaces?

## Decision
Use **Kotlin** as the primary programming language for the entire platform. 
We will leverage Kotlin for both JVM targets (core backend engine execution, database transactions, object storage drivers) and multiplatform targets (Compose Multiplatform Desktop Control Center).

## Alternatives Considered
- **Java**: Highly performant, but extremely verbose, lacks native coroutines (forcing thread-per-request or complex Virtual Thread configurations), and misses modern ergonomic syntaxes (null-safety, extension functions, data classes).
- **TypeScript (Node.js)**: Exceptional UI integration ecosystem, but suffers from single-threaded limitations, struggles with massive multi-threaded DAG workflows, lacks enterprise compilation guarantees, and has high RAM consumption.
- **Python**: Universal ML standard, but lacks rigid enterprise type-safety, has high runtime latencies, and lacks performance for intense concurrent operational loops under load.

## Advantages
- **Ergonomics & Safety**: Built-in null safety, properties, and data structures reduce boilerplate code by 40-50% compared to Java.
- **Structured Concurrency**: Kotlin Coroutines provide highly optimized, lightweight, non-blocking asynchronous execution, permitting millions of concurrent tasks with minimal memory footprint.
- **Multiplatform Capability**: Enables shared codebases between backend engines and visual desktop interfaces via Kotlin Multiplatform (KMP), conserving valuable development cycles.
- **Enterprise Ecosystem**: Absolute interoperability with the vast Java/JVM library catalog, databases, metrics, and security libraries.

## Disadvantages
- **Compilation Speed**: Kotlin compilation is slower than Java and Go due to compiler verification steps.
- **Talent Pool**: Finding skilled Kotlin JVM developers who are comfortable outside the standard Android/Spring Boot ecosystem can be challenging compared to Python or Node.js.

## Consequences
- The backend core engine and SDK structures will be compiled targeting the JVM.
- Gradle becomes the primary build tool.
- Developers must align on idiomatic Kotlin styles (e.g., standard standard library utilities, extension patterns, Coroutines).

## Risks
- Incorrect use of coroutines can block threads if blocking calls are not correctly offloaded to `Dispatchers.IO`. This will be mitigated by enforcing strict static analysis and code review guidelines.

## Migration Strategy
N/A - Established as the starting baseline.

## Future Considerations
Revisit only if native WebAssembly (Wasm) or native machine compilation constraints dictate transitioning major portions of the engine to Rust.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0005: Use Ktor as the Server Framework Over Spring Boot](./0005-ktor-over-spring.md)

## References
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html)
