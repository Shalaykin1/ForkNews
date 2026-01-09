package com.forknews.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.forknews.utils.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RepositoryAdapter
    
    private val viewModel: MainViewModel by viewModels {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "forknews_database"
        ).build()
        val repository = RepositoryRepository(database.repositoryDao())
        MainViewModelFactory(repository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme
        PreferencesManager.init(this)
        PreferencesManager.applyTheme()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupSwipeRefresh()
        observeData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = RepositoryAdapter(
            onItemClick = { repository ->
                repository.latestReleaseUrl?.let { url ->
                    viewModel.markReleaseAsViewed(repository.id)
                    openUrl(url)
                }
            },
            onNotificationToggle = { repository ->
                viewModel.toggleNotifications(repository)
            },
            onDelete = { }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // Swipe to delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
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
    
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddRepositoryDialog()
        }
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
        val options = arrayOf("GitHub репозиторий", "GameHub")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showGitHubUrlDialog()
                    1 -> {
                        viewModel.addGameHub()
                        Toast.makeText(this, "GameHub добавлен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
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
}
