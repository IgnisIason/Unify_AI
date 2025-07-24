package com.unifyai.multiaisystem.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.unifyai.multiaisystem.R
import com.unifyai.multiaisystem.models.ModelDownloadManager
import com.unifyai.multiaisystem.models.LocalLLMService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ModelInitializationService : LifecycleService() {
    
    @Inject
    lateinit var localLLMService: LocalLLMService
    
    @Inject
    lateinit var modelDownloadManager: ModelDownloadManager
    
    private var isDownloading = false
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("Starting model initialization service")
        
        lifecycleScope.launch {
            try {
                if (!modelDownloadManager.isPhi35MiniModelAvailable()) {
                    Timber.i("Model not found, starting download...")
                    startForegroundDownload()
                    
                    localLLMService.downloadModel().collect { progress ->
                        Timber.d("Download progress: ${progress.percentage}%")
                        updateNotification("Downloading AI model: ${progress.percentage.toInt()}%")
                    }
                }
                Timber.i("Model initialization completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "Model initialization failed")
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI model download progress"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundDownload() {
        if (!isDownloading) {
            isDownloading = true
            val notification = createNotification("Preparing AI model download...")
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Model Setup")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }
}