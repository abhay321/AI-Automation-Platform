package com.aiplatform.platform.eventbus.bridge

import com.aiplatform.platform.eventbus.api.EventEnvelope

/**
 * External message broker bridge interface.
 * Implemented by adapters (Kafka, RabbitMQ) to relay in-memory events to external systems.
 */
interface EventBridge {
    /**
     * Unique target connection string/name.
     */
    val targetName: String

    /**
     * Evaluates if this event category should be bridged externally.
     */
    fun shouldBridge(envelope: EventEnvelope<*>): Boolean

    /**
     * Forwards the structured event to the third-party broker asynchronously.
     */
    suspend fun bridgeEvent(envelope: EventEnvelope<*>)
}

/**
 * Registry coordinating registered messaging bridges.
 */
interface EventBridgeRegistry {
    fun registerBridge(bridge: EventBridge)
    fun deregisterBridge(bridge: EventBridge)
    val activeBridges: List<EventBridge>
}
