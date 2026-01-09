package com.forknews.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.forknews.R
import com.forknews.databinding.ActivitySettingsBinding
import com.forknews.utils.PreferencesManager
import com.forknews.workers.UpdateCheckWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupCheckInterval()
        setupThemeSettings()
        setupNotifications()
        setupCustomTime()
        
        loadSettings()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupCheckInterval() {
        val intervals = arrayOf(
            "30 минут",
            "1 час",
            "2 часа",
            "6 часов",
            "12 часов",
            "Пользовательское время"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCheckInterval.adapter = adapter
        
        binding.spinnerCheckInterval.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    lifecycleScope.launch {
                        val minutes = when (position) {
                            0 -> 30L
                            1 -> 60L
                            2 -> 120L
                            3 -> 360L
                            4 -> 720L
                            5 -> {
                                binding.layoutCustomTime.visibility = android.view.View.VISIBLE
                                PreferencesManager.setCustomTimeEnabled(true)
                                -1L
                            }
                            else -> 60L
                        }
                        
                        if (position != 5) {
                            binding.layoutCustomTime.visibility = android.view.View.GONE
                            PreferencesManager.setCustomTimeEnabled(false)
                        }
                        
                        if (minutes > 0) {
                            PreferencesManager.setCheckInterval(minutes)
                            UpdateCheckWorker.schedulePeriodicWork(this@SettingsActivity, minutes)
                        }
                    }
                }
                
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        )
    }
    
    private fun setupThemeSettings() {
        binding.switchSystemTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setUseSystemTheme(isChecked)
                binding.layoutManualTheme.visibility = if (isChecked) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
                PreferencesManager.applyTheme()
            }
        }
        
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setDarkTheme(isChecked)
                PreferencesManager.applyTheme()
            }
        }
    }
    
    private fun setupNotifications() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setNotificationsEnabled(isChecked)
            }
        }
    }
    
    private fun setupCustomTime() {
        binding.btnSelectTime.setOnClickListener {
            lifecycleScope.launch {
                val hour = PreferencesManager.getCustomTimeHour().first()
                val minute = PreferencesManager.getCustomTimeMinute().first()
                
                TimePickerDialog(
                    this@SettingsActivity,
                    { _, selectedHour, selectedMinute ->
                        lifecycleScope.launch {
                            PreferencesManager.setCustomTime(selectedHour, selectedMinute)
                            binding.tvSelectedTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
                            UpdateCheckWorker.scheduleCustomTimeWork(
                                this@SettingsActivity,
                                selectedHour,
                                selectedMinute
                            )
                        }
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            // Load check interval
            val checkInterval = PreferencesManager.getCheckInterval().first()
            val position = when (checkInterval) {
                30L -> 0
                60L -> 1
                120L -> 2
                360L -> 3
                720L -> 4
                else -> 1
            }
            binding.spinnerCheckInterval.setSelection(position)
            
            // Load custom time
            val customTimeEnabled = PreferencesManager.getCustomTimeEnabled().first()
            if (customTimeEnabled) {
                binding.spinnerCheckInterval.setSelection(5)
                binding.layoutCustomTime.visibility = android.view.View.VISIBLE
                val hour = PreferencesManager.getCustomTimeHour().first()
                val minute = PreferencesManager.getCustomTimeMinute().first()
                binding.tvSelectedTime.text = String.format("%02d:%02d", hour, minute)
            }
            
            // Load theme settings
            val useSystemTheme = PreferencesManager.getUseSystemTheme().first()
            binding.switchSystemTheme.isChecked = useSystemTheme
            binding.layoutManualTheme.visibility = if (useSystemTheme) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            
            val darkTheme = PreferencesManager.getDarkTheme().first()
            binding.switchDarkTheme.isChecked = darkTheme
            
            // Load notifications
            val notificationsEnabled = PreferencesManager.getNotificationsEnabled().first()
            binding.switchNotifications.isChecked = notificationsEnabled
        }
    }
}
