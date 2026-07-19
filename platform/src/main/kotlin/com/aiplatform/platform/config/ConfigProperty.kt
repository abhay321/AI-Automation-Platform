package com.aiplatform.platform.config

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigProperty(
    val description: String,
    val defaultValue: String = "",
    val isRequired: Boolean = true,
    val validationRules: String = "",
    val example: String = "",
    val displayName: String = ""
)

data class PropertyMetadata(
    val path: String,
    val description: String,
    val defaultValue: String,
    val isRequired: Boolean,
    val validationRules: String,
    val example: String,
    val displayName: String,
    val typeName: String
)
