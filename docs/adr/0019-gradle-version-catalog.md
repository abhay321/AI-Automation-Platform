# ADR 0019: Standardize on Gradle Version Catalogs

## Status
Accepted

## Date
2026-07-19

## Context
Our platform is structured as a multi-project Gradle monorepo containing dozens of submodules (core domains, applications, infrastructures, dynamic plugin SDKs, and multiple custom plugins). 

Historically, multi-project Gradle builds suffer from "dependency drift". A developer updates the version of a library (e.g., Jackson or Ktor) in one submodule but forgets to update it in another. This results in hard-to-debug classpath conflicts (`NoSuchMethodError`, `ClassCastException`) during runtime executions.

We need a central, type-safe, declarative location that coordinates all dependency versions, plugins, and libraries across all submodules in the entire monorepo.

## Problem Statement
How should the platform manage library and compiler plugin dependencies across multiple Gradle modules to guarantee absolute version consistency?

## Decision
Standardize on **Gradle Version Catalogs** using the default file location `/gradle/libs.versions.toml`. 

All dependencies, compiler plugins, and version definitions must be declared inside this file. Submodules reference libraries from this catalog using type-safe accessors (e.g., `libs.ktor.server.core`) rather than hardcoded string parameters.

```
                        +--------------------------------+
                        |   /gradle/libs.versions.toml   |
                        +---------------+----------------+
                                        | Coordinates
                                        |
              +-------------------------+-------------------------+
              |                                                   |
+-------------v--------------+                      +-------------v--------------+
|     /platform/             |                      |   /core-infrastructure/    |
|   build.gradle.kts         |                      |     build.gradle.kts       |
|                            |                      |                            |
|  implementation(libs.koin) |                      |  implementation(libs.ktor) |
+----------------------------+                      +----------------------------+
```

## Alternatives Considered
- **Hardcoded Strings inside each build.gradle.kts**: Simple initially, but leads to massive dependency drift and unmaintainable upgrades as modules scale.
- **Gradle extra properties (ext block)**: A historic standard, but lacks type-safety, lacks IDE autocomplete support, and is prone to spelling errors.

## Advantages
- **Single Source of Truth**: Upgrading a library across the entire platform involves editing a single line inside `libs.versions.toml`.
- **Type-Safe Accessors**: Gradle automatically compiles catalog keys into type-safe accessor methods, enabling seamless IDE autocomplete and immediate build-time error detection.
- **Plugin Bundling**: Allows us to declare and apply unified plugins (e.g., Kotlin serialization) across modules with identical versions.

## Disadvantages
- **Slight Configuration Learning Curve**: Requires developers to follow TOML syntax structures for adding new libraries.

## Consequences
- No hardcoded dependency versions are allowed inside subprojects' `build.gradle.kts` files.
- All libraries are accessed through the `libs` namespace.

## Risks
- Typing errors inside the TOML file can break Gradle configuration phases. We mitigate this by validating build structures in our CI pipelines on every commit.

## Migration Strategy
Applied during our initial Module 1 monorepo scaffold.

## Future Considerations
Enforce periodic dependency update audits using tools like the `ben-manes` dependency updates plugin in later project phases.

## Related ADRs
- [ADR 0004: Standardize on a Modular Monolith Architecture](./0004-modular-monolith.md)

## References
- [Gradle Version Catalogs Documentation](https://docs.gradle.org/current/userguide/platforms.html)
- [Managing Dependencies with TOML Catalogs](https://developer.android.com/build/migrate-to-catalogs)
