package com.aiplatform.platform.eventbus.api

import java.time.Instant
import java.util.UUID

/**
 * Type-safe, metadata-rich envelope carrying event payloads across the Platform Event Bus.
 */
data class EventEnvelope<T : Any>(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val eventName: String,
    val eventVersion: Int = 1,
    val payload: T,
    val priority: EventPriority = EventPriority.NORMAL,
    
    // Unified Observability Tracking
    val traceId: String?,
    val spanId: String?,
    val correlationId: String?,
    val userId: String? = null,
    val workspaceId: String? = null,
    
    // Routing & Delivery parameters
    val isSticky: Boolean = false,
    val isReplayEnabled: Boolean = false,
    val metadata: Map<String, Any?> = emptyMap()
)
