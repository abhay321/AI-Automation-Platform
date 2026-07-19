package com.aiplatform.platform.logging.context

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * ThreadContextElement implementation to seamlessly propagate the Platform CorrelationContext 
 * across thread hops, yields, suspensions, and resumptions in Kotlin Coroutines.
 */
class CorrelationContextElement(
    private val contextSnapshot: Map<String, String> = CorrelationContext.asMap()
) : ThreadContextElement<Map<String, String>>, AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<CorrelationContextElement>

    override fun updateThreadContext(context: CoroutineContext): Map<String, String> {
        val oldState = CorrelationContext.asMap()
        CorrelationContext.setFromMap(contextSnapshot)
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Map<String, String>) {
        CorrelationContext.setFromMap(oldState)
    }
}
