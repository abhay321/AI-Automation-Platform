package com.aiplatform.platform.di

import com.aiplatform.platform.config.*
import com.aiplatform.platform.lifecycle.api.LifecycleState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class DependencyInjectionPlatformTest {

    // Simple MockConfigLoader to prevent loading real YAML resources during tests
    private class MockConfigLoader : ConfigLoader {
        override val activeProfile: String = "test"
        
        override fun load(): PlatformConfig {
            return PlatformConfig(
                server = ServerConfig(port = 8080, host = "localhost"),
                database = DatabaseConfig(url = "jdbc:postgresql://localhost:5432/test_db", username = "test", password = "pwd"),
                redis = RedisConfig(),
                rabbitmq = RabbitMqConfig(),
                minio = MinioConfig(accessKey = "minio", secretKey = "secret"),
                qdrant = QdrantConfig(),
                security = SecurityConfig(encryptionKey = "some_secret_key_long_enough", tokenSecret = "token_secret"),
                metrics = MetricsConfig()
            )
        }
    }

    private lateinit var diPlatform: DependencyInjectionPlatform

    @BeforeEach
    fun setUp() {
        diPlatform = DependencyInjectionPlatform(MockConfigLoader())
    }

    @AfterEach
    fun tearDown() {
        // Clean up any remaining Koin instances
        diPlatform.onShutdown()
        diPlatform.onCleanup()
        stopKoin()
    }

    @Test
    fun `test successful DI container setup and config bindings`() {
        diPlatform.onBootstrap()
        diPlatform.onInitialize()
        diPlatform.onStartup()

        // Verify configuration bindings can be resolved via DiContext
        val config = DiContext.get<PlatformConfig>()
        assertNotNull(config)
        assertEquals(8080, config.server.port)
        assertEquals("localhost", config.server.host)

        val serverConfig = DiContext.get<ServerConfig>()
        assertEquals(8080, serverConfig.port)

        val databaseConfig = DiContext.get<DatabaseConfig>()
        assertEquals("jdbc:postgresql://localhost:5432/test_db", databaseConfig.url)
        assertEquals("test", databaseConfig.username)

        val securityConfig = DiContext.get<SecurityConfig>()
        assertEquals("some_secret_key_long_enough", securityConfig.encryptionKey)
    }

    @Test
    fun `test custom modules dynamic registration`() {
        val customModule = module {
            single { "My Custom String Dependency" }
        }

        diPlatform.registerModule(customModule)
        diPlatform.onInitialize()

        // Verify custom dependency is resolved
        val customDependency = DiContext.get<String>()
        assertEquals("My Custom String Dependency", customDependency)
    }

    @Test
    fun `test registering module after initialization throws IllegalStateException`() {
        diPlatform.onInitialize()

        val extraModule = module {
            single { 12345 }
        }

        assertThrows(IllegalStateException::class.java) {
            diPlatform.registerModule(extraModule)
        }
    }

    @Test
    fun `test shutdown stops context and clears custom modules`() {
        val customModule = module {
            single { 3.14159 }
        }
        diPlatform.registerModule(customModule)
        diPlatform.onInitialize()

        assertNotNull(DiContext.get<Double>())

        diPlatform.onShutdown()
        diPlatform.onCleanup()

        // Resolving now should fail
        assertThrows(IllegalStateException::class.java) {
            DiContext.get<Double>()
        }
        assertNull(DiContext.getOrNull<Double>())
    }

    @Test
    fun `test context locator fails when Koin not started`() {
        assertThrows(IllegalStateException::class.java) {
            DiContext.get<PlatformConfig>()
        }
        assertNull(DiContext.getOrNull<PlatformConfig>())
    }
}
