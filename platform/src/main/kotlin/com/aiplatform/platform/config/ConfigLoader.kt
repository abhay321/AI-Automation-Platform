package com.aiplatform.platform.config

interface ConfigLoader {
    /**
     * Loads the platform configuration based on active profile, system properties, and environment variables.
     */
    fun load(): PlatformConfig

    /**
     * Gets the currently active profile (e.g. dev, test, prod).
     */
    val activeProfile: String
}
