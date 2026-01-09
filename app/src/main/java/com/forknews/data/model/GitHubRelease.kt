package com.forknews.data.model

data class GitHubRelease(
    val id: Long,
    val tag_name: String,
    val name: String?,
    val html_url: String,
    val published_at: String,
    val body: String?
)
