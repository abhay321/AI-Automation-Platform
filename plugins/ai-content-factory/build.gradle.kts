plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":sdk:plugin-sdk"))
    implementation(project(":sdk:workflow-sdk"))
    implementation(project(":common"))
}
