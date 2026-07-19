package com.aiplatform.platform.eventbus.engine

import com.aiplatform.platform.eventbus.api.*
import com.aiplatform.platform.eventbus.bridge.EventBridge
import com.aiplatform.platform.eventbus.bridge.EventBridgeRegistry
import com.aiplatform.platform.lifecycle.api.LifecycleAware
import com.aiplatform.platform.lifecycle.api.PlatformModule
import com.aiplatform.platform.logging.context.CorrelationContext
import com.aiplatform.platform.logging.context.CorrelationContextElement
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Robust, production-grade, thread-safe EventBus implementation.
 * Supports pattern-matching topic subscriptions (* and > wildcards), sticky events,
 * delayed delivery, external bridging, and correlation context propagation.
 */
@PlatformModule(id = "event-bus", isRequired = true)
class DefaultEventBus : EventBus, EventBridgeRegistry, LifecycleAware {

    private val log = LoggerFactory.getLogger(DefaultEventBus::class.java)

    // Module Lifecycle IDs and dependencies
    override val id: String = "event-bus"
    override val dependencies: List<String> = emptyList()

    // Subscriptions
    private val subscriptions = CopyOnWriteArrayList<Subscription<*>>()

    // Sticky events registry
    private val stickyEvents = ConcurrentHashMap<String, EventEnvelope<*>>()

    // Active External Bridges
    private val bridges = CopyOnWriteArrayList<EventBridge>()
    override val activeBridges: List<EventBridge> get() = ArrayList(bridges)

    // Coroutine execution context
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private data class Subscription<T : Any>(
        val pattern: String,
        val regex: Regex,
        val subscriber: EventSubscriber<T>
    )

    // -------------------------------------------------------------
    // LifecycleAware implementation
    // -------------------------------------------------------------

    override fun onBootstrap() {
        log.info("Bootstrapping Event Bus engine...")
    }

    override fun onInitialize() {
        log.info("Initializing Event Bus routing rules...")
    }

    override fun onStartup() {
        log.info("Event Bus fully started and listening for publishers.")
    }

    override fun onShutdown() {
        log.info("Event Bus shutting down. Canceling coroutine scope and draining subscriptions...")
        job.cancel()
    }

    override fun onCleanup() {
        subscriptions.clear()
        stickyEvents.clear()
        bridges.clear()
        log.info("Event Bus caches and registries cleared.")
    }

    // -------------------------------------------------------------
    // EventBridgeRegistry implementation
    // -------------------------------------------------------------

    override fun registerBridge(bridge: EventBridge) {
        bridges.add(bridge)
        log.info("Registered external message bridge target: {}", bridge.targetName)
    }

    override fun deregisterBridge(bridge: EventBridge) {
        bridges.remove(bridge)
        log.info("Deregistered external message bridge target: {}", bridge.targetName)
    }

    // -------------------------------------------------------------
    // EventBus implementation
    // -------------------------------------------------------------

    override suspend fun <T : Any> publish(envelope: EventEnvelope<T>) {
        dispatchAsync(envelope)
    }

    override fun <T : Any> publishSync(envelope: EventEnvelope<T>) {
        runBlocking {
            dispatchSyncInternal(envelope)
        }
    }

    override suspend fun <T : Any> publishDelayed(envelope: EventEnvelope<T>, delay: Duration) {
        val element = createCorrelationContext(envelope)
        scope.launch(element) {
            delay(delay.toMillis())
            publish(envelope)
        }
    }

    override suspend fun <T : Any> publishSticky(envelope: EventEnvelope<T>) {
        stickyEvents[envelope.eventName] = envelope
        dispatchAsync(envelope)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> subscribe(topicPattern: String, subscriber: EventSubscriber<T>): SubscriptionHandle {
        val regex = topicToRegex(topicPattern)
        val subscription = Subscription(topicPattern, regex, subscriber)
        subscriptions.add(subscription)

        log.debug("Subscribed pattern: '{}'", topicPattern)

        // Asynchronously replay matching sticky events
        val element = CorrelationContextElement()
        scope.launch(element) {
            for ((eventName, stickyEnvelope) in stickyEvents) {
                if (regex.matches(eventName)) {
                    try {
                        subscriber.onEvent(stickyEnvelope as EventEnvelope<T>)
                    } catch (e: Exception) {
                        log.error("Error replaying sticky event '{}' to subscriber of pattern '{}'", eventName, topicPattern, e)
                    }
                }
            }
        }

        return object : SubscriptionHandle {
            override fun cancel() {
                subscriptions.remove(subscription)
                log.debug("Canceled subscription pattern: '{}'", topicPattern)
            }
        }
    }

    override fun <T : Any> asFlow(topicPattern: String): Flow<EventEnvelope<T>> = callbackFlow {
        val subscriber = object : EventSubscriber<T> {
            override suspend fun onEvent(envelope: EventEnvelope<T>) {
                send(envelope)
            }
        }
        val handle = subscribe(topicPattern, subscriber)
        awaitClose {
            handle.cancel()
        }
    }

    override fun clearSticky(topicPattern: String) {
        val regex = topicToRegex(topicPattern)
        stickyEvents.keys.removeIf { regex.matches(it) }
        log.debug("Cleared sticky events matching pattern: '{}'", topicPattern)
    }

    // -------------------------------------------------------------
    // Internal Helper Methods
    // -------------------------------------------------------------

    private fun <T : Any> createCorrelationContext(envelope: EventEnvelope<T>): CoroutineContext {
        val map = mutableMapOf<String, String>()
        envelope.traceId?.let { map[CorrelationContext.TRACE_ID] = it }
        envelope.correlationId?.let { map[CorrelationContext.CORRELATION_ID] = it }
        envelope.userId?.let { map[CorrelationContext.USER_ID] = it }
        envelope.workspaceId?.let { map[CorrelationContext.WORKSPACE_ID] = it }
        return CorrelationContextElement(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> dispatchAsync(envelope: EventEnvelope<T>) {
        val element = createCorrelationContext(envelope)
        scope.launch(element) {
            val eventName = envelope.eventName
            var matchCount = 0

            // 1. Local delivery
            for (sub in subscriptions) {
                if (sub.regex.matches(eventName)) {
                    matchCount++
                    try {
                        val typedSubscriber = sub.subscriber as EventSubscriber<T>
                        typedSubscriber.onEvent(envelope)
                    } catch (e: Exception) {
                        log.error("Uncaught exception in subscriber of pattern '{}' handling event '{}'", sub.pattern, eventName, e)
                    }
                }
            }

            // 2. Bridge external delivery
            for (bridge in bridges) {
                if (bridge.shouldBridge(envelope)) {
                    try {
                        bridge.bridgeEvent(envelope)
                    } catch (e: Exception) {
                        log.error("Failed to bridge event '{}' to external target '{}'", eventName, bridge.targetName, e)
                    }
                }
            }

            log.trace("Dispatched event '{}' (priority: {}) to {} local subscribers and bridges.", eventName, envelope.priority, matchCount)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> dispatchSyncInternal(envelope: EventEnvelope<T>) {
        val eventName = envelope.eventName
        for (sub in subscriptions) {
            if (sub.regex.matches(eventName)) {
                try {
                    val typedSubscriber = sub.subscriber as EventSubscriber<T>
                    typedSubscriber.onEvent(envelope)
                } catch (e: Exception) {
                    log.error("Uncaught exception in sync subscriber of pattern '{}' handling event '{}'", sub.pattern, eventName, e)
                }
            }
        }
    }

    /**
     * Converts an n8n/RabbitMQ style dot-separated pattern with wildcards to a Regex.
     * Supports:
     * - `*` matches a single word/level (e.g. `user.*` matches `user.created` but not `user.created.profile`)
     * - `>` matches any tail levels recursively (e.g. `user.>` matches `user.created` and `user.created.profile`)
     */
    private fun topicToRegex(pattern: String): Regex {
        val escaped = pattern.split(".").joinToString("\\.") { segment ->
            when (segment) {
                "*" -> "[^.]+"
                ">" -> ".*"
                else -> Regex.escape(segment)
            }
        }
        val finalPattern = escaped.replace("\\..*", "(\\..*)?")
        return Regex("^$finalPattern$")
    }
}
