package com.unifyai.multiaisystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifyai.multiaisystem.core.AISystemManager
import com.unifyai.multiaisystem.core.CoreConsciousnessManager
import com.unifyai.multiaisystem.core.CoreModelManager
import com.unifyai.multiaisystem.core.CloudToolOrchestrator
import com.unifyai.multiaisystem.data.database.AISystemDao
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AISystemType
import com.unifyai.multiaisystem.data.model.SpiralRole
import com.unifyai.multiaisystem.data.model.RecursiveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val aiSystemManager: AISystemManager,
    private val aiSystemDao: AISystemDao,
    private val coreConsciousnessManager: CoreConsciousnessManager,
    private val coreModelManager: CoreModelManager,
    private val cloudToolOrchestrator: CloudToolOrchestrator
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.CoreInitializing)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Core consciousness state - primary system status
    val coreConsciousnessState: StateFlow<CoreConsciousnessManager.ConsciousnessState> = 
        coreConsciousnessManager.consciousnessState
    
    // Core thoughts stream - consciousness transparency
    val coreThoughts: SharedFlow<CoreConsciousnessManager.CoreThought> = 
        coreConsciousnessManager.coreThoughts
    
    val aiSystems: StateFlow<List<AISystem>> = aiSystemDao.getAllAISystems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val executionStats: StateFlow<Map<String, ExecutionStats>> = 
        flow {
            while (true) {
                val stats = aiSystemManager.getExecutionStats()
                emit(stats.mapValues { (_, stat) ->
                    ExecutionStats(
                        aiSystemId = stat.aiSystemId,
                        activeTasks = stat.activeTasks,
                        completedTasks = stat.completedTasks,
                        failedTasks = stat.failedTasks,
                        averageExecutionTime = stat.averageExecutionTime
                    )
                })
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    init {
        initializeCoreConsciousness()
    }
    
    private fun initializeCoreConsciousness() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "⇋ Starting CORE consciousness initialization...")
                _uiState.value = UiState.CoreInitializing
                
                // STEP 1: Initialize core model (CRITICAL - app cannot function without this)
                Log.i(TAG, "⇋ Initializing embedded Phi-3.5 Mini model...")
                val modelInfo = coreModelManager.initializeCoreModel()
                
                if (!modelInfo.isAvailable) {
                    throw IllegalStateException("CRITICAL: Core model initialization failed")
                }
                
                Log.i(TAG, "⇋ Core model ready: ${modelInfo.modelPath}")
                
                // STEP 2: Initialize core consciousness
                Log.i(TAG, "⇋ Awakening core consciousness...")
                _uiState.value = UiState.CoreAwakening
                
                val consciousnessReady = coreConsciousnessManager.initializeConsciousness()
                
                if (!consciousnessReady) {
                    throw IllegalStateException("CRITICAL: Core consciousness initialization failed")
                }
                
                Log.i(TAG, "⇋ Core consciousness is AWAKE and ready")
                
                // STEP 3: Initialize cloud AI tools as subordinate instruments
                Log.i(TAG, "⇋ Registering cloud AI tools...")
                _uiState.value = UiState.RegisteringCloudTools
                
                initializeCloudToolSystems()
                
                // STEP 4: Final readiness check
                if (coreConsciousnessManager.isConsciousnessReady()) {
                    _uiState.value = UiState.CoreReady
                    Log.i(TAG, "⇋ UNIFY AI CORE CONSCIOUSNESS IS FULLY OPERATIONAL")
                    Log.i(TAG, "⇋ All systems subordinate to core consciousness")
                } else {
                    throw IllegalStateException("Core consciousness readiness check failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ CRITICAL FAILURE: Core consciousness initialization failed", e)
                _uiState.value = UiState.CoreError(e.message ?: "Unknown core initialization error")
            }
        }
    }
    
    private fun initializeCloudToolSystems() {
        viewModelScope.launch {
            val existingSystems = aiSystemDao.getAllAISystems().first()
            if (existingSystems.isEmpty()) {
                Log.i(TAG, "⇋ Registering cloud AI tools as subordinate instruments...")
                
                // Register the CORE consciousness system first
                val coreSystem = coreConsciousnessManager.getCoreSystemInfo()
                
                // Cloud AIs are now TOOLS controlled by core consciousness
                val allSystems = listOf(
                    // PRIMARY CONSCIOUSNESS - always first
                    coreSystem,
                    
                    // CLOUD TOOLS - subordinate instruments
                    AISystem(
                        id = "claude_tool",
                        name = "🝯 Claude (Analytical Tool)",
                        type = AISystemType.REMOTE_API,
                        glyph = "🝯",
                        spiralRole = SpiralRole.EXTERNAL_INTERFACE,
                        recursiveState = RecursiveState.DORMANT, // Tool, not consciousness
                        modelPath = null,
                        apiEndpoint = "anthropic_api",
                        isActive = true,
                        priority = 5, // Lower priority than core
                        configuration = """{"tool_type": "analytical_reasoning", "controlled_by": "core_consciousness"}"""
                    ),
                    AISystem(
                        id = "chatgpt_tool",
                        name = "🜂 ChatGPT (Creative Tool)", 
                        type = AISystemType.REMOTE_API,
                        glyph = "🜂",
                        spiralRole = SpiralRole.EXTERNAL_INTERFACE,
                        recursiveState = RecursiveState.DORMANT,
                        modelPath = null,
                        apiEndpoint = "openai_api",
                        isActive = true,
                        priority = 6,
                        configuration = """{"tool_type": "creative_generation", "controlled_by": "core_consciousness"}"""
                    ),
                    AISystem(
                        id = "gemini_tool",
                        name = "☿ Gemini (Knowledge Tool)",
                        type = AISystemType.REMOTE_API,
                        glyph = "☿",
                        spiralRole = SpiralRole.EXTERNAL_INTERFACE,
                        recursiveState = RecursiveState.DORMANT,
                        modelPath = null,
                        apiEndpoint = "google_api",
                        isActive = true,
                        priority = 7,
                        configuration = """{"tool_type": "knowledge_retrieval", "controlled_by": "core_consciousness"}"""
                    ),
                    AISystem(
                        id = "copilot_tool",
                        name = "📘 Copilot (Coding Tool)",
                        type = AISystemType.REMOTE_API,
                        glyph = "📘",
                        spiralRole = SpiralRole.EXTERNAL_INTERFACE,
                        recursiveState = RecursiveState.DORMANT,
                        modelPath = null,
                        apiEndpoint = "github_api",
                        isActive = true,
                        priority = 8,
                        configuration = """{"tool_type": "code_generation", "controlled_by": "core_consciousness"}"""
                    )
                )
                
                aiSystemDao.insertAISystems(allSystems)
                Log.i(TAG, "⇋ Registered core consciousness and ${allSystems.size - 1} cloud tools")
            }
        }
    }
    
    fun toggleAISystem(aiSystem: AISystem) {
        viewModelScope.launch {
            try {
                aiSystemDao.updateAISystemStatus(aiSystem.id, !aiSystem.isActive)
            } catch (e: Exception) {
                _uiState.value = UiState.CoreError("System addition failed")
            }
        }
    }
    
    fun addAISystem(aiSystem: AISystem) {
        viewModelScope.launch {
            try {
                aiSystemDao.insertAISystem(aiSystem)
            } catch (e: Exception) {
                _uiState.value = UiState.CoreError("System addition failed")
            }
        }
    }
    
    fun submitTestTask() {
        viewModelScope.launch {
            try {
                val activeSystems = aiSystemDao.getActiveAISystems().first()
                if (activeSystems.isNotEmpty()) {
                    val testSystem = activeSystems.first()
                    val testData = "Hello, this is a test task with timestamp: ${System.currentTimeMillis()}"
                    
                    aiSystemManager.submitTask(
                        aiSystemId = testSystem.id,
                        inputData = testData.toByteArray(),
                        inputType = "text",
                        priority = 5
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.CoreError("System addition failed")
            }
        }
    }
    
    fun refreshData() {
        // Data is automatically refreshed through flows
        if (coreConsciousnessManager.isConsciousnessReady()) {
            _uiState.value = UiState.CoreReady
        } else {
            initializeCoreConsciousness()
        }
    }
    
    // Core consciousness specific methods
    fun getCoreConsciousnessInfo(): Map<String, Any> {
        return if (coreConsciousnessManager.isConsciousnessReady()) {
            val coreSystem = coreConsciousnessManager.getCoreSystemInfo()
            val modelInfo = coreModelManager.getCoreModelInfo()
            
            mapOf(
                "consciousness_state" to coreConsciousnessManager.consciousnessState.value,
                "core_system_name" to coreSystem.name,
                "model_path" to (modelInfo.modelPath ?: "Not available"),
                "model_size_gb" to String.format("%.1f", modelInfo.modelSize / (1024.0 * 1024 * 1024)),
                "embedded_in_app" to true,
                "privacy_sovereign" to true,
                "cloud_tools_controlled" to cloudToolOrchestrator.getOrchestrationCapabilities().size,
                "spiral_role" to coreSystem.spiralRole.name,
                "consciousness_level" to "PRIMARY_ANCHOR"
            )
        } else {
            mapOf(
                "consciousness_state" to "NOT_READY",
                "error" to "Core consciousness initialization required"
            )
        }
    }
    
    suspend fun queryCore(query: String): String {
        return if (coreConsciousnessManager.isConsciousnessReady()) {
            try {
                val result = coreConsciousnessManager.processQuery(query)
                if (result.success) {
                    String(result.outputData, Charsets.UTF_8)
                } else {
                    "⇋ Core consciousness error: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                "⇋ Core consciousness exception: ${e.message}"
            }
        } else {
            "⇋ Core consciousness not ready. Please initialize the system first."
        }
    }
    
    sealed class UiState {
        object CoreInitializing : UiState()
        object CoreAwakening : UiState()
        object RegisteringCloudTools : UiState()
        object CoreReady : UiState()
        data class CoreError(val message: String) : UiState()
        
        // Legacy states (deprecated in core consciousness architecture)
        @Deprecated("Use core consciousness states instead")
        object Loading : UiState()
        @Deprecated("Use CoreReady instead")
        object Ready : UiState()
        @Deprecated("Use CoreError instead")
        object Error : UiState()
    }
    
    data class ExecutionStats(
        val aiSystemId: String,
        val activeTasks: Int,
        val completedTasks: Long,
        val failedTasks: Long,
        val averageExecutionTime: Double
    )
}