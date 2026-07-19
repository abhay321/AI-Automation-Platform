package com.aiplatform.platform.lifecycle.api

/**
 * Unified contract for any platform service, plugin, or connector that requires participation
 * in orderly startup and shutdown sequences.
 */
interface LifecycleAware {
    /**
     * Unique identifier representing this component (e.g. "DatabaseService", "RedisCache").
     */
    val id: String

    /**
     * Set of unique component IDs that MUST be fully initialized before this component starts up.
     * This defines the Directed Acyclic Graph (DAG) topology edges.
     */
    val dependencies: List<String> get() = emptyList()

    /**
     * Triggered during the BOOTSTRAP phase. Only fundamental tasks such as loading custom environment
     * overrides or local config property structures are permitted here.
     */
    fun onBootstrap() {}

    /**
     * Triggered during the INITIALIZATION phase. Permitted work includes creating connection pool
     * properties, setting up local filesystem storage caches, or performing migration evaluations.
     */
    fun onInitialize() {}

    /**
     * Triggered during the STARTUP phase. Permitted work includes binding network port sockets,
     * launching queue consumer pools, starting cron tasks, and setting readiness.
     */
    fun onStartup() {}

    /**
     * Triggered during the SHUTTING_DOWN phase. Must coordinate connection draining, stop taking
     * new commands, disable message loops, and signal readiness degradation.
     */
    fun onShutdown() {}

    /**
     * Triggered during the CLEANUP phase. Releases all memory tables, shuts down local executor pools,
     * closes socket handles, and guarantees no file descriptors are left open.
     */
    fun onCleanup() {}
}
