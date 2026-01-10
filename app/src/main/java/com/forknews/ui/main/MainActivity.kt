package com.forknews.ui.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.forknews.R
import com.forknews.data.local.AppDatabase
import com.forknews.data.repository.RepositoryRepository
import com.forknews.databinding.ActivityMainBinding
import com.forknews.ui.settings.SettingsActivity
import com.forknews.utils.DiagnosticLogger
import com.forknews.utils.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RepositoryAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var autoRefreshRunnable: Runnable? = null
    private var timeLogRunnable: Runnable? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Уведомления разрешены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val viewModel: MainViewModel by viewModels {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "forknews_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
        val repository = RepositoryRepository(database.repositoryDao())
        MainViewModelFactory(repository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        DiagnosticLogger.log("MainActivity", "=== ЗАПУСК ПРИЛОЖЕНИЯ ===")
        DiagnosticLogger.log("MainActivity", "onCreate вызван")
        
        // Apply theme
        PreferencesManager.init(this)
        PreferencesManager.applyTheme()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeData()
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
        
        // Инициализируем предустановленные репозитории при первом запуске
        DiagnosticLogger.log("MainActivity", "Вызов initDefaultRepositoriesIfNeeded()")
        initDefaultRepositoriesIfNeeded()
        
        // Запустить фоновую проверку обновлений
        lifecycleScope.launch {
            val interval = PreferencesManager.getCheckInterval().first()
            DiagnosticLogger.log("MainActivity", "Запуск WorkManager с интервалом: $interval мин")
            com.forknews.workers.UpdateCheckWorker.schedulePeriodicWork(this@MainActivity, interval)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Запустить автообновление каждые 60 секунд когда приложение открыто
        startAutoRefresh()
        // Запустить таймер логирования времени
        startTimeLogger()
    }
    
    override fun onPause() {
        super.onPause()
        // Остановить автообновление когда приложение свернуто
        stopAutoRefresh()
        // Остановить таймер логирования
        stopTimeLogger()
    }
    
    private fun startAutoRefresh() {
        stopAutoRefresh() // Остановить предыдущий, если был
        autoRefreshRunnable = object : Runnable {
            override fun run() {
                viewModel.refreshAll()
                handler.postDelayed(this, 300_000) // 5 минут
            }
        }
        handler.postDelayed(autoRefreshRunnable!!, 300_000)
    }
    
    private fun stopAutoRefresh() {
        autoRefreshRunnable?.let { handler.removeCallbacks(it) }
        autoRefreshRunnable = null
    }
    
    private fun startTimeLogger() {
        stopTimeLogger() // Остановить предыдущий, если был
        timeLogRunnable = object : Runnable {
            override fun run() {
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                DiagnosticLogger.log("TimeLogger", "Текущее время: $currentTime")
                handler.postDelayed(this, 10_000) // 10 секунд
            }
        }
        handler.post(timeLogRunnable!!)
    }
    
    private fun stopTimeLogger() {
        timeLogRunnable?.let { handler.removeCallbacks(it) }
        timeLogRunnable = null
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                showAddRepositoryDialog()
                true
            }
            R.id.action_diagnostic -> {
                showDiagnosticDialog()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                DiagnosticLogger.log("MainActivity", "Запрос отключения оптимизации батареи")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Фоновая работа")
                    .setMessage("Для надёжной работы уведомлений в фоне рекомендуется отключить оптимизацию батареи для ForkNews.\n\nЭто позволит приложению проверять обновления даже в фоновом режиме.")
                    .setPositiveButton("Настроить") { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                            DiagnosticLogger.log("MainActivity", "Открыт экран настроек батареи")
                        } catch (e: Exception) {
                            DiagnosticLogger.error("MainActivity", "Ошибка открытия настроек батареи: ${e.message}", e)
                            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Позже", null)
                    .show()
            } else {
                DiagnosticLogger.log("MainActivity", "Оптимизация батареи уже отключена")
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale and request permission
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Разрешение на уведомления")
                        .setMessage("ForkNews нужны уведомления для информирования о новых релизах")
                        .setPositiveButton("Разрешить") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupRecyclerView() {
        adapter = RepositoryAdapter(
            onItemClick = { repository ->
                if (repository.latestReleaseUrl != null) {
                    viewModel.markReleaseAsViewed(repository.id)
                    openUrl(repository.latestReleaseUrl)
                } else {
                    Toast.makeText(this, "Релиз еще не загружен", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // Swipe to delete and drag to reorder
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                
                // Создаём новый список с обновлёнными позициями
                val currentList = adapter.currentList.toMutableList()
                val item = currentList.removeAt(fromPosition)
                currentList.add(toPosition, item)
                
                // Обновляем адаптер для немедленной визуализации
                adapter.submitList(currentList)
                
                // Сохраняем новые позиции в БД
                viewModel.moveRepository(fromPosition, toPosition, currentList)
                
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val repository = adapter.currentList[position]
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Удалить репозиторий?")
                    .setMessage("Вы действительно хотите удалить ${repository.name}?")
                    .setPositiveButton("Удалить") { _, _ ->
                        viewModel.deleteRepository(repository)
                    }
                    .setNegativeButton("Отмена") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAll()
        }
        
        lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefresh.isRefreshing = isRefreshing
            }
        }
    }
    
    private fun observeData() {
        lifecycleScope.launch {
            viewModel.repositories.collect { repositories ->
                adapter.submitList(repositories)
            }
        }
    }
    
    private fun showAddRepositoryDialog() {
        showGitHubUrlDialog()
    }
    
    private fun showGitHubUrlDialog() {
        val input = TextInputEditText(this)
        input.hint = "https://github.com/owner/repo"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить GitHub репозиторий")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val url = input.text.toString()
                if (url.isNotEmpty()) {
                    viewModel.addRepository(url)
                    Toast.makeText(this, "Репозиторий добавлен", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showDiagnosticDialog() {
        val logs = DiagnosticLogger.getAllLogs()
        val scrollView = android.widget.ScrollView(this).apply {
            setPadding(48, 24, 48, 24)
        }
        val textView = android.widget.TextView(this).apply {
            text = logs
            textSize = 12f
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(textView)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Диагностика (${logs.lines().size} строк)")
            .setView(scrollView)
            .setPositiveButton("Скопировать") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ForkNews Diagnostic Log", logs)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Лог скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Поделиться") { _, _ ->
                shareLogFile(logs)
            }
            .setNegativeButton("Очистить") { _, _ ->
                DiagnosticLogger.clear()
                Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun shareLogFile(logs: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("HH-mm-ss_yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "log_$timestamp.txt"
            val file = java.io.File(cacheDir, fileName)
            file.writeText(logs)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ForkNews Diagnostic Log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Поделиться логом"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при создании файла: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initDefaultRepositoriesIfNeeded() {
        lifecycleScope.launch {
            try {
                DiagnosticLogger.log("MainActivity", "initDefaultRepositoriesIfNeeded: начало")
                val existingRepos = viewModel.repository.getAllRepositoriesList()
                DiagnosticLogger.log("MainActivity", "Найдено существующих репозиториев: ${existingRepos.size}")
                
                // Если репозиториев меньше 8, добавляем недостающие
                if (existingRepos.size < 8) {
                    DiagnosticLogger.log("MainActivity", "Требуется добавить репозитории")
                    // Список репозиториев для добавления
                    val defaultRepos = listOf(
                        com.forknews.data.model.Repository(
                            name = "AdrenoToolsDrivers",
                            owner = "K11MCH1",
                            url = "https://github.com/K11MCH1/AdrenoToolsDrivers",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "winlator",
                            owner = "coffincolors",
                            url = "https://github.com/coffincolors/winlator",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "Winlator-Ludashi",
                            owner = "StevenMXZ",
                            url = "https://github.com/StevenMXZ/Winlator-Ludashi",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "winlator",
                            owner = "brunodev85",
                            url = "https://github.com/brunodev85/winlator",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "ForkNews",
                            owner = "Shalaykin1",
                            url = "https://github.com/Shalaykin1/ForkNews",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "purple-turnip",
                            owner = "MrPurple666",
                            url = "https://github.com/MrPurple666/purple-turnip",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "Releases",
                            owner = "eden-emulator",
                            url = "https://github.com/eden-emulator/Releases",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        ),
                        com.forknews.data.model.Repository(
                            name = "aps3e",
                            owner = "aenu1",
                            url = "https://github.com/aenu1/aps3e",
                            type = com.forknews.data.model.RepositoryType.GITHUB,
                            notificationsEnabled = true
                        )
                    )
                    
                    // Добавляем только те репозитории, которых еще нет
                    // И СРАЗУ загружаем релизы для каждого (синхронно)
                    for (repo in defaultRepos) {
                        val exists = existingRepos.any { it.url == repo.url }
                        if (!exists) {
                            DiagnosticLogger.log("MainActivity", "Добавляем репозиторий: ${repo.owner}/${repo.name}")
                            val repoId = viewModel.repository.addRepository(repo)
                            // ВАЖНО: Синхронно загружаем релиз для каждого репозитория
                            val addedRepo = viewModel.repository.getRepositoryById(repoId)
                            if (addedRepo != null) {
                                DiagnosticLogger.log("MainActivity", "Загружаем релиз для: ${addedRepo.owner}/${addedRepo.name}")
                                val updated = viewModel.repository.checkForUpdates(addedRepo)
                                DiagnosticLogger.log("MainActivity", "Релиз загружен для ${addedRepo.owner}/${addedRepo.name}: updated=$updated")
                            } else {
                                DiagnosticLogger.error("MainActivity", "Не удалось получить репозиторий с ID: $repoId")
                            }
                        }
                    }
                    DiagnosticLogger.log("MainActivity", "initDefaultRepositoriesIfNeeded: все репозитории обработаны")
                } else {
                    DiagnosticLogger.log("MainActivity", "Репозитории уже инициализированы (${existingRepos.size})")
                }
            } catch (e: Exception) {
                DiagnosticLogger.error("MainActivity", "Ошибка в initDefaultRepositoriesIfNeeded: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
}
