package com.forknews.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forknews.data.model.Repository
import com.forknews.data.model.RepositoryType
import com.forknews.data.repository.RepositoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    val repository: RepositoryRepository
) : ViewModel() {
    
    val repositories = repository.allRepositories
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    fun addRepository(url: String) {
        viewModelScope.launch {
            try {
                val repo = parseRepositoryUrl(url)
                if (repo != null) {
                    val repoId = repository.addRepository(repo)
                    // Сразу проверяем релиз после добавления
                    val addedRepo = repository.getRepositoryById(repoId)
                    if (addedRepo != null) {
                        repository.checkForUpdates(addedRepo)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    
    fun deleteRepository(repo: Repository) {
        viewModelScope.launch {
            repository.deleteRepository(repo)
        }
    }
    
    fun toggleNotifications(repo: Repository) {
        viewModelScope.launch {
            repository.updateRepository(
                repo.copy(notificationsEnabled = !repo.notificationsEnabled)
            )
        }
    }
    
    fun markReleaseAsViewed(repoId: Long) {
        viewModelScope.launch {
            repository.markReleaseAsViewed(repoId)
        }
    }
    
    fun moveRepository(fromPosition: Int, toPosition: Int, repositories: List<Repository>) {
        viewModelScope.launch {
            // Обновляем позиции для всех затронутых репозиториев
            repositories.forEachIndexed { index, repo ->
                repository.updatePosition(repo.id, index)
            }
        }
    }
    
    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val repos = repository.getAllRepositoriesList()
                repos.forEach { repo ->
                    repository.checkForUpdates(repo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    private fun parseRepositoryUrl(url: String): Repository? {
        // Parse GitHub URL like https://github.com/owner/repo
        val githubRegex = Regex("""github\.com/([^/]+)/([^/]+)""")
        val match = githubRegex.find(url)
        
        return if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")
            Repository(
                name = repo,
                owner = owner,
                url = "https://github.com/$owner/$repo",
                type = RepositoryType.GITHUB,
                notificationsEnabled = true
            )
        } else {
            null
        }
    }
}

class MainViewModelFactory(
    private val repository: RepositoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
