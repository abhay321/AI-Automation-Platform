package com.aiplatform.platform.lifecycle.engine

import com.aiplatform.platform.lifecycle.api.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

/**
 * High-performance, production-grade orchestrator implementing LifecycleManager.
 * Manages atomic state transitions, topological DAG sorting, component phase triggers,
 * and graceful shutdown time bounds.
 */
class DefaultLifecycleManager : LifecycleManager {

    private val log = LoggerFactory.getLogger(DefaultLifecycleManager::class.java)

    private val _currentState = AtomicReference(LifecycleState.OFFLINE)
    override val currentState: LifecycleState get() = _currentState.get()

    private val components = CopyOnWriteArrayList<LifecycleAware>()
    private val listeners = CopyOnWriteArrayList<LifecycleListener>()

    @Volatile
    private var bootstrapped = false

    @Volatile
    private var sortedComponents: List<LifecycleAware> = emptyList()

    /**
     * Adds a lifecycle observer.
     */
    fun addListener(listener: LifecycleListener) {
        listeners.add(listener)
    }

    /**
     * Removes a lifecycle observer.
     */
    fun removeListener(listener: LifecycleListener) {
        listeners.remove(listener)
    }

    override fun register(component: LifecycleAware) {
        if (bootstrapped) {
            throw IllegalStateException("Cannot register component '${component.id}' after bootstrapping phase has completed.")
        }
        components.add(component)
        log.debug("Registered lifecycle-aware component: {}", component.id)
    }

    override fun bootstrapAndRun() {
        try {
            // 1. BOOTSTRAP PHASE
            transitionTo(LifecycleState.BOOTSTRAPPING)
            log.info("Transitioning to BOOTSTRAPPING phase...")

            // Execute onBootstrap for all registered components
            for (component in components) {
                executePhase(component, "Bootstrap") { component.onBootstrap() }
            }
            bootstrapped = true

            // 2. INITIALIZATION PHASE
            transitionTo(LifecycleState.INITIALIZING)
            log.info("Transitioning to INITIALIZING phase...")

            // Compile the DAG and sort components topologically
            log.info("Sorting dependencies and preparing component DAG...")
            sortedComponents = DependencySorter.sort(components)
            log.info("Topologically sorted components: {}", sortedComponents.map { it.id })

            // Execute onInitialize in topological order
            for (component in sortedComponents) {
                executePhase(component, "Initialize") { component.onInitialize() }
            }

            // 3. STARTUP PHASE
            transitionTo(LifecycleState.STARTING)
            log.info("Transitioning to STARTING phase...")

            // Execute onStartup in topological order
            for (component in sortedComponents) {
                executePhase(component, "Startup") { component.onStartup() }
            }

            // 4. RUNNING STATE
            transitionTo(LifecycleState.RUNNING)
            log.info("Platform is now fully RUNNING. Readiness = TRUE, Liveness = TRUE.")

        } catch (t: Throwable) {
            log.error("Fatal failure encountered during platform boot sequence. Initiating fallback...", t)
            transitionTo(LifecycleState.FAILED)
            notifyListenersFailure(currentState, null, t)
            throw t
        }
    }

    override fun initiateShutdown(gracePeriod: Duration) {
        val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "platform-shutdown-worker") }
        try {
            log.info("Shutdown initiated. State: {}. Grace period: {} seconds.", currentState, gracePeriod.seconds)
            
            val future = executor.submit {
                runShutdownAndCleanup()
            }

            try {
                future.get(gracePeriod.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                log.warn("Shutdown did not complete within the grace period of {} seconds. Force-terminating remaining threads...", gracePeriod.seconds)
                future.cancel(true)
                transitionTo(LifecycleState.FAILED)
                notifyListenersFailure(LifecycleState.SHUTTING_DOWN, null, RuntimeException("Shutdown timeout of ${gracePeriod.seconds} seconds exceeded"))
            } catch (e: ExecutionException) {
                log.error("Error occurred during platform shutdown sequence", e.cause)
                transitionTo(LifecycleState.FAILED)
                notifyListenersFailure(LifecycleState.SHUTTING_DOWN, null, e.cause ?: e)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun runShutdownAndCleanup() {
        try {
            // 1. SHUTTING_DOWN PHASE
            transitionTo(LifecycleState.SHUTTING_DOWN)
            log.info("Transitioning to SHUTTING_DOWN phase...")

            // Execute onShutdown in reverse topological order
            val reverseComponents = sortedComponents.reversed()
            for (component in reverseComponents) {
                executePhase(component, "Shutdown") { component.onShutdown() }
            }

            // 2. CLEANUP PHASE
            transitionTo(LifecycleState.CLEANING_UP)
            log.info("Transitioning to CLEANING_UP phase...")

            // Execute onCleanup in reverse topological order
            for (component in reverseComponents) {
                executePhase(component, "Cleanup") { component.onCleanup() }
            }

            // 3. TERMINATED STATE
            transitionTo(LifecycleState.TERMINATED)
            log.info("Platform has cleanly TERMINATED.")

        } catch (t: Throwable) {
            log.error("Error occurred during shutdown/cleanup sequence", t)
            transitionTo(LifecycleState.FAILED)
            notifyListenersFailure(currentState, null, t)
            throw t
        }
    }

    private fun transitionTo(newState: LifecycleState) {
        val oldState = _currentState.getAndSet(newState)
        if (oldState != newState) {
            log.debug("State transition: {} -> {}", oldState, newState)
            for (listener in listeners) {
                try {
                    listener.onStateTransition(oldState, newState)
                } catch (e: Exception) {
                    log.error("Error in state transition listener", e)
                }
            }
        }
    }

    private fun executePhase(component: LifecycleAware, phaseName: String, block: () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            log.trace("Executing {} phase for component: {}", phaseName, component.id)
            block()
            val duration = System.currentTimeMillis() - start
            notifyListenersComponentCompleted(component.id, phaseName, duration)
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - start
            val isRequired = isModuleRequired(component)
            if (isRequired) {
                log.error("Component '{}' failed in {} phase. Module is required. Halting startup.", component.id, phaseName, t)
                notifyListenersFailure(currentState, component.id, t)
                throw t
            } else {
                log.warn("Component '{}' failed in {} phase. Module is optional. Skipping module and continuing in degraded mode.", component.id, phaseName, t)
                notifyListenersFailure(currentState, component.id, t)
                // Since optional, we log, notify, and let the boot sequence continue!
            }
        }
    }

    private fun isModuleRequired(component: LifecycleAware): Boolean {
        val annotation = component::class.java.getAnnotation(PlatformModule::class.java)
        return annotation?.isRequired ?: true
    }

    private fun notifyListenersComponentCompleted(componentId: String, phase: String, durationMs: Long) {
        for (listener in listeners) {
            try {
                listener.onComponentPhaseCompleted(componentId, phase, durationMs)
            } catch (e: Exception) {
                log.error("Error in component completed listener", e)
            }
        }
    }

    private fun notifyListenersFailure(phase: LifecycleState, componentId: String?, throwable: Throwable) {
        for (listener in listeners) {
            try {
                listener.onLifecycleFailure(phase, componentId, throwable)
            } catch (e: Exception) {
                log.error("Error in lifecycle failure listener", e)
            }
        }
    }
}
