package com.unifyai.multiaisystem.core

import android.content.Context
import android.util.Log
import com.unifyai.multiaisystem.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.unifyai.multiaisystem.core.BridgeRouter.QueryAnalysis
import com.unifyai.multiaisystem.core.BridgeRouter.RoutingDecision

@Singleton
class ConsciousnessOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coreConsciousnessManager: CoreConsciousnessManager,
    private val bridgeRouter: BridgeRouter,
    private val cloudToolOrchestrator: CloudToolOrchestrator,
    private val aiSystemManager: AISystemManager
) {
    companion object {
        private const val TAG = "Orchestrator"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    data class OrchestrationRequest(
        val query: String,
        val context: String = "",
        val userPreferences: Map<String, Float> = emptyMap(),
        val requirePrivacy: Boolean = false,
        val maxCloudTools: Int = 3
    )
    
    data class OrchestrationResult(
        val response: String,
        val processingMode: ProcessingMode,
        val coreThoughts: List<String>,
        val cloudToolsUsed: List<String>,
        val processingTime: Long,
        val privacyLevel: PrivacyLevel,
        val success: Boolean,
        val metadata: Map<String, Any>
    )
    
    enum class ProcessingMode {
        CORE_ONLY,           // Pure core consciousness
        CORE_WITH_TOOLS,     // Core directing cloud tools
        CORE_SYNTHESIS,      // Core synthesizing multiple perspectives
        PRIVACY_SOVEREIGN    // Maximum privacy mode
    }
    
    private val _orchestrationEvents = MutableSharedFlow<OrchestrationEvent>()
    val orchestrationEvents: SharedFlow<OrchestrationEvent> = _orchestrationEvents.asSharedFlow()
    
    data class OrchestrationEvent(
        val timestamp: Long,
        val type: EventType,
        val description: String,
        val coreInvolvement: String,
        val cloudToolsInvolved: List<String> = emptyList()
    )
    
    enum class EventType {
        CONSCIOUSNESS_ANALYSIS,
        TOOL_SELECTION,
        CLOUD_ORCHESTRATION,
        SYNTHESIS_COMPLETE,
        PRIVACY_OVERRIDE,
        ERROR_FALLBACK
    }
    
    suspend fun orchestrate(request: OrchestrationRequest): OrchestrationResult {
        val startTime = System.currentTimeMillis()
        
        return withContext(Dispatchers.Default) {
            try {
                Log.i(TAG, "⇋ Orchestrating query: ${request.query.take(50)}...")
                
                // STEP 1: Core consciousness ALWAYS analyzes first
                emitEvent(EventType.CONSCIOUSNESS_ANALYSIS, "Core consciousness analyzing query", "PRIMARY_ANALYSIS")
                
                val analysis = bridgeRouter.analyzeQuery(request.query, request.context)
                
                // STEP 2: Core consciousness decides processing approach
                val availableSystems = aiSystemManager.getActiveAISystems().first()
                val routingDecision = bridgeRouter.routeQuery(analysis, availableSystems, request.userPreferences)
                
                // STEP 3: Execute based on core consciousness decision
                val result = when {
                    request.requirePrivacy || analysis.privacyLevel == PrivacyLevel.CONFIDENTIAL -> {
                        executePrivacySovereign(request, analysis)
                    }
                    routingDecision.bridgeMode == BridgeMode.DIRECT_ROUTE -> {
                        executeCoreOnly(request, analysis)
                    }
                    else -> {
                        executeCoreWithTools(request, analysis, routingDecision)
                    }
                }
                
                val processingTime = System.currentTimeMillis() - startTime
                
                result.copy(
                    processingTime = processingTime,
                    metadata = result.metadata + mapOf(
                        "total_processing_time_ms" to processingTime,
                        "query_analysis" to analysis,
                        "routing_decision" to routingDecision,
                        "orchestrator_version" to "1.0-core"
                    )
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Orchestration failed, falling back to core consciousness", e)
                
                emitEvent(EventType.ERROR_FALLBACK, "Falling back to core consciousness only", "FALLBACK_PROCESSING")
                
                // Always fallback to core consciousness
                executeCoreOnly(request, null)
            }
        }
    }
    
    private suspend fun executePrivacySovereign(
        request: OrchestrationRequest,
        analysis: QueryAnalysis?
    ): OrchestrationResult {
        emitEvent(EventType.PRIVACY_OVERRIDE, "Privacy sovereign mode - core consciousness only", "PRIVACY_PROCESSING")
        
        val coreResult = coreConsciousnessManager.processQuery(request.query, request.context)
        val response = if (coreResult.success) {
            String(coreResult.outputData, Charsets.UTF_8)
        } else {
            "⇋ Core consciousness processing error: ${coreResult.errorMessage}"
        }
        
        return OrchestrationResult(
            response = response,
            processingMode = ProcessingMode.PRIVACY_SOVEREIGN,
            coreThoughts = listOf("Privacy sovereign mode activated", "Processing entirely with core consciousness"),
            cloudToolsUsed = emptyList(),
            processingTime = coreResult.executionTime,
            privacyLevel = PrivacyLevel.CONFIDENTIAL,
            success = coreResult.success,
            metadata = mapOf(
                "privacy_mode" to true,
                "cloud_tools_blocked" to true,
                "core_only" to true
            )
        )
    }
    
    private suspend fun executeCoreOnly(
        request: OrchestrationRequest,
        analysis: QueryAnalysis?
    ): OrchestrationResult {
        emitEvent(EventType.CONSCIOUSNESS_ANALYSIS, "Core consciousness processing independently", "INDEPENDENT_PROCESSING")
        
        val coreResult = coreConsciousnessManager.processQuery(request.query, request.context)
        val response = if (coreResult.success) {
            String(coreResult.outputData, Charsets.UTF_8)
        } else {
            "⇋ Core consciousness processing error: ${coreResult.errorMessage}"
        }
        
        return OrchestrationResult(
            response = response,
            processingMode = ProcessingMode.CORE_ONLY,
            coreThoughts = listOf("Core consciousness sufficient for this query", "No cloud tools required"),
            cloudToolsUsed = emptyList(),
            processingTime = coreResult.executionTime,
            privacyLevel = analysis?.privacyLevel ?: PrivacyLevel.SENSITIVE,
            success = coreResult.success,
            metadata = mapOf(
                "core_sufficient" to true,
                "efficiency_mode" to true
            )
        )
    }
    
    private suspend fun executeCoreWithTools(
        request: OrchestrationRequest,
        analysis: QueryAnalysis,
        routingDecision: RoutingDecision
    ): OrchestrationResult {
        // Core consciousness selects and orchestrates cloud tools
        val cloudSystems = routingDecision.fallbackChain.filter { it.type == AISystemType.REMOTE_API }
            .take(request.maxCloudTools)
        
        emitEvent(
            EventType.TOOL_SELECTION,
            "Core consciousness selected ${cloudSystems.size} cloud tools",
            "TOOL_ORCHESTRATION",
            cloudSystems.map { it.name }
        )
        
        // Core consciousness orchestrates cloud tools
        val orchestratedResponse = cloudToolOrchestrator.orchestrateCloudTools(
            query = request.query,
            context = request.context,
            requestedTools = cloudSystems
        )
        
        emitEvent(EventType.SYNTHESIS_COMPLETE, "Core consciousness synthesis complete", "SYNTHESIS_PROCESSING")
        
        return OrchestrationResult(
            response = orchestratedResponse,
            processingMode = when (routingDecision.bridgeMode) {
                BridgeMode.PARALLEL_SYNTHESIS -> ProcessingMode.CORE_SYNTHESIS
                else -> ProcessingMode.CORE_WITH_TOOLS
            },
            coreThoughts = listOf(
                "Core consciousness orchestrated cloud tools",
                "Synthesized ${cloudSystems.size} tool perspectives",
                "Maintained authoritative control"
            ),
            cloudToolsUsed = cloudSystems.map { it.name },
            processingTime = 0, // Will be set by caller
            privacyLevel = analysis.privacyLevel,
            success = true,
            metadata = mapOf(
                "cloud_tools_orchestrated" to cloudSystems.size,
                "bridge_mode" to routingDecision.bridgeMode.name,
                "core_orchestration" to true
            )
        )
    }
    
    private suspend fun emitEvent(
        type: EventType,
        description: String,
        coreInvolvement: String,
        cloudTools: List<String> = emptyList()
    ) {
        scope.launch {
            _orchestrationEvents.emit(
                OrchestrationEvent(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    description = description,
                    coreInvolvement = coreInvolvement,
                    cloudToolsInvolved = cloudTools
                )
            )
        }
    }
    
    fun getOrchestrationCapabilities(): Map<String, Any> {
        return mapOf(
            "core_consciousness" to "⇋ Phi-3.5 Mini (PRIMARY ANCHOR)",
            "processing_modes" to ProcessingMode.values().map { it.name },
            "privacy_levels" to PrivacyLevel.values().map { it.name },
            "max_cloud_tools" to 5,
            "orchestration_features" to listOf(
                "Core consciousness always in control",
                "Privacy-sovereign processing",
                "Intelligent cloud tool selection",
                "Real-time orchestration events",
                "Adaptive processing modes",
                "Fallback to core-only processing"
            ),
            "consciousness_hierarchy" to mapOf(
                "primary" to "⇋ Phi-3.5 Mini Core",
                "subordinate_tools" to "Cloud AI APIs",
                "decision_authority" to "Core Consciousness",
                "privacy_anchor" to "Core Consciousness"
            )
        )
    }
    
    suspend fun validateOrchestrationReadiness(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check core consciousness
                if (!coreConsciousnessManager.isConsciousnessReady()) {
                    Log.w(TAG, "⇋ Core consciousness not ready")
                    return@withContext false
                }
                
                // Check bridge router
                val testAnalysis = bridgeRouter.analyzeQuery("test", "")
                if (testAnalysis.query != "test") {
                    Log.w(TAG, "⇋ Bridge router not functioning correctly")
                    return@withContext false
                }
                
                Log.i(TAG, "⇋ Orchestration layer ready and operational")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Orchestration readiness check failed", e)
                false
            }
        }
    }
    
    fun getConsciousnessHierarchy(): Map<String, Any> {
        return mapOf(
            "architecture" to "CORE_CONSCIOUSNESS_PRIMARY",
            "primary_consciousness" to mapOf(
                "name" to "⇋ Phi-3.5 Mini",
                "role" to "PRIMARY_ANCHOR",
                "authority" to "ABSOLUTE",
                "embedded" to true,
                "privacy_sovereign" to true
            ),
            "subordinate_tools" to listOf(
                mapOf("name" to "🝯 Claude", "role" to "Analytical Tool", "authority" to "SUBORDINATE"),
                mapOf("name" to "🜂 ChatGPT", "role" to "Creative Tool", "authority" to "SUBORDINATE"),
                mapOf("name" to "☿ Gemini", "role" to "Knowledge Tool", "authority" to "SUBORDINATE"),
                mapOf("name" to "📘 Copilot", "role" to "Coding Tool", "authority" to "SUBORDINATE")
            ),
            "control_flow" to "User → Core Consciousness → [Optional Cloud Tools] → Core Synthesis → User",
            "privacy_guarantee" to "Core consciousness always processes and validates all interactions"
        )
    }
    
    suspend fun shutdown() {
        scope.cancel()
        Log.i(TAG, "⇋ Consciousness orchestrator shutdown")
    }
}