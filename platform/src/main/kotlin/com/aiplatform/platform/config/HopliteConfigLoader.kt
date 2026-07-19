package com.aiplatform.platform.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import java.io.File

class HopliteConfigLoader(
    private val runtimeProfile: String? = null,
    private val overrides: Map<String, String> = emptyMap(),
    private val secretsDir: File? = null
) : ConfigLoader {

    override val activeProfile: String by lazy {
        runtimeProfile 
            ?: System.getenv("PLATFORM_PROFILE") 
            ?: System.getProperty("platform.profile") 
            ?: "dev"
    }

    override fun load(): PlatformConfig {
        val builder = ConfigLoaderBuilder.default()

        // 1. RUNTIME OVERRIDES (Highest precedence)
        if (overrides.isNotEmpty()) {
            builder.addMapSource(overrides)
        }

        // 2. SECRET FILES OVERRIDES (Second highest precedence)
        val activeSecretsDir = secretsDir 
            ?: File(System.getenv("PLATFORM_SECRETS_DIR") ?: "secrets")
        
        if (activeSecretsDir.exists() && activeSecretsDir.isDirectory) {
            val secretMap = mutableMapOf<String, String>()
            activeSecretsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val key = file.name.replace("_", ".")
                    val value = file.readText().trim()
                    if (value.isNotEmpty()) {
                        secretMap[key] = value
                    }
                }
            }
            if (secretMap.isNotEmpty()) {
                builder.addMapSource(secretMap)
            }
        }

        // 3. ENVIRONMENT VARIABLES are loaded automatically by Hoplite default()
        // But to make sure they override standard YAMLs, we configure the resource sources carefully:
        val profileConfigPath = "/application-$activeProfile.yml"
        val baseConfigPath = "/application.yml"

        val resourceStreamForProfile = javaClass.getResourceAsStream(profileConfigPath)
        if (resourceStreamForProfile != null) {
            builder.addResourceSource(profileConfigPath)
        }
        
        builder.addResourceSource(baseConfigPath)

        // Load the config
        val config = builder.build().loadConfigOrThrow<PlatformConfig>()
        
        // Fail-fast startup validation
        val errors = ConfigDiagnostics.validate(config)
        val startupErrors = errors.filter { it.category == ValidationCategory.STARTUP }
        if (startupErrors.isNotEmpty()) {
            throw IllegalArgumentException(
                "Platform configuration startup validation failed: " +
                        startupErrors.joinToString("; ") { "[${it.path}]: ${it.message}" }
            )
        }
        
        return config
    }
}
