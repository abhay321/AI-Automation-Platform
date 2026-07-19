package com.aiplatform.platform.logging.api

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL;

    fun toSlf4jLevel(): org.slf4j.event.Level {
        return when (this) {
            TRACE -> org.slf4j.event.Level.TRACE
            DEBUG -> org.slf4j.event.Level.DEBUG
            INFO -> org.slf4j.event.Level.INFO
            WARN -> org.slf4j.event.Level.WARN
            ERROR -> org.slf4j.event.Level.ERROR
            FATAL -> org.slf4j.event.Level.ERROR // SLF4J has no FATAL, maps to ERROR with special marker or priority
        }
    }
}
