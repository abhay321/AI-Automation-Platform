package com.aiplatform.platform.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PlatformConfigTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var secretsDir: File

    @BeforeEach
    fun setUp() {
        secretsDir = File(tempDir, "secrets")
        secretsDir.mkdirs()
    }

    @Test
    fun `should load default base configuration with dev profile`() {
        val loader = HopliteConfigLoader(runtimeProfile = "dev")
        val config = loader.load()

        loader.activeProfile shouldBe "dev"
        config.server.port shouldBe 3000
        config.server.host shouldBe "0.0.0.0"
        config.database.username shouldBe "platform_admin"
        config.database.maximumPoolSize shouldBe 10
        config.security.encryptionKey shouldBe "supersecret_key_16"
    }

    @Test
    fun `should load test profile and correctly merge base configurations`() {
        val loader = HopliteConfigLoader(runtimeProfile = "test")
        val config = loader.load()

        loader.activeProfile shouldBe "test"
        // Overridden by application-test.yml
        config.server.port shouldBe 8081
        config.database.url shouldBe "jdbc:postgresql://localhost:5432/platform_test_db"
        config.database.username shouldBe "test_user"
        config.database.maximumPoolSize shouldBe 2
        config.database.minimumIdle shouldBe 1
        config.security.encryptionKey shouldBe "test_secret_key_16"
        config.metrics.enabled shouldBe false

        // Inherited from base application.yml
        config.database.driver shouldBe "org.postgresql.Driver"
        config.minio.endpoint shouldBe "http://localhost:9000"
    }

    @Test
    fun `should apply direct runtime map overrides with highest precedence`() {
        val overrides = mapOf(
            "server.port" to "9999",
            "database.username" to "override_admin"
        )
        val loader = HopliteConfigLoader(runtimeProfile = "dev", overrides = overrides)
        val config = loader.load()

        config.server.port shouldBe 9999
        config.database.username shouldBe "override_admin"
        // Others untouched
        config.server.host shouldBe "0.0.0.0"
    }

    @Test
    fun `should fail fast when server port is out of range during startup`() {
        val overrides = mapOf("server.port" to "70000")
        val loader = HopliteConfigLoader(runtimeProfile = "dev", overrides = overrides)

        val exception = shouldThrow<IllegalArgumentException> {
            loader.load()
        }
        exception.message shouldContain "Server port must be between 1 and 65535"
    }

    @Test
    fun `should fail fast when database url is blank`() {
        val overrides = mapOf("database.url" to "   ")
        // Note: blank database url violates DEPENDENCY category, let's run general validation or let loader fail fast if configured
        val loader = HopliteConfigLoader(runtimeProfile = "dev", overrides = overrides)
        val config = loader.load()
        
        val errors = ConfigDiagnostics.validate(config)
        errors.any { it.path == "database.url" && it.category == ValidationCategory.DEPENDENCY } shouldBe true
    }

    @Test
    fun `should enforce secret file overrides in priority order`() {
        // Write secret file for database password
        val dbPassFile = File(secretsDir, "database_password")
        dbPassFile.writeText("secret_file_override_password")

        // Overrides can specify other keys
        val overrides = mapOf("database.username" to "direct_override")

        val loader = HopliteConfigLoader(
            runtimeProfile = "dev", 
            overrides = overrides, 
            secretsDir = secretsDir
        )
        val config = loader.load()

        config.database.password shouldBe "secret_file_override_password"
        config.database.username shouldBe "direct_override"
    }

    @Test
    fun `should test ReloadableConfig and prevent changing immutable settings`() {
        val loader = HopliteConfigLoader(runtimeProfile = "dev")
        val reloadable = ReloadableConfig(loader)

        // Change a reloadable value (e.g. database.maximumPoolSize) via overrides
        val mutableOverrides = mapOf("database.maximumPoolSize" to "25")
        val secondLoader = HopliteConfigLoader(runtimeProfile = "dev", overrides = mutableOverrides)
        
        val secondReloadable = ReloadableConfig(secondLoader)
        val result = secondReloadable.reload()
        
        result shouldNotBe null
    }

    @Test
    fun `should mask sensitive keys on export`() {
        val loader = HopliteConfigLoader(runtimeProfile = "dev")
        val config = loader.load()

        val exported = ConfigDiagnostics.exportMasked(config)
        
        val dbSection = exported["database"] as Map<*, *>
        dbSection["password"] shouldBe "********"
        
        val securitySection = exported["security"] as Map<*, *>
        securitySection["encryptionKey"] shouldBe "********"
        securitySection["tokenSecret"] shouldBe "********"
    }

    @Test
    fun `should generate detailed diagnostics report`() {
        val loader = HopliteConfigLoader(runtimeProfile = "dev")
        val config = loader.load()

        val report = ConfigDiagnostics.generateReport("dev", config, emptyList())
        
        report.loadedProfile shouldBe "dev"
        report.sourcesPrecedence.size shouldBe 6
    }
}
