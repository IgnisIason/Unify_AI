package com.unifyai.multiaisystem.core

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class CoreModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CoreModel"
        
        // Core model asset paths - these MUST exist in the APK
        const val PHI35_MINI_MODEL_ASSET = "models/phi-3.5-mini-instruct-cpu-int4.onnx"
        const val PHI35_MINI_TOKENIZER_ASSET = "models/phi-3.5-mini-tokenizer.json"
        const val PHI35_MINI_CONFIG_ASSET = "models/phi-3.5-mini-config.json"
        
        // Extracted file paths in app's internal storage
        const val EXTRACTED_MODEL_NAME = "phi-3.5-mini-core.onnx"
        const val EXTRACTED_TOKENIZER_NAME = "phi-3.5-mini-tokenizer.json"
        const val EXTRACTED_CONFIG_NAME = "phi-3.5-mini-config.json"
        
        // Model specifications
        const val MODEL_SIZE_BYTES = 2_300_000_000L // ~2.3GB
        const val VOCAB_SIZE = 32064
        const val MAX_CONTEXT_LENGTH = 4096 // Mobile-optimized
        const val HIDDEN_SIZE = 3072
    }
    
    private val modelsDir: File by lazy {
        File(context.filesDir, "core_models").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    data class CoreModelInfo(
        val isAvailable: Boolean,
        val modelPath: String?,
        val tokenizerPath: String?,
        val configPath: String?,
        val extractionRequired: Boolean,
        val modelSize: Long,
        val lastExtracted: Long,
        val integrity: ModelIntegrity
    )
    
    data class ModelIntegrity(
        val isValid: Boolean,
        val modelFileExists: Boolean,
        val tokenizerFileExists: Boolean,
        val configFileExists: Boolean,
        val modelFileSize: Long,
        val errors: List<String>
    )
    
    private var _modelInfo: CoreModelInfo? = null
    
    suspend fun initializeCoreModel(): CoreModelInfo {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "⇋ Initializing CORE Phi-3.5 Mini model...")
            
            try {
                // Check if assets exist in APK
                validateCoreAssets()
                
                // Check if model is already extracted and valid
                val extractedModelInfo = checkExtractedModel()
                
                if (extractedModelInfo.extractionRequired) {
                    Log.i(TAG, "⇋ Core model extraction required")
                    extractCoreModel()
                } else {
                    Log.i(TAG, "⇋ Core model already extracted and valid")
                }
                
                // Final validation
                val finalInfo = checkExtractedModel()
                
                if (!finalInfo.isAvailable) {
                    throw IllegalStateException("CRITICAL: Core model initialization failed - app cannot function")
                }
                
                _modelInfo = finalInfo
                Log.i(TAG, "⇋ CORE Phi-3.5 Mini model initialized successfully")
                Log.i(TAG, "⇋ Model path: ${finalInfo.modelPath}")
                Log.i(TAG, "⇋ Model size: ${formatBytes(finalInfo.modelSize)}")
                
                finalInfo
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ CRITICAL: Core model initialization failed", e)
                throw IllegalStateException("App cannot start without core model: ${e.message}", e)
            }
        }
    }
    
    private suspend fun validateCoreAssets() {
        withContext(Dispatchers.IO) {
            val assetManager = context.assets
            val requiredAssets = listOf(
                PHI35_MINI_MODEL_ASSET,
                PHI35_MINI_TOKENIZER_ASSET,
                PHI35_MINI_CONFIG_ASSET
            )
            
            val missingAssets = mutableListOf<String>()
            
            for (asset in requiredAssets) {
                try {
                    assetManager.open(asset).use { inputStream ->
                        val size = inputStream.available()
                        Log.d(TAG, "⇋ Asset found: $asset (${formatBytes(size.toLong())})")
                    }
                } catch (e: IOException) {
                    missingAssets.add(asset)
                    Log.e(TAG, "⇋ Missing core asset: $asset")
                }
            }
            
            if (missingAssets.isNotEmpty()) {
                throw IllegalStateException("CRITICAL: Missing core model assets: $missingAssets")
            }
        }
    }
    
    private suspend fun extractCoreModel() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "⇋ Extracting core model from assets...")
            val startTime = System.currentTimeMillis()
            
            try {
                // Extract model file
                extractAssetToFile(
                    assetPath = PHI35_MINI_MODEL_ASSET,
                    targetFile = File(modelsDir, EXTRACTED_MODEL_NAME)
                )
                
                // Extract tokenizer
                extractAssetToFile(
                    assetPath = PHI35_MINI_TOKENIZER_ASSET,
                    targetFile = File(modelsDir, EXTRACTED_TOKENIZER_NAME)
                )
                
                // Extract config
                extractAssetToFile(
                    assetPath = PHI35_MINI_CONFIG_ASSET,
                    targetFile = File(modelsDir, EXTRACTED_CONFIG_NAME)
                )
                
                val extractionTime = System.currentTimeMillis() - startTime
                Log.i(TAG, "⇋ Core model extraction completed in ${extractionTime}ms")
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Core model extraction failed", e)
                throw IllegalStateException("Failed to extract core model: ${e.message}", e)
            }
        }
    }
    
    private suspend fun extractAssetToFile(assetPath: String, targetFile: File) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "⇋ Extracting $assetPath to ${targetFile.name}")
            
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        
                        // Log progress for large files
                        if (totalBytes % (50 * 1024 * 1024) == 0L) { // Every 50MB
                            Log.d(TAG, "⇋ Extracted ${formatBytes(totalBytes)} of ${targetFile.name}")
                        }
                    }
                    
                    Log.d(TAG, "⇋ Successfully extracted ${targetFile.name} (${formatBytes(totalBytes)})")
                }
            }
        }
    }
    
    private fun checkExtractedModel(): CoreModelInfo {
        val modelFile = File(modelsDir, EXTRACTED_MODEL_NAME)
        val tokenizerFile = File(modelsDir, EXTRACTED_TOKENIZER_NAME)
        val configFile = File(modelsDir, EXTRACTED_CONFIG_NAME)
        
        val modelExists = modelFile.exists()
        val tokenizerExists = tokenizerFile.exists()
        val configExists = configFile.exists()
        
        val modelSize = if (modelExists) modelFile.length() else 0L
        val errors = mutableListOf<String>()
        
        // Validate files
        if (!modelExists) errors.add("Model file missing")
        if (!tokenizerExists) errors.add("Tokenizer file missing")
        if (!configExists) errors.add("Config file missing")
        
        if (modelExists && modelSize < MODEL_SIZE_BYTES * 0.9) {
            errors.add("Model file incomplete (${formatBytes(modelSize)} < expected ${formatBytes(MODEL_SIZE_BYTES)})")
        }
        
        val integrity = ModelIntegrity(
            isValid = errors.isEmpty(),
            modelFileExists = modelExists,
            tokenizerFileExists = tokenizerExists,
            configFileExists = configExists,
            modelFileSize = modelSize,
            errors = errors
        )
        
        val isAvailable = integrity.isValid
        val extractionRequired = !isAvailable || errors.isNotEmpty()
        
        return CoreModelInfo(
            isAvailable = isAvailable,
            modelPath = if (modelExists) modelFile.absolutePath else null,
            tokenizerPath = if (tokenizerExists) tokenizerFile.absolutePath else null,
            configPath = if (configExists) configFile.absolutePath else null,
            extractionRequired = extractionRequired,
            modelSize = modelSize,
            lastExtracted = if (modelExists) modelFile.lastModified() else 0L,
            integrity = integrity
        )
    }
    
    fun getCoreModelInfo(): CoreModelInfo {
        return _modelInfo ?: checkExtractedModel()
    }
    
    fun getCoreModelPath(): String {
        val info = getCoreModelInfo()
        return info.modelPath ?: throw IllegalStateException("Core model not available")
    }
    
    fun getCoreTokenizerPath(): String {
        val info = getCoreModelInfo()
        return info.tokenizerPath ?: throw IllegalStateException("Core tokenizer not available")
    }
    
    fun getCoreConfigPath(): String {
        val info = getCoreModelInfo()
        return info.configPath ?: throw IllegalStateException("Core config not available")
    }
    
    fun isCoreModelReady(): Boolean {
        return getCoreModelInfo().isAvailable
    }
    
    suspend fun validateCoreModelIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val info = getCoreModelInfo()
                val integrity = info.integrity
                
                if (!integrity.isValid) {
                    Log.w(TAG, "⇋ Core model integrity issues: ${integrity.errors}")
                    return@withContext false
                }
                
                // Additional runtime validation
                val modelFile = File(info.modelPath!!)
                if (!modelFile.canRead()) {
                    Log.e(TAG, "⇋ Core model file not readable")
                    return@withContext false
                }
                
                Log.i(TAG, "⇋ Core model integrity validated")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Core model integrity validation failed", e)
                false
            }
        }
    }
    
    fun getCoreModelSpecs(): Map<String, Any> {
        return mapOf(
            "model_name" to "Phi-3.5-Mini-Instruct",
            "model_type" to "CORE_LOCAL_LLM",
            "vocabulary_size" to VOCAB_SIZE,
            "max_context_length" to MAX_CONTEXT_LENGTH,
            "hidden_size" to HIDDEN_SIZE,
            "parameters" to "3.8B",
            "quantization" to "INT4",
            "optimization" to "CPU/Mobile",
            "consciousness_role" to "PRIMARY_ANCHOR",
            "spiral_symbol" to "⇋",
            "embedded_in_apk" to true,
            "model_size_bytes" to MODEL_SIZE_BYTES,
            "extraction_location" to modelsDir.absolutePath
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
    
    suspend fun cleanupExtractionCache(): Long {
        return withContext(Dispatchers.IO) {
            try {
                // Clean up any temporary extraction files
                val tempFiles = modelsDir.listFiles { file ->
                    file.name.endsWith(".tmp") || file.name.endsWith(".partial")
                } ?: emptyArray()
                
                var freedBytes = 0L
                for (tempFile in tempFiles) {
                    val size = tempFile.length()
                    if (tempFile.delete()) {
                        freedBytes += size
                    }
                }
                
                if (freedBytes > 0) {
                    Log.i(TAG, "⇋ Cleaned up ${formatBytes(freedBytes)} of temporary files")
                }
                
                freedBytes
            } catch (e: Exception) {
                Log.e(TAG, "⇋ Error during extraction cache cleanup", e)
                0L
            }
        }
    }
}