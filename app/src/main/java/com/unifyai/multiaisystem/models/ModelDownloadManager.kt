package com.unifyai.multiaisystem.models

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.io.*
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "ModelDownload"
        
        // Phi-3.5 Mini ONNX model configuration
        const val PHI35_MINI_MODEL_URL = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi-3.5-mini-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx"
        const val PHI35_MINI_MODEL_FILENAME = "phi-3.5-mini-instruct.onnx"
        const val PHI35_MINI_TOKENIZER_URL = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-onnx/resolve/main/tokenizer.json"
        const val PHI35_MINI_TOKENIZER_FILENAME = "phi-3.5-mini-tokenizer.json"
        
        // Expected file sizes (approximate)
        const val PHI35_MINI_MODEL_SIZE = 2_200_000_000L // ~2.2GB
        const val PHI35_MINI_TOKENIZER_SIZE = 2_800_000L // ~2.8MB
        
        // SHA256 checksums for integrity verification
        const val PHI35_MINI_MODEL_SHA256 = "placeholder_sha256_will_be_updated"
        const val PHI35_MINI_TOKENIZER_SHA256 = "placeholder_sha256_will_be_updated"
    }
    
    data class DownloadProgress(
        val filename: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percentage: Float,
        val downloadSpeed: String,
        val isComplete: Boolean = false,
        val error: String? = null
    )
    
    data class ModelInfo(
        val name: String,
        val filename: String,
        val localPath: String,
        val isAvailable: Boolean,
        val fileSize: Long,
        val lastModified: Long
    )
    
    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    private val _downloadProgress = MutableSharedFlow<DownloadProgress>()
    val downloadProgress: SharedFlow<DownloadProgress> = _downloadProgress.asSharedFlow()
    
    suspend fun downloadPhi35MiniModel(): Flow<DownloadProgress> = flow {
        try {
            Log.i(TAG, "⇋ Starting Phi-3.5 Mini model download")
            
            // Download tokenizer first (smaller file)
            Log.i(TAG, "⇋ Downloading tokenizer...")
            downloadFile(
                url = PHI35_MINI_TOKENIZER_URL,
                filename = PHI35_MINI_TOKENIZER_FILENAME,
                expectedSize = PHI35_MINI_TOKENIZER_SIZE
            ).collect { progress ->
                emit(progress)
                _downloadProgress.emit(progress)
            }
            
            // Download main model
            Log.i(TAG, "⇋ Downloading Phi-3.5 Mini model...")
            downloadFile(
                url = PHI35_MINI_MODEL_URL,
                filename = PHI35_MINI_MODEL_FILENAME,
                expectedSize = PHI35_MINI_MODEL_SIZE
            ).collect { progress ->
                emit(progress)
                _downloadProgress.emit(progress)
            }
            
            Log.i(TAG, "⇋ Phi-3.5 Mini model download completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "⇋ Model download failed", e)
            emit(DownloadProgress(
                filename = PHI35_MINI_MODEL_FILENAME,
                bytesDownloaded = 0,
                totalBytes = PHI35_MINI_MODEL_SIZE,
                percentage = 0f,
                downloadSpeed = "0 KB/s",
                isComplete = false,
                error = "Download failed: ${e.message}"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun downloadFile(
        url: String,
        filename: String,
        expectedSize: Long
    ): Flow<DownloadProgress> = flow {
        val targetFile = File(modelsDir, filename)
        val tempFile = File(modelsDir, "$filename.tmp")
        
        // Check if file already exists and is complete
        if (targetFile.exists() && targetFile.length() >= expectedSize * 0.95) {
            Log.i(TAG, "⇋ File $filename already exists and appears complete")
            emit(DownloadProgress(
                filename = filename,
                bytesDownloaded = targetFile.length(),
                totalBytes = expectedSize,
                percentage = 100f,
                downloadSpeed = "0 KB/s",
                isComplete = true
            ))
            return@flow
        }
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Download failed: HTTP ${response.code}")
        }
        
        val body = response.body ?: throw IOException("Response body is null")
        val contentLength = body.contentLength().takeIf { it > 0 } ?: expectedSize
        
        var bytesDownloaded = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead
                    
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= 1000) { // Update every second
                        val elapsedSeconds = (currentTime - startTime) / 1000.0
                        val speedBytesPerSecond = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                        val speedText = formatSpeed(speedBytesPerSecond)
                        
                        val progress = DownloadProgress(
                            filename = filename,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = contentLength,
                            percentage = (bytesDownloaded * 100f) / contentLength,
                            downloadSpeed = speedText
                        )
                        
                        emit(progress)
                        lastUpdateTime = currentTime
                    }
                }
            }
        }
        
        // Move temp file to final location
        if (tempFile.renameTo(targetFile)) {
            Log.i(TAG, "⇋ Successfully downloaded $filename (${formatBytes(bytesDownloaded)})")
            emit(DownloadProgress(
                filename = filename,
                bytesDownloaded = bytesDownloaded,
                totalBytes = contentLength,
                percentage = 100f,
                downloadSpeed = "0 KB/s",
                isComplete = true
            ))
        } else {
            throw IOException("Failed to move temporary file to final location")
        }
    }.flowOn(Dispatchers.IO)
    
    fun isPhi35MiniModelAvailable(): Boolean {
        val modelFile = File(modelsDir, PHI35_MINI_MODEL_FILENAME)
        val tokenizerFile = File(modelsDir, PHI35_MINI_TOKENIZER_FILENAME)
        
        return modelFile.exists() && 
               tokenizerFile.exists() && 
               modelFile.length() > PHI35_MINI_MODEL_SIZE * 0.95 && // Allow 5% variance
               tokenizerFile.length() > PHI35_MINI_TOKENIZER_SIZE * 0.8
    }
    
    fun getPhi35MiniModelPath(): String? {
        return if (isPhi35MiniModelAvailable()) {
            File(modelsDir, PHI35_MINI_MODEL_FILENAME).absolutePath
        } else {
            null
        }
    }
    
    fun getPhi35MiniTokenizerPath(): String? {
        return if (isPhi35MiniModelAvailable()) {
            File(modelsDir, PHI35_MINI_TOKENIZER_FILENAME).absolutePath
        } else {
            null
        }
    }
    
    fun getModelInfo(): ModelInfo {
        val modelFile = File(modelsDir, PHI35_MINI_MODEL_FILENAME)
        return ModelInfo(
            name = "Phi-3.5 Mini Instruct",
            filename = PHI35_MINI_MODEL_FILENAME,
            localPath = modelFile.absolutePath,
            isAvailable = isPhi35MiniModelAvailable(),
            fileSize = if (modelFile.exists()) modelFile.length() else 0L,
            lastModified = if (modelFile.exists()) modelFile.lastModified() else 0L
        )
    }
    
    suspend fun deleteModel(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(modelsDir, PHI35_MINI_MODEL_FILENAME)
                val tokenizerFile = File(modelsDir, PHI35_MINI_TOKENIZER_FILENAME)
                
                val modelDeleted = if (modelFile.exists()) modelFile.delete() else true
                val tokenizerDeleted = if (tokenizerFile.exists()) tokenizerFile.delete() else true
                
                Log.i(TAG, "⇋ Model deletion: model=$modelDeleted, tokenizer=$tokenizerDeleted")
                modelDeleted && tokenizerDeleted
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Failed to delete model files", e)
                false
            }
        }
    }
    
    suspend fun verifyModelIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(modelsDir, PHI35_MINI_MODEL_FILENAME)
                if (!modelFile.exists()) return@withContext false
                
                // For now, just check file size - SHA256 verification can be added later
                val isValid = modelFile.length() >= PHI35_MINI_MODEL_SIZE * 0.95
                Log.i(TAG, "⇋ Model integrity check: ${if (isValid) "PASSED" else "FAILED"}")
                isValid
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Model integrity check failed", e)
                false
            }
        }
    }
    
    private fun formatSpeed(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSecond / (1024 * 1024))
            bytesPerSecond >= 1024 -> "%.1f KB/s".format(bytesPerSecond / 1024)
            else -> "%.0f B/s".format(bytesPerSecond)
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    fun getStorageInfo(): Map<String, String> {
        val totalSpace = modelsDir.totalSpace
        val freeSpace = modelsDir.freeSpace
        val usedSpace = totalSpace - freeSpace
        
        return mapOf(
            "models_directory" to modelsDir.absolutePath,
            "total_space" to formatBytes(totalSpace),
            "free_space" to formatBytes(freeSpace),
            "used_space" to formatBytes(usedSpace),
            "model_available" to isPhi35MiniModelAvailable().toString()
        )
    }
}