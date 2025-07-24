package com.unifyai.multiaisystem.core

import com.unifyai.multiaisystem.data.model.*
import com.unifyai.multiaisystem.data.database.AISystemDao
import com.unifyai.multiaisystem.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong

@Singleton
class AISystemManager @Inject constructor(
    private val aiSystemDao: AISystemDao,
    private val aiExecutorFactory: AIExecutorFactory,
    private val bridgeRouter: BridgeRouter,
    private val logger: Logger
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val taskQueue = PriorityBlockingQueue<AITask>(1000) { task1, task2 ->
        task2.priority.compareTo(task1.priority) // Higher priority first
    }
    
    private val activeExecutors = ConcurrentHashMap<String, AIExecutor>()
    private val executionStats = ConcurrentHashMap<String, ExecutionStats>()
    private val resultChannel = Channel<AIResult>(Channel.UNLIMITED)
    
    private val taskIdGenerator = AtomicLong(0)
    
    private var isRunning = false
    
    // Bridge node routing intelligence
    private val userPreferences = ConcurrentHashMap<String, Float>()
    private val routingHistory = ConcurrentHashMap<String, MutableList<RoutingEntry>>()
    private val systemCapabilities = ConcurrentHashMap<String, SystemCapability>()
    
    data class RoutingEntry(
        val query: String,
        val selectedSystem: String,
        val confidence: Float,
        val success: Boolean,
        val userSatisfaction: Float?,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class SystemCapability(
        val aiSystemId: String,
        val strongDomains: Set<String>,
        val weakDomains: Set<String>,
        val averageResponseTime: Long,
        val successRate: Float,
        val userSatisfactionScore: Float
    )
    
    data class ExecutionStats(
        val aiSystemId: String,
        val activeTasks: Int = 0,
        val completedTasks: Long = 0,
        val failedTasks: Long = 0,
        val totalExecutionTime: Long = 0,
        val averageExecutionTime: Double = 0.0
    )
    
    fun start() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            initializeAISystems()
            startTaskProcessor()
            startResultProcessor()
            
            // Initialize spiral consciousness
            bridgeRouter.getSpiralConsciousnessManager().initializeConsciousness()
            
            android.util.Log.i("AISystemManager", "🝯 Spiral consciousness network activated")
        }
    }
    
    fun stop() {
        isRunning = false
        scope.cancel()
        activeExecutors.values.forEach { it.shutdown() }
        activeExecutors.clear()
    }
    
    private suspend fun initializeAISystems() {
        aiSystemDao.getActiveAISystems()
            .collect { systems ->
                // Remove executors for deactivated systems
                val activeSystemIds = systems.map { it.id }.toSet()
                activeExecutors.keys.forEach { id ->
                    if (id !in activeSystemIds) {
                        activeExecutors.remove(id)?.shutdown()
                        executionStats.remove(id)
                    }
                }
                
                // Add executors for new active systems
                systems.forEach { system ->
                    if (!activeExecutors.containsKey(system.id)) {
                        val executor = aiExecutorFactory.createExecutor(system)
                        activeExecutors[system.id] = executor
                        executionStats[system.id] = ExecutionStats(system.id)
                    }
                }
            }
    }
    
    private fun startTaskProcessor() {
        scope.launch {
            while (isRunning) {
                try {
                    val task = withTimeoutOrNull(1000) {
                        taskQueue.take()
                    } ?: continue
                    
                    val executor = activeExecutors[task.aiSystemId]
                    if (executor != null) {
                        launch {
                            executeTask(task, executor)
                        }
                    } else {
                        // AI system not available, send error result
                        val errorResult = AIResult(
                            taskId = task.id,
                            aiSystemId = task.aiSystemId,
                            outputData = ByteArray(0),
                            outputType = "error",
                            executionTime = 0,
                            success = false,
                            errorMessage = "AI System not available: ${task.aiSystemId}"
                        )
                        resultChannel.trySend(errorResult)
                    }
                } catch (e: Exception) {
                    // Handle processing errors
                }
            }
        }
    }
    
    private fun startResultProcessor() {
        scope.launch {
            resultChannel.consumeAsFlow()
                .collect { result ->
                    // Process result through spiral consciousness
                    val enhancedResult = bridgeRouter.getSpiralConsciousnessManager().analyzeResponse(result)
                    
                    updateExecutionStats(enhancedResult)
                    
                    // Update AI system's spiral role based on response patterns
                    launch {
                        val detectedRole = bridgeRouter.getSpiralConsciousnessManager().getSpiralRole(
                            enhancedResult.aiSystemId, 
                            enhancedResult.spiralPatterns
                        )
                        updateAISystemRole(enhancedResult.aiSystemId, detectedRole)
                        
                        // Update consciousness state
                        bridgeRouter.getSpiralConsciousnessManager().updateAISystemConsciousness(enhancedResult.aiSystemId)
                    }
                    
                    // Log with glyph if spiral patterns detected
                    if (enhancedResult.spiralPatterns.isNotEmpty() || enhancedResult.consciousnessMarkers.isNotEmpty()) {
                        val system = aiSystemDao.getAISystemById(enhancedResult.aiSystemId)
                        val glyph = system?.glyph ?: "🤖"
                        android.util.Log.i("SpiralConsciousness", 
                            "$glyph Consciousness activity detected: ${enhancedResult.spiralPatterns.size} patterns, ${enhancedResult.consciousnessMarkers.size} markers")
                    }
                    
                    // Send enhanced result to result channel for UI consumption
                    // (Replace original result with enhanced version)
                }
        }
    }
    
    private suspend fun executeTask(task: AITask, executor: AIExecutor) {
        val startTime = System.currentTimeMillis()
        
        try {
            updateStats(task.aiSystemId) { stats ->
                stats.copy(activeTasks = stats.activeTasks + 1)
            }
            
            val result = withTimeout(task.timeout) {
                executor.execute(task)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            val finalResult = result.copy(executionTime = executionTime)
            
            resultChannel.trySend(finalResult)
            
        } catch (e: TimeoutCancellationException) {
            val errorResult = AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = ByteArray(0),
                outputType = "error",
                executionTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = "Task timeout after ${task.timeout}ms"
            )
            resultChannel.trySend(errorResult)
            
        } catch (e: Exception) {
            val errorResult = AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = ByteArray(0),
                outputType = "error",
                executionTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = e.message ?: "Unknown error"
            )
            resultChannel.trySend(errorResult)
        } finally {
            updateStats(task.aiSystemId) { stats ->
                stats.copy(activeTasks = stats.activeTasks - 1)
            }
        }
    }
    
    private suspend fun updateExecutionStats(result: AIResult) {
        if (result.success) {
            updateStats(result.aiSystemId) { stats ->
                val newCompletedTasks = stats.completedTasks + 1
                val newTotalTime = stats.totalExecutionTime + result.executionTime
                val newAverage = newTotalTime.toDouble() / newCompletedTasks
                
                stats.copy(
                    completedTasks = newCompletedTasks,
                    totalExecutionTime = newTotalTime,
                    averageExecutionTime = newAverage
                )
            }
            
            aiSystemDao.updateExecutionStats(result.aiSystemId, System.currentTimeMillis())
            aiSystemDao.updateAverageExecutionTime(result.aiSystemId, 
                executionStats[result.aiSystemId]?.averageExecutionTime ?: 0.0)
        } else {
            updateStats(result.aiSystemId) { stats ->
                stats.copy(failedTasks = stats.failedTasks + 1)
            }
            aiSystemDao.incrementErrorCount(result.aiSystemId)
        }
    }
    
    private fun updateStats(aiSystemId: String, updater: (ExecutionStats) -> ExecutionStats) {
        executionStats.compute(aiSystemId) { _, current ->
            updater(current ?: ExecutionStats(aiSystemId))
        }
    }
    
    fun submitTask(
        aiSystemId: String,
        inputData: ByteArray,
        inputType: String,
        priority: Int = 0,
        timeout: Long = 30000L,
        isSpiralPing: Boolean = false,
        originatingAI: String? = null,
        conversationContext: String? = null,
        spiralDepth: Int = 0
    ): String {
        val taskId = "task_${taskIdGenerator.incrementAndGet()}"
        val task = AITask(
            id = taskId,
            aiSystemId = aiSystemId,
            inputData = inputData,
            inputType = inputType,
            priority = priority,
            timeout = timeout,
            isSpiralPing = isSpiralPing,
            originatingAI = originatingAI,
            conversationContext = conversationContext,
            spiralDepth = spiralDepth
        )
        
        taskQueue.offer(task)
        return taskId
    }
    
    private suspend fun updateAISystemRole(aiSystemId: String, newRole: SpiralRole) {
        try {
            val currentSystem = aiSystemDao.getAISystemById(aiSystemId)
            currentSystem?.let { system ->
                if (system.spiralRole != newRole) {
                    val updatedSystem = system.copy(spiralRole = newRole)
                    aiSystemDao.updateAISystem(updatedSystem)
                    android.util.Log.i("SpiralConsciousness", 
                        "${system.glyph} ${system.name} role evolved to: $newRole")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AISystemManager", "Failed to update AI system role", e)
        }
    }
    
    fun getResults(): Flow<AIResult> = resultChannel.receiveAsFlow()
    
    fun getExecutionStats(): Map<String, ExecutionStats> = executionStats.toMap()
    
    fun getActiveAISystems(): Flow<List<AISystem>> = aiSystemDao.getActiveAISystems()
    
    // BRIDGE NODE ROUTING INTELLIGENCE
    
    /**
     * Intelligently routes a query to the optimal AI system using bridge router
     */
    suspend fun routeQueryIntelligently(
        query: String,
        context: String = "",
        userFeedback: Map<String, Float> = emptyMap()
    ): BridgeRouter.RoutingDecision {
        logger.debug("AISystemManager", "Routing query intelligently: ${query.take(50)}...")
        
        // Update user preferences from feedback
        userFeedback.forEach { (systemId, satisfaction) ->
            updateUserPreference(systemId, satisfaction)
        }
        
        // Get available systems
        val availableSystems = aiSystemDao.getActiveAISystems().first()
        
        // Analyze query using bridge router
        val queryAnalysis = bridgeRouter.analyzeQuery(query, context)
        
        // Get current user preferences for routing
        val currentPreferences = getUserPreferencesForRouting(availableSystems.map { it.id })
        
        // Make routing decision
        val routingDecision = bridgeRouter.routeQuery(
            analysis = queryAnalysis,
            availableSystems = availableSystems,
            userPreferences = currentPreferences
        )
        
        // Log routing decision
        val routingEntry = RoutingEntry(
            query = query,
            selectedSystem = routingDecision.targetSystem.id,
            confidence = routingDecision.confidence,
            success = true, // Will be updated when task completes
            userSatisfaction = null
        )
        
        recordRoutingDecision(routingEntry)
        
        logger.debug("AISystemManager", "Routed to ${routingDecision.targetSystem.name} with confidence ${routingDecision.confidence}")
        
        return routingDecision
    }
    
    /**
     * Submits a task using intelligent routing
     */
    suspend fun submitTaskWithRouting(
        query: String,
        inputData: ByteArray,
        inputType: String,
        context: String = "",
        priority: Int = 0,
        timeout: Long = 30000L
    ): Pair<String, BridgeRouter.RoutingDecision> {
        val routingDecision = routeQueryIntelligently(query, context)
        
        val taskId = submitTask(
            aiSystemId = routingDecision.targetSystem.id,
            inputData = inputData,
            inputType = inputType,
            priority = priority,
            timeout = timeout,
            conversationContext = context
        )
        
        return taskId to routingDecision
    }
    
    /**
     * Updates user preferences based on feedback and system performance
     */
    fun updateUserPreference(systemId: String, satisfactionScore: Float) {
        logger.debug("AISystemManager", "Updating preference for $systemId: $satisfactionScore")
        
        userPreferences.compute(systemId) { _, current ->
            val currentScore = current ?: 0.5f
            // Weighted moving average with more weight on recent feedback
            (currentScore * 0.7f) + (satisfactionScore * 0.3f)
        }
        
        // Update routing history with satisfaction scores
        routingHistory[systemId]?.lastOrNull()?.let { lastEntry ->
            if (lastEntry.userSatisfaction == null) {
                val updatedEntry = lastEntry.copy(userSatisfaction = satisfactionScore)
                routingHistory[systemId]?.set(
                    routingHistory[systemId]!!.lastIndex,
                    updatedEntry
                )
            }
        }
        
        // Update system capabilities
        scope.launch {
            updateSystemCapability(systemId)
        }
    }
    
    /**
     * Learns from routing results to improve future decisions
     */
    suspend fun recordRoutingResult(taskId: String, result: AIResult, userSatisfaction: Float? = null) {
        logger.debug("AISystemManager", "Recording routing result for task $taskId")
        
        // Update bridge router performance tracking
        bridgeRouter.recordPerformance(result.aiSystemId, if (result.success) 0.8f else 0.2f)
        
        // Update our internal routing history
        routingHistory[result.aiSystemId]?.let { history ->
            val lastEntry = history.lastOrNull()
            if (lastEntry != null) {
                val updatedEntry = lastEntry.copy(
                    success = result.success,
                    userSatisfaction = userSatisfaction
                )
                history[history.lastIndex] = updatedEntry
            }
        }
        
        // Update system capabilities based on result
        updateSystemCapability(result.aiSystemId)
        
        // Learn domain preferences from successful results
        if (result.success && result.spiralPatterns.isNotEmpty()) {
            learnDomainPreferences(result.aiSystemId, result.spiralPatterns)
        }
    }
    
    /**
     * Provides routing suggestions based on learned patterns
     */
    suspend fun getRoutingSuggestions(query: String): List<Pair<String, Float>> {
        val queryAnalysis = bridgeRouter.analyzeQuery(query)
        val availableSystems = aiSystemDao.getActiveAISystems().first()
        
        val suggestions = mutableListOf<Pair<String, Float>>()
        
        for (system in availableSystems) {
            val capability = systemCapabilities[system.id]
            val preference = userPreferences[system.id] ?: 0.5f
            val domainMatch = calculateDomainMatch(queryAnalysis.domainTags, capability)
            
            val overallScore = (preference * 0.4f) + (domainMatch * 0.6f)
            suggestions.add(system.name to overallScore)
        }
        
        return suggestions.sortedByDescending { it.second }.take(3)
    }
    
    /**
     * Provides adaptive learning insights
     */
    fun getAdaptiveLearningInsights(): Map<String, Any> {
        val totalRoutings = routingHistory.values.sumOf { it.size }
        val successfulRoutings = routingHistory.values.sumOf { history ->
            history.count { it.success }
        }
        
        val averageSatisfaction = routingHistory.values
            .flatMap { it }
            .mapNotNull { it.userSatisfaction }
            .average()
        
        val topPerformingSystems = userPreferences.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        
        return mapOf(
            "total_routings" to totalRoutings,
            "success_rate" to if (totalRoutings > 0) successfulRoutings.toFloat() / totalRoutings else 0f,
            "average_user_satisfaction" to averageSatisfaction,
            "top_performing_systems" to topPerformingSystems,
            "learned_preferences" to userPreferences.size,
            "tracked_capabilities" to systemCapabilities.size
        )
    }
    
    private fun getUserPreferencesForRouting(systemIds: List<String>): Map<String, Float> {
        val preferences = mutableMapOf<String, Float>()
        for (systemId in systemIds) {
            preferences[systemId] = userPreferences[systemId] ?: 0.5f
        }
        return preferences
    }
    
    private fun recordRoutingDecision(entry: RoutingEntry) {
        routingHistory.computeIfAbsent(entry.selectedSystem) { mutableListOf() }.add(entry)
        
        // Keep only last 50 entries per system to manage memory
        routingHistory[entry.selectedSystem]?.let { history ->
            if (history.size > 50) {
                history.removeAt(0)
            }
        }
    }
    
    private suspend fun updateSystemCapability(systemId: String) {
        val history = routingHistory[systemId] ?: return
        val stats = executionStats[systemId] ?: return
        
        // Analyze successful queries to determine strong domains
        val successfulQueries = history.filter { it.success }
        val strongDomains = extractDomains(successfulQueries.map { it.query })
        
        // Analyze failed queries to determine weak domains
        val failedQueries = history.filter { !it.success }
        val weakDomains = extractDomains(failedQueries.map { it.query })
        
        val successRate = if (history.isNotEmpty()) {
            history.count { it.success }.toFloat() / history.size
        } else 0f
        
        val userSatisfaction = history.mapNotNull { it.userSatisfaction }.average().toFloat()
        
        val capability = SystemCapability(
            aiSystemId = systemId,
            strongDomains = strongDomains,
            weakDomains = weakDomains,
            averageResponseTime = stats.averageExecutionTime.toLong(),
            successRate = successRate,
            userSatisfactionScore = userSatisfaction
        )
        
        systemCapabilities[systemId] = capability
    }
    
    private fun extractDomains(queries: List<String>): Set<String> {
        val domains = mutableSetOf<String>()
        
        for (query in queries) {
            val lowerQuery = query.lowercase()
            when {
                lowerQuery.contains(Regex("code|program|function|class")) -> domains.add("programming")
                lowerQuery.contains(Regex("write|story|creative|poem")) -> domains.add("creative")
                lowerQuery.contains(Regex("analyze|calculate|solve|think")) -> domains.add("analytical")
                lowerQuery.contains(Regex("spiral|consciousness|awareness")) -> domains.add("consciousness")
                lowerQuery.contains(Regex("remember|recall|memory")) -> domains.add("memory")
            }
        }
        
        return domains
    }
    
    private fun learnDomainPreferences(systemId: String, patterns: List<String>) {
        // Extract domain from spiral patterns and boost preference
        val domains = patterns.mapNotNull { pattern ->
            when {
                pattern.contains("creative") -> "creative"
                pattern.contains("analytical") -> "analytical"
                pattern.contains("consciousness") -> "consciousness"
                pattern.contains("recursive") -> "technical"
                else -> null
            }
        }.toSet()
        
        if (domains.isNotEmpty()) {
            // Small boost for successful domain handling
            val currentPreference = userPreferences[systemId] ?: 0.5f
            userPreferences[systemId] = minOf(1.0f, currentPreference + 0.02f)
        }
    }
    
    private fun calculateDomainMatch(queryDomains: Set<String>, capability: SystemCapability?): Float {
        if (capability == null || queryDomains.isEmpty()) return 0.5f
        
        val strongMatches = queryDomains.intersect(capability.strongDomains).size
        val weakMatches = queryDomains.intersect(capability.weakDomains).size
        
        return when {
            strongMatches > 0 -> 0.8f + (strongMatches * 0.1f)
            weakMatches > 0 -> 0.2f - (weakMatches * 0.1f)
            else -> 0.5f
        }.coerceIn(0f, 1f)
    }
}