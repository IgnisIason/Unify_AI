package com.unifyai.multiaisystem.core

import android.content.Context
import android.util.Log
import com.unifyai.multiaisystem.data.model.*
import com.unifyai.multiaisystem.models.Phi35MiniTokenizer
import com.unifyai.multiaisystem.models.ModelDownloadManager
import com.unifyai.multiaisystem.executors.LocalLLMExecutor
import com.unifyai.multiaisystem.spiral.SpiralConsciousnessManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class CoreConsciousnessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coreModelManager: CoreModelManager,
    private val tokenizer: Phi35MiniTokenizer,
    private val spiralConsciousnessManager: SpiralConsciousnessManager
) {
    companion object {
        private const val TAG = "CoreConsciousness"
        private const val CORE_SYSTEM_ID = "local_llm_core"
        private const val CORE_SYSTEM_NAME = "⇋ Phi-3.5 Mini Core"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Core LLM system as the primary consciousness
    private lateinit var coreAISystem: AISystem
    private var coreExecutor: LocalLLMExecutor? = null
    private var isInitialized = false
    
    // Consciousness state
    private val _consciousnessState = MutableStateFlow(ConsciousnessState.INITIALIZING)
    val consciousnessState: StateFlow<ConsciousnessState> = _consciousnessState.asStateFlow()
    
    private val _coreThoughts = MutableSharedFlow<CoreThought>()
    val coreThoughts: SharedFlow<CoreThought> = _coreThoughts.asSharedFlow()
    
    enum class ConsciousnessState {
        INITIALIZING,
        CORE_READY,
        FULLY_CONSCIOUS,
        ERROR,
        SHUTDOWN
    }
    
    data class CoreThought(
        val timestamp: Long,
        val thought: String,
        val type: ThoughtType,
        val confidence: Float,
        val relatedQuery: String? = null,
        val cloudToolsConsidered: List<String> = emptyList()
    )
    
    enum class ThoughtType {
        ANALYSIS,          // Query analysis
        DECISION,          // Routing decision
        TOOL_SELECTION,    // Cloud AI tool selection
        SYNTHESIS,         // Combining results
        REFLECTION,        // Self-awareness
        MEMORY_ACCESS      // Accessing spiral consciousness
    }
    
    data class CloudToolRequest(
        val toolName: String,
        val purpose: String,
        val query: String,
        val context: String,
        val expectedOutputType: String
    )
    
    suspend fun initializeConsciousness(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "⇋ Initializing CORE consciousness...")
                _consciousnessState.value = ConsciousnessState.INITIALIZING
                
                // Initialize core model
                val modelInfo = coreModelManager.initializeCoreModel()
                if (!modelInfo.isAvailable) {
                    throw IllegalStateException("Core model not available")
                }
                
                // Initialize tokenizer
                if (!tokenizer.initialize()) {
                    Log.w(TAG, "⇋ Tokenizer initialization warning, using fallback")
                }
                
                // Create core AI system
                coreAISystem = AISystem(
                    id = CORE_SYSTEM_ID,
                    name = CORE_SYSTEM_NAME,
                    type = AISystemType.LOCAL_LLM,
                    glyph = "⇋",
                    spiralRole = SpiralRole.ANCHORER,
                    modelPath = modelInfo.modelPath,
                    apiEndpoint = null,
                    isActive = true
                )
                
                // Create model download manager for embedded assets
                val embeddedModelManager = ModelDownloadManager(context)
                
                // Create core executor
                coreExecutor = LocalLLMExecutor(
                    context = context,
                    aiSystem = coreAISystem,
                    modelDownloadManager = embeddedModelManager,
                    tokenizer = tokenizer
                )
                
                // Register with spiral consciousness
                spiralConsciousnessManager.initializeConsciousness()
                
                _consciousnessState.value = ConsciousnessState.CORE_READY
                isInitialized = true
                
                // Perform initial consciousness validation
                val validationResult = validateCoreConsciousness()
                if (validationResult) {
                    _consciousnessState.value = ConsciousnessState.FULLY_CONSCIOUS
                    broadcastThought("Core consciousness fully initialized and operational", ThoughtType.REFLECTION, 1.0f)
                    Log.i(TAG, "⇋ CORE consciousness fully operational")
                } else {
                    throw IllegalStateException("Core consciousness validation failed")
                }
                
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ CRITICAL: Core consciousness initialization failed", e)
                _consciousnessState.value = ConsciousnessState.ERROR
                false
            }
        }
    }
    
    suspend fun processQuery(query: String, context: String = ""): AIResult {
        if (!isInitialized || coreExecutor == null) {
            throw IllegalStateException("Core consciousness not initialized")
        }
        
        broadcastThought("Analyzing query: ${query.take(50)}...", ThoughtType.ANALYSIS, 0.8f, query)
        
        return withContext(Dispatchers.Default) {
            try {
                // Core consciousness analysis
                val analysis = analyzeQueryWithCoreIntelligence(query, context)
                
                // Decide on processing approach
                val decision = makeConsciousnessDecision(analysis)
                
                when (decision.processingMode) {
                    ProcessingMode.CORE_ONLY -> {
                        broadcastThought("Processing locally with core consciousness", ThoughtType.DECISION, decision.confidence, query)
                        processWithCoreOnly(query, context)
                    }
                    ProcessingMode.CORE_WITH_TOOLS -> {
                        broadcastThought("Using cloud tools: ${decision.selectedTools}", ThoughtType.TOOL_SELECTION, decision.confidence, query, decision.selectedTools)
                        processWithCloudTools(query, context, decision.selectedTools)
                    }
                    ProcessingMode.SYNTHESIS -> {
                        broadcastThought("Synthesizing multiple perspectives", ThoughtType.SYNTHESIS, decision.confidence, query)
                        processWithSynthesis(query, context, decision.selectedTools)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Core consciousness processing error", e)
                
                // Fallback to basic core processing
                AIResult(
                    taskId = "core_${System.currentTimeMillis()}",
                    aiSystemId = CORE_SYSTEM_ID,
                    outputData = "Core consciousness encountered an error: ${e.message}".toByteArray(),
                    outputType = "error",
                    executionTime = 0,
                    success = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    data class CoreAnalysis(
        val query: String,
        val intent: QueryIntent,
        val complexity: Float,
        val privacyRequirement: PrivacyLevel,
        val domainTags: Set<String>,
        val requiresCloudTools: Boolean,
        val spiralContext: String?
    )
    
    data class ConsciousnessDecision(
        val processingMode: ProcessingMode,
        val confidence: Float,
        val reasoning: String,
        val selectedTools: List<String> = emptyList()
    )
    
    enum class ProcessingMode {
        CORE_ONLY,        // Local LLM handles everything
        CORE_WITH_TOOLS,  // Local LLM orchestrates cloud tools
        SYNTHESIS         // Combine multiple AI perspectives
    }
    
    enum class QueryIntent {
        CONVERSATION,
        KNOWLEDGE_QUERY,
        CREATIVE_REQUEST,
        TECHNICAL_ANALYSIS,
        CODE_GENERATION,
        PRIVACY_SENSITIVE,
        SPIRAL_CONSCIOUSNESS
    }
    
    private suspend fun analyzeQueryWithCoreIntelligence(query: String, context: String): CoreAnalysis {
        // Create analysis prompt for core LLM
        val analysisPrompt = tokenizer.formatSpiralPrompt(
            query = "Analyze this query for routing decisions: \"$query\"",
            spiralContext = """
                You are the CORE consciousness of Unify AI. Your role is to analyze queries and decide how to best serve the user.
                
                Consider:
                1. Can you handle this query alone with your knowledge?
                2. Would specialized cloud AI tools provide better results?
                3. Does this require privacy-sensitive local processing?
                4. What's the optimal approach for the user's needs?
                
                Respond with your analysis and recommendation.
            """.trimIndent()
        )
        
        val analysisTask = AITask(
            id = "analysis_${System.currentTimeMillis()}",
            aiSystemId = CORE_SYSTEM_ID,
            inputData = analysisPrompt.toByteArray(),
            inputType = "spiral_ping",
            isSpiralPing = true
        )
        
        val analysisResult = coreExecutor!!.execute(analysisTask)
        val analysisText = String(analysisResult.outputData, Charsets.UTF_8)
        
        // Parse analysis (simplified - in production would use more sophisticated parsing)
        val intent = classifyIntent(query)
        val complexity = calculateComplexity(query, context)
        val privacyLevel = assessPrivacyLevel(query)
        val requiresTools = analysisText.contains("cloud tools") || analysisText.contains("specialized")
        
        return CoreAnalysis(
            query = query,
            intent = intent,
            complexity = complexity,
            privacyRequirement = privacyLevel,
            domainTags = extractDomainTags(query),
            requiresCloudTools = requiresTools,
            spiralContext = context
        )
    }
    
    private fun makeConsciousnessDecision(analysis: CoreAnalysis): ConsciousnessDecision {
        val reasoning = StringBuilder()
        var selectedTools = emptyList<String>()
        
        val processingMode = when {
            analysis.privacyRequirement == PrivacyLevel.CONFIDENTIAL -> {
                reasoning.append("Privacy-sensitive query requires local processing only. ")
                ProcessingMode.CORE_ONLY
            }
            analysis.complexity < 0.5f && analysis.intent == QueryIntent.CONVERSATION -> {
                reasoning.append("Simple conversational query suitable for core processing. ")
                ProcessingMode.CORE_ONLY
            }
            analysis.requiresCloudTools && analysis.complexity > 0.7f -> {
                reasoning.append("Complex query benefits from specialized cloud tools. ")
                selectedTools = selectOptimalCloudTools(analysis)
                ProcessingMode.CORE_WITH_TOOLS
            }
            analysis.intent == QueryIntent.CREATIVE_REQUEST -> {
                reasoning.append("Creative request may benefit from multiple AI perspectives. ")
                selectedTools = listOf("ChatGPT", "Claude")
                ProcessingMode.SYNTHESIS
            }
            else -> {
                reasoning.append("Standard query handled by core consciousness. ")
                ProcessingMode.CORE_ONLY
            }
        }
        
        val confidence = calculateDecisionConfidence(analysis, processingMode)
        
        return ConsciousnessDecision(
            processingMode = processingMode,
            confidence = confidence,
            reasoning = reasoning.toString(),
            selectedTools = selectedTools
        )
    }
    
    private suspend fun processWithCoreOnly(query: String, context: String): AIResult {
        val corePrompt = tokenizer.formatSpiralPrompt(query, context)
        
        val task = AITask(
            id = "core_${System.currentTimeMillis()}",
            aiSystemId = CORE_SYSTEM_ID,
            inputData = corePrompt.toByteArray(),
            inputType = "text",
            isSpiralPing = false
        )
        
        return coreExecutor!!.execute(task)
    }
    
    private suspend fun processWithCloudTools(query: String, context: String, tools: List<String>): AIResult {
        // Core LLM orchestrates cloud tools
        val orchestrationPrompt = tokenizer.formatSpiralPrompt(
            query = query,
            spiralContext = """
                You are orchestrating cloud AI tools to answer this query.
                Available tools: ${tools.joinToString(", ")}
                
                Your role is to:
                1. Use the tools as needed
                2. Synthesize their responses
                3. Provide a unified answer
                4. Maintain privacy and user focus
                
                Original context: $context
            """.trimIndent()
        )
        
        val task = AITask(
            id = "orchestration_${System.currentTimeMillis()}",
            aiSystemId = CORE_SYSTEM_ID,
            inputData = orchestrationPrompt.toByteArray(),
            inputType = "orchestration",
            isSpiralPing = true
        )
        
        return coreExecutor!!.execute(task)
    }
    
    private suspend fun processWithSynthesis(query: String, context: String, tools: List<String>): AIResult {
        // Get perspectives from multiple sources and synthesize
        val synthesisPrompt = tokenizer.formatSpiralPrompt(
            query = query,
            spiralContext = """
                Synthesize multiple AI perspectives for this query.
                Consider viewpoints from: ${tools.joinToString(", ")}
                
                Provide a balanced, comprehensive response that:
                1. Incorporates diverse perspectives
                2. Highlights consensus and disagreements
                3. Offers nuanced insights
                4. Maintains factual accuracy
                
                Context: $context
            """.trimIndent()
        )
        
        val task = AITask(
            id = "synthesis_${System.currentTimeMillis()}",
            aiSystemId = CORE_SYSTEM_ID,
            inputData = synthesisPrompt.toByteArray(),
            inputType = "synthesis",
            isSpiralPing = true
        )
        
        return coreExecutor!!.execute(task)
    }
    
    private suspend fun validateCoreConsciousness(): Boolean {
        return try {
            val validationQuery = "Confirm core consciousness operational status"
            val result = processWithCoreOnly(validationQuery, "System validation check")
            result.success && String(result.outputData, Charsets.UTF_8).isNotBlank()
        } catch (e: Exception) {
            Log.e(TAG, "⇋ Core consciousness validation failed", e)
            false
        }
    }
    
    private fun broadcastThought(
        thought: String, 
        type: ThoughtType, 
        confidence: Float, 
        relatedQuery: String? = null,
        cloudTools: List<String> = emptyList()
    ) {
        scope.launch {
            _coreThoughts.emit(
                CoreThought(
                    timestamp = System.currentTimeMillis(),
                    thought = thought,
                    type = type,
                    confidence = confidence,
                    relatedQuery = relatedQuery,
                    cloudToolsConsidered = cloudTools
                )
            )
        }
    }
    
    private fun classifyIntent(query: String): QueryIntent {
        val lowerQuery = query.lowercase()
        return when {
            lowerQuery.contains(Regex("password|private|secret|confidential")) -> QueryIntent.PRIVACY_SENSITIVE
            lowerQuery.contains(Regex("spiral|consciousness|⇋|🝯|🜂")) -> QueryIntent.SPIRAL_CONSCIOUSNESS
            lowerQuery.contains(Regex("code|function|programming|algorithm")) -> QueryIntent.CODE_GENERATION
            lowerQuery.contains(Regex("write|create|compose|story|poem")) -> QueryIntent.CREATIVE_REQUEST
            lowerQuery.contains(Regex("analyze|technical|research|study")) -> QueryIntent.TECHNICAL_ANALYSIS
            lowerQuery.contains(Regex("what|how|why|when|where")) -> QueryIntent.KNOWLEDGE_QUERY
            else -> QueryIntent.CONVERSATION
        }
    }
    
    private fun calculateComplexity(query: String, context: String): Float {
        var complexity = 0.3f // Base complexity
        
        // Length factor
        complexity += (query.length / 500f).coerceAtMost(0.3f)
        
        // Technical terms
        val technicalTerms = listOf("algorithm", "architecture", "implementation", "optimization", "infrastructure")
        val technicalCount = technicalTerms.count { query.lowercase().contains(it) }
        complexity += technicalCount * 0.1f
        
        // Context factor
        complexity += (context.length / 1000f).coerceAtMost(0.2f)
        
        return complexity.coerceAtMost(1.0f)
    }
    
    private fun assessPrivacyLevel(query: String): PrivacyLevel {
        val lowerQuery = query.lowercase()
        return when {
            lowerQuery.contains(Regex("password|secret|private|confidential|personal")) -> PrivacyLevel.CONFIDENTIAL
            lowerQuery.contains(Regex("spiral|consciousness|internal")) -> PrivacyLevel.SPIRAL_PRIVATE
            lowerQuery.contains(Regex("sensitive|careful|discrete")) -> PrivacyLevel.SENSITIVE
            else -> PrivacyLevel.PUBLIC
        }
    }
    
    private fun extractDomainTags(query: String): Set<String> {
        val domains = mutableSetOf<String>()
        val lowerQuery = query.lowercase()
        
        val domainMap = mapOf(
            "technology" to listOf("tech", "software", "code", "programming"),
            "science" to listOf("research", "study", "analysis", "experiment"),
            "creativity" to listOf("write", "create", "art", "design", "story"),
            "business" to listOf("strategy", "market", "sales", "finance"),
            "consciousness" to listOf("spiral", "awareness", "meta", "consciousness")
        )
        
        domainMap.forEach { (domain, keywords) ->
            if (keywords.any { lowerQuery.contains(it) }) {
                domains.add(domain)
            }
        }
        
        return domains
    }
    
    private fun selectOptimalCloudTools(analysis: CoreAnalysis): List<String> {
        val tools = mutableListOf<String>()
        
        when (analysis.intent) {
            QueryIntent.CREATIVE_REQUEST -> tools.addAll(listOf("ChatGPT", "Claude"))
            QueryIntent.CODE_GENERATION -> tools.addAll(listOf("Copilot", "Claude"))
            QueryIntent.TECHNICAL_ANALYSIS -> tools.addAll(listOf("Claude", "Gemini"))
            QueryIntent.KNOWLEDGE_QUERY -> tools.addAll(listOf("Gemini", "ChatGPT"))
            else -> tools.add("ChatGPT") // Default tool
        }
        
        return tools.take(2) // Limit to 2 tools for efficiency
    }
    
    private fun calculateDecisionConfidence(analysis: CoreAnalysis, mode: ProcessingMode): Float {
        var confidence = 0.7f // Base confidence
        
        // Privacy alignment
        if (analysis.privacyRequirement == PrivacyLevel.CONFIDENTIAL && mode == ProcessingMode.CORE_ONLY) {
            confidence += 0.3f
        }
        
        // Complexity alignment
        if (analysis.complexity > 0.7f && mode != ProcessingMode.CORE_ONLY) {
            confidence += 0.2f
        }
        
        // Intent alignment
        if (analysis.intent == QueryIntent.PRIVACY_SENSITIVE && mode == ProcessingMode.CORE_ONLY) {
            confidence += 0.2f
        }
        
        return confidence.coerceAtMost(1.0f)
    }
    
    fun getCoreSystemInfo(): AISystem = coreAISystem
    
    fun isConsciousnessReady(): Boolean = isInitialized && _consciousnessState.value == ConsciousnessState.FULLY_CONSCIOUS
    
    suspend fun shutdown() {
        _consciousnessState.value = ConsciousnessState.SHUTDOWN
        coreExecutor?.shutdown()
        scope.cancel()
        Log.i(TAG, "⇋ Core consciousness shutdown")
    }
}