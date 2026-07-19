package com.aiplatform.platform.eventbus.api

import kotlinx.coroutines.flow.Flow
import java.time.Duration

/**
 * Handles incoming events matching the subscribed topic criteria.
 */
interface EventSubscriber<T : Any> {
    /**
     * Suspension block called when an event matching subscription parameters is published.
     */
    suspend fun onEvent(envelope: EventEnvelope<T>)
}

/**
 * Represents an active subscription. Must be canceled when no longer needed
 * to release resources and prevent memory leaks.
 */
interface SubscriptionHandle {
    /**
     * Unregisters the subscriber from the Event Bus dispatcher.
     */
    fun cancel()
}

/**
 * The core Event Bus managing synchronous, asynchronous, sticky, and delayed routing events.
 */
interface EventBus {
    /**
     * Asynchronously publishes an event onto the bus. Subscribers process it using coroutine execution pools.
     */
    suspend fun <T : Any> publish(envelope: EventEnvelope<T>)

    /**
     * Publishes an event synchronously. The dispatcher blocks until all registered
     * synchronous subscribers have successfully processed the event.
     */
    fun <T : Any> publishSync(envelope: EventEnvelope<T>)

    /**
     * Dispatches an event after a configured delay duration.
     */
    suspend fun <T : Any> publishDelayed(envelope: EventEnvelope<T>, delay: Duration)

    /**
     * Publishes an event and retains it. The latest event of matching topic
     * will be immediately replayed to any new subscriber that registers.
     */
    suspend fun <T : Any> publishSticky(envelope: EventEnvelope<T>)

    /**
     * Registers a subscriber for matching topics. Wildcards (* and >) are supported.
     */
    fun <T : Any> subscribe(topicPattern: String, subscriber: EventSubscriber<T>): SubscriptionHandle

    /**
     * Natively exposes event streams as a Kotlin Flow.
     */
    fun <T : Any> asFlow(topicPattern: String): Flow<EventEnvelope<T>>

    /**
     * Purges cached sticky events matching the given topic pattern.
     */
    fun clearSticky(topicPattern: String)
}
