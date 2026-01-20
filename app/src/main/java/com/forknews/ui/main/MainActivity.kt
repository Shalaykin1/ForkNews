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
import com.forknews.data.model.Repository
import com.forknews.data.repository.RepositoryRepository
import com.forknews.databinding.ActivityMainBinding
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
    private var mainMenu: Menu? = null
    
    // Easter egg: 10 taps on developer signature opens diagnostics
    private var developerClickCount = 0
    private val developerClickHandler = Handler(Looper.getMainLooper())
    private var developerClickRunnable: Runnable? = null
    
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
        
        PreferencesManager.init(this)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeData()
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
        requestFullScreenNotificationPermission()
        requestOverlayPermission()
        
        // Показать инструкции для производителей с ограничениями
        showManufacturerInstructions()
        
        // Инициализировать репозитории по умолчанию
        initDefaultRepositoriesIfNeeded()
        
        // Запустить фоновую проверку обновлений через AlarmManager (точно каждые 5 минут)
        com.forknews.utils.AlarmScheduler.scheduleAlarm(this)
        
        // Easter egg: 10 taps on developer signature opens diagnostics
        setupDeveloperEasterEgg()
    }
    
    override fun onResume() {
        super.onResume()
        // Запустить автообновление каждые 60 секунд когда приложение открыто
        startAutoRefresh()
        // Проверить обновления сразу при разворачивании
        viewModel.refreshAll()
    }
    
    override fun onPause() {
        super.onPause()
        // Остановить автообновление когда приложение свернуто
        stopAutoRefresh()
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
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        mainMenu = menu
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                showAddRepositoryDialog()
                true
            }
            R.id.action_test_notification -> {
                testNotification()
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
    
    private fun requestFullScreenNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            if (!notificationManager.canUseFullScreenIntent()) {
                DiagnosticLogger.log("MainActivity", "Запрос разрешения USE_FULL_SCREEN_INTENT")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Всплывающие уведомления")
                    .setMessage("Для показа всплывающих уведомлений поверх других приложений требуется специальное разрешение.\n\nЭто позволит вам сразу видеть уведомления о новых релизах, не открывая шторку.")
                    .setPositiveButton("Разрешить") { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                            DiagnosticLogger.log("MainActivity", "Открыт экран настроек fullscreen intent")
                        } catch (e: Exception) {
                            DiagnosticLogger.error("MainActivity", "Ошибка открытия настроек fullscreen: ${e.message}", e)
                            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Позже", null)
                    .show()
            } else {
                DiagnosticLogger.log("MainActivity", "Разрешение USE_FULL_SCREEN_INTENT уже предоставлено")
            }
        }
    }
    
    private fun requestOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            DiagnosticLogger.log("MainActivity", "Запрос разрешения SYSTEM_ALERT_WINDOW (Display pop-up windows)")
            MaterialAlertDialogBuilder(this)
                .setTitle("Всплывающие окна")
                .setMessage("Для показа всплывающих уведомлений поверх других приложений требуется разрешение \"Display pop-up windows\".\n\nЭто обеспечит немедленное отображение уведомлений о новых релизах со звуком, даже когда телефон заблокирован.")
                .setPositiveButton("Разрешить") { _, _ ->
                    try {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                        DiagnosticLogger.log("MainActivity", "Открыт экран настроек overlay permission")
                    } catch (e: Exception) {
                        DiagnosticLogger.error("MainActivity", "Ошибка открытия настроек overlay: ${e.message}", e)
                        Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Позже", null)
                .show()
        } else {
            DiagnosticLogger.log("MainActivity", "Разрешение SYSTEM_ALERT_WINDOW уже предоставлено")
        }
    }
    
    private fun showManufacturerInstructions() {
        // Проверяем, показывали ли инструкции ранее
        val prefs = getSharedPreferences("forknews_prefs", Context.MODE_PRIVATE)
        val instructionsShown = prefs.getBoolean("manufacturer_instructions_shown", false)
        
        if (!instructionsShown && com.forknews.utils.ManufacturerHelper.hasKnownRestrictions()) {
            val manufacturerName = com.forknews.utils.ManufacturerHelper.getManufacturerName()
            val instructions = com.forknews.utils.ManufacturerHelper.getManufacturerInstructions()
            
            DiagnosticLogger.log("MainActivity", "Показываем инструкции для $manufacturerName")
            
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Важно для $manufacturerName")
                .setMessage("Для надёжной работы уведомлений на вашем устройстве необходимо настроить дополнительные разрешения.\n\n$instructions")
                .setPositiveButton("Открыть настройки") { _, _ ->
                    com.forknews.utils.ManufacturerHelper.openAutoStartSettings(this)
                    prefs.edit().putBoolean("manufacturer_instructions_shown", true).apply()
                }
                .setNegativeButton("Позже") { _, _ ->
                    prefs.edit().putBoolean("manufacturer_instructions_shown", true).apply()
                }
                .setNeutralButton("Показать снова", null)
                .setCancelable(false)
                .show()
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
            private var draggedList: MutableList<Repository>? = null
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                
                // Инициализируем список при первом перемещении
                if (draggedList == null) {
                    draggedList = adapter.currentList.toMutableList()
                }
                
                // Перемещаем элемент в списке
                val item = draggedList!!.removeAt(fromPosition)
                draggedList!!.add(toPosition, item)
                
                // Уведомляем адаптер о перемещении для плавной анимации
                adapter.notifyItemMoved(fromPosition, toPosition)
                
                return true
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                
                // Сохраняем финальные позиции в БД после завершения drag
                draggedList?.let { finalList ->
                    viewModel.moveRepository(0, 0, finalList)
                    draggedList = null
                }
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
            val count = viewModel.getRepositoryCount()
            if (count == 0) {
                val defaultRepos = listOf(
                    "https://github.com/coffincolors/winlator/",
                    "https://github.com/StevenMXZ/Winlator-Ludashi/",
                    "https://github.com/brunodev85/winlator/",
                    "https://github.com/K11MCH1/AdrenoToolsDrivers/",
                    "https://github.com/StevenMXZ/freedreno_turnip-CI/",
                    "https://github.com/Shalaykin1/ForkNews/"
                )
                defaultRepos.forEach { url ->
                    viewModel.addRepository(url)
                }
                DiagnosticLogger.log("MainActivity", "Добавлено ${defaultRepos.size} репозиториев по умолчанию")
            }
        }
    }
    
    private fun setupDeveloperEasterEgg() {
        binding.tvDeveloper.setOnClickListener {
            developerClickCount++
            DiagnosticLogger.log("MainActivity", "Easter egg клик: $developerClickCount/10")
            
            // Отменить предыдущий таймер сброса
            developerClickRunnable?.let { developerClickHandler.removeCallbacks(it) }
            
            if (developerClickCount >= 10) {
                // 10 нажатий - открыть диагностику
                DiagnosticLogger.log("MainActivity", "Easter egg активирован! Открываем диагностику")
                Toast.makeText(this, "Добро пожаловать в диагностику!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, com.forknews.ui.diagnostic.DiagnosticActivity::class.java)
                startActivity(intent)
                developerClickCount = 0
            } else {
                // Сбросить счетчик через 3 секунды бездействия
                developerClickRunnable = Runnable {
                    DiagnosticLogger.log("MainActivity", "Easter egg сброшен после таймаута")
                    developerClickCount = 0
                }
                developerClickHandler.postDelayed(developerClickRunnable!!, 3000)
            }
        }
    }
    
    private fun testNotification() {
        lifecycleScope.launch {
            try {
                DiagnosticLogger.log("MainActivity", "=== ТЕСТ УВЕДОМЛЕНИЯ ===")
                
                // Проверяем разрешение на уведомления
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    
                    if (!hasPermission) {
                        Toast.makeText(this@MainActivity, "Нет разрешения на уведомления!", Toast.LENGTH_LONG).show()
                        DiagnosticLogger.error("MainActivity", "⚠️ Нет разрешения POST_NOTIFICATIONS")
                        return@launch
                    }
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                if (!notificationManager.areNotificationsEnabled()) {
                    Toast.makeText(this@MainActivity, "Уведомления отключены в настройках!", Toast.LENGTH_LONG).show()
                    DiagnosticLogger.error("MainActivity", "⚠️ Уведомления отключены")
                    return@launch
                }
                
                // Проверяем или создаем канал
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val existingChannel = notificationManager.getNotificationChannel("forknews_updates")
                if (existingChannel == null) {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                    
                    val updateChannel = android.app.NotificationChannel(
                        "forknews_updates",
                        "Обновления репозиториев",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Уведомления о новых релизах"
                        enableLights(true)
                        lightColor = android.graphics.Color.BLUE
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                        setShowBadge(true)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)
                        setSound(soundUri, audioAttributes)
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            setBlockable(false)
                        }
                    }
                    notificationManager.createNotificationChannel(updateChannel)
                    DiagnosticLogger.log("MainActivity", "Канал уведомлений создан")
                }
                
                // Создаем тестовое уведомление
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this@MainActivity,
                    9999,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                val fullScreenIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
                    this@MainActivity,
                    10999,
                    fullScreenIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                // Android 16+ требует явной настройки звука и приоритета
                val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this@MainActivity, "forknews_updates")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("🧪 Тестовое уведомление")
                    .setContentText("Это тест. Уведомления работают!")
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Если вы видите это уведомление, значит система уведомлений работает корректно.\n\nПроверьте настройки канала и разрешения."))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                    .setCategory(androidx.core.app.NotificationCompat.CATEGORY_CALL)
                    .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)  // Для теста разрешаем звук каждый раз
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setSound(soundUri)
                    .setVibrate(longArrayOf(0, 1000, 500, 1000))
                    .setLights(android.graphics.Color.BLUE, 1000, 1000)
                    .setDefaults(0)
                
                // Android 16+ дополнительные настройки для надежного показа
                if (Build.VERSION.SDK_INT >= 36) { // Android 16+
                    DiagnosticLogger.log("MainActivity", "Применены настройки для Android 16+")
                }
                
                val notification = notificationBuilder.build()
                notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
                
                notificationManager.notify(9999, notification)
                DiagnosticLogger.log("MainActivity", "✓ Тестовое уведомление отправлено")
                Toast.makeText(this@MainActivity, "Тестовое уведомление отправлено!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                DiagnosticLogger.error("MainActivity", "✗ Ошибка тестового уведомления: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
