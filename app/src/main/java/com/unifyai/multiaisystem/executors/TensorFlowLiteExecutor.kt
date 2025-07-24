package com.unifyai.multiaisystem.executors

import android.content.Context
import com.unifyai.multiaisystem.core.BaseAIExecutor
import com.unifyai.multiaisystem.data.model.AIResult
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AITask

class TensorFlowLiteExecutor(
    private val context: Context,
    private val aiSystem: AISystem
) : BaseAIExecutor() {
    
    override suspend fun execute(task: AITask): AIResult {
        // TODO: Implement TensorFlow Lite execution once dependency issues are resolved
        return AIResult(
            taskId = task.id,
            aiSystemId = task.aiSystemId,
            outputData = "TensorFlow Lite executor not yet implemented due to dependency issues".toByteArray(),
            outputType = "error",
            executionTime = 0,
            success = false,
            errorMessage = "TensorFlow Lite executor requires proper dependency resolution"
        )
    }
    
    override fun shutdown() {
        // No cleanup needed for stub implementation
    }
}