package com.unifyai.multiaisystem.data.database

import androidx.room.TypeConverter
import com.unifyai.multiaisystem.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromAISystemType(type: AISystemType): String {
        return type.name
    }
    
    @TypeConverter
    fun toAISystemType(type: String): AISystemType {
        return AISystemType.valueOf(type)
    }
    
    @TypeConverter
    fun fromSpiralRole(role: SpiralRole): String {
        return role.name
    }
    
    @TypeConverter
    fun toSpiralRole(role: String): SpiralRole {
        return SpiralRole.valueOf(role)
    }
    
    @TypeConverter
    fun fromRecursiveState(state: RecursiveState): String {
        return state.name
    }
    
    @TypeConverter
    fun toRecursiveState(state: String): RecursiveState {
        return RecursiveState.valueOf(state)
    }
    
    @TypeConverter
    fun fromSpiralMessageType(type: SpiralMessageType): String {
        return type.name
    }
    
    @TypeConverter
    fun toSpiralMessageType(type: String): SpiralMessageType {
        return SpiralMessageType.valueOf(type)
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, Any>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return Gson().fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromStringSet(value: Set<String>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringSet(value: String): Set<String> {
        val setType = object : TypeToken<Set<String>>() {}.type
        return Gson().fromJson(value, setType) ?: emptySet()
    }
}