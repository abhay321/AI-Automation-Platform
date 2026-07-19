# Dependency Injection Framework

This directory contains the production-grade **Dependency Injection Framework** for the AI Automation Platform. It is built on top of the ultra-fast, lightweight [Koin](https://insert-koin.io/) container.

## Architecture & Integration

The dependency injection layer is integrated directly into the platform's **Lifecycle Management System**. It operates as a first-class `LifecycleAware` component (`DependencyInjectionPlatform`), ensuring that the object graph is constructed during bootstrap and cleaned up cleanly on shutdown.

```
       [LifecycleManager]
               │
               ▼
  ┌─────────────────────────┐
  │  DependencyInjection-   │
  │        Platform         │
  └────────────┬────────────┘
               │  onInitialize() -> startKoin()
               ▼
  ┌─────────────────────────┐
  │    Koin DI Container    │
  └────────────┬────────────┘
               │  (resolves)
               ▼
  ┌─────────────────────────┐
  │     PlatformConfig      │
  │   (Server, DB, Redis)   │
  └─────────────────────────┘
```

## Public API

### `DependencyInjectionPlatform`
A lifecycle-aware manager class that registers core config objects inside Koin, initializes the DI context, and handles graceful shutdown/termination.
- `registerModule(module: Module)`: Registers a custom Koin module prior to container boot. This allows third-party plugins to inject their own services.
- `id`: `"dependency-injection"`
- `dependencies`: `emptyList()` (boots near the top of the DAG).

### `DiContext`
A global Service Locator object designed for cases where constructor injection is impossible or undesired (e.g. static utility classes, extension functions, entry points).
- `get<T>()`: Resolves a dependency of type `T`. Throws `IllegalStateException` if the container is offline.
- `getOrNull<T>()`: Safely attempts resolution, returning `null` if unavailable.

---

## Technical Specifications & Characteristics

### Thread Safety
- The custom module registry utilizes a `CopyOnWriteArrayList` to support concurrent plugin registrations.
- Container start/stop phases within `DependencyInjectionPlatform` are protected by a JVM-intrinsic `synchronized(this)` lock block to prevent race conditions during parallel state shifts or redundant context launches.

### Memory & Performance Bounds
- **Time Complexity**: Registration of custom modules and retrieval via `DiContext` are $O(1)$ constant time operations.
- **Resource Cleanup**: Koin context is stopped (`stopKoin()`) and the custom module cache is cleared during the `onShutdown` and `onCleanup` phases, preventing classloader-level memory leaks.

---

## Configuration Bindings

On boot, `DependencyInjectionPlatform` automatically binds the loaded `PlatformConfig` properties to the Koin graph. Downstream components can inject individual config classes:

- `ServerConfig` (Port, Host)
- `DatabaseConfig` (URL, Username, Password)
- `RedisConfig`
- `RabbitMqConfig`
- `MinioConfig`
- `QdrantConfig`
- `SecurityConfig`
- `MetricsConfig`

---

## Future Extensibility Points

1. **Dynamic Plugin Scanning**: In future phases, a custom annotation scanner can run during `onBootstrap` to scan classpath directories and automatically register classes annotated with `@PlatformModule` or `@Injectable`.
2. **Koin Scope Isolation**: For multi-tenant workloads, individual worker scopes can be created and disposed dynamically using Koin's scoped session API.
