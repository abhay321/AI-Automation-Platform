package com.aiplatform.platform.logging.context

import org.slf4j.MDC
import java.util.UUID

object CorrelationContext {
    private val threadContext = ThreadLocal.withInitial { mutableMapOf<String, String>() }

    const val TRACE_ID = "traceId"
    const val REQUEST_ID = "requestId"
    const val CORRELATION_ID = "correlationId"
    const val EXECUTION_ID = "executionId"
    const val WORKSPACE_ID = "workspaceId"
    const val PROJECT_ID = "projectId"
    const val PLUGIN_ID = "pluginId"
    const val AGENT_ID = "agentId"
    const val WORKFLOW_ID = "workflowId"
    const val USER_ID = "userId"
    const val SESSION_ID = "sessionId"
    const val PROVIDER_ID = "providerId"
    const val CONNECTOR_ID = "connectorId"

    private val allKeys = listOf(
        TRACE_ID, REQUEST_ID, CORRELATION_ID, EXECUTION_ID,
        WORKSPACE_ID, PROJECT_ID, PLUGIN_ID, AGENT_ID,
        WORKFLOW_ID, USER_ID, SESSION_ID, PROVIDER_ID, CONNECTOR_ID
    )

    fun get(key: String): String? {
        return threadContext.get()[key]
    }

    fun set(key: String, value: String?) {
        val map = threadContext.get()
        if (value == null) {
            map.remove(key)
            MDC.remove(key)
        } else {
            map[key] = value
            MDC.put(key, value)
        }
    }

    // Specific Getters / Setters
    var traceId: String?
        get() = get(TRACE_ID)
        set(value) = set(TRACE_ID, value)

    var requestId: String?
        get() = get(REQUEST_ID)
        set(value) = set(REQUEST_ID, value)

    var correlationId: String?
        get() = get(CORRELATION_ID)
        set(value) = set(CORRELATION_ID, value)

    var executionId: String?
        get() = get(EXECUTION_ID)
        set(value) = set(EXECUTION_ID, value)

    var workspaceId: String?
        get() = get(WORKSPACE_ID)
        set(value) = set(WORKSPACE_ID, value)

    var projectId: String?
        get() = get(PROJECT_ID)
        set(value) = set(PROJECT_ID, value)

    var pluginId: String?
        get() = get(PLUGIN_ID)
        set(value) = set(PLUGIN_ID, value)

    var agentId: String?
        get() = get(AGENT_ID)
        set(value) = set(AGENT_ID, value)

    var workflowId: String?
        get() = get(WORKFLOW_ID)
        set(value) = set(WORKFLOW_ID, value)

    var userId: String?
        get() = get(USER_ID)
        set(value) = set(USER_ID, value)

    var sessionId: String?
        get() = get(SESSION_ID)
        set(value) = set(SESSION_ID, value)

    var providerId: String?
        get() = get(PROVIDER_ID)
        set(value) = set(PROVIDER_ID, value)

    var connectorId: String?
        get() = get(CONNECTOR_ID)
        set(value) = set(CONNECTOR_ID, value)

    /**
     * Initializes a new unique trace/request/correlation session context if none are present.
     */
    fun initTracing(
        existingTraceId: String? = null,
        existingRequestId: String? = null,
        existingCorrelationId: String? = null
    ) {
        traceId = existingTraceId ?: "tr-${UUID.randomUUID()}"
        requestId = existingRequestId ?: "req-${UUID.randomUUID()}"
        correlationId = existingCorrelationId ?: "corr-${UUID.randomUUID()}"
    }

    /**
     * Exports current context values as a safe read-only map.
     */
    fun asMap(): Map<String, String> {
        return HashMap(threadContext.get())
    }

    /**
     * Replaces the current thread's context map with a new one, updating SLF4J MDC.
     */
    fun setFromMap(map: Map<String, String>) {
        val current = threadContext.get()
        current.clear()
        
        // Remove old SLF4J MDC entries
        allKeys.forEach { MDC.remove(it) }

        map.forEach { (k, v) ->
            current[k] = v
            MDC.put(k, v)
        }
    }

    /**
     * Completely cleans the current thread's context map and corresponding SLF4J MDC.
     */
    fun clear() {
        threadContext.get().clear()
        allKeys.forEach { MDC.remove(it) }
    }

    /**
     * Temporarily alters context for a block of code, restoring prior values afterward.
     */
    inline fun <T> withContext(tempContext: Map<String, String>, block: () -> T): T {
        val original = asMap()
        val merged = original.toMutableMap()
        merged.putAll(tempContext)
        setFromMap(merged)
        return try {
            block()
        } finally {
            setFromMap(original)
        }
    }
}
