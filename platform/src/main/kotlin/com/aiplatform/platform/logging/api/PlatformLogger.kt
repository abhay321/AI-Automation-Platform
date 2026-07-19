package com.aiplatform.platform.logging.api

interface PlatformLogger {
    val name: String

    // Standard Log Level Status
    fun isEnabled(level: LogLevel): Boolean
    val isTraceEnabled: Boolean get() = isEnabled(LogLevel.TRACE)
    val isDebugEnabled: Boolean get() = isEnabled(LogLevel.DEBUG)
    val isInfoEnabled: Boolean get() = isEnabled(LogLevel.INFO)
    val isWarnEnabled: Boolean get() = isEnabled(LogLevel.WARN)
    val isErrorEnabled: Boolean get() = isEnabled(LogLevel.ERROR)

    // Standard Logging Methods
    fun trace(msg: String)
    fun trace(msg: String, t: Throwable)
    fun debug(msg: String)
    fun debug(msg: String, t: Throwable)
    fun info(msg: String)
    fun info(msg: String, t: Throwable)
    fun warn(msg: String)
    fun warn(msg: String, t: Throwable)
    fun error(msg: String)
    fun error(msg: String, t: Throwable)
    fun fatal(msg: String)
    fun fatal(msg: String, t: Throwable)

    // Structured Dynamic DSL Methods
    fun trace(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})
    fun debug(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})
    fun info(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})
    fun warn(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})
    fun error(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})
    fun fatal(eventName: String, message: String, block: StructuredLogBuilder.() -> Unit = {})

    // Runtime Level Management
    fun setLevel(level: LogLevel)
}
