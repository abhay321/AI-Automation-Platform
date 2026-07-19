# ADR 0017: Use Compose Multiplatform for Desktop Control Center

## Status
Accepted

## Date
2026-07-19

## Context
Our early-stage plan evaluated a React/Vite web application as the primary user interface. However, for a high-performance automation system that runs locally and offline-first, a browser-based UI introduces limitations:
1. **CPU & RAM overhead**: Modern browser engines (and Electron wrappers) consumer massive memory arrays (often 500MB+ for a single running window).
2. **Double Declaration**: We must maintain duplicate domain models and DTO structures in both Kotlin (backend) and TypeScript (frontend).
3. **Canvas Performance**: Rendering massive node graphs containing hundreds of connected wires using HTML DOM or SVG can suffer from rendering lag, dragging speeds, and stutter.

We need a technology that aligns our frontend with our backend language (Kotlin), compiles to native high-performance machine pipelines, and runs smoothly with minimal RAM overhead.

## Problem Statement
What technology stack should implement the primary user-facing visual node canvas and operational control center for the platform?

## Decision
Use **Compose Multiplatform** (targeting Desktop/JVM) as our primary visual UI platform. 

Compose Multiplatform is a declarative UI framework developed by JetBrains, backed by the high-performance **Skia** 2D graphics library. This allows us to write our UI 100% in Kotlin, rendering directly to the system canvas at a smooth 60 frames per second.

```
+------------------------------------+
|     COMPOSE DESKTOP UI (KOTLIN)     |
+-----------------+------------------+
                  |  REST / WebSockets
                  v
+-----------------+------------------+
|      LOCAL KTOR CORE SERVICES      |
+------------------------------------+
```

## Alternatives Considered
- **React / Vite (SPA in Browser)**: Simple to build, but requires maintaining a duplicate TypeScript model system, lacks native OS integration, and struggles with massive SVG node rendering speeds.
- **Electron with React**: Unacceptably heavy, bundling an entire Chromium browser and Node.js process, yielding huge bundle sizes (150MB+) and high memory usage (300MB+ idle).
- **JavaFX**: Mature, but has an old, imperative layout paradigm that is less productive and modern compared to modern declarative, state-driven Compose code.

## Advantages
- **Single-Language Stack**: The entire platform—from ORM models, use cases, Ktor routes, to visual desktop buttons and canvas lines—is written exclusively in **Kotlin**.
- **Extreme Performance**: Skia-backed hardware accelerated canvas renders dense node-graphs, smooth panning, and animations beautifully at 60fps.
- **Shared Domain Models**: The desktop application imports domain structures from our shared monorepo modules directly, eliminating JSON serialization duplications and double declarations.
- **Low Memory Overhead**: Runs natively inside a tiny JVM process, using under 60-80MB of RAM.

## Disadvantages
- **Distribution Sizing**: Shipping a packaged desktop JVM app requires bundling a custom runtime image (using jlink), resulting in a larger initial installation package (usually 40-70MB).

## Consequences
- The React frontend is completely removed from the MVP.
- We establish the `:desktop-control-center` project module.
- The UI code can be compiled to other targets (WebAssembly/Wasm, macOS, Windows, Linux) in the future without rewriting core rendering structures.

## Risks
- Developers unfamiliar with Compose or declarative state patterns might introduce memory leaks inside mutable state parameters. We address this by establishing clear UI state management best practices.

## Migration Strategy
N/A - Direct setup.

## Future Considerations
Examine compiling the exact same Compose UI target to WebAssembly (Wasm) in later milestones to offer a web-based playground alternative.

## Related ADRs
- [ADR 0001: Use Kotlin as the Primary Platform Language](./0001-use-kotlin.md)
- [ADR 0004: Standardize on a Modular Monolith Architecture](./0004-modular-monolith.md)

## References
- [Compose Multiplatform Official Site](https://github.com/JetBrains/compose-multiplatform)
- [Skia Graphics Library](https://skia.org/)
