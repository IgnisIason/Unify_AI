package com.unifyai.multiaisystem.utils

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor() {
    
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    fun error(tag: String, message: String) {
        Log.e(tag, message)
    }
    
    fun error(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
    
    fun verbose(tag: String, message: String) {
        Log.v(tag, message)
    }
    
    fun wtf(tag: String, message: String) {
        Log.wtf(tag, message)
    }
    
    fun isDebugEnabled(): Boolean {
        return Log.isLoggable("DEBUG", Log.DEBUG)
    }
}