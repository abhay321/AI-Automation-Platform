plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":common"))

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Structured logging dependencies
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.logback.json)
    implementation(libs.logback.jackson)
    implementation(libs.jackson.databind)

    // Configuration parsing
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)

    // DI
    implementation(libs.koin.core)

    // Testing Suite
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

