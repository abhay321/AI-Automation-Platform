package com.aiplatform.platform.config

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

enum class ValidationCategory {
    STARTUP,
    RUNTIME,
    SECURITY,
    DEPENDENCY
}

data class ValidationError(
    val path: String,
    val category: ValidationCategory,
    val message: String
)

data class DiagnosticsReport(
    val loadedProfile: String,
    val sourcesPrecedence: List<String>,
    val missingSecrets: List<String>,
    val deprecatedProperties: List<String>,
    val validationErrors: List<ValidationError>,
    val unknownProperties: List<String>
)

object ConfigDiagnostics {

    private val sensitivePatterns = listOf(
        "password", "secret", "key", "token", "accesskey", "secretkey", "credential"
    )

    /**
     * Inspects and validates a PlatformConfig, categorized by Startup, Runtime, Security, and Dependency validation.
     */
    fun validate(config: PlatformConfig): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // 1. STARTUP VALIDATION (Crucial values needed to boot the container/JVM)
        if (config.server.port !in 1..65535) {
            errors.add(ValidationError("server.port", ValidationCategory.STARTUP, "Server port must be between 1 and 65535. Given: ${config.server.port}"))
        }
        if (config.server.host.isBlank()) {
            errors.add(ValidationError("server.host", ValidationCategory.STARTUP, "Server host bind address cannot be blank."))
        }

        // 2. RUNTIME VALIDATION (Values that direct internal execution bounds)
        if (config.server.requestTimeoutMs <= 0) {
            errors.add(ValidationError("server.requestTimeoutMs", ValidationCategory.RUNTIME, "Request timeout must be a positive integer."))
        }
        if (config.database.maximumPoolSize <= 0) {
            errors.add(ValidationError("database.maximumPoolSize", ValidationCategory.RUNTIME, "Database pool size must be greater than zero."))
        }
        if (config.database.minimumIdle > config.database.maximumPoolSize) {
            errors.add(ValidationError("database.minimumIdle", ValidationCategory.RUNTIME, "Database minimum idle connections (${config.database.minimumIdle}) cannot exceed maximum pool size (${config.database.maximumPoolSize})."))
        }

        // 3. SECURITY VALIDATION (Encryption, JWT signature secret checks)
        if (config.security.encryptionKey.length < 16) {
            errors.add(ValidationError("security.encryptionKey", ValidationCategory.SECURITY, "Security encryption key length (${config.security.encryptionKey.length}) must be at least 16 characters for reliable AES strength."))
        }
        if (config.security.tokenSecret.isBlank() || config.security.tokenSecret == "jwt_signature_secret_key_for_platform") {
            errors.add(ValidationError("security.tokenSecret", ValidationCategory.SECURITY, "Token secret signature is using default or blank value, presenting high vulnerability in non-development profiles."))
        }

        // 4. DEPENDENCY VALIDATION (Connection URLs and driver packages)
        if (config.database.url.isBlank()) {
            errors.add(ValidationError("database.url", ValidationCategory.DEPENDENCY, "Database connection URL cannot be blank."))
        } else if (!config.database.url.startsWith("jdbc:postgresql://")) {
            errors.add(ValidationError("database.url", ValidationCategory.DEPENDENCY, "Database URL must start with 'jdbc:postgresql://' scheme. Given: ${config.database.url}"))
        }
        if (config.minio.endpoint.isBlank()) {
            errors.add(ValidationError("minio.endpoint", ValidationCategory.DEPENDENCY, "MinIO storage endpoint URL cannot be blank."))
        }

        return errors
    }

    /**
     * Exports the configuration as a nested map, automatically masking any sensitive values.
     */
    fun exportMasked(config: PlatformConfig): Map<String, Any> {
        val rawMap = serializeToMap(config)
        return maskSecrets(rawMap)
    }

    /**
     * Generates a complete diagnostic audit of the current configuration setup.
     */
    fun generateReport(
        activeProfile: String,
        config: PlatformConfig,
        providedSecretsKeys: List<String>
    ): DiagnosticsReport {
        val validationErrors = validate(config)
        
        // Define required secret keys
        val requiredSecrets = listOf("database.password", "minio.secretKey", "security.encryptionKey", "security.tokenSecret")
        val missingSecrets = requiredSecrets.filter { key ->
            val value = getNestedValue(config, key)
            value.isNullOrBlank() || value == "secret_password" || value == "minio_password"
        }

        // Identify deprecated keys
        val deprecated = mutableListOf<String>()
        // Ex: "database.driver" is standard, but if custom drivers are configured we might log warnings
        if (config.database.driver != "org.postgresql.Driver") {
            deprecated.add("database.driver: Custom database drivers are deprecated. Rely on PostgreSQL driver standard.")
        }

        return DiagnosticsReport(
            loadedProfile = activeProfile,
            sourcesPrecedence = listOf(
                "1. Runtime Map Overrides (Highest)",
                "2. Secret Volume Files",
                "3. System Environment Variables",
                "4. Profile-Specific YAML (application-$activeProfile.yml)",
                "5. Base Configuration YAML (application.yml)",
                "6. Default Constructor Values (Lowest)"
            ),
            missingSecrets = missingSecrets,
            deprecatedProperties = deprecated,
            validationErrors = validationErrors,
            unknownProperties = emptyList() // Will be expanded as parsing detects unmapped parameters
        )
    }

    private fun getNestedValue(config: PlatformConfig, path: String): String? {
        return try {
            val parts = path.split(".")
            var current: Any = config
            for (part in parts) {
                val prop = current::class.memberProperties.firstOrNull { it.name == part } ?: return null
                current = prop.getter.call(current) ?: return null
            }
            current.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeToMap(obj: Any): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        obj::class.memberProperties.forEach { prop ->
            val value = prop.getter.call(obj)
            if (value != null) {
                if (value::class.isData) {
                    map[prop.name] = serializeToMap(value)
                } else {
                    map[prop.name] = value
                }
            }
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun maskSecrets(map: Map<String, Any>): Map<String, Any> {
        val maskedMap = mutableMapOf<String, Any>()
        map.forEach { (key, value) ->
            val isSensitive = sensitivePatterns.any { pattern -> key.lowercase().contains(pattern) }
            if (isSensitive && value is String) {
                maskedMap[key] = "********"
            } else if (value is Map<*, *>) {
                maskedMap[key] = maskSecrets(value as Map<String, Any>)
            } else {
                maskedMap[key] = value
            }
        }
        return maskedMap
    }
}
