package com.unifyai.multiaisystem.utils

import android.content.Context
import android.util.Log
import com.unifyai.multiaisystem.data.database.AISystemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    private val aiSystemDao: AISystemDao
) {
    companion object {
        private const val TAG = "MultiAI_ErrorHandler"
        private const val MAX_CONSECUTIVE_ERRORS = 5
        private const val ERROR_RECOVERY_DELAY = 30000L // 30 seconds
    }
    
    private val consecutiveErrors = mutableMapOf<String, Int>()
    private val lastErrorTime = mutableMapOf<String, Long>()
    
    fun handleAISystemError(
        aiSystemId: String, 
        error: Throwable, 
        context: String,
        scope: CoroutineScope
    ) {
        Log.e(TAG, "AI System error in $context for system $aiSystemId", error)
        
        scope.launch {
            // Increment error count
            val errorCount = consecutiveErrors.getOrDefault(aiSystemId, 0) + 1
            consecutiveErrors[aiSystemId] = errorCount
            lastErrorTime[aiSystemId] = System.currentTimeMillis()
            
            // Update database
            aiSystemDao.incrementErrorCount(aiSystemId)
            
            // Check if system should be disabled
            if (errorCount >= MAX_CONSECUTIVE_ERRORS) {
                Log.w(TAG, "Disabling AI system $aiSystemId due to consecutive errors: $errorCount")
                aiSystemDao.updateAISystemStatus(aiSystemId, false)
                
                // Schedule recovery attempt
                scheduleRecovery(aiSystemId, scope)
            }
        }
    }
    
    private fun scheduleRecovery(aiSystemId: String, scope: CoroutineScope) {
        scope.launch {
            kotlinx.coroutines.delay(ERROR_RECOVERY_DELAY)
            
            Log.i(TAG, "Attempting recovery for AI system: $aiSystemId")
            
            // Reset error count and re-enable system
            consecutiveErrors.remove(aiSystemId)
            aiSystemDao.updateAISystemStatus(aiSystemId, true)
        }
    }
    
    fun handleTaskError(taskId: String, aiSystemId: String, error: Throwable) {
        Log.e(TAG, "Task error for task $taskId on system $aiSystemId", error)
        
        // Categorize error for better handling
        when (error) {
            is OutOfMemoryError -> {
                Log.e(TAG, "Out of memory error - system may need resource adjustment")
            }
            is SecurityException -> {
                Log.e(TAG, "Security error - check permissions")
            }
            is java.io.IOException -> {
                Log.e(TAG, "IO error - check network connectivity or file access")
            }
        }
    }
    
    fun resetErrorCount(aiSystemId: String) {
        consecutiveErrors.remove(aiSystemId)
        lastErrorTime.remove(aiSystemId)
    }
    
    fun getErrorCount(aiSystemId: String): Int {
        return consecutiveErrors[aiSystemId] ?: 0
    }
    
    fun shouldRetryTask(aiSystemId: String, currentRetryCount: Int, maxRetries: Int): Boolean {
        val errorCount = getErrorCount(aiSystemId)
        val timeSinceLastError = System.currentTimeMillis() - (lastErrorTime[aiSystemId] ?: 0)
        
        return currentRetryCount < maxRetries && 
               errorCount < MAX_CONSECUTIVE_ERRORS && 
               timeSinceLastError > 5000 // Wait at least 5 seconds
    }
}