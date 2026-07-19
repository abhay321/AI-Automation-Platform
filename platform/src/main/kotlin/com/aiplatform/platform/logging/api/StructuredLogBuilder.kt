package com.aiplatform.platform.logging.api

@DslMarker
annotation class LoggingDsl

@LoggingDsl
class StructuredLogBuilder(val message: String) {
    var category: LogCategory = LogCategory.PLATFORM
    var eventVersion: Int = 1
    var eventType: String = "System"
    
    // Explicit dynamic context overrides
    var traceId: String? = null
    var requestId: String? = null
    var correlationId: String? = null
    var executionId: String? = null
    var workspaceId: String? = null
    var projectId: String? = null
    var pluginId: String? = null
    var agentId: String? = null
    var workflowId: String? = null
    var userId: String? = null
    var sessionId: String? = null
    var providerId: String? = null
    var connectorId: String? = null
    
    private val _metadata = mutableMapOf<String, Any?>()
    val metadata: Map<String, Any?> get() = _metadata

    private var _exception: Throwable? = null
    private var _retryable: Boolean = false
    private var _retryAttempt: Int? = null
    private var _recoverySuggestion: String? = null

    fun exception(throwable: Throwable, retryable: Boolean = false, retryAttempt: Int? = null, recoverySuggestion: String? = null) {
        _exception = throwable
        _retryable = retryable
        _retryAttempt = retryAttempt
        _recoverySuggestion = recoverySuggestion
    }

    fun getException(): Throwable? = _exception
    fun isRetryable(): Boolean = _retryable
    fun getRetryAttempt(): Int? = _retryAttempt
    fun getRecoverySuggestion(): String? = _recoverySuggestion

    fun payload(key: String, value: Any?) {
        _metadata[key] = value
    }

    fun payload(map: Map<String, Any?>) {
        _metadata.putAll(map)
    }

    fun metadata(block: MetadataBuilder.() -> Unit) {
        val builder = MetadataBuilder()
        builder.block()
        _metadata.putAll(builder.build())
    }
}

@LoggingDsl
class MetadataBuilder {
    private val map = mutableMapOf<String, Any?>()

    infix fun String.to(value: Any?) {
        map[this] = value
    }

    fun build(): Map<String, Any?> = map
}
