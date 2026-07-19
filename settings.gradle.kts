pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ai-automation-platform"

// Subprojects configuration
include(":common")
include(":platform")
include(":core-domain")
include(":core-application")
include(":core-infrastructure")

// Empty SDK modules for Phase 1
include(":sdk:plugin-sdk")
include(":sdk:workflow-sdk")
include(":sdk:provider-sdk")
include(":sdk:connector-sdk")
include(":sdk:agent-sdk")

// Plugins
include(":plugins:ai-content-factory")
include(":plugins:slack-connector")
include(":plugins:google-workspace")

// Native Operational Control Center Desktop Client
include(":desktop-control-center")
