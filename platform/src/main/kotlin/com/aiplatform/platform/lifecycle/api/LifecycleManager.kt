package com.aiplatform.platform.lifecycle.api

import java.time.Duration

/**
 * High-performance orchestrator managing active platform states, Topological DAG sorting,
 * thread-safe transitions, and graceful shutdown timeouts.
 */
interface LifecycleManager {
    /**
     * Reads the current atomic state of the platform.
     */
    val currentState: LifecycleState

    /**
     * Registers a modular component into the dependency graph context.
     * Throws an exception if called after BOOTSTRAP has concluded.
     */
    fun register(component: LifecycleAware)

    /**
     * Starts the application, stepping sequentially through BOOTSTRAP, INITIALIZATION, and STARTUP.
     * Completing successfully marks state as RUNNING.
     */
    fun bootstrapAndRun()

    /**
     * Gracefully exits the application by sequentially stepping through SHUTTING_DOWN and CLEANUP.
     * Accepts a hard limit timeout after which remaining threads are force-terminated.
     */
    fun initiateShutdown(gracePeriod: Duration = Duration.ofSeconds(30))
}
