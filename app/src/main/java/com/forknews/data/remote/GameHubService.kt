package com.forknews.data.remote

import org.jsoup.Jsoup

class GameHubService {
    suspend fun getLatestApkName(): String? {
        return try {
            val doc = Jsoup.connect("https://gamehub.xiaoji.com/download/").get()
            val downloadButton = doc.select("a[download]").firstOrNull()
            downloadButton?.attr("download")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
