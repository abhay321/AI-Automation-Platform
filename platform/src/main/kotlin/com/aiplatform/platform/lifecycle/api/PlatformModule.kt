package com.aiplatform.platform.lifecycle.api

/**
 * Annotation to supply metadata for platform services, declaring dependency topologies
 * and system importance boundaries for automatic discovery.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PlatformModule(
    /**
     * Unique identifier representing this platform module.
     */
    val id: String,

    /**
     * Set of module IDs that this component depends on.
     */
    val dependsOn: Array<String> = [],

    /**
     * If true, failure to boot or initialize this module halts the platform startup.
     */
    val isRequired: Boolean = true,

    /**
     * Brief human-readable description of what capability this module provides.
     */
    val description: String = ""
)
