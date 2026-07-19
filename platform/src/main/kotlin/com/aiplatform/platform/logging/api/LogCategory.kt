package com.aiplatform.platform.logging.api

enum class LogCategory(val value: String) {
    PLATFORM("Platform"),
    INFRASTRUCTURE("Infrastructure"),
    SECURITY("Security"),
    CONFIGURATION("Configuration"),
    WORKFLOW("Workflow"),
    PLUGIN("Plugin"),
    PROVIDER("Provider"),
    CONNECTOR("Connector"),
    AI("AI"),
    HTTP("HTTP"),
    DATABASE("Database"),
    SCHEDULER("Scheduler"),
    DESKTOP("Desktop")
}
