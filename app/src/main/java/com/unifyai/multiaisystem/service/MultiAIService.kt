package com.unifyai.multiaisystem.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.unifyai.multiaisystem.R
import com.unifyai.multiaisystem.core.AISystemManager
import com.unifyai.multiaisystem.data.model.AIResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MultiAIService : LifecycleService() {
    
    @Inject
    lateinit var aiSystemManager: AISystemManager
    
    private val channelId = "multi_ai_service_channel"
    private val notificationId = 1001
    
    private var isServiceRunning = false
    private var totalTasksProcessed = 0
    private var successfulTasks = 0
    private var failedTasks = 0
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        initializeAIManager()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                if (!isServiceRunning) {
                    startAIProcessing()
                }
            }
            ACTION_STOP_SERVICE -> {
                stopAIProcessing()
                stopSelf()
            }
            ACTION_RESTART_SERVICE -> {
                restartAIProcessing()
            }
        }
        
        return START_STICKY // Restart if killed by system
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // Not a bound service
    }
    
    override fun onDestroy() {
        stopAIProcessing()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Multi-AI System Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent AI processing service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = createNotification(
            "Multi-AI System",
            "Initializing AI systems..."
        )
        startForeground(notificationId, notification)
    }
    
    private fun createNotification(title: String, content: String): Notification {
        val stopIntent = Intent(this, MultiAIService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val restartIntent = Intent(this, MultiAIService::class.java).apply {
            action = ACTION_RESTART_SERVICE
        }
        val restartPendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_ai_service)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_restart,
                "Restart",
                restartPendingIntent
            )
            .build()
    }
    
    private fun initializeAIManager() {
        lifecycleScope.launch {
            try {
                aiSystemManager.start()
                
                // Monitor AI results
                aiSystemManager.getResults().collectLatest { result ->
                    handleAIResult(result)
                }
            } catch (e: Exception) {
                updateNotification(
                    "Multi-AI System - Error",
                    "Failed to initialize: ${e.message}"
                )
            }
        }
    }
    
    private fun startAIProcessing() {
        if (isServiceRunning) return
        
        isServiceRunning = true
        updateNotification(
            "Multi-AI System - Running",
            "Processing AI tasks - $totalTasksProcessed tasks completed"
        )
    }
    
    private fun stopAIProcessing() {
        if (!isServiceRunning) return
        
        isServiceRunning = false
        aiSystemManager.stop()
        updateNotification(
            "Multi-AI System - Stopped",
            "AI processing stopped - $totalTasksProcessed total tasks processed"
        )
    }
    
    private fun restartAIProcessing() {
        stopAIProcessing()
        // Reset counters
        totalTasksProcessed = 0
        successfulTasks = 0
        failedTasks = 0
        
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000) // Brief delay before restart
            aiSystemManager.start()
            startAIProcessing()
        }
    }
    
    private fun handleAIResult(result: AIResult) {
        totalTasksProcessed++
        
        if (result.success) {
            successfulTasks++
        } else {
            failedTasks++
        }
        
        // Update notification every 10 tasks or on errors
        if (totalTasksProcessed % 10 == 0 || !result.success) {
            val successRate = if (totalTasksProcessed > 0) {
                (successfulTasks * 100) / totalTasksProcessed
            } else 0
            
            updateNotification(
                "Multi-AI System - Active",
                "Tasks: $totalTasksProcessed | Success: $successRate% | Active Systems: ${getActiveSystemCount()}"
            )
        }
        
        // Log significant events
        if (!result.success) {
            android.util.Log.w("MultiAIService", "Task failed: ${result.errorMessage}")
        }
    }
    
    private fun getActiveSystemCount(): Int {
        return aiSystemManager.getExecutionStats().size
    }
    
    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    companion object {
        const val ACTION_START_SERVICE = "com.unifyai.multiaisystem.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.unifyai.multiaisystem.STOP_SERVICE"
        const val ACTION_RESTART_SERVICE = "com.unifyai.multiaisystem.RESTART_SERVICE"
        
        fun startService(context: Context) {
            val intent = Intent(context, MultiAIService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, MultiAIService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
        
        fun restartService(context: Context) {
            val intent = Intent(context, MultiAIService::class.java).apply {
                action = ACTION_RESTART_SERVICE
            }
            context.startService(intent)
        }
    }
}