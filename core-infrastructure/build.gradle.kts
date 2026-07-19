plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core-application"))
    implementation(project(":platform"))
    implementation(project(":common"))

    // Exposed ORM & PostgreSQL
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari.cp)
    implementation(libs.postgresql.jdbc)

    // Redisson (Redis client)
    implementation(libs.redis.client)

    // RabbitMQ
    implementation(libs.rabbitmq.client)

    // Minio Object Storage
    implementation(libs.minio.client)

    // Qdrant Vector Search
    implementation(libs.qdrant.client)

    // Monitoring and Metrics
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // Ktor Server and Transport layer
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content-negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.status-pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.websockets)

    // Ktor Client for integrations
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content-negotiation)

    // Testing Suite
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

tasks.test {
    useJUnitPlatform()
}
