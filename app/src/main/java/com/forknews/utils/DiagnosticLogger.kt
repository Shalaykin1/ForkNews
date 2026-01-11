package com.forknews.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object DiagnosticLogger {
    private val logs = mutableListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private const val MAX_LOGS = 500
    private var logFile: File? = null
    
    fun init(context: Context) {
        if (logFile == null) {
            logFile = File(context.filesDir, "forknews_diagnostic.log")
            loadLogsFromFile()
        }
    }
    
    private fun loadLogsFromFile() {
        try {
            if (logFile?.exists() == true) {
                val fileContent = logFile?.readText() ?: ""
                val lines = fileContent.lines().takeLast(MAX_LOGS)
                synchronized(logs) {
                    logs.clear()
                    logs.addAll(lines.filter { it.isNotBlank() })
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DiagnosticLogger", "Ошибка загрузки логов из файла: ${e.message}")
        }
    }
    
    private fun saveLogToFile(logEntry: String) {
        try {
            logFile?.appendText("$logEntry\n")
            // Ограничиваем размер файла
            if (logFile?.length() ?: 0 > 1024 * 1024) { // 1MB
                val allLines = logFile?.readLines() ?: emptyList()
                val trimmedLines = allLines.takeLast(MAX_LOGS)
                logFile?.writeText(trimmedLines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            android.util.Log.e("DiagnosticLogger", "Ошибка записи в файл: ${e.message}")
        }
    }
    
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $tag: $message"
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) {
                logs.removeAt(0)
            }
        }
        saveLogToFile(logEntry)
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
        saveLogToFile(logEntry)
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
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            android.util.Log.e("DiagnosticLogger", "Ошибка очистки файла логов: ${e.message}")
        }
    }
}
