package com.aiplatform.platform.lifecycle.api

/**
 * Defines the strict, irreversible state machine progression of the Platform Runtime Lifecycle.
 */
enum class LifecycleState {
    /**
     * Platform has not started booting yet.
     */
    OFFLINE,

    /**
     * Critical phase loading base configurations, environment contexts, and core logging capabilities.
     */
    BOOTSTRAPPING,

    /**
     * Resolves dependency orders, constructs databases connection pools, allocates memory caches,
     * and compiles the module DAG.
     */
    INITIALIZING,

    /**
     * Launches external connection parameters, binds network port sockets, and starts worker task schedulers.
     */
    STARTING,

    /**
     * Operational state indicating that the platform is fully healthy, with readiness checks active.
     */
    RUNNING,

    /**
     * Transition phase after SIGTERM/SIGINT. Drains active requests, stops queue consumers, and refuses new tasks.
     */
    SHUTTING_DOWN,

    /**
     * Releases system resource locks, shuts down thread executor pools, and cleanly disconnects databases.
     */
    CLEANING_UP,

    /**
     * Safely offline and ready for standard process termination.
     */
    TERMINATED,

    /**
     * Boot or execution phase failure. Diagnostics are dumped for system administrators.
     */
    FAILED
}
