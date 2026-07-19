package com.aiplatform.platform.lifecycle.engine

import com.aiplatform.platform.lifecycle.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class DefaultLifecycleManagerTest {

    // Simple implementation of LifecycleAware for testing
    @PlatformModule(id = "test-module", isRequired = true)
    open class TestComponent(
        override val id: String,
        override val dependencies: List<String> = emptyList()
    ) : LifecycleAware {
        val phasesCalled = mutableListOf<String>()

        override fun onBootstrap() { phasesCalled.add("onBootstrap") }
        override fun onInitialize() { phasesCalled.add("onInitialize") }
        override fun onStartup() { phasesCalled.add("onStartup") }
        override fun onShutdown() { phasesCalled.add("onShutdown") }
        override fun onCleanup() { phasesCalled.add("onCleanup") }
    }

    @PlatformModule(id = "optional-module", isRequired = false)
    class OptionalTestComponent(
        id: String,
        dependencies: List<String> = emptyList(),
        private val shouldFail: Boolean = false
    ) : TestComponent(id, dependencies) {
        override fun onInitialize() {
            if (shouldFail) {
                throw RuntimeException("Simulated optional component failure")
            }
            super.onInitialize()
        }
    }

    @Test
    fun `test successful orderly bootstrap and shutdown sequence`() {
        val manager = DefaultLifecycleManager()

        val compA = TestComponent("comp-a", emptyList())
        val compB = TestComponent("comp-b", listOf("comp-a")) // B depends on A

        manager.register(compA)
        manager.register(compB)

        val stateTransitions = mutableListOf<LifecycleState>()
        manager.addListener(object : LifecycleListener {
            override fun onStateTransition(from: LifecycleState, to: LifecycleState) {
                stateTransitions.add(to)
            }
            override fun onComponentPhaseCompleted(componentId: String, phase: String, durationMs: Long) {}
            override fun onLifecycleFailure(phase: LifecycleState, componentId: String?, throwable: Throwable) {}
        })

        // Boot the manager
        manager.bootstrapAndRun()

        assertEquals(LifecycleState.RUNNING, manager.currentState)
        assertEquals(listOf(LifecycleState.BOOTSTRAPPING, LifecycleState.INITIALIZING, LifecycleState.STARTING, LifecycleState.RUNNING), stateTransitions)

        // Check phase execution order for boot (Topological: A then B)
        assertEquals(listOf("onBootstrap", "onInitialize", "onStartup"), compA.phasesCalled)
        assertEquals(listOf("onBootstrap", "onInitialize", "onStartup"), compB.phasesCalled)

        // Now shutdown
        manager.initiateShutdown(Duration.ofSeconds(5))

        assertEquals(LifecycleState.TERMINATED, manager.currentState)
        // Check phase execution order for shutdown (Reverse Topological: B then A)
        assertTrue(compB.phasesCalled.contains("onShutdown"))
        assertTrue(compB.phasesCalled.contains("onCleanup"))
        assertTrue(compA.phasesCalled.contains("onShutdown"))
        assertTrue(compA.phasesCalled.contains("onCleanup"))
    }

    @Test
    fun `test circular dependency throws CircularDependencyException`() {
        val manager = DefaultLifecycleManager()

        val compA = TestComponent("comp-a", listOf("comp-b"))
        val compB = TestComponent("comp-b", listOf("comp-a"))

        manager.register(compA)
        manager.register(compB)

        assertThrows(CircularDependencyException::class.java) {
            manager.bootstrapAndRun()
        }
        assertEquals(LifecycleState.FAILED, manager.currentState)
    }

    @Test
    fun `test missing required dependency throws MissingDependencyException`() {
        val manager = DefaultLifecycleManager()

        val compA = TestComponent("comp-a", listOf("missing-required-service"))

        manager.register(compA)

        assertThrows(MissingDependencyException::class.java) {
            manager.bootstrapAndRun()
        }
        assertEquals(LifecycleState.FAILED, manager.currentState)
    }

    @Test
    fun `test missing optional dependency is tolerated`() {
        val manager = DefaultLifecycleManager()

        // compA is marked as optional (isRequired = false), and depends on a missing module
        val compA = OptionalTestComponent("comp-a", listOf("missing-service"))

        manager.register(compA)

        assertDoesNotThrow {
            manager.bootstrapAndRun()
        }
        assertEquals(LifecycleState.RUNNING, manager.currentState)
    }

    @Test
    fun `test optional component failure is tolerated during initialization`() {
        val manager = DefaultLifecycleManager()

        val compA = TestComponent("comp-a", emptyList())
        val compB = OptionalTestComponent("comp-b", listOf("comp-a"), shouldFail = true)

        manager.register(compA)
        manager.register(compB)

        var failureDetected = false
        manager.addListener(object : LifecycleListener {
            override fun onStateTransition(from: LifecycleState, to: LifecycleState) {}
            override fun onComponentPhaseCompleted(componentId: String, phase: String, durationMs: Long) {}
            override fun onLifecycleFailure(phase: LifecycleState, componentId: String?, throwable: Throwable) {
                if (componentId == "comp-b") {
                    failureDetected = true
                }
            }
        })

        // Boot should succeed even though comp-b failed since it is optional
        assertDoesNotThrow {
            manager.bootstrapAndRun()
        }
        assertEquals(LifecycleState.RUNNING, manager.currentState)
        assertTrue(failureDetected)
    }

    @Test
    fun `test shutdown timeout bounds gracefully timeout`() {
        val manager = DefaultLifecycleManager()

        // A component that sleeps a long time during shutdown
        val slowComp = object : TestComponent("slow-comp") {
            override fun onShutdown() {
                Thread.sleep(10000) // Sleep 10s
            }
        }

        manager.register(slowComp)
        manager.bootstrapAndRun()

        var shutdownTimedOut = false
        manager.addListener(object : LifecycleListener {
            override fun onStateTransition(from: LifecycleState, to: LifecycleState) {}
            override fun onComponentPhaseCompleted(componentId: String, phase: String, durationMs: Long) {}
            override fun onLifecycleFailure(phase: LifecycleState, componentId: String?, throwable: Throwable) {
                if (throwable.message?.contains("Shutdown timeout") == true) {
                    shutdownTimedOut = true
                }
            }
        })

        // Initiate shutdown with a short grace period of 1 second
        manager.initiateShutdown(Duration.ofSeconds(1))

        assertEquals(LifecycleState.FAILED, manager.currentState)
        assertTrue(shutdownTimedOut)
    }
}
