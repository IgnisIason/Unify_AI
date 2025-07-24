package com.unifyai.multiaisystem

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unifyai.multiaisystem.core.AISystemManager
import com.unifyai.multiaisystem.service.MultiAIService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiSystemManager: AISystemManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Check if AI systems are responsive
            val activeSystems = aiSystemManager.getActiveAISystems().first()
            val executionStats = aiSystemManager.getExecutionStats()
            
            // Check for systems with high error rates
            val problematicSystems = executionStats.filter { (_, stats) ->
                val totalTasks = stats.completedTasks + stats.failedTasks
                if (totalTasks > 10) {
                    val errorRate = stats.failedTasks.toDouble() / totalTasks
                    errorRate > 0.5 // More than 50% error rate
                } else false
            }
            
            // Check for stuck systems (no activity in last 5 minutes)
            val currentTime = System.currentTimeMillis()
            val stuckSystems = activeSystems.filter { system ->
                currentTime - system.lastExecutionTime > 5 * 60 * 1000 // 5 minutes
            }
            
            // Log health status
            android.util.Log.i("HealthCheck", 
                "Active: ${activeSystems.size}, Problematic: ${problematicSystems.size}, Stuck: ${stuckSystems.size}")
            
            // Restart service if too many issues
            if (problematicSystems.size > activeSystems.size / 2 || stuckSystems.size > 2) {
                android.util.Log.w("HealthCheck", "Restarting AI service due to health issues")
                MultiAIService.restartService(applicationContext)
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("HealthCheck", "Health check failed", e)
            Result.retry()
        }
    }
}