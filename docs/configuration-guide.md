# Configuration Framework Guide

## Version 1.0 (Platform Runtime)

This guide provides a comprehensive overview of the **Unified Configuration Framework** built for the platform's multi-module architecture. Built with Kotlin, **Hoplite** (type-safe YAML parsing), and standard reflection, this module powers the loading, validation, secret isolation, dynamic reloading, and setting-UI metadata for every component across development, testing, and production environments.

---

## 1. Configuration Sources & Precedence

To support flexible deployments (from local development machines to Kubernetes pods), properties are loaded in a strict, descending priority order. Values defined in higher layers completely override values in lower layers:

```
  ┌─────────────────────────────────────────┐
  │ 1. Runtime Map Overrides (Highest)      │  ◄── Passed programmatically / setting overrides
  └────────────────────┬────────────────────┘
                       ▼
  ┌─────────────────────────────────────────┐
  │ 2. Secret Volume Files                  │  ◄── Mounted inside secure pods (e.g. /run/secrets)
  └────────────────────┬────────────────────┘
                       ▼
  ┌─────────────────────────────────────────┐
  │ 3. System Environment Variables         │  ◄── Standard shell env (e.g. DATABASE_URL)
  └────────────────────┬────────────────────┘
                       ▼
  ┌─────────────────────────────────────────┐
  │ 4. Profile-Specific YAML Config         │  ◄── application-dev.yml, application-prod.yml
  └────────────────────┬────────────────────┘
                       ▼
  ┌─────────────────────────────────────────┐
  │ 5. Base YAML Configuration              │  ◄── application.yml (common defaults)
  └────────────────────┬────────────────────┘
                       ▼
  ┌─────────────────────────────────────────┐
  │ 6. Hardcoded Constructor Defaults       │  ◄── fallback val parameters in Kotlin classes
  └─────────────────────────────────────────┘
```

### Precedence Priority Detail
1. **Runtime Overrides**: High-priority programmatically provided configuration maps. This permits hot patches, integration-test level overrides, or user settings UI changes.
2. **Secret Volume Files**: Files located in the directory defined by the `PLATFORM_SECRETS_DIR` environment variable (defaults to the `./secrets` folder). The file name corresponds to the config path where underscores are replaced by dots (e.g. `database_password` maps to the `database.password` property).
3. **System Environment Variables**: Read directly from the system context. Variables like `DATABASE_URL` map cleanly via Hoplite's standard environment resolvers.
4. **Profile-Specific YAML**: Files matching `application-${profile}.yml` located in resources. Supported profiles are:
   - `dev` (Local development, debug modes active, mock databases allowed)
   - `test` (Embedded services, isolation, fast startup)
   - `prod` (Immutable clusters, strict security rules, real databases required)
5. **Base YAML Config**: `application.yml` contains common defaults shared by all environments.
6. **Hardcoded Defaults**: Declared directly on the properties of the data classes inside `PlatformConfig.kt` (e.g., `val port: Int = 3000`).

---

## 2. Configuration Metadata & Annotations

Every property inside `PlatformConfig.kt` is decorated with the custom `@ConfigProperty` annotation. This metadata is parsed at runtime to construct diagnostics and will later power the setting customization UI:

```kotlin
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigProperty(
    val description: String,
    val defaultValue: String = "",
    val isRequired: Boolean = true,
    val validationRules: String = "",
    val example: String = "",
    val displayName: String = ""
)
```

### Example Usage
```kotlin
data class ServerConfig(
    @ConfigProperty(
        description = "Port on which the HTTP server binds and listens",
        defaultValue = "3000",
        isRequired = true,
        validationRules = "Must be between 1 and 65535",
        example = "3000",
        displayName = "Server Port"
    )
    val port: Int = 3000
)
```

---

## 3. Structured Validation & Categories

The framework isolates configuration issues immediately using structured, categorized validations. This avoids cascade errors down-the-road during message queue listening, DB transactions, or socket read/writes.

Errors are grouped under one of the following four **Validation Categories**:

| Category | Intended Scope | Failure Action | Example Property |
| :--- | :--- | :--- | :--- |
| **STARTUP** | Crucial properties needed to initialize the process or bind a network socket. | Fail-Fast immediately (Throw Exception on Startup) | `server.port`, `server.host` |
| **RUNTIME** | Algorithmic thresholds or bounds that direct execution. | Warning Log or Throw on dynamic reload | `server.requestTimeoutMs`, `database.maximumPoolSize` |
| **SECURITY** | Sensitive encryption, authorization secrets, and signature keys. | Warning / Strict Mode enforcement in `prod` | `security.encryptionKey`, `security.tokenSecret` |
| **DEPENDENCY** | Parameters that define external cluster connectivity, hosts, or storage locations. | Fail-fast during initialization or client lookup | `database.url`, `minio.endpoint` |

### Code Validation Execution
```kotlin
val errors = ConfigDiagnostics.validate(config)
// Filter for startup failures
val startupErrors = errors.filter { it.category == ValidationCategory.STARTUP }
if (startupErrors.isNotEmpty()) {
    throw IllegalArgumentException("Startup aborted: Missing or malformed keys.")
}
```

---

## 4. Dynamic Reload Capabilities

Not all configuration modifications require a heavy JVM restart. The framework divides parameters into **Immutable (Restart Required)** and **Mutable (Refreshable)**:

### Immutable Properties (Require Application Restart)
- **Networking/Sockets**: `server.port`, `server.host`, `server.contextPath`. Rebinding active sockets breaks client routing pools.
- **Primary Data Sources**: `database.url`, `database.driver` mapping. Altering these at runtime is highly disruptive and violates cluster transactions.
- **Symmetric Keys**: `security.encryptionKey`. Changing this dynamically would lead to un-decryptable database states.

### Mutable Properties (Refreshable Dynamically)
- **Database Connection Sizing**: `database.maximumPoolSize`, `database.minimumIdle`, `database.idleTimeoutMs`, `database.connectionTimeoutMs`. (HikariCP natively supports scaling pool sizes dynamically).
- **Timeouts**: `redis.timeoutMs`, `rabbitmq.connectionTimeoutMs`.
- **JWT Lifetimes**: `security.tokenExpirationMinutes`.
- **Metrics Scraping Toggles**: `metrics.enabled`, `metrics.stepSeconds`.

### Event-Driven Propagation
`ReloadableConfig` wraps the underlying configuration and registers listeners:
```kotlin
val reloadableConfig = ReloadableConfig(loader)

// Subscribe to reload events
reloadableConfig.addListener { newConfig ->
    println("Configuration dynamically updated. New Max Pool Size: ${newConfig.database.maximumPoolSize}")
}

// Trigger reload safely
val result = reloadableConfig.reload()
```

---

## 5. Security & Sensitive Value Masking

To prevent accidental leaks of sensitive credentials in logs, diagnostics, or telemetry endpoints, the framework provides a masked export utility. 

Any property key containing sensitive terms (`password`, `secret`, `key`, `token`, `credential`, `accesskey`, `secretkey`) is replaced with `********` during serialization:

```kotlin
// Safely output to administrative console or JSON telemetry API
val maskedJson = ConfigDiagnostics.exportMasked(config)
println(maskedJson) 
// Output: { database: { username: "platform_admin", password: "********" } }
```

---

## 6. Diagnostics & Reports

Administrators can request a detailed configuration audit through diagnostic commands or API endpoints:

```kotlin
val report = ConfigDiagnostics.generateReport(
    activeProfile = "prod",
    config = loadedConfig,
    providedSecretsKeys = emptyList()
)
```

The resulting `DiagnosticsReport` contains:
- **Loaded Profile**: Active environment name (e.g. `dev`, `test`, `prod`).
- **Sources Precedence**: Visual audit of the loading paths.
- **Missing Secrets**: High-priority alert of missing or default production secrets.
- **Deprecated Properties**: Highlight of keys marked for removal in future versions.
- **Validation Errors**: All active errors across all validation categories.
- **Unknown Properties**: Properties configured in files that do not correspond to any field in `PlatformConfig`.
