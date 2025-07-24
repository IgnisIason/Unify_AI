package com.unifyai.multiaisystem.core

import android.content.Context
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AISystemType
import com.unifyai.multiaisystem.executors.*
import com.unifyai.multiaisystem.models.ModelDownloadManager
import com.unifyai.multiaisystem.models.Phi35MiniTokenizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIExecutorFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
    private val tokenizer: Phi35MiniTokenizer
) {
    
    fun createExecutor(aiSystem: AISystem): AIExecutor {
        return when (aiSystem.type) {
            AISystemType.TENSORFLOW_LITE -> {
                TensorFlowLiteExecutor(context, aiSystem)
            }
            AISystemType.ONNX_RUNTIME -> {
                ONNXRuntimeExecutor(context, aiSystem)
            }
            AISystemType.REMOTE_API -> {
                RemoteAPIExecutor(aiSystem)
            }
            AISystemType.CUSTOM -> {
                CustomExecutor(aiSystem)
            }
            AISystemType.LOCAL_LLM -> {
                LocalLLMExecutor(context, aiSystem, modelDownloadManager, tokenizer)
            }
        }
    }
}