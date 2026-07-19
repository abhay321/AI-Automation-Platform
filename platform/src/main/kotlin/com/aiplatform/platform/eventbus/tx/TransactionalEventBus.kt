package com.aiplatform.platform.eventbus.tx

import com.aiplatform.platform.eventbus.api.EventEnvelope

/**
 * Contract supporting Transactional Outbox publishing pattern.
 * This guarantees events are persistent in database first and released only upon transaction commit.
 */
interface TransactionalEventBus {
    /**
     * Persists the event in local database Outbox, releasing to event loop on transaction commit.
     */
    suspend fun <T : Any> publishTransactional(envelope: EventEnvelope<T>)
}
