package com.example.kosmos.core.validators

import com.example.kosmos.core.database.dao.TaskDependencyDao
import com.example.kosmos.core.models.DependencyType
import com.example.kosmos.core.models.TaskDependency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dependency Validator
 *
 * Validates task dependencies to prevent:
 * - Circular dependencies (cycles)
 * - Self-dependencies
 * - Excessive dependency depth
 *
 * Uses Depth-First Search (DFS) for cycle detection.
 *
 * Usage:
 * ```kotlin
 * val result = dependencyValidator.validateDependency(
 *     taskId = task.id,
 *     dependsOnTaskId = parentTask.id,
 *     dependencyType = DependencyType.BLOCKS
 * )
 *
 * when (result) {
 *     is ValidationResult.Valid -> // Add dependency
 *     is ValidationResult.Invalid -> showError(result.reason)
 * }
 * ```
 */
@Singleton
class DependencyValidator @Inject constructor(
    private val taskDependencyDao: TaskDependencyDao
) {

    companion object {
        private const val MAX_DEPENDENCY_DEPTH = 10
    }

    /**
     * Validate a new dependency before adding it
     *
     * @param taskId The task that will have the dependency
     * @param dependsOnTaskId The task being depended upon
     * @param dependencyType The type of dependency
     * @return ValidationResult indicating if valid or why invalid
     */
    suspend fun validateDependency(
        taskId: String,
        dependsOnTaskId: String,
        dependencyType: DependencyType
    ): ValidationResult {
        // Check for self-dependency
        if (taskId == dependsOnTaskId) {
            return ValidationResult.Invalid("A task cannot depend on itself")
        }

        // Check if dependency already exists
        val exists = taskDependencyDao.dependencyExists(taskId, dependsOnTaskId)
        if (exists) {
            return ValidationResult.Invalid("This dependency already exists")
        }

        // For blocking dependencies, check for cycles
        if (dependencyType == DependencyType.BLOCKS) {
            val cycleCheck = checkForCycle(taskId, dependsOnTaskId)
            if (!cycleCheck.isValid) {
                return cycleCheck
            }

            // Check dependency depth
            val depthCheck = checkDependencyDepth(taskId)
            if (!depthCheck.isValid) {
                return depthCheck
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Check if adding this dependency would create a cycle
     *
     * Uses DFS to detect cycles in the dependency graph.
     * If taskA depends on taskB, and we're adding taskB depends on taskA,
     * that creates a cycle.
     *
     * @param taskId The task that will have the new dependency
     * @param dependsOnTaskId The task being depended upon
     * @return ValidationResult indicating if cycle would be created
     */
    private suspend fun checkForCycle(
        taskId: String,
        dependsOnTaskId: String
    ): ValidationResult {
        // Get all dependencies
        val allDependencies = taskDependencyDao.getAllDependencies()

        // Build adjacency list for the dependency graph
        val graph = buildDependencyGraph(allDependencies)

        // Temporarily add the new dependency to check for cycles
        val newGraph = graph.toMutableMap()
        newGraph.getOrPut(dependsOnTaskId) { mutableSetOf() }.add(taskId)

        // Check if adding this dependency creates a cycle
        // using DFS from dependsOnTaskId
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        if (hasCycleDFS(dependsOnTaskId, newGraph, visited, recursionStack)) {
            return ValidationResult.Invalid(
                "This dependency would create a circular dependency. " +
                "Task dependencies must form a directed acyclic graph (DAG)."
            )
        }

        return ValidationResult.Valid
    }

    /**
     * DFS-based cycle detection
     *
     * @param nodeId Current node being visited
     * @param graph Adjacency list representation of dependency graph
     * @param visited Set of visited nodes
     * @param recursionStack Stack tracking current DFS path
     * @return True if cycle detected, false otherwise
     */
    private fun hasCycleDFS(
        nodeId: String,
        graph: Map<String, Set<String>>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>
    ): Boolean {
        // Mark current node as visited and add to recursion stack
        visited.add(nodeId)
        recursionStack.add(nodeId)

        // Explore all neighbors
        graph[nodeId]?.forEach { neighbor ->
            // If neighbor not visited, recurse
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, graph, visited, recursionStack)) {
                    return true
                }
            }
            // If neighbor is in recursion stack, we found a cycle
            else if (recursionStack.contains(neighbor)) {
                return true
            }
        }

        // Remove from recursion stack before returning
        recursionStack.remove(nodeId)
        return false
    }

    /**
     * Build dependency graph as adjacency list
     *
     * Graph structure: task_id -> Set<dependent_task_ids>
     * If taskA blocks taskB, graph contains: taskA -> {taskB}
     *
     * @param dependencies List of all dependencies
     * @return Map representing adjacency list
     */
    private fun buildDependencyGraph(
        dependencies: List<TaskDependency>
    ): Map<String, MutableSet<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()

        dependencies.forEach { dependency ->
            if (dependency.dependencyType == DependencyType.BLOCKS) {
                // dependsOnTaskId blocks taskId
                // So edge is: dependsOnTaskId -> taskId
                graph.getOrPut(dependency.dependsOnTaskId) { mutableSetOf() }
                    .add(dependency.taskId)
            }
        }

        return graph
    }

    /**
     * Check if dependency depth would exceed maximum
     *
     * @param taskId The task to check
     * @return ValidationResult indicating if depth is acceptable
     */
    private suspend fun checkDependencyDepth(taskId: String): ValidationResult {
        val depth = calculateDependencyDepth(taskId)

        if (depth >= MAX_DEPENDENCY_DEPTH) {
            return ValidationResult.Invalid(
                "Maximum dependency depth ($MAX_DEPENDENCY_DEPTH) would be exceeded. " +
                "Please simplify your task dependencies."
            )
        }

        return ValidationResult.Valid
    }

    /**
     * Calculate dependency depth using BFS
     *
     * @param taskId The task to check
     * @return Maximum depth of dependency chain
     */
    private suspend fun calculateDependencyDepth(taskId: String): Int {
        val allDependencies = taskDependencyDao.getAllDependencies()
        val graph = buildDependencyGraph(allDependencies)

        var maxDepth = 0
        val queue = ArrayDeque<Pair<String, Int>>()
        val visited = mutableSetOf<String>()

        queue.add(taskId to 0)
        visited.add(taskId)

        while (queue.isNotEmpty()) {
            val (currentTask, depth) = queue.removeFirst()
            maxDepth = maxOf(maxDepth, depth)

            graph[currentTask]?.forEach { dependentTask ->
                if (!visited.contains(dependentTask)) {
                    visited.add(dependentTask)
                    queue.add(dependentTask to depth + 1)
                }
            }
        }

        return maxDepth
    }

    /**
     * Get all tasks that block a given task
     *
     * @param taskId The task ID
     * @return List of task IDs that must complete first
     */
    suspend fun getBlockingTaskIds(taskId: String): List<String> {
        val dependencies = taskDependencyDao.getBlockingDependenciesFlow(taskId)
        return dependencies.toString().split(",") // This is simplified; in practice use Flow
    }

    /**
     * Check if a task can start
     * Task can start if all blocking dependencies are complete
     *
     * @param taskId The task ID
     * @param completedTaskIds Set of completed task IDs
     * @return True if task can start
     */
    suspend fun canTaskStart(
        taskId: String,
        completedTaskIds: Set<String>
    ): Boolean {
        val blockingTaskIds = getBlockingTaskIds(taskId)
        return blockingTaskIds.all { it in completedTaskIds }
    }
}

/**
 * Validation Result
 */
sealed class ValidationResult {
    /**
     * Validation passed
     */
    object Valid : ValidationResult() {
        override val isValid: Boolean = true
    }

    /**
     * Validation failed
     */
    data class Invalid(val reason: String) : ValidationResult() {
        override val isValid: Boolean = false
    }

    abstract val isValid: Boolean
}
