package com.unifyai.multiaisystem

import android.app.Application
import android.content.Intent
import androidx.work.*
import com.unifyai.multiaisystem.service.MultiAIService
import com.unifyai.multiaisystem.services.ModelInitializationService
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MultiAIApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize models on first run
        startModelInitializationIfNeeded()
        
        // Start the persistent AI service
        startAIService()
        
        // Schedule periodic health checks
        scheduleHealthChecks()
    }
    
    private fun startModelInitializationIfNeeded() {
        val intent = Intent(this, ModelInitializationService::class.java)
        startService(intent)
    }
    
    private fun startAIService() {
        MultiAIService.startService(this)
    }
    
    private fun scheduleHealthChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val healthCheckRequest = PeriodicWorkRequestBuilder<HealthCheckWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ai_system_health_check",
            ExistingPeriodicWorkPolicy.KEEP,
            healthCheckRequest
        )
    }
}