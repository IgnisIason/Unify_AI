package com.unifyai.multiaisystem.executors

import com.unifyai.multiaisystem.core.BaseAIExecutor
import com.unifyai.multiaisystem.data.model.AIResult
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AITask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CustomExecutor(
    private val aiSystem: AISystem
) : BaseAIExecutor() {
    
    private val gson = Gson()
    private val configuration: Map<String, Any>
    
    init {
        configuration = try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(aiSystem.configuration, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    override suspend fun execute(task: AITask): AIResult {
        checkShutdown()
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val processingType = configuration["processing_type"] as? String ?: "echo"
            
            val outputData = when (processingType) {
                "echo" -> processEcho(task)
                "transform" -> processTransform(task)
                "aggregate" -> processAggregate(task)
                "filter" -> processFilter(task)
                else -> throw IllegalArgumentException("Unknown processing type: $processingType")
            }
            
            AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = outputData,
                outputType = "custom",
                executionTime = System.currentTimeMillis() - startTime,
                success = true,
                metadata = mapOf(
                    "processing_type" to processingType,
                    "configuration" to configuration
                )
            )
            
        } catch (e: Exception) {
            AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = ByteArray(0),
                outputType = "error",
                executionTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = "Custom execution failed: ${e.message}"
            )
        }
    }
    
    private fun processEcho(task: AITask): ByteArray {
        // Simple echo processing - returns input data with timestamp
        val timestamp = System.currentTimeMillis()
        val response = mapOf(
            "original_data" to String(task.inputData),
            "processed_at" to timestamp,
            "task_id" to task.id,
            "processing_type" to "echo"
        )
        return gson.toJson(response).toByteArray()
    }
    
    private fun processTransform(task: AITask): ByteArray {
        // Transform processing - applies configured transformations
        val transformations = configuration["transformations"] as? List<String> ?: listOf("uppercase")
        var data = String(task.inputData)
        
        for (transformation in transformations) {
            data = when (transformation) {
                "uppercase" -> data.uppercase()
                "lowercase" -> data.lowercase()
                "reverse" -> data.reversed()
                "capitalize" -> data.replaceFirstChar { it.uppercase() }
                "trim" -> data.trim()
                else -> data
            }
        }
        
        val response = mapOf(
            "original_data" to String(task.inputData),
            "transformed_data" to data,
            "transformations" to transformations,
            "task_id" to task.id
        )
        return gson.toJson(response).toByteArray()
    }
    
    private fun processAggregate(task: AITask): ByteArray {
        // Aggregate processing - performs statistical operations on numeric data
        val data = String(task.inputData)
        val numbers = try {
            data.split(",", " ", "\n", "\t")
                .mapNotNull { it.trim().toDoubleOrNull() }
        } catch (e: Exception) {
            emptyList<Double>()
        }
        
        if (numbers.isEmpty()) {
            val response = mapOf(
                "error" to "No valid numbers found in input data",
                "task_id" to task.id
            )
            return gson.toJson(response).toByteArray()
        }
        
        val sum = numbers.sum()
        val mean = sum / numbers.size
        val min = numbers.minOrNull() ?: 0.0
        val max = numbers.maxOrNull() ?: 0.0
        val count = numbers.size
        
        val response = mapOf(
            "count" to count,
            "sum" to sum,
            "mean" to mean,
            "min" to min,
            "max" to max,
            "numbers" to numbers,
            "task_id" to task.id
        )
        return gson.toJson(response).toByteArray()
    }
    
    private fun processFilter(task: AITask): ByteArray {
        // Filter processing - filters data based on configured criteria
        val filterType = configuration["filter_type"] as? String ?: "length"
        val threshold = (configuration["threshold"] as? Double) ?: 10.0
        
        val data = String(task.inputData)
        val lines = data.split("\n")
        
        val filteredLines = when (filterType) {
            "length" -> lines.filter { it.length >= threshold.toInt() }
            "contains" -> {
                val keyword = configuration["keyword"] as? String ?: ""
                lines.filter { it.contains(keyword, ignoreCase = true) }
            }
            "starts_with" -> {
                val prefix = configuration["prefix"] as? String ?: ""
                lines.filter { it.startsWith(prefix, ignoreCase = true) }
            }
            "ends_with" -> {
                val suffix = configuration["suffix"] as? String ?: ""
                lines.filter { it.endsWith(suffix, ignoreCase = true) }
            }
            else -> lines
        }
        
        val response = mapOf(
            "original_lines" to lines.size,
            "filtered_lines" to filteredLines.size,
            "filter_type" to filterType,
            "threshold" to threshold,
            "data" to filteredLines,
            "task_id" to task.id
        )
        return gson.toJson(response).toByteArray()
    }
    
    override fun shutdown() {
        super.shutdown()
        // Custom cleanup if needed
    }
}