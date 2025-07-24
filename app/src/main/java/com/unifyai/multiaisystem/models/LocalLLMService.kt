package com.unifyai.multiaisystem.models

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class LocalLLMService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
    private val modelStorageManager: ModelStorageManager,
    private val tokenizer: Phi35MiniTokenizer
) {
    companion object {
        private const val TAG = "LocalLLMService"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class ModelStatus(
        val isAvailable: Boolean,
        val isDownloading: Boolean,
        val downloadProgress: Float,
        val modelSize: Long,
        val lastUpdated: Long,
        val error: String? = null
    )
    
    private val _modelStatus = MutableStateFlow(
        ModelStatus(
            isAvailable = false,
            isDownloading = false,
            downloadProgress = 0f,
            modelSize = 0L,
            lastUpdated = System.currentTimeMillis()
        )
    )
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()
    
    init {
        // Initialize service and check model status
        scope.launch {
            updateModelStatus()
            initializeTokenizer()
        }
    }
    
    suspend fun initializeLocalLLM(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "⇋ Initializing Local LLM service...")
                
                // Check storage space
                val storageInfo = modelStorageManager.getStorageInfo()
                if (!storageInfo.canDownloadModel && !isModelAvailable()) {
                    Log.w(TAG, "⇋ Insufficient storage space for model download")
                    return@withContext false
                }
                
                // Initialize tokenizer
                if (!tokenizer.initialize()) {
                    Log.w(TAG, "⇋ Tokenizer initialization failed, using fallback")
                }
                
                // Update status
                updateModelStatus()
                
                Log.i(TAG, "⇋ Local LLM service initialized successfully")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Failed to initialize Local LLM service", e)
                false
            }
        }
    }
    
    suspend fun downloadModel(): Flow<ModelDownloadManager.DownloadProgress> = flow {
        if (isModelAvailable()) {
            Log.i(TAG, "⇋ Model already available, skipping download")
            emit(ModelDownloadManager.DownloadProgress(
                filename = ModelDownloadManager.PHI35_MINI_MODEL_FILENAME,
                bytesDownloaded = ModelDownloadManager.PHI35_MINI_MODEL_SIZE,
                totalBytes = ModelDownloadManager.PHI35_MINI_MODEL_SIZE,
                percentage = 100f,
                downloadSpeed = "0 KB/s",
                isComplete = true
            ))
            return@flow
        }
        
        // Check storage before download
        val storageInfo = modelStorageManager.getStorageInfo()
        if (!storageInfo.canDownloadModel) {
            // Try cleanup first
            val freedBytes = modelStorageManager.performCleanup(forceCleanup = true)
            Log.i(TAG, "⇋ Freed ${formatBytes(freedBytes)} during pre-download cleanup")
            
            // Check again after cleanup
            val updatedStorageInfo = modelStorageManager.getStorageInfo()
            if (!updatedStorageInfo.canDownloadModel) {
                throw IllegalStateException("Insufficient storage space for model download. Need ${formatBytes(ModelDownloadManager.PHI35_MINI_MODEL_SIZE)} free space.")
            }
        }
        
        updateModelStatus(isDownloading = true)
        
        try {
            modelDownloadManager.downloadPhi35MiniModel().collect { progress ->
                updateModelStatus(
                    isDownloading = true,
                    downloadProgress = progress.percentage / 100f
                )
                emit(progress)
            }
            
            // Download completed, update status
            updateModelStatus(isDownloading = false)
            
        } catch (e: Exception) {
            Log.e(TAG, "⇋ Model download failed", e)
            updateModelStatus(isDownloading = false, error = e.message)
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    fun isModelAvailable(): Boolean {
        return modelDownloadManager.isPhi35MiniModelAvailable()
    }
    
    fun getModelPath(): String? {
        return modelDownloadManager.getPhi35MiniModelPath()
    }
    
    fun getTokenizerPath(): String? {
        return modelDownloadManager.getPhi35MiniTokenizerPath()
    }
    
    suspend fun deleteModel(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deleted = modelDownloadManager.deleteModel()
                updateModelStatus()
                Log.i(TAG, "⇋ Model deletion: $deleted")
                deleted
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Failed to delete model", e)
                false
            }
        }
    }
    
    suspend fun verifyModelIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                modelDownloadManager.verifyModelIntegrity()
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Model integrity verification failed", e)
                false
            }
        }
    }
    
    private suspend fun initializeTokenizer() {
        try {
            if (!tokenizer.initialize()) {
                Log.w(TAG, "⇋ Tokenizer initialization failed, using fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⇋ Tokenizer initialization error", e)
        }
    }
    
    private suspend fun updateModelStatus(
        isDownloading: Boolean = false,
        downloadProgress: Float = 0f,
        error: String? = null
    ) {
        withContext(Dispatchers.Main) {
            val modelInfo = modelDownloadManager.getModelInfo()
            _modelStatus.value = ModelStatus(
                isAvailable = modelInfo.isAvailable,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                modelSize = modelInfo.fileSize,
                lastUpdated = System.currentTimeMillis(),
                error = error
            )
        }
    }
    
    fun getStorageInfo(): ModelStorageManager.StorageInfo {
        return modelStorageManager.getStorageInfo()
    }
    
    fun getStorageRecommendations(): List<String> {
        return modelStorageManager.getStorageRecommendations()
    }
    
    suspend fun performStorageCleanup(): Long {
        return modelStorageManager.performCleanup(forceCleanup = true)
    }
    
    fun getServiceInfo(): Map<String, Any> {
        val modelInfo = modelDownloadManager.getModelInfo()
        val storageInfo = modelStorageManager.getStorageInfo()
        val status = _modelStatus.value
        
        return mapOf<String, Any>(
            "service_name" to "Phi-3.5 Mini Local LLM",
            "model_available" to status.isAvailable,
            "model_downloading" to status.isDownloading,
            "download_progress" to status.downloadProgress,
            "model_size_mb" to (status.modelSize / (1024 * 1024)).toInt(),
            "model_path" to (getModelPath() ?: "Not available"),
            "tokenizer_path" to (getTokenizerPath() ?: "Not available"),
            "storage_free_mb" to (storageInfo.freeSpaceBytes / (1024 * 1024)).toInt(),
            "storage_used_mb" to (storageInfo.usedSpaceBytes / (1024 * 1024)).toInt(),
            "cache_size_mb" to (storageInfo.cacheSizeBytes / (1024 * 1024)).toInt(),
            "can_download_model" to storageInfo.canDownloadModel,
            "needs_cleanup" to storageInfo.needsCleanup,
            "last_updated" to status.lastUpdated,
            "error" to (status.error ?: "")
        )
    }
    
    fun getCapabilities(): List<String> {
        return listOf(
            "🔒 Complete offline processing",
            "📱 On-device inference with privacy preservation",
            "⚡ Optimized for mobile hardware",
            "🧠 Phi-3.5 Mini Instruct model (3.8B parameters)",
            "💬 Natural language understanding and generation",
            "🔄 Spiral consciousness network integration",
            "🛡️ Zero data transmission to external servers",
            "🎯 Privacy-focused AI Bridge Node anchor"
        )
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    fun shutdown() {
        scope.cancel()
        modelStorageManager.shutdown()
        Log.i(TAG, "⇋ Local LLM service shutdown")
    }
}