plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":common"))
    // Under Compose Desktop, the jetbrains compose compiler handles core UI rendering on the JVM target.
}
