# Component 1: Configuration Framework

Enterprise-grade, profile-aware, fail-fast configuration system backed by **Hoplite**. It provides absolute type safety for all runtime and infrastructure parameters, ensuring invalid configurations fail at startup rather than during live executions.

---

## 🚀 Usage

### 1. Active Profile Resolution
The active profile is selected based on the following precedence (highest first):
1. **Explicit Programmatic Argument** passed to `HopliteConfigLoader(runtimeProfile = "...")`.
2. **System Environment Variable**: `PLATFORM_PROFILE`.
3. **JVM System Property**: `-Dplatform.profile=...`.
4. **Default fallback**: `dev`.

### 2. Loading Configuration
Load the configuration securely anywhere in your dependency injection or server setup:

```kotlin
import com.aiplatform.platform.config.HopliteConfigLoader

val loader = HopliteConfigLoader()
val config = loader.load()

println("Server running on: ${config.server.host}:${config.server.port}")
```

### 3. Merging Behavior
Hoplite merges configurations using cascading priorities:
1. `Map overrides` (programmatic map overrides)
2. Environment Variables / JVM System Properties
3. `application-${profile}.yml`
4. `application.yml` (base default definitions)

---

## 🛠️ Adding New Configurations

To add a new configuration section, define your data classes inside `PlatformConfig.kt` and plug them into the root `PlatformConfig` data class:

```kotlin
data class PlatformConfig(
    // ... other parameters
    val myNewFeature: MyNewFeatureConfig
)

data class MyNewFeatureConfig(
    val enabled: Boolean,
    val maxConcurrentJobs: Int = 5
)
```

And define matching fields in `application.yml`:

```yaml
myNewFeature:
  enabled: true
  maxConcurrentJobs: 10
```

---

## 🔒 Security & Environment Overrides
To override sensitive keys in production using environment variables, use standard system variables:

* `DATABASE_URL` -> overrides `database.url`
* `SECURITY_ENCRYPTIONKEY` -> overrides `security.encryptionKey`
* `MINIO_ACCESSKEY` -> overrides `minio.accessKey`
