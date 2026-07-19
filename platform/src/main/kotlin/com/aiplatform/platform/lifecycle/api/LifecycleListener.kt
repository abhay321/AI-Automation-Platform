package com.aiplatform.platform.lifecycle.api

/**
 * Interface for components that wish to listen to major lifecycle state changes.
 * This can be used for logging, metrics, alerting, or health integrations.
 */
interface LifecycleListener {
    /**
     * Called when the platform transitions from one state to another.
     * 
     * @param from The previous lifecycle state.
     * @param to The new lifecycle state.
     */
    fun onStateTransition(from: LifecycleState, to: LifecycleState)

    /**
     * Called when a specific component completes a lifecycle phase.
     * 
     * @param componentId The ID of the component.
     * @param phase The phase that completed (e.g., "Bootstrap", "Initialize", "Startup").
     * @param durationMs The time in milliseconds that the phase took to execute.
     */
    fun onComponentPhaseCompleted(componentId: String, phase: String, durationMs: Long)

    /**
     * Called when a lifecycle phase or component execution fails.
     * 
     * @param phase The current active phase.
     * @param componentId The ID of the component that failed (null if phase level).
     * @param throwable The exception or error that triggered the failure.
     */
    fun onLifecycleFailure(phase: LifecycleState, componentId: String?, throwable: Throwable)
}
