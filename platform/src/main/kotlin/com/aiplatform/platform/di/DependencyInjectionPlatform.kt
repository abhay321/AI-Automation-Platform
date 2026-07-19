package com.aiplatform.platform.di

import com.aiplatform.platform.config.*
import com.aiplatform.platform.lifecycle.api.*
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Production-grade Dependency Injection Platform built on top of Koin.
 * Serves as a modular, lifecycle-aware controller that manages core singleton
 * injection registrations, dynamic component discovery, and clean resource reclamation during shutdown.
 */
@PlatformModule(id = "dependency-injection", isRequired = true, description = "Platform Dependency Injection Engine")
class DependencyInjectionPlatform(
    private val configLoader: ConfigLoader = HopliteConfigLoader()
) : LifecycleAware {

    private val log = LoggerFactory.getLogger(DependencyInjectionPlatform::class.java)

    override val id: String = "dependency-injection"
    override val dependencies: List<String> = emptyList()

    // Thread-safe registry for external custom Koin modules
    private val customModules = CopyOnWriteArrayList<Module>()

    @Volatile
    private var isKoinStarted = false

    /**
     * Registers a custom Koin module prior to container initialization.
     */
    fun registerModule(module: Module) {
        if (isKoinStarted) {
            throw IllegalStateException("Cannot register Koin module after DI container has been initialized.")
        }
        customModules.add(module)
        log.debug("Registered custom Koin dependency module.")
    }

    // -------------------------------------------------------------
    // LifecycleAware implementation
    // -------------------------------------------------------------

    override fun onBootstrap() {
        log.info("Bootstrapping Dependency Injection framework...")
    }

    override fun onInitialize() {
        log.info("Initializing Dependency Injection container context...")

        synchronized(this) {
            if (GlobalContext.getOrNull() != null) {
                log.warn("Koin context was already active. Stopping existing container to guarantee clean context boundaries.")
                stopKoin()
            }

            // Load platform configuration
            val platformConfig = try {
                configLoader.load()
            } catch (t: Throwable) {
                log.error("Failed to load platform configuration during DI initialization.", t)
                throw t
            }

            // Define core modules
            val coreModule = module {
                // Main Configuration Bindings
                single<PlatformConfig> { platformConfig }
                single<ServerConfig> { platformConfig.server }
                single<DatabaseConfig> { platformConfig.database }
                single<RedisConfig> { platformConfig.redis }
                single<RabbitMqConfig> { platformConfig.rabbitmq }
                single<MinioConfig> { platformConfig.minio }
                single<QdrantConfig> { platformConfig.qdrant }
                single<SecurityConfig> { platformConfig.security }
                single<MetricsConfig> { platformConfig.metrics }
            }

            val allModules = ArrayList<Module>().apply {
                add(coreModule)
                addAll(customModules)
            }

            // Start Koin Container
            startKoin {
                modules(allModules)
            }
            isKoinStarted = true
            log.info("DI Container started successfully with {} registered configuration & service modules.", allModules.size)
        }
    }

    override fun onStartup() {
        log.info("Dependency Injection container fully validated and active.")
    }

    override fun onShutdown() {
        log.info("Stopping DI container context...")
        synchronized(this) {
            if (GlobalContext.getOrNull() != null) {
                stopKoin()
                isKoinStarted = false
                log.info("Koin context stopped cleanly.")
            } else {
                log.debug("No active Koin context found to stop.")
            }
        }
    }

    override fun onCleanup() {
        customModules.clear()
        log.info("DI Custom Module registry cleared.")
    }
}
