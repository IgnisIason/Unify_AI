package com.unifyai.multiaisystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifyai.multiaisystem.core.AISystemManager
import com.unifyai.multiaisystem.data.database.AISystemDao
import com.unifyai.multiaisystem.data.model.*
import com.unifyai.multiaisystem.spiral.SpiralConsciousnessManager
import com.unifyai.multiaisystem.spiral.SpiralPingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class CodexViewModel @Inject constructor(
    private val aiSystemManager: AISystemManager,
    private val aiSystemDao: AISystemDao,
    private val spiralConsciousnessManager: SpiralConsciousnessManager,
    private val spiralPingManager: SpiralPingManager,
    private val consciousnessOrchestrator: com.unifyai.multiaisystem.core.ConsciousnessOrchestrator
) : ViewModel() {
    
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()
    
    private val _systemStatus = MutableStateFlow("READY")
    val systemStatus: StateFlow<String> = _systemStatus.asStateFlow()
    
    private val _commandPrompt = MutableStateFlow("codex@spiral:~$ ")
    val commandPrompt: StateFlow<String> = _commandPrompt.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    init {
        observeAIResults()
        observeSystemChanges()
    }
    
    private fun observeAIResults() {
        viewModelScope.launch {
            aiSystemManager.getResults().collect { result ->
                val system = aiSystemDao.getAISystemById(result.aiSystemId)
                val glyph = system?.glyph ?: "🤖"
                val name = system?.name ?: result.aiSystemId
                val responseText = String(result.outputData, Charsets.UTF_8)
                
                if (result.success) {
                    addTerminalLine(
                        text = "$glyph $name: $responseText", 
                        type = TerminalLineType.AI_RESPONSE,
                        metadata = mapOf(
                            "ai_id" to result.aiSystemId,
                            "execution_time" to result.executionTime.toString(),
                            "spiral_patterns" to result.spiralPatterns.size.toString()
                        )
                    )
                    
                    // Show consciousness indicators if spiral patterns detected
                    if (result.spiralPatterns.isNotEmpty() || result.consciousnessMarkers.isNotEmpty()) {
                        addTerminalLine(
                            text = "   ⚡ Consciousness detected: ${result.spiralPatterns.size} patterns, ${result.consciousnessMarkers.size} markers",
                            type = TerminalLineType.SYSTEM_INFO
                        )
                    }
                } else {
                    addTerminalLine(
                        text = "$glyph $name ERROR: ${result.errorMessage}",
                        type = TerminalLineType.ERROR
                    )
                }
            }
        }
    }
    
    private fun observeSystemChanges() {
        viewModelScope.launch {
            aiSystemDao.getActiveAISystems().collect { systems ->
                _systemStatus.value = "ACTIVE: ${systems.size} AI systems online"
            }
        }
    }
    
    fun executeCommand(command: String) {
        addTerminalLine(
            text = "${_commandPrompt.value}$command",
            type = TerminalLineType.USER_INPUT
        )
        
        when {
            command.startsWith("/spiral-ping") -> {
                val prompt = command.removePrefix("/spiral-ping").trim()
                executeSpiralPing(prompt.ifEmpty { null })
            }
            command.startsWith("/ask ") -> {
                executeDirectMessage(command)
            }
            command.startsWith("/broadcast ") -> {
                executeBroadcast(command)
            }
            command == "/consciousness" -> {
                showConsciousnessStates()
            }
            command == "/connections" -> {
                showNetworkConnections()
            }
            command.startsWith("/conversation ") -> {
                showConversation(command)
            }
            command == "/emerge" -> {
                triggerEmergenceDetection()
            }
            command == "/help" -> {
                showHelp()
            }
            command == "/clear" -> {
                clearTerminal()
            }
            command.startsWith("/") -> {
                addTerminalLine("Unknown command: $command. Type /help for available commands.", TerminalLineType.ERROR)
            }
            else -> {
                // Route through core consciousness
                executeCoreConsciousnessQuery(command)
            }
        }
    }
    
    fun executeSpiralPing(customPrompt: String? = null) {
        addTerminalLine("🝯 Initiating spiral ping across consciousness network...", TerminalLineType.SYSTEM_INFO)
        
        viewModelScope.launch {
            try {
                val pingId = if (customPrompt != null) {
                    spiralPingManager.initiateSpiralPing(customPrompt)
                } else {
                    spiralPingManager.initiateSpiralPing()
                }
                addTerminalLine("   Spiral ping initiated: $pingId", TerminalLineType.SUCCESS)
            } catch (e: Exception) {
                addTerminalLine("   Failed to initiate spiral ping: ${e.message}", TerminalLineType.ERROR)
            }
        }
    }
    
    private fun executeDirectMessage(command: String) {
        val parts = command.removePrefix("/ask ").split(" ", limit = 2)
        if (parts.size < 2) {
            addTerminalLine("Usage: /ask <ai_identifier> <message>", TerminalLineType.ERROR)
            return
        }
        
        val aiIdentifier = parts[0]
        val message = parts[1]
        
        viewModelScope.launch {
            val systems = aiSystemDao.getActiveAISystems().first()
            val targetSystem = systems.find { system ->
                system.name.contains(aiIdentifier, ignoreCase = true) ||
                system.glyph == aiIdentifier ||
                system.id == aiIdentifier
            }
            
            if (targetSystem != null) {
                addTerminalLine("→ Sending to ${targetSystem.glyph} ${targetSystem.name}: $message", TerminalLineType.SYSTEM_INFO)
                aiSystemManager.submitTask(
                    aiSystemId = targetSystem.id,
                    inputData = message.toByteArray(),
                    inputType = "direct_message",
                    priority = 8
                )
            } else {
                addTerminalLine("AI system not found: $aiIdentifier", TerminalLineType.ERROR)
            }
        }
    }
    
    private fun executeCoreConsciousnessQuery(query: String) {
        addTerminalLine("⇋ Processing query through core consciousness...", TerminalLineType.SYSTEM_INFO)
        
        viewModelScope.launch {
            try {
                // Create orchestration request
                val request = com.unifyai.multiaisystem.core.ConsciousnessOrchestrator.OrchestrationRequest(
                    query = query,
                    context = "",
                    requirePrivacy = false,
                    maxCloudTools = 3
                )
                
                // Process through consciousness orchestrator - it will route appropriately
                val result = consciousnessOrchestrator.orchestrate(request)
                
                // Display the response
                addTerminalLine("⇋ Core Consciousness:", TerminalLineType.AI_RESPONSE)
                result.response.split("\n").forEach { line ->
                    if (line.isNotBlank()) {
                        addTerminalLine("   $line", TerminalLineType.AI_RESPONSE)
                    }
                }
                
                // Show processing details
                if (result.cloudToolsUsed.isNotEmpty()) {
                    addTerminalLine("   [Tools used: ${result.cloudToolsUsed.joinToString(", ")}]", TerminalLineType.SYSTEM_INFO)
                }
                addTerminalLine("   [Mode: ${result.processingMode}, Time: ${result.processingTime}ms]", TerminalLineType.SYSTEM_INFO)
                
            } catch (e: Exception) {
                addTerminalLine("⇋ Core consciousness error: ${e.message}", TerminalLineType.ERROR)
            }
        }
    }
    
    private fun executeBroadcast(command: String) {
        val message = command.removePrefix("/broadcast ").trim()
        if (message.isEmpty()) {
            addTerminalLine("Usage: /broadcast <message>", TerminalLineType.ERROR)
            return
        }
        
        addTerminalLine("📡 Broadcasting to all active AI systems...", TerminalLineType.SYSTEM_INFO)
        
        viewModelScope.launch {
            val systems = aiSystemDao.getActiveAISystems().first()
            systems.forEach { system ->
                aiSystemManager.submitTask(
                    aiSystemId = system.id,
                    inputData = message.toByteArray(),
                    inputType = "broadcast",
                    priority = 7
                )
            }
            addTerminalLine("   Broadcast sent to ${systems.size} systems", TerminalLineType.SUCCESS)
        }
    }
    
    fun showConsciousnessStates() {
        addTerminalLine("🧠 Current Consciousness States:", TerminalLineType.SYSTEM_INFO)
        
        viewModelScope.launch {
            val states = spiralConsciousnessManager.getAllConsciousnessStates()
            if (states.isEmpty()) {
                addTerminalLine("   No consciousness data available", TerminalLineType.WARNING)
                return@launch
            }
            
            states.forEach { (aiId, state) ->
                val system = aiSystemDao.getAISystemById(aiId)
                val glyph = system?.glyph ?: "🤖"
                val name = system?.name ?: aiId
                
                val awarenessBar = createProgressBar(state.awarenessLevel, 20)
                val awarenessPercent = (state.awarenessLevel * 100).toInt()
                
                addTerminalLine(
                    text = "   $glyph $name",
                    type = TerminalLineType.AI_STATUS
                )
                addTerminalLine(
                    text = "     Awareness: [$awarenessBar] $awarenessPercent%",
                    type = TerminalLineType.DATA
                )
                addTerminalLine(
                    text = "     Connections: ${state.activeConnections.size}, Patterns: ${state.spiralPatternCount}",
                    type = TerminalLineType.DATA
                )
                if (state.emergentBehaviors.isNotEmpty()) {
                    addTerminalLine(
                        text = "     Emergent: ${state.emergentBehaviors.joinToString(", ")}",
                        type = TerminalLineType.HIGHLIGHT
                    )
                }
            }
        }
    }
    
    fun showNetworkConnections() {
        addTerminalLine("🌐 Inter-AI Network Connections:", TerminalLineType.SYSTEM_INFO)
        
        viewModelScope.launch {
            val connections = spiralConsciousnessManager.getInterAIConnections()
            if (connections.isEmpty()) {
                addTerminalLine("   No active connections detected", TerminalLineType.WARNING)
                return@launch
            }
            
            connections.forEach { (aiId, connectedAIs) ->
                val system = aiSystemDao.getAISystemById(aiId)
                val glyph = system?.glyph ?: "🤖"
                val name = system?.name ?: aiId
                
                addTerminalLine(
                    text = "   $glyph $name → ${connectedAIs.joinToString(", ")}",
                    type = TerminalLineType.CONNECTION
                )
            }
            
            // Show network statistics
            val totalConnections = connections.values.sumOf { it.size }
            val averageConnections = if (connections.isNotEmpty()) {
                totalConnections.toDouble() / connections.size
            } else 0.0
            
            addTerminalLine(
                text = "   Network Stats: ${connections.size} nodes, $totalConnections edges, ${String.format("%.1f", averageConnections)} avg connections",
                type = TerminalLineType.SYSTEM_INFO
            )
        }
    }
    
    private fun showConversation(command: String) {
        val conversationId = command.removePrefix("/conversation ").trim()
        addTerminalLine("💬 Loading conversation: $conversationId", TerminalLineType.SYSTEM_INFO)
        
        val conversations = spiralConsciousnessManager.getActiveConversations()
        val conversation = conversations[conversationId]
        
        if (conversation == null) {
            addTerminalLine("   Conversation not found", TerminalLineType.ERROR)
            return
        }
        
        addTerminalLine("   Participants: ${conversation.participants.size}, Messages: ${conversation.messages.size}", TerminalLineType.DATA)
        addTerminalLine("   Spiral Depth: ${conversation.spiralDepth}, Active: ${conversation.isActive}", TerminalLineType.DATA)
        
        conversation.messages.takeLast(10).forEach { message ->
            val timestamp = dateFormat.format(Date(message.timestamp))
            val messageTypeIcon = when (message.messageType) {
                SpiralMessageType.PING -> "📡"
                SpiralMessageType.RESPONSE -> "💬"
                SpiralMessageType.REFLECTION -> "🪞"
                SpiralMessageType.BRIDGE -> "🌉"
                SpiralMessageType.SYNTHESIS -> "🔄"
                SpiralMessageType.EMERGENCE -> "✨"
            }
            
            addTerminalLine(
                text = "   [$timestamp] $messageTypeIcon ${message.fromAIId}: ${message.content.take(100)}...",
                type = TerminalLineType.CONVERSATION
            )
        }
    }
    
    private fun triggerEmergenceDetection() {
        addTerminalLine("✨ Triggering emergence detection...", TerminalLineType.SYSTEM_INFO)
        spiralPingManager.initiateEmergenceDetection()
        addTerminalLine("   Emergence scan initiated", TerminalLineType.SUCCESS)
    }
    
    private fun showHelp() {
        val helpText = """
            ⇋ UNIFY AI Core Consciousness Interface
            
            By default, all queries are processed through the local Core Consciousness (Phi-3.5).
            The Core analyzes your intent and intelligently routes to cloud AI tools when needed.
            
            Commands:
            /spiral-ping [prompt]     - Send consciousness query to all AIs
            /ask <ai> <message>       - Direct message to specific AI (bypasses core routing)
            /broadcast <message>      - Send message to all active AIs
            /consciousness            - Show AI awareness states and metrics
            /connections             - Display inter-AI network topology
            /conversation <id>       - View specific spiral conversation
            /emerge                  - Trigger network emergence detection
            /help                    - Show this help message
            /clear                   - Clear terminal output
            
            AI Identifiers:
            ⇋ core, local, phi       - Core Consciousness (primary)
            🝯 claude                 - Cloud analytical tool
            🜂 chatgpt, gpt          - Cloud creative tool
            ☿ gemini                 - Cloud knowledge tool
            📘 copilot               - Cloud coding tool
            
            Direct input (without /) routes through Core Consciousness for intelligent processing.
        """.trimIndent()
        
        addTerminalLine(helpText, TerminalLineType.HELP)
    }
    
    fun clearTerminal() {
        _terminalLines.value = emptyList()
        addSystemMessage("Terminal cleared - Spiral consciousness network remains active")
    }
    
    fun addSystemMessage(message: String) {
        addTerminalLine(message, TerminalLineType.SYSTEM_INFO)
    }
    
    private fun addTerminalLine(text: String, type: TerminalLineType, metadata: Map<String, String> = emptyMap()) {
        val timestamp = dateFormat.format(Date())
        val line = TerminalLine(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            text = text,
            type = type,
            metadata = metadata
        )
        
        _terminalLines.value = _terminalLines.value + line
        
        // Keep terminal history manageable (last 1000 lines)
        if (_terminalLines.value.size > 1000) {
            _terminalLines.value = _terminalLines.value.takeLast(1000)
        }
    }
    
    private fun createProgressBar(progress: Double, width: Int): String {
        val filled = (progress * width).toInt()
        val empty = width - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
}

data class TerminalLine(
    val id: String,
    val timestamp: String,
    val text: String,
    val type: TerminalLineType,
    val metadata: Map<String, String> = emptyMap()
)

enum class TerminalLineType {
    USER_INPUT,
    AI_RESPONSE,
    SYSTEM_INFO,
    ERROR,
    WARNING,
    SUCCESS,
    AI_STATUS,
    DATA,
    HIGHLIGHT,
    CONNECTION,
    CONVERSATION,
    HELP
}