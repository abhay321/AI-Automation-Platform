package com.aiplatform.platform.lifecycle.engine

import com.aiplatform.platform.lifecycle.api.LifecycleAware
import com.aiplatform.platform.lifecycle.api.PlatformModule

/**
 * Exception thrown when a circular dependency is detected within the module graph.
 */
class CircularDependencyException(message: String) : RuntimeException(message)

/**
 * Exception thrown when a required dependency is missing from the registered components.
 */
class MissingDependencyException(message: String) : RuntimeException(message)

/**
 * High-performance, deterministic topological dependency sorter with DFS cycle-detection.
 */
object DependencySorter {

    /**
     * Node coloring for DFS cycle detection.
     */
    private enum class Color {
        WHITE, // Unvisited
        GRAY,  // Visiting (currently in DFS path)
        BLACK  // Visited
    }

    /**
     * Sorts a collection of LifecycleAware components topologically based on their declared dependencies.
     * 
     * @param components The collection of components to sort.
     * @return A list of components sorted in correct execution order.
     * @throws CircularDependencyException if a cycle is detected.
     * @throws MissingDependencyException if a required dependency is missing.
     */
    fun sort(components: Collection<LifecycleAware>): List<LifecycleAware> {
        val componentMap = components.associateBy { it.id }
        val visited = mutableMapOf<String, Color>()
        components.forEach { visited[it.id] = Color.WHITE }

        val sortedList = mutableListOf<LifecycleAware>()

        // DFS function to visit nodes
        fun visit(component: LifecycleAware, path: MutableList<String>) {
            val id = component.id
            val color = visited[id] ?: Color.WHITE

            if (color == Color.GRAY) {
                val cyclePath = path.subList(path.indexOf(id), path.size).joinToString(" -> ") + " -> $id"
                throw CircularDependencyException("Circular dependency detected: $cyclePath")
            }

            if (color == Color.WHITE) {
                visited[id] = Color.GRAY
                path.add(id)

                // Retrieve defined dependencies
                val deps = component.dependencies

                for (depId in deps) {
                    val depComponent = componentMap[depId]
                    if (depComponent == null) {
                        // Dependency is missing. Check if this is required.
                        val isRequired = isModuleRequired(component)
                        if (isRequired) {
                            throw MissingDependencyException("Required dependency '$depId' is missing for component '${component.id}'")
                        } else {
                            // If optional, skip or log warning (will not be included as dependency edge)
                            continue
                        }
                    }
                    visit(depComponent, path)
                }

                visited[id] = Color.BLACK
                path.removeAt(path.size - 1)
                sortedList.add(component)
            }
        }

        // Run DFS on all components to build topological order
        for (component in components) {
            if (visited[component.id] == Color.WHITE) {
                visit(component, mutableListOf())
            }
        }

        return sortedList
    }

    /**
     * Checks if a component's module is required or optional via @PlatformModule annotation.
     */
    private fun isModuleRequired(component: LifecycleAware): Boolean {
        val annotation = component::class.java.getAnnotation(PlatformModule::class.java)
        return annotation?.isRequired ?: true
    }
}
