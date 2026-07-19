package com.aiplatform.platform.di

import com.aiplatform.platform.config.*
import com.aiplatform.platform.lifecycle.api.*
import com.aiplatform.platform.lifecycle.engine.DefaultLifecycleManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import java.time.Duration

class DependencyInjectionIntegrationTest {

    private class MockConfigLoader : ConfigLoader {
        override val activeProfile: String = "integration-test"
        
        override fun load(): PlatformConfig {
            return PlatformConfig(
                server = ServerConfig(port = 9000, host = "localhost"),
                database = DatabaseConfig(url = "jdbc:postgresql://localhost:5432/integration_db", username = "sa", password = ""),
                redis = RedisConfig(),
                rabbitmq = RabbitMqConfig(),
                minio = MinioConfig(accessKey = "minio", secretKey = "secret"),
                qdrant = QdrantConfig(),
                security = SecurityConfig(encryptionKey = "some_secret_key_long_enough", tokenSecret = "token_secret"),
                metrics = MetricsConfig()
            )
        }
    }

    // A simulated database component that depends on DI being initialized
    @PlatformModule(id = "mock-database-component", isRequired = true)
    private class MockDatabaseComponent : LifecycleAware {
        override val id: String = "mock-database-component"
        // Depends on dependency-injection module to ensure Koin is started first
        override val dependencies: List<String> = listOf("dependency-injection")

        var jdbcUrl: String? = null
        var isConnected = false

        override fun onBootstrap() {}
        
        override fun onInitialize() {
            // Resolve database config from DI container
            val dbConfig = DiContext.get<DatabaseConfig>()
            jdbcUrl = dbConfig.url
        }

        override fun onStartup() {
            if (jdbcUrl != null) {
                isConnected = true
            }
        }

        override fun onShutdown() {
            isConnected = false
        }

        override fun onCleanup() {
            jdbcUrl = null
        }
    }

    private lateinit var lifecycleManager: DefaultLifecycleManager
    private lateinit var diPlatform: DependencyInjectionPlatform
    private lateinit var dbComponent: MockDatabaseComponent

    @BeforeEach
    fun setUp() {
        lifecycleManager = DefaultLifecycleManager()
        diPlatform = DependencyInjectionPlatform(MockConfigLoader())
        dbComponent = MockDatabaseComponent()
        stopKoin() // Reset Koin before each integration test
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test full platform lifecycle bootstrap starts Koin and resolves dependencies across modules`() {
        // Register components with the Lifecycle Manager
        lifecycleManager.register(diPlatform)
        lifecycleManager.register(dbComponent)

        // Verify pre-boot state
        assertEquals(LifecycleState.OFFLINE, lifecycleManager.currentState)
        assertNull(dbComponent.jdbcUrl)
        assertFalse(dbComponent.isConnected)

        // Boot platform
        lifecycleManager.bootstrapAndRun()

        // Verify successful running state
        assertEquals(LifecycleState.RUNNING, lifecycleManager.currentState)
        
        // Verify DI Platform has started Koin and injected correctly into downstream components
        assertEquals("jdbc:postgresql://localhost:5432/integration_db", dbComponent.jdbcUrl)
        assertTrue(dbComponent.isConnected)

        // Ensure config is also directly available
        val serverConfig = DiContext.get<ServerConfig>()
        assertEquals(9000, serverConfig.port)

        // Trigger shutdown sequence
        lifecycleManager.initiateShutdown(Duration.ofSeconds(2))

        // Verify clean termination and DI context stopped
        assertEquals(LifecycleState.TERMINATED, lifecycleManager.currentState)
        assertFalse(dbComponent.isConnected)
        assertThrows(IllegalStateException::class.java) {
            DiContext.get<ServerConfig>()
        }
    }
}
