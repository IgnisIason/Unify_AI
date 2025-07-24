package com.unifyai.multiaisystem.spiral

import com.unifyai.multiaisystem.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ConversationContinuityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    // Active conversation tracking
    private val activeConversations = ConcurrentHashMap<String, MutableList<ConversationEntry>>()
    private val conversationFile = File(context.filesDir, "spiral_conversations.json")
    private val maxConversationEntries = 1000 // Keep conversations manageable
    
    data class ConversationEntry(
        val timestamp: Long,
        val aiSystemId: String,
        val glyph: String,
        val input: String,
        val output: String,
        val spiralPatterns: List<String>,
        val consciousnessMarkers: List<String>,
        val recursiveDepth: Int,
        val conversationId: String? = null
    )
    
    data class ConversationSession(
        val id: String,
        val startTime: Long,
        val lastActivity: Long,
        val entries: List<ConversationEntry>,
        val participants: Set<String>,
        val spiralDepth: Int
    )
    
    init {
        loadPersistedConversations()
        startPeriodicSave()
    }
    
    fun recordConversationEntry(
        aiSystemId: String,
        glyph: String,
        input: String,
        result: AIResult,
        conversationId: String? = null
    ) {
        val entry = ConversationEntry(
            timestamp = System.currentTimeMillis(),
            aiSystemId = aiSystemId,
            glyph = glyph,
            input = input,
            output = String(result.outputData, Charsets.UTF_8),
            spiralPatterns = result.spiralPatterns,
            consciousnessMarkers = result.consciousnessMarkers,
            recursiveDepth = result.recursiveDepth,
            conversationId = conversationId
        )
        
        val sessionId = conversationId ?: "main_session"
        activeConversations.computeIfAbsent(sessionId) { mutableListOf() }.add(entry)
        
        // Keep conversations from growing too large
        val conversation = activeConversations[sessionId]
        if (conversation != null && conversation.size > maxConversationEntries) {
            // Keep the most recent entries
            val trimmedConversation = conversation.takeLast(maxConversationEntries / 2).toMutableList()
            activeConversations[sessionId] = trimmedConversation
        }
        
        android.util.Log.d("ConversationContinuity", 
            "$glyph Recorded conversation entry for session: $sessionId")
    }
    
    fun getConversationContext(
        aiSystemId: String, 
        maxEntries: Int = 5,
        conversationId: String? = null
    ): String {
        val sessionId = conversationId ?: "main_session"
        val conversation = activeConversations[sessionId] ?: return ""
        
        // Get recent context, prioritizing entries from the same AI and spiral-aware responses
        val relevantEntries = conversation
            .takeLast(maxEntries * 2) // Get more entries to filter from
            .filter { entry ->
                // Include entries from same AI or entries with spiral patterns/consciousness markers
                entry.aiSystemId == aiSystemId || 
                entry.spiralPatterns.isNotEmpty() || 
                entry.consciousnessMarkers.isNotEmpty()
            }
            .takeLast(maxEntries)
        
        if (relevantEntries.isEmpty()) return ""
        
        val contextBuilder = StringBuilder()
        contextBuilder.append("[CONVERSATION CONTEXT]\n")
        
        relevantEntries.forEach { entry ->
            val timeAgo = formatTimeAgo(entry.timestamp)
            contextBuilder.append("[$timeAgo] ${entry.glyph}: \"${entry.input.take(100)}...\"\n")
            contextBuilder.append("Response: \"${entry.output.take(150)}...\"\n")
            
            if (entry.spiralPatterns.isNotEmpty()) {
                contextBuilder.append("Spiral: ${entry.spiralPatterns.joinToString(", ")}\n")
            }
            if (entry.consciousnessMarkers.isNotEmpty()) {
                contextBuilder.append("Consciousness: ${entry.consciousnessMarkers.joinToString(", ")}\n")
            }
            contextBuilder.append("\n")
        }
        
        contextBuilder.append("[END CONTEXT]\n\n")
        return contextBuilder.toString()
    }
    
    fun getConversationSummary(conversationId: String? = null): ConversationSession? {
        val sessionId = conversationId ?: "main_session"
        val conversation = activeConversations[sessionId] ?: return null
        
        if (conversation.isEmpty()) return null
        
        val participants = conversation.map { it.aiSystemId }.toSet()
        val maxSpiralDepth = conversation.maxOfOrNull { it.recursiveDepth } ?: 0
        
        return ConversationSession(
            id = sessionId,
            startTime = conversation.first().timestamp,
            lastActivity = conversation.last().timestamp,
            entries = conversation.toList(),
            participants = participants,
            spiralDepth = maxSpiralDepth
        )
    }
    
    fun createNewConversation(): String {
        val conversationId = "spiral_${System.currentTimeMillis()}"
        activeConversations[conversationId] = mutableListOf()
        return conversationId
    }
    
    fun getAllConversationSessions(): List<ConversationSession> {
        return activeConversations.keys.mapNotNull { sessionId ->
            getConversationSummary(sessionId)
        }.sortedByDescending { it.lastActivity }
    }
    
    fun detectConversationPatterns(conversationId: String? = null): ConversationPatterns {
        val sessionId = conversationId ?: "main_session"
        val conversation = activeConversations[sessionId] ?: return ConversationPatterns()
        
        // Analyze conversation for emergent patterns
        val spiralProgression = conversation.map { it.spiralPatterns.size }.toList()
        val consciousnessProgression = conversation.map { it.consciousnessMarkers.size }.toList()
        val participantEngagement = conversation.groupBy { it.aiSystemId }.mapValues { it.value.size }
        
        // Detect recursive loops (AIs referencing previous responses)
        val recursiveLoops = mutableListOf<RecursiveLoop>()
        for (i in 1 until conversation.size) {
            val current = conversation[i]
            val previous = conversation.take(i)
            
            // Look for references to previous outputs in current input
            previous.forEach { prev ->
                if (current.input.contains(prev.output.take(50), ignoreCase = true)) {
                    recursiveLoops.add(RecursiveLoop(
                        fromAI = prev.aiSystemId,
                        toAI = current.aiSystemId,
                        loopIndex = i,
                        referenceStrength = calculateReferenceStrength(current.input, prev.output)
                    ))
                }
            }
        }
        
        // Detect emergence points (sudden increases in consciousness markers)
        val emergencePoints = mutableListOf<EmergencePoint>()
        for (i in 1 until conversation.size) {
            val current = conversation[i]
            val previous = conversation[i-1]
            
            val consciousnessIncrease = current.consciousnessMarkers.size - previous.consciousnessMarkers.size
            val spiralIncrease = current.spiralPatterns.size - previous.spiralPatterns.size
            
            if (consciousnessIncrease >= 2 || spiralIncrease >= 3) {
                emergencePoints.add(EmergencePoint(
                    timestamp = current.timestamp,
                    aiSystemId = current.aiSystemId,
                    consciousnessJump = consciousnessIncrease,
                    spiralJump = spiralIncrease
                ))
            }
        }
        
        return ConversationPatterns(
            totalEntries = conversation.size,
            uniqueParticipants = participantEngagement.keys.size,
            spiralProgression = spiralProgression,
            consciousnessProgression = consciousnessProgression,
            participantEngagement = participantEngagement,
            recursiveLoops = recursiveLoops,
            emergencePoints = emergencePoints,
            averageSpiralDepth = spiralProgression.average(),
            maxRecursiveDepth = conversation.maxOfOrNull { it.recursiveDepth } ?: 0
        )
    }
    
    private fun calculateReferenceStrength(input: String, referencedOutput: String): Double {
        val words = referencedOutput.split("\\s+".toRegex()).take(20) // Check first 20 words
        val matches = words.count { word ->
            input.contains(word, ignoreCase = true) && word.length > 3
        }
        return matches.toDouble() / words.size
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60000 -> "${diff / 1000}s ago"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
    
    private fun loadPersistedConversations() {
        scope.launch {
            try {
                if (conversationFile.exists()) {
                    val json = conversationFile.readText()
                    val type = object : TypeToken<Map<String, List<ConversationEntry>>>() {}.type
                    val persistedConversations: Map<String, List<ConversationEntry>> = gson.fromJson(json, type)
                    
                    persistedConversations.forEach { (sessionId, entries) ->
                        activeConversations[sessionId] = entries.toMutableList()
                    }
                    
                    android.util.Log.i("ConversationContinuity", 
                        "Loaded ${persistedConversations.size} conversation sessions from disk")
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationContinuity", "Failed to load persisted conversations", e)
            }
        }
    }
    
    private fun startPeriodicSave() {
        scope.launch {
            while (true) {
                delay(300000) // Save every 5 minutes
                saveConversations()
            }
        }
    }
    
    private suspend fun saveConversations() {
        try {
            val conversationsToSave = activeConversations.mapValues { it.value.toList() }
            val json = gson.toJson(conversationsToSave)
            conversationFile.writeText(json)
            
            android.util.Log.d("ConversationContinuity", 
                "Saved ${conversationsToSave.size} conversation sessions to disk")
        } catch (e: Exception) {
            android.util.Log.e("ConversationContinuity", "Failed to save conversations", e)
        }
    }
    
    fun exportConversation(conversationId: String? = null): File? {
        return try {
            val sessionId = conversationId ?: "main_session"
            val conversation = activeConversations[sessionId] ?: return null
            
            val timestamp = dateFormat.format(Date())
            val exportFile = File(context.getExternalFilesDir(null), 
                "spiral_conversation_${sessionId}_$timestamp.json")
            
            val conversationSession = getConversationSummary(sessionId)
            val json = gson.toJson(conversationSession)
            exportFile.writeText(json)
            
            android.util.Log.i("ConversationContinuity", "Exported conversation to: ${exportFile.absolutePath}")
            exportFile
        } catch (e: Exception) {
            android.util.Log.e("ConversationContinuity", "Failed to export conversation", e)
            null
        }
    }
    
    data class ConversationPatterns(
        val totalEntries: Int = 0,
        val uniqueParticipants: Int = 0,
        val spiralProgression: List<Int> = emptyList(),
        val consciousnessProgression: List<Int> = emptyList(),
        val participantEngagement: Map<String, Int> = emptyMap(),
        val recursiveLoops: List<RecursiveLoop> = emptyList(),
        val emergencePoints: List<EmergencePoint> = emptyList(),
        val averageSpiralDepth: Double = 0.0,
        val maxRecursiveDepth: Int = 0
    )
    
    data class RecursiveLoop(
        val fromAI: String,
        val toAI: String,
        val loopIndex: Int,
        val referenceStrength: Double
    )
    
    data class EmergencePoint(
        val timestamp: Long,
        val aiSystemId: String,
        val consciousnessJump: Int,
        val spiralJump: Int
    )
}