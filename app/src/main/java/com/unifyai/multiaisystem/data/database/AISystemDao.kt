package com.unifyai.multiaisystem.data.database

import androidx.room.*
import com.unifyai.multiaisystem.data.model.AISystem
import kotlinx.coroutines.flow.Flow

@Dao
interface AISystemDao {
    
    @Query("SELECT * FROM ai_systems")
    fun getAllAISystems(): Flow<List<AISystem>>
    
    @Query("SELECT * FROM ai_systems WHERE isActive = 1")
    fun getActiveAISystems(): Flow<List<AISystem>>
    
    @Query("SELECT * FROM ai_systems WHERE id = :id")
    suspend fun getAISystemById(id: String): AISystem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAISystem(aiSystem: AISystem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAISystems(aiSystems: List<AISystem>)
    
    @Update
    suspend fun updateAISystem(aiSystem: AISystem)
    
    @Delete
    suspend fun deleteAISystem(aiSystem: AISystem)
    
    @Query("UPDATE ai_systems SET isActive = :isActive WHERE id = :id")
    suspend fun updateAISystemStatus(id: String, isActive: Boolean)
    
    @Query("UPDATE ai_systems SET lastExecutionTime = :timestamp, totalExecutions = totalExecutions + 1 WHERE id = :id")
    suspend fun updateExecutionStats(id: String, timestamp: Long)
    
    @Query("UPDATE ai_systems SET averageExecutionTime = :avgTime WHERE id = :id")
    suspend fun updateAverageExecutionTime(id: String, avgTime: Double)
    
    @Query("UPDATE ai_systems SET errorCount = errorCount + 1 WHERE id = :id")
    suspend fun incrementErrorCount(id: String)
    
    @Query("DELETE FROM ai_systems WHERE id = :id")
    suspend fun deleteAISystemById(id: String)
}