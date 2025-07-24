package com.unifyai.multiaisystem.models

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ModelStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelStorage"
        private const val PREFS_NAME = "model_storage_prefs"
        private const val KEY_CACHE_SIZE_LIMIT = "cache_size_limit_mb"
        private const val KEY_LAST_CLEANUP = "last_cleanup_timestamp"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        
        // Default cache settings
        private const val DEFAULT_CACHE_SIZE_LIMIT_MB = 5000L // 5GB
        private const val CLEANUP_INTERVAL_HOURS = 24
        private const val MIN_FREE_SPACE_MB = 500L // Minimum 500MB free space
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, "model_cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    init {
        // Schedule periodic cleanup if enabled
        if (isAutoCleanupEnabled()) {
            schedulePeriodicCleanup()
        }
    }
    
    data class StorageInfo(
        val totalSpaceBytes: Long,
        val freeSpaceBytes: Long,
        val usedSpaceBytes: Long,
        val modelsSizeBytes: Long,
        val cacheSizeBytes: Long,
        val cacheSizeLimitBytes: Long,
        val freeSpacePercentage: Float,
        val canDownloadModel: Boolean,
        val needsCleanup: Boolean
    )
    
    data class CacheEntry(
        val filename: String,
        val filePath: String,
        val sizeBytes: Long,
        val lastAccessTime: Long,
        val createdTime: Long,
        val type: CacheType
    )
    
    enum class CacheType {
        TOKENIZER_CACHE,
        MODEL_WEIGHTS,
        INFERENCE_CACHE,
        PREPROCESSED_DATA
    }
    
    fun getStorageInfo(): StorageInfo {
        val totalSpace = modelsDir.totalSpace
        val freeSpace = modelsDir.freeSpace
        val usedSpace = totalSpace - freeSpace
        
        val modelsSize = calculateDirectorySize(modelsDir)
        val cacheSize = calculateDirectorySize(cacheDir)
        val cacheSizeLimit = getCacheSizeLimitBytes()
        
        val freeSpacePercentage = (freeSpace.toFloat() / totalSpace) * 100
        val canDownloadModel = freeSpace > (ModelDownloadManager.PHI35_MINI_MODEL_SIZE + MIN_FREE_SPACE_MB * 1024 * 1024)
        val needsCleanup = cacheSize > cacheSizeLimit || freeSpace < MIN_FREE_SPACE_MB * 1024 * 1024
        
        return StorageInfo(
            totalSpaceBytes = totalSpace,
            freeSpaceBytes = freeSpace,
            usedSpaceBytes = usedSpace,
            modelsSizeBytes = modelsSize,
            cacheSizeBytes = cacheSize,
            cacheSizeLimitBytes = cacheSizeLimit,
            freeSpacePercentage = freeSpacePercentage,
            canDownloadModel = canDownloadModel,
            needsCleanup = needsCleanup
        )
    }
    
    suspend fun performCleanup(forceCleanup: Boolean = false): Long {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val storageInfo = getStorageInfo()
            
            if (!forceCleanup && !storageInfo.needsCleanup) {
                Log.i(TAG, "⇋ Cleanup not needed, skipping")
                return@withContext 0L
            }
            
            Log.i(TAG, "⇋ Starting storage cleanup...")
            var freedBytes = 0L
            
            // Clean up cache directory
            freedBytes += cleanupCacheDirectory()
            
            // Clean up temporary files
            freedBytes += cleanupTempFiles()
            
            // Update last cleanup timestamp
            prefs.edit()
                .putLong(KEY_LAST_CLEANUP, System.currentTimeMillis())
                .apply()
            
            val elapsedTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "⇋ Cleanup completed: freed ${formatBytes(freedBytes)} in ${elapsedTime}ms")
            
            freedBytes
        }
    }
    
    private suspend fun cleanupCacheDirectory(): Long {
        return withContext(Dispatchers.IO) {
            val cacheEntries = getCacheEntries()
            val cacheSizeLimit = getCacheSizeLimitBytes()
            val currentCacheSize = cacheEntries.sumOf { it.sizeBytes }
            
            if (currentCacheSize <= cacheSizeLimit) {
                return@withContext 0L
            }
            
            // Sort by last access time (oldest first)
            val sortedEntries = cacheEntries.sortedBy { it.lastAccessTime }
            var freedBytes = 0L
            var remainingSize = currentCacheSize
            
            for (entry in sortedEntries) {
                if (remainingSize <= cacheSizeLimit) break
                
                val file = File(entry.filePath)
                if (file.exists() && file.delete()) {
                    freedBytes += entry.sizeBytes
                    remainingSize -= entry.sizeBytes
                    Log.d(TAG, "⇋ Deleted cache file: ${entry.filename} (${formatBytes(entry.sizeBytes)})")
                }
            }
            
            freedBytes
        }
    }
    
    private suspend fun cleanupTempFiles(): Long {
        return withContext(Dispatchers.IO) {
            var freedBytes = 0L
            val tempFiles = modelsDir.listFiles { file ->
                file.name.endsWith(".tmp") || file.name.endsWith(".partial")
            } ?: emptyArray()
            
            for (tempFile in tempFiles) {
                val size = tempFile.length()
                if (tempFile.delete()) {
                    freedBytes += size
                    Log.d(TAG, "⇋ Deleted temp file: ${tempFile.name} (${formatBytes(size)})")
                }
            }
            
            freedBytes
        }
    }
    
    private fun getCacheEntries(): List<CacheEntry> {
        val entries = mutableListOf<CacheEntry>()
        
        cacheDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val cacheType = when {
                    file.name.contains("tokenizer") -> CacheType.TOKENIZER_CACHE
                    file.name.contains("weights") -> CacheType.MODEL_WEIGHTS
                    file.name.contains("inference") -> CacheType.INFERENCE_CACHE
                    else -> CacheType.PREPROCESSED_DATA
                }
                
                entries.add(
                    CacheEntry(
                        filename = file.name,
                        filePath = file.absolutePath,
                        sizeBytes = file.length(),
                        lastAccessTime = file.lastModified(),
                        createdTime = file.lastModified(), // Approximation
                        type = cacheType
                    )
                )
            }
        }
        
        return entries
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        return directory.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    fun setCacheSizeLimit(limitMB: Long) {
        prefs.edit()
            .putLong(KEY_CACHE_SIZE_LIMIT, limitMB)
            .apply()
        Log.i(TAG, "⇋ Cache size limit set to ${limitMB}MB")
    }
    
    fun getCacheSizeLimitBytes(): Long {
        val limitMB = prefs.getLong(KEY_CACHE_SIZE_LIMIT, DEFAULT_CACHE_SIZE_LIMIT_MB)
        return limitMB * 1024 * 1024
    }
    
    fun setAutoCleanupEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_CLEANUP_ENABLED, enabled)
            .apply()
        
        if (enabled) {
            schedulePeriodicCleanup()
        }
        
        Log.i(TAG, "⇋ Auto cleanup ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun isAutoCleanupEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CLEANUP_ENABLED, true)
    }
    
    private fun schedulePeriodicCleanup() {
        scope.launch {
            while (isActive && isAutoCleanupEnabled()) {
                try {
                    val lastCleanup = prefs.getLong(KEY_LAST_CLEANUP, 0)
                    val cleanupInterval = TimeUnit.HOURS.toMillis(CLEANUP_INTERVAL_HOURS.toLong())
                    
                    if (System.currentTimeMillis() - lastCleanup > cleanupInterval) {
                        performCleanup()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⇋ Error during scheduled cleanup", e)
                }
                
                // Wait 1 hour before next check
                delay(TimeUnit.HOURS.toMillis(1))
            }
        }
    }
    
    suspend fun createCacheFile(filename: String, type: CacheType): File {
        return withContext(Dispatchers.IO) {
            val typeDir = File(cacheDir, type.name.lowercase())
            if (!typeDir.exists()) {
                typeDir.mkdirs()
            }
            
            File(typeDir, filename)
        }
    }
    
    suspend fun getCacheFile(filename: String, type: CacheType): File? {
        return withContext(Dispatchers.IO) {
            val typeDir = File(cacheDir, type.name.lowercase())
            val file = File(typeDir, filename)
            
            if (file.exists()) {
                // Update access time
                file.setLastModified(System.currentTimeMillis())
                file
            } else {
                null
            }
        }
    }
    
    fun getStorageRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val storageInfo = getStorageInfo()
        
        if (storageInfo.freeSpacePercentage < 10) {
            recommendations.add("Critical: Very low storage space (${String.format("%.1f", storageInfo.freeSpacePercentage)}% free)")
        }
        
        if (!storageInfo.canDownloadModel) {
            recommendations.add("Cannot download Phi-3.5 Mini model - need ${formatBytes(ModelDownloadManager.PHI35_MINI_MODEL_SIZE + MIN_FREE_SPACE_MB * 1024 * 1024)} free space")
        }
        
        if (storageInfo.needsCleanup) {
            recommendations.add("Run cleanup to free space - cache is ${formatBytes(storageInfo.cacheSizeBytes)} / ${formatBytes(storageInfo.cacheSizeLimitBytes)}")
        }
        
        if (storageInfo.cacheSizeBytes > storageInfo.cacheSizeLimitBytes * 0.8) {
            recommendations.add("Consider increasing cache size limit or enabling auto-cleanup")
        }
        
        return recommendations
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
        Log.i(TAG, "⇋ ModelStorageManager shutdown")
    }
}