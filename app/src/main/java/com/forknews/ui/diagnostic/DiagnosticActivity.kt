package com.forknews.ui.diagnostic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.forknews.databinding.ActivityDiagnosticBinding
import com.forknews.utils.DiagnosticLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDiagnosticBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Инициализируем DiagnosticLogger
        DiagnosticLogger.init(applicationContext)
        
        setupToolbar()
        loadLogs()
        setupButtons()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем логи при возврате на экран
        loadLogs()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Диагностика"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun loadLogs() {
        val logs = DiagnosticLogger.getAllLogs()
        binding.tvLogs.text = logs
        
        // Автопрокрутка вниз к последним логам
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
    
    private fun setupButtons() {
        binding.btnCopyLogs.setOnClickListener {
            copyLogsToClipboard()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadLogs()
            Toast.makeText(this, "Логи обновлены", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnShare.setOnClickListener {
            shareLogsAsFile()
        }
        
        binding.btnClear.setOnClickListener {
            DiagnosticLogger.clear()
            loadLogs()
            Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun copyLogsToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val logs = DiagnosticLogger.getAllLogs()
        val clip = ClipData.newPlainText("ForkNews Logs", logs)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Логи скопированы в буфер обмена", Toast.LENGTH_SHORT).show()
    }
    
    private fun shareLogsAsFile() {
        try {
            val logs = DiagnosticLogger.getAllLogs()
            
            // Создаём временный файл в кэше
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "forknews_logs_$timestamp.txt"
            val file = File(cacheDir, fileName)
            
            file.writeText(logs)
            
            // Создаём URI через FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            // Создаём Intent для отправки
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ForkNews Diagnostic Logs")
                putExtra(Intent.EXTRA_TEXT, "Логи диагностики ForkNews")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Поделиться логами"))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка создания файла: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
