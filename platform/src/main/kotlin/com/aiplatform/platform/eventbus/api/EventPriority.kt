package com.aiplatform.platform.eventbus.api

/**
 * Defines the priority tier of the event. Under high system load, events are sorted
 * and dispatched according to their priority level.
 */
enum class EventPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}
