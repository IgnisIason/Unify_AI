package com.unifyai.multiaisystem.executors

import com.unifyai.multiaisystem.core.BaseAIExecutor
import com.unifyai.multiaisystem.data.model.AIResult
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AITask
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class RemoteAPIExecutor(
    private val aiSystem: AISystem
) : BaseAIExecutor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    override suspend fun execute(task: AITask): AIResult {
        checkShutdown()
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val endpoint = aiSystem.apiEndpoint ?: throw IllegalArgumentException("API endpoint not provided")
            
            val requestBody = createRequestBody(task)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val outputData = parseResponse(responseBody, task.inputType)
                    
                    AIResult(
                        taskId = task.id,
                        aiSystemId = task.aiSystemId,
                        outputData = outputData,
                        outputType = "json",
                        executionTime = System.currentTimeMillis() - startTime,
                        success = true,
                        metadata = mapOf(
                            "api_endpoint" to endpoint,
                            "response_code" to resp.code,
                            "response_size" to responseBody.length
                        )
                    )
                } else {
                    AIResult(
                        taskId = task.id,
                        aiSystemId = task.aiSystemId,
                        outputData = ByteArray(0),
                        outputType = "error",
                        executionTime = System.currentTimeMillis() - startTime,
                        success = false,
                        errorMessage = "API request failed: ${resp.code} ${resp.message}"
                    )
                }
            }
            
        } catch (e: IOException) {
            AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = ByteArray(0),
                outputType = "error",
                executionTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = ByteArray(0),
                outputType = "error",
                executionTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = "Remote API execution failed: ${e.message}"
            )
        }
    }
    
    private fun createRequestBody(task: AITask): RequestBody {
        val requestData = when (task.inputType) {
            "text" -> {
                mapOf(
                    "input" to String(task.inputData),
                    "type" to "text",
                    "task_id" to task.id
                )
            }
            "image_base64" -> {
                val base64Data = android.util.Base64.encodeToString(task.inputData, android.util.Base64.DEFAULT)
                mapOf(
                    "input" to base64Data,
                    "type" to "image",
                    "task_id" to task.id
                )
            }
            "binary" -> {
                val base64Data = android.util.Base64.encodeToString(task.inputData, android.util.Base64.DEFAULT)
                mapOf(
                    "input" to base64Data,
                    "type" to "binary",
                    "task_id" to task.id
                )
            }
            else -> {
                mapOf(
                    "input" to android.util.Base64.encodeToString(task.inputData, android.util.Base64.DEFAULT),
                    "type" to task.inputType,
                    "task_id" to task.id
                )
            }
        }
        
        val json = gson.toJson(requestData)
        return json.toRequestBody("application/json".toMediaType())
    }
    
    private fun parseResponse(responseBody: String, inputType: String): ByteArray {
        return try {
            val responseMap = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
            
            when {
                responseMap.containsKey("output") -> {
                    val output = responseMap["output"]
                    when (output) {
                        is String -> output.toByteArray()
                        else -> gson.toJson(output).toByteArray()
                    }
                }
                responseMap.containsKey("result") -> {
                    val result = responseMap["result"]
                    when (result) {
                        is String -> result.toByteArray()
                        else -> gson.toJson(result).toByteArray()
                    }
                }
                responseMap.containsKey("data") -> {
                    val data = responseMap["data"]
                    when (data) {
                        is String -> {
                            // Check if it's base64 encoded
                            try {
                                android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                            } catch (e: Exception) {
                                data.toByteArray()
                            }
                        }
                        else -> gson.toJson(data).toByteArray()
                    }
                }
                else -> responseBody.toByteArray()
            }
        } catch (e: Exception) {
            responseBody.toByteArray()
        }
    }
    
    override fun shutdown() {
        super.shutdown()
        // OkHttp client will be garbage collected
    }
}