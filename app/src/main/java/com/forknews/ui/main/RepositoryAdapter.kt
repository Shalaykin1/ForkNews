package com.forknews.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forknews.data.model.Repository
import com.forknews.databinding.ItemRepositoryBinding
import java.text.SimpleDateFormat
import java.util.*

class RepositoryAdapter(
    private val onItemClick: (Repository) -> Unit,
    private val onDelete: (Repository) -> Unit
) : ListAdapter<Repository, RepositoryAdapter.RepositoryViewHolder>(RepositoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryViewHolder {
        val binding = ItemRepositoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RepositoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RepositoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RepositoryViewHolder(
        private val binding: ItemRepositoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(repository: Repository) {
            binding.apply {
                tvRepoName.text = if (repository.owner.isNotEmpty()) {
                    "${repository.owner}/${repository.name}"
                } else {
                    repository.name
                }
                
                // Отображаем название релиза + версию
                tvLatestRelease.text = if (repository.latestRelease != null) {
                    if (repository.releaseName != null && repository.releaseName != repository.latestRelease) {
                        "${repository.releaseName} (${repository.latestRelease})"
                    } else {
                        repository.latestRelease
                    }
                } else {
                    "Нет релизов"
                }
                
                // Отображаем тип релиза с цветом
                if (repository.latestRelease != null) {
                    tvReleaseType.visibility = android.view.View.VISIBLE
                    if (repository.isPrerelease) {
                        tvReleaseType.text = "Pre-release"
                        tvReleaseType.setTextColor(android.graphics.Color.parseColor("#D2691E")) // Терракотовый
                    } else {
                        tvReleaseType.text = "Latest"
                        tvReleaseType.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Зеленый
                    }
                } else {
                    tvReleaseType.visibility = android.view.View.GONE
                }
                
                // Отображаем дату публикации
                if (repository.publishedAt != null) {
                    tvPublishedDate.visibility = android.view.View.VISIBLE
                    tvPublishedDate.text = formatPublishedDate(repository.publishedAt)
                } else {
                    tvPublishedDate.visibility = android.view.View.GONE
                }
                
                ivNewBadge.visibility = if (repository.hasNewRelease) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                root.setOnClickListener {
                    onItemClick(repository)
                }
            }
        }
        
        private fun formatPublishedDate(dateString: String): String {
            return try {
                // GitHub формат: 2024-01-10T15:30:00Z
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                
                if (date != null) {
                    val now = System.currentTimeMillis()
                    val diff = now - date.time
                    
                    val seconds = diff / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    val days = hours / 24
                    val weeks = days / 7
                    val months = days / 30
                    val years = days / 365
                    
                    val relativeTime = when {
                        seconds < 60 -> "только что"
                        minutes < 2 -> "минуту назад"
                        minutes < 5 -> "$minutes минуты назад"
                        minutes < 60 -> "$minutes минут назад"
                        hours < 2 -> "час назад"
                        hours < 5 -> "$hours часа назад"
                        hours < 24 -> "$hours часов назад"
                        days < 2 -> "вчера"
                        days < 7 -> "$days дня назад"
                        weeks < 2 -> "на прошлой неделе"
                        weeks < 4 -> "$weeks недели назад"
                        months < 2 -> "в прошлом месяце"
                        months < 12 -> "$months месяца назад"
                        years < 2 -> "год назад"
                        else -> "$years лет назад"
                    }
                    
                    relativeTime
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    private class RepositoryDiffCallback : DiffUtil.ItemCallback<Repository>() {
        override fun areItemsTheSame(oldItem: Repository, newItem: Repository): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Repository, newItem: Repository): Boolean {
            return oldItem == newItem
        }
    }
}
