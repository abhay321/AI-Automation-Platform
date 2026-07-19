package com.aiplatform.platform.config

import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class ReloadableConfig(
    private var configLoader: ConfigLoader
) {
    @Volatile
    private var currentConfig: PlatformConfig = configLoader.load()

    private val reloadListeners = CopyOnWriteArrayList<(PlatformConfig) -> Unit>()

    fun get(): PlatformConfig = currentConfig

    /**
     * Reloads the configuration from its loaders and notifies registered listeners.
     */
    @Synchronized
    fun reload(): ReloadResult {
        return try {
            val oldConfig = currentConfig
            val newConfig = configLoader.load()

            // Validate the newly loaded configuration
            validateReload(oldConfig, newConfig)

            currentConfig = newConfig

            // Notify listeners of the reload
            reloadListeners.forEach { listener ->
                try {
                    listener(newConfig)
                } catch (e: Exception) {
                    // Suppress listener exceptions to avoid halting the notification chain
                }
            }

            ReloadResult.Success(newConfig)
        } catch (e: Exception) {
            ReloadResult.Failure(e.message ?: "Unknown reload failure", e)
        }
    }

    fun addListener(listener: (PlatformConfig) -> Unit) {
        reloadListeners.add(listener)
    }

    fun removeListener(listener: (PlatformConfig) -> Unit) {
        reloadListeners.remove(listener)
    }

    /**
     * Enforces rules about which parameters can be reloaded at runtime vs which require restart.
     */
    private fun validateReload(old: PlatformConfig, new: PlatformConfig) {
        val requiresRestartErrors = mutableListOf<String>()

        if (old.server.port != new.server.port) {
            requiresRestartErrors.add("server.port cannot be reloaded dynamically. Requires application restart.")
        }
        if (old.server.host != new.server.host) {
            requiresRestartErrors.add("server.host cannot be reloaded dynamically. Requires application restart.")
        }
        if (old.server.contextPath != new.server.contextPath) {
            requiresRestartErrors.add("server.contextPath cannot be reloaded dynamically. Requires application restart.")
        }
        if (old.database.url != new.database.url) {
            requiresRestartErrors.add("database.url cannot be reloaded dynamically. Requires application restart.")
        }
        if (old.database.driver != new.database.driver) {
            requiresRestartErrors.add("database.driver cannot be reloaded dynamically. Requires application restart.")
        }
        if (old.security.encryptionKey != new.security.encryptionKey) {
            requiresRestartErrors.add("security.encryptionKey changes require data re-encryption. Cannot be updated dynamically.")
        }

        if (requiresRestartErrors.isNotEmpty()) {
            throw IllegalArgumentException(
                "Dynamic reload rejected due to changes in immutable settings: " +
                        requiresRestartErrors.joinToString("; ")
            )
        }
    }
}

sealed interface ReloadResult {
    data class Success(val config: PlatformConfig) : ReloadResult
    data class Failure(val message: String, val error: Throwable) : ReloadResult
}
