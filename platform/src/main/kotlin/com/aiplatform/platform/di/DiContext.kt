package com.aiplatform.platform.di

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.slf4j.LoggerFactory

/**
 * Global Service Locator utility to retrieve dependencies when constructor injection
 * is not possible or desired.
 */
object DiContext {

    private val log = LoggerFactory.getLogger(DiContext::class.java)

    /**
     * Resolves a dependency from the active Koin container.
     * 
     * @param T The type of dependency to resolve.
     * @param qualifier Optional qualifier for named bindings.
     * @param parameters Optional parameters to pass to the factory.
     * @return The resolved instance.
     * @throws IllegalStateException if Koin context is not initialized or the component cannot be found.
     */
    inline fun <reified T : Any> get(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ): T {
        val koin = GlobalContext.getOrNull() 
            ?: throw IllegalStateException("Koin DI container context is not active. Please ensure the platform has been initialized.")
        
        return try {
            koin.get(qualifier, parameters)
        } catch (e: Exception) {
            log.error("Failed to resolve dependency of type '{}' from DI container.", T::class.java.name, e)
            throw e
        }
    }

    /**
     * Safely attempts to resolve a dependency, returning null if the component or container is unavailable.
     */
    inline fun <reified T : Any> getOrNull(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ): T? {
        val koin = GlobalContext.getOrNull() ?: return null
        return koin.getOrNull(qualifier, parameters)
    }
}
