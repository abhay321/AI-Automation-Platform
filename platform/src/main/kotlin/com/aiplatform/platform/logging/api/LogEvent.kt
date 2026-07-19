package com.aiplatform.platform.logging.api

import java.time.Instant

data class ExceptionDetails(
    val type: String,
    val message: String?,
    val stackTrace: String,
    val rootCauseType: String?,
    val rootCauseMessage: String?,
    val suppressed: List<String> = emptyList(),
    val retryable: Boolean = false,
    val retryAttempt: Int? = null,
    val recoverySuggestion: String? = null
)

data class LogEvent(
    val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val appName: String,
    val appVersion: String,
    val environment: String,
    val hostName: String,
    val threadName: String = Thread.currentThread().name,
    val coroutineId: String? = null,
    
    // Core Event Sourcing Metadata
    val eventName: String,
    val eventVersion: Int = 1,
    val eventType: String = "System", // Ex: "System", "Audit", "Metric", "Security", "Error"
    val category: LogCategory,
    
    // Core Message
    val message: String,
    
    // Context Keys
    val traceId: String?,
    val requestId: String?,
    val correlationId: String?,
    val executionId: String? = null,
    val workspaceId: String? = null,
    val projectId: String? = null,
    val pluginId: String? = null,
    val agentId: String? = null,
    val workflowId: String? = null,
    val userId: String? = null,
    val sessionId: String? = null,
    val providerId: String? = null,
    val connectorId: String? = null,
    
    // Extended Metadata
    val metadata: Map<String, Any?> = emptyMap(),
    
    // Nested Exception Details
    val exception: ExceptionDetails? = null
)
