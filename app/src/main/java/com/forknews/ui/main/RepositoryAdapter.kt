package com.forknews.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forknews.data.model.Repository
import com.forknews.databinding.ItemRepositoryBinding

class RepositoryAdapter(
    private val onItemClick: (Repository) -> Unit,
    private val onNotificationToggle: (Repository) -> Unit,
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
                
                tvLatestRelease.text = repository.latestRelease ?: "Нет релизов"
                
                ivNewBadge.visibility = if (repository.hasNewRelease) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                switchNotifications.isChecked = repository.notificationsEnabled
                switchNotifications.setOnCheckedChangeListener { _, _ ->
                    onNotificationToggle(repository)
                }
                
                root.setOnClickListener {
                    onItemClick(repository)
                }
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
