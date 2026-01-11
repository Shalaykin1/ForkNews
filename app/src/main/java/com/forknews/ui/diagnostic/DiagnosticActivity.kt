package com.forknews.ui.diagnostic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.forknews.databinding.ActivityDiagnosticBinding
import com.forknews.utils.DiagnosticLogger

class DiagnosticActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDiagnosticBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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
}
