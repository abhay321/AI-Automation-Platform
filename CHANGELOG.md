# Changelog

All notable changes to the AI Automation Platform will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

---

## [1.0.0-M2] - 2026-07-19

### Component
- **Dependency Injection**

### Files Added
- `/platform/src/main/kotlin/com/aiplatform/platform/di/DependencyInjectionPlatform.kt`
- `/platform/src/main/kotlin/com/aiplatform/platform/di/DiContext.kt`
- `/platform/src/test/kotlin/com/aiplatform/platform/di/DependencyInjectionPlatformTest.kt`

### Files Modified
- `/docs/MILESTONES.md` (Progress status updated)

### Breaking Changes
- None

### Migration Notes
- Standard Hoplite configurations and platform registries are now registered and resolvable directly from the central DI container via Koin or the static lookup wrapper `DiContext`.

### Tests Added
- `com.aiplatform.platform.di.DependencyInjectionPlatformTest`
  - `test successful DI container setup and config bindings` (verifies PlatformConfig and subcomponents)
  - `test custom modules dynamic registration` (verifies external plugins can extend container)
  - `test registering module after initialization throws IllegalStateException` (prevents runtime modifications)
  - `test shutdown stops context and clears custom modules` (verifies clean resource cleanup)
  - `test context locator fails when Koin not started` (verifies robust failure boundaries)

### Known Limitations
- Dynamic class scanning at runtime is bypassed in favor of explicit module registration (`DependencyInjectionPlatform#registerModule`) to preserve startup speed and GraalVM native image compatibility.

---

## [1.0.0-M1] - 2026-07-19

### Component
- **Event Bus**

### Files Added
- `/platform/src/main/kotlin/com/aiplatform/platform/eventbus/engine/DefaultEventBus.kt`
- `/platform/src/test/kotlin/com/aiplatform/platform/eventbus/engine/DefaultEventBusTest.kt`

### Files Modified
- `/docs/MILESTONES.md` (Milestone progress updated)

### Breaking Changes
- None

### Migration Notes
- Replaced stub event bus with standard core-native, coroutine-powered `DefaultEventBus` supporting recursive pattern wildcards, sticky events, and tracing contexts.

### Tests Added
- `com.aiplatform.platform.eventbus.engine.DefaultEventBusTest`
  - Exact/wildcard matching delivery
  - Synchronous publishing block verification
  - Sticky event playback
  - Delayed delivery
  - Kotlin Flows mapping
  - MDC / Correlation trace context propagation
  - RabbitMQ-style external bridging

---

## [1.0.0-M0] - 2026-07-19
- **Initial Baseline Release** (Configuration, Observability, Runtime Kernel frozen architecture).
