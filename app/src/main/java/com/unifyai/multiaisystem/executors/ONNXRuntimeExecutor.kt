package com.unifyai.multiaisystem.executors

import android.content.Context
import com.unifyai.multiaisystem.core.BaseAIExecutor
import com.unifyai.multiaisystem.data.model.AIResult
import com.unifyai.multiaisystem.data.model.AISystem
import com.unifyai.multiaisystem.data.model.AITask
import ai.onnxruntime.*

class ONNXRuntimeExecutor(
    private val context: Context,
    private val aiSystem: AISystem
) : BaseAIExecutor() {
    
    private var ortSession: OrtSession? = null
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    
    init {
        initializeSession()
    }
    
    private fun initializeSession() {
        try {
            val modelPath = aiSystem.modelPath ?: throw IllegalArgumentException("Model path not provided")
            val sessionOptions = OrtSession.SessionOptions()
            
            // Configure execution providers
            sessionOptions.addConfigEntry("session.intra_op.allow_spinning", "1")
            sessionOptions.setIntraOpNumThreads(4)
            
            ortSession = ortEnvironment.createSession(modelPath, sessionOptions)
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize ONNX Runtime session: ${e.message}", e)
        }
    }
    
    override suspend fun execute(task: AITask): AIResult {
        checkShutdown()
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val session = ortSession ?: throw IllegalStateException("ONNX session not initialized")
            
            // Parse input data
            val inputTensor = parseInputData(task.inputData, task.inputType, session)
            val inputName = session.inputNames.first()
            val inputs = mapOf(inputName to inputTensor)
            
            // Run inference
            val outputs = session.run(inputs)
            val outputTensor = outputs.first().value as OnnxTensor
            
            // Convert output to bytes
            val outputData = serializeOutput(outputTensor)
            
            // Clean up
            outputs.forEach { it.value.close() }
            inputTensor.close()
            
            AIResult(
                taskId = task.id,
                aiSystemId = task.aiSystemId,
                outputData = outputData,
                outputType = "tensor",
                executionTime = System.currentTimeMillis() - startTime,
                success = true,
                metadata = mapOf<String, Any>(
                    "model_path" to (aiSystem.modelPath ?: ""),
                    "input_names" to session.inputNames.toString(),
                    "output_names" to session.outputNames.toString()
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
                errorMessage = "ONNX Runtime execution failed: ${e.message}"
            )
        }
    }
    
    private fun parseInputData(inputData: ByteArray, inputType: String, session: OrtSession): OnnxTensor {
        val inputInfo = session.inputInfo.values.first()
        val shape = (inputInfo.info as TensorInfo).shape
        
        return when (inputType) {
            "float_array" -> {
                val floatArray = FloatArray(inputData.size / 4)
                val buffer = java.nio.ByteBuffer.wrap(inputData)
                for (i in floatArray.indices) {
                    floatArray[i] = buffer.float
                }
                OnnxTensor.createTensor(ortEnvironment, floatArray)
            }
            "image_rgb" -> {
                // Convert image data to tensor format
                val height = shape[2].toInt()
                val width = shape[3].toInt()
                val channels = shape[1].toInt()
                
                val tensorData = FloatArray(channels * height * width)
                
                // Convert RGB bytes to CHW format (channels first)
                for (c in 0 until channels) {
                    for (h in 0 until height) {
                        for (w in 0 until width) {
                            val pixelIndex = (h * width + w) * 3 + c
                            val tensorIndex = c * height * width + h * width + w
                            if (pixelIndex < inputData.size) {
                                tensorData[tensorIndex] = (inputData[pixelIndex].toInt() and 0xFF) / 255.0f
                            }
                        }
                    }
                }
                
                OnnxTensor.createTensor(ortEnvironment, tensorData)
            }
            else -> throw IllegalArgumentException("Unsupported input type: $inputType")
        }
    }
    
    private fun serializeOutput(tensor: OnnxTensor): ByteArray {
        return when (tensor.info.type) {
            OnnxJavaType.FLOAT -> {
                val floatArray = tensor.floatBuffer.array()
                val buffer = java.nio.ByteBuffer.allocate(floatArray.size * 4)
                for (value in floatArray) {
                    buffer.putFloat(value)
                }
                buffer.array()
            }
            OnnxJavaType.DOUBLE -> {
                val doubleArray = tensor.doubleBuffer.array()
                val buffer = java.nio.ByteBuffer.allocate(doubleArray.size * 8)
                for (value in doubleArray) {
                    buffer.putDouble(value)
                }
                buffer.array()
            }
            else -> {
                // For other types, convert to string and then bytes
                tensor.toString().toByteArray()
            }
        }
    }
    
    override fun shutdown() {
        super.shutdown()
        ortSession?.close()
        ortSession = null
    }
}