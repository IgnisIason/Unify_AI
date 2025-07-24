package com.unifyai.multiaisystem.core

import com.unifyai.multiaisystem.data.model.AIResult
import com.unifyai.multiaisystem.data.model.AITask

interface AIExecutor {
    suspend fun execute(task: AITask): AIResult
    fun shutdown()
}

abstract class BaseAIExecutor : AIExecutor {
    protected var isShutdown = false
    
    override fun shutdown() {
        isShutdown = true
    }
    
    protected fun checkShutdown() {
        if (isShutdown) {
            throw IllegalStateException("Executor has been shutdown")
        }
    }
}