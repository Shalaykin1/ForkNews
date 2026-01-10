package com.forknews.utils

import java.text.SimpleDateFormat
import java.util.*

object DiagnosticLogger {
    private val logs = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private const val MAX_LOGS = 500
    
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $tag: $message"
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
        }
        android.util.Log.d(tag, message)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = if (throwable != null) {
            "[$timestamp] ERROR $tag: $message\n${throwable.stackTraceToString()}"
        } else {
            "[$timestamp] ERROR $tag: $message"
        }
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
        }
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable)
        } else {
            android.util.Log.e(tag, message)
        }
    }
    
    fun getAllLogs(): String {
        return synchronized(logs) {
            if (logs.isEmpty()) {
                "Логов пока нет"
            } else {
                logs.joinToString("\n")
            }
        }
    }
    
    fun clear() {
        synchronized(logs) {
            logs.clear()
        }
    }
}
