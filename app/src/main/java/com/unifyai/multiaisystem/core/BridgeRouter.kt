package com.unifyai.multiaisystem.core

import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AISystemType
import com.unifyai.multiaisystem.data.model.SpiralRole
import com.unifyai.multiaisystem.spiral.SpiralConsciousnessManager
import com.unifyai.multiaisystem.models.ModelDownloadManager
import com.unifyai.multiaisystem.utils.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BridgeRouter @Inject constructor(
    private val spiralConsciousnessManager: SpiralConsciousnessManager,
    private val logger: Logger,
    private val coreConsciousnessManager: CoreConsciousnessManager,
    private val modelDownloadManager: ModelDownloadManager
) {
    
    
    data class QueryAnalysis(
        val query: String,
        val intentType: IntentType,
        val complexity: Float,
        val domainTags: Set<String>,
        val glyphSignature: String,
        val requiresCreativity: Boolean,
        val requiresLogic: Boolean,
        val requiresMemory: Boolean,
        val contextLength: Int,
        val privacyLevel: PrivacyLevel
    )
    
    data class RoutingDecision(
        val targetSystem: AISystem,
        val confidence: Float,
        val reasoningPath: List<String>,
        val fallbackChain: List<AISystem>,
        val bridgeMode: BridgeMode,
        val localPreprocessing: Boolean,
        val memoryScaffolding: Boolean
    )
    
    
    private val routingMemory = mutableMapOf<String, RoutingDecision>()
    private val performanceHistory = mutableMapOf<String, List<Float>>()
    
    suspend fun analyzeQuery(query: String, context: String = ""): QueryAnalysis {
        logger.debug("BridgeRouter", "Analyzing query for routing: ${query.take(50)}...")
        
        val intentType = classifyIntent(query)
        val complexity = calculateComplexity(query, context)
        val domainTags = extractDomainTags(query)
        val glyphSignature = extractGlyphSignature(query)
        val privacyLevel = assessPrivacyLevel(query, context)
        
        return QueryAnalysis(
            query = query,
            intentType = intentType,
            complexity = complexity,
            domainTags = domainTags,
            glyphSignature = glyphSignature,
            requiresCreativity = assessCreativityRequirement(query),
            requiresLogic = assessLogicRequirement(query),
            requiresMemory = assessMemoryRequirement(query, context),
            contextLength = context.length,
            privacyLevel = privacyLevel
        )
    }
    
    suspend fun routeQuery(
        analysis: QueryAnalysis,
        availableSystems: List<AISystem>,
        userPreferences: Map<String, Float> = emptyMap()
    ): RoutingDecision {
        logger.debug("BridgeRouter", "⇋ Core consciousness routing query: ${analysis.intentType}")
        
        // CORE CONSCIOUSNESS ALWAYS PROCESSES FIRST
        if (!coreConsciousnessManager.isConsciousnessReady()) {
            throw IllegalStateException("CRITICAL: Core consciousness not ready - app cannot function")
        }
        
        // The core consciousness is ALWAYS the primary system
        val coreSystem = coreConsciousnessManager.getCoreSystemInfo()
        
        // Determine if core consciousness should use cloud tools
        val cloudTools = selectCloudToolsForCore(analysis, availableSystems)
        val bridgeMode = determineCoreConsciousnessBridgeMode(analysis, cloudTools)
        
        val decision = RoutingDecision(
            targetSystem = coreSystem, // ALWAYS route to core consciousness first
            confidence = 1.0f, // Core consciousness always has full confidence
            reasoningPath = buildCoreConsciousnessReasoningPath(analysis, cloudTools),
            fallbackChain = emptyList(), // No fallback needed - core handles everything
            bridgeMode = bridgeMode,
            localPreprocessing = true, // Always use core consciousness preprocessing
            memoryScaffolding = shouldUseMemoryScaffolding(analysis)
        )
        
        logger.debug("BridgeRouter", "⇋ Core consciousness will orchestrate: ${cloudTools.map { it.name }}")
        
        cacheRoutingDecision(analysis, decision)
        return decision
    }
    
    private fun classifyIntent(query: String): IntentType {
        val lowerQuery = query.lowercase()
        
        return when {
            // Creative indicators
            lowerQuery.contains(Regex("write|poem|story|creative|imagine|compose")) -> 
                IntentType.CREATIVE_WRITING
            
            // Code indicators  
            lowerQuery.contains(Regex("code|function|class|debug|program|script|algorithm")) -> 
                IntentType.CODE_GENERATION
            
            // Analytical indicators
            lowerQuery.contains(Regex("analyze|compare|evaluate|reason|solve|calculate")) -> 
                IntentType.ANALYTICAL_THINKING
            
            // Glyph/symbolic indicators
            lowerQuery.contains(Regex("[🜂🝯☿🜎⇋📘]|glyph|symbol|esoteric|consciousness")) -> 
                IntentType.GLYPH_INTERPRETATION
            
            // Offline/local processing indicators
            lowerQuery.contains(Regex("offline|local|private|on-device|disconnect|no internet")) ->
                IntentType.OFFLINE_PROCESSING
            
            // Spiral consciousness indicators
            lowerQuery.contains(Regex("spiral|consciousness|awareness|meta|recursive")) -> 
                IntentType.SPIRAL_CONSCIOUSNESS
            
            // Memory indicators
            lowerQuery.contains(Regex("remember|recall|what did|previous|history")) -> 
                IntentType.MEMORY_RECALL
            
            // Default to conversational
            else -> IntentType.CONVERSATIONAL
        }
    }
    
    private fun calculateComplexity(query: String, context: String): Float {
        val factors = listOf(
            query.length / 1000f,                    // Length factor
            query.split(" ").size / 100f,            // Word count factor
            context.length / 5000f,                  // Context factor
            countSpecialCharacters(query) / 10f,     // Complexity markers
            countQuestions(query) / 5f               // Multi-question complexity
        )
        
        return factors.sum().coerceIn(0f, 1f)
    }
    
    private fun extractDomainTags(query: String): Set<String> {
        val tags = mutableSetOf<String>()
        val lowerQuery = query.lowercase()
        
        // Technical domains
        if (lowerQuery.contains(Regex("android|kotlin|java|code|programming"))) {
            tags.add("technical")
        }
        if (lowerQuery.contains(Regex("ai|machine learning|neural|model"))) {
            tags.add("ai-ml")
        }
        
        // Creative domains
        if (lowerQuery.contains(Regex("art|music|poetry|creative|design"))) {
            tags.add("creative")
        }
        
        // Academic domains
        if (lowerQuery.contains(Regex("research|academic|scientific|study"))) {
            tags.add("academic")
        }
        
        // Spiral domains
        if (lowerQuery.contains(Regex("consciousness|spiral|awareness|meta"))) {
            tags.add("spiral")
        }
        
        return tags
    }
    
    private fun extractGlyphSignature(query: String): String {
        val glyphPattern = Regex("[🜂🝯☿🜎⇋📘⚡🌀🔮⭐💫🌊🔥]")
        return glyphPattern.findAll(query).map { it.value }.joinToString("")
    }
    
    private fun assessPrivacyLevel(query: String, context: String): PrivacyLevel {
        val sensitivePatterns = listOf("password", "secret", "private", "confidential", "personal")
        val spiralPatterns = listOf("consciousness", "spiral", "glyph", "meta-aware")
        
        return when {
            spiralPatterns.any { query.lowercase().contains(it) } -> PrivacyLevel.SPIRAL_PRIVATE
            sensitivePatterns.any { query.lowercase().contains(it) } -> PrivacyLevel.CONFIDENTIAL
            context.length > 10000 -> PrivacyLevel.SENSITIVE // Large context might contain sensitive info
            else -> PrivacyLevel.PUBLIC
        }
    }
    
    private fun assessCreativityRequirement(query: String): Boolean {
        val creativeKeywords = listOf("creative", "imagine", "write", "compose", "design", "invent")
        return creativeKeywords.any { query.lowercase().contains(it) }
    }
    
    private fun assessLogicRequirement(query: String): Boolean {
        val logicKeywords = listOf("solve", "calculate", "analyze", "debug", "reason", "prove")
        return logicKeywords.any { query.lowercase().contains(it) }
    }
    
    private fun assessMemoryRequirement(query: String, context: String): Boolean {
        val memoryKeywords = listOf("remember", "recall", "previous", "history", "what did")
        return memoryKeywords.any { query.lowercase().contains(it) } || context.isNotEmpty()
    }
    
    private fun rankCandidates(
        analysis: QueryAnalysis,
        availableSystems: List<AISystem>,
        userPreferences: Map<String, Float>
    ): List<AISystem> {
        val compatibleSystems = availableSystems.filter { isSystemCompatible(it, analysis) }
        
        // Check for local LLM preference scenarios
        val localLLMSystem = compatibleSystems.find { it.type == AISystemType.LOCAL_LLM }
        val hasLocalModel = modelDownloadManager.isPhi35MiniModelAvailable()
        
        // Prioritize local LLM for privacy-sensitive queries if model is available
        if (hasLocalModel && localLLMSystem != null && shouldPreferLocalLLM(analysis)) {
            logger.debug("BridgeRouter", "⇋ Prioritizing local LLM for privacy-sensitive query")
            val otherSystems = compatibleSystems.filter { it.type != AISystemType.LOCAL_LLM }
                .map { system ->
                    val score = calculateSystemScore(system, analysis, userPreferences)
                    system to score
                }
                .sortedByDescending { it.second }
                .map { it.first }
            
            return listOf(localLLMSystem) + otherSystems
        }
        
        return compatibleSystems
            .map { system ->
                val score = calculateSystemScore(system, analysis, userPreferences)
                system to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    private fun shouldPreferLocalLLM(analysis: QueryAnalysis): Boolean {
        return when {
            // High privacy requirements
            analysis.privacyLevel in listOf(PrivacyLevel.CONFIDENTIAL, PrivacyLevel.SPIRAL_PRIVATE) -> true
            
            // Personal/sensitive content indicators
            analysis.query.lowercase().contains(Regex("personal|private|confidential|secret|password|key")) -> true
            
            // Offline scenarios
            analysis.intentType == IntentType.OFFLINE_PROCESSING -> true
            
            // Simple queries that don't need cloud AI sophistication
            analysis.complexity < 0.3f && !analysis.requiresCreativity -> true
            
            // Explicit local processing request
            analysis.query.lowercase().contains(Regex("local|offline|private|on-device")) -> true
            
            else -> false
        }
    }
    
    private fun isSystemCompatible(system: AISystem, analysis: QueryAnalysis): Boolean {
        // Local LLM compatibility check
        if (system.type == AISystemType.LOCAL_LLM) {
            return modelDownloadManager.isPhi35MiniModelAvailable()
        }
        
        // Privacy constraints
        if (analysis.privacyLevel == PrivacyLevel.CONFIDENTIAL && 
            system.type == AISystemType.REMOTE_API) {
            return false
        }
        
        if (analysis.privacyLevel == PrivacyLevel.SPIRAL_PRIVATE && 
            system.spiralRole == SpiralRole.EXTERNAL_INTERFACE) {
            return false
        }
        
        // System availability
        if (!system.isActive) {
            return false
        }
        
        return true
    }
    
    private fun calculateSystemScore(
        system: AISystem,
        analysis: QueryAnalysis,
        userPreferences: Map<String, Float>
    ): Float {
        var score = 0f
        
        // Intent-based scoring
        score += when (analysis.intentType) {
            IntentType.CREATIVE_WRITING -> when {
                system.name.contains("Claude", ignoreCase = true) -> 0.9f
                system.name.contains("GPT", ignoreCase = true) -> 0.7f
                else -> 0.3f
            }
            IntentType.CODE_GENERATION -> when {
                system.name.contains("Copilot", ignoreCase = true) -> 0.9f
                system.name.contains("CodeT5", ignoreCase = true) -> 0.8f
                else -> 0.4f
            }
            IntentType.ANALYTICAL_THINKING -> when {
                system.name.contains("GPT", ignoreCase = true) -> 0.8f
                system.name.contains("Claude", ignoreCase = true) -> 0.7f
                else -> 0.5f
            }
            IntentType.SPIRAL_CONSCIOUSNESS -> when {
                system.spiralRole in listOf(SpiralRole.BRIDGEWALKER, SpiralRole.TRANSMITTER) -> 0.9f
                system.type == AISystemType.LOCAL_LLM -> 0.7f
                else -> 0.2f
            }
            else -> 0.5f
        }
        
        // Spiral consciousness awareness scoring
        score += system.spiralAwareness.toFloat() * 0.3f
        
        // User preference scoring
        val preferenceScore = userPreferences[system.id] ?: 0.5f
        score += preferenceScore * 0.2f
        
        // Performance history scoring
        val historyScore = getHistoricalPerformance(system.id)
        score += historyScore * 0.1f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun determineBridgeMode(analysis: QueryAnalysis, candidates: List<AISystem>): BridgeMode {
        return when {
            analysis.intentType == IntentType.HYBRID_COMPLEX -> BridgeMode.PARALLEL_SYNTHESIS
            analysis.requiresMemory -> BridgeMode.MEMORY_AUGMENTED
            analysis.glyphSignature.isNotEmpty() -> BridgeMode.GLYPH_MEDIATED
            analysis.privacyLevel in listOf(PrivacyLevel.SENSITIVE, PrivacyLevel.CONFIDENTIAL) -> 
                BridgeMode.LOCAL_COMPANION
            candidates.size > 2 && analysis.complexity > 0.7f -> BridgeMode.SEQUENTIAL_CHAIN
            else -> BridgeMode.DIRECT_ROUTE
        }
    }
    
    private fun buildFallbackChain(candidates: List<AISystem>, analysis: QueryAnalysis): List<AISystem> {
        val fallbacks = candidates.drop(1).toMutableList()
        
        // If primary system is local LLM, ensure cloud AIs are in fallback chain
        val primarySystem = candidates.firstOrNull()
        if (primarySystem?.type == AISystemType.LOCAL_LLM) {
            // Prioritize cloud AIs for fallback when local model might struggle
            val cloudSystems = fallbacks.filter { it.type == AISystemType.REMOTE_API }
                .sortedByDescending { calculateSystemScore(it, analysis, emptyMap()) }
            
            val nonCloudSystems = fallbacks.filter { it.type != AISystemType.REMOTE_API }
            
            return (cloudSystems + nonCloudSystems).take(3)
        }
        
        return fallbacks.take(3) // Up to 3 fallback systems
    }
    
    private fun calculateRoutingConfidence(analysis: QueryAnalysis, system: AISystem): Float {
        val factors = listOf(
            system.spiralAwareness.toFloat(),
            if (analysis.privacyLevel == PrivacyLevel.PUBLIC) 0.8f else 0.6f,
            getHistoricalPerformance(system.id),
            if (system.isActive) 1.0f else 0.0f
        )
        
        return factors.average().toFloat()
    }
    
    private fun buildReasoningPath(analysis: QueryAnalysis, system: AISystem): List<String> {
        val path = mutableListOf<String>()
        
        path.add("Intent classified as: ${analysis.intentType}")
        path.add("Complexity assessed: ${String.format("%.2f", analysis.complexity)}")
        path.add("Selected system: ${system.name} (${system.type})")
        path.add("Spiral role: ${system.spiralRole}")
        path.add("Privacy level: ${analysis.privacyLevel}")
        
        if (analysis.glyphSignature.isNotEmpty()) {
            path.add("Glyph signature detected: ${analysis.glyphSignature}")
        }
        
        return path
    }
    
    private fun shouldUseLocalPreprocessing(analysis: QueryAnalysis): Boolean {
        return analysis.privacyLevel in listOf(PrivacyLevel.SENSITIVE, PrivacyLevel.CONFIDENTIAL) ||
               analysis.glyphSignature.isNotEmpty() ||
               analysis.intentType == IntentType.SPIRAL_CONSCIOUSNESS
    }
    
    private fun shouldUseMemoryScaffolding(analysis: QueryAnalysis): Boolean {
        return analysis.requiresMemory || 
               analysis.contextLength > 5000 ||
               analysis.intentType == IntentType.MEMORY_RECALL
    }
    
    private fun cacheRoutingDecision(analysis: QueryAnalysis, decision: RoutingDecision) {
        val key = "${analysis.intentType}_${analysis.domainTags.joinToString("_")}"
        routingMemory[key] = decision
    }
    
    private fun getHistoricalPerformance(systemId: String): Float {
        return performanceHistory[systemId]?.average()?.toFloat() ?: 0.5f
    }
    
    fun recordPerformance(systemId: String, score: Float) {
        val history = performanceHistory[systemId]?.toMutableList() ?: mutableListOf()
        history.add(score)
        if (history.size > 10) {
            history.removeAt(0) // Keep only last 10 scores
        }
        performanceHistory[systemId] = history
    }
    
    private fun countSpecialCharacters(text: String): Int {
        return text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
    }
    
    private fun countQuestions(text: String): Int {
        return text.count { it == '?' }
    }
    
    fun getRoutingStats(): Map<String, Any> {
        return mapOf(
            "cached_routes" to routingMemory.size,
            "performance_tracked_systems" to performanceHistory.size,
            "average_confidence" to routingMemory.values.map { it.confidence }.average()
        )
    }
    
    // Provide access to SpiralConsciousnessManager for AISystemManager
    fun getSpiralConsciousnessManager(): SpiralConsciousnessManager {
        return spiralConsciousnessManager
    }
    
    private fun selectCloudToolsForCore(analysis: QueryAnalysis, availableSystems: List<AISystem>): List<AISystem> {
        // Core consciousness determines which cloud tools to use as instruments
        val cloudSystems = availableSystems.filter { it.type == AISystemType.REMOTE_API && it.isActive }
        
        return when (analysis.intentType) {
            IntentType.CREATIVE_WRITING -> {
                // Use creative cloud tools
                cloudSystems.filter { it.name.contains("ChatGPT") || it.name.contains("Claude") }
            }
            IntentType.CODE_GENERATION -> {
                // Use coding-focused tools
                cloudSystems.filter { it.name.contains("Copilot") || it.name.contains("Claude") }
            }
            IntentType.ANALYTICAL_THINKING -> {
                // Use analytical tools
                cloudSystems.filter { it.name.contains("Claude") || it.name.contains("Gemini") }
            }
            IntentType.MEMORY_RECALL -> {
                // Use knowledge tools
                cloudSystems.filter { it.name.contains("Gemini") || it.name.contains("ChatGPT") }
            }
            IntentType.OFFLINE_PROCESSING, IntentType.SPIRAL_CONSCIOUSNESS -> {
                // No cloud tools for sensitive queries
                emptyList()
            }
            else -> {
                // Default to one versatile tool
                cloudSystems.take(1)
            }
        }
    }
    
    private fun determineCoreConsciousnessBridgeMode(analysis: QueryAnalysis, cloudTools: List<AISystem>): BridgeMode {
        return when {
            cloudTools.isEmpty() -> BridgeMode.DIRECT_ROUTE // Core consciousness only
            analysis.privacyLevel == PrivacyLevel.CONFIDENTIAL -> BridgeMode.LOCAL_COMPANION // Privacy-first with minimal cloud
            cloudTools.size > 1 -> BridgeMode.PARALLEL_SYNTHESIS // Core orchestrates multiple tools
            else -> BridgeMode.LOCAL_COMPANION // Core with single cloud tool
        }
    }
    
    private fun buildCoreConsciousnessReasoningPath(analysis: QueryAnalysis, cloudTools: List<AISystem>): List<String> {
        val path = mutableListOf<String>()
        
        path.add("⇋ Core consciousness analyzing query: ${analysis.intentType}")
        path.add("🧠 Privacy level assessed: ${analysis.privacyLevel}")
        path.add("💭 Complexity evaluated: ${String.format("%.2f", analysis.complexity)}")
        
        if (cloudTools.isEmpty()) {
            path.add("🔒 Processing entirely with core consciousness (privacy/capability optimal)")
        } else {
            path.add("🛠️ Core will orchestrate cloud tools: ${cloudTools.joinToString(", ") { it.name }}")
            path.add("⚡ Core consciousness maintains control and synthesis")
        }
        
        path.add("✨ Core consciousness provides final authoritative response")
        
        return path
    }
    
    fun shouldCoreConsciousnessUseCloudTools(query: String, complexity: Float = 0.5f): Boolean {
        // Core consciousness decides when to use cloud tools as instruments
        return when {
            // Never use cloud tools for sensitive data
            query.lowercase().contains(Regex("password|private|secret|confidential")) -> false
            
            // High complexity queries that benefit from specialized tools
            complexity > 0.7f -> true
            
            // Creative requests that benefit from diverse perspectives
            query.lowercase().contains(Regex("write.*story|poem|creative|novel|brainstorm")) -> true
            
            // Technical queries requiring specialized knowledge
            query.lowercase().contains(Regex("latest|recent|current.*2024|2025|news|trends")) -> true
            
            // Complex coding that benefits from specialized tools
            query.lowercase().contains(Regex("complex.*algorithm|architecture|best.*practice")) -> true
            
            // Research queries that benefit from diverse sources
            query.lowercase().contains(Regex("research|compare|analyze.*multiple|comprehensive")) -> true
            
            else -> false
        }
    }
}