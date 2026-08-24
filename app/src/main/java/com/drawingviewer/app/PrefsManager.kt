package com.drawingviewer.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// 偏好设置管理 - 收藏、最近浏览
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("drawing_viewer_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENT = "recent"
        private const val MAX_RECENT = 50
    }

    // ========== 收藏功能 ==========

    fun getFavorites(): List<String> {
        val json = prefs.getString(KEY_FAVORITES, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addFavorite(path: String) {
        val favorites = getFavorites().toMutableList()
        if (path !in favorites) {
            favorites.add(0, path)
            prefs.edit().putString(KEY_FAVORITES, gson.toJson(favorites)).apply()
        }
    }

    fun removeFavorite(path: String) {
        val favorites = getFavorites().toMutableList()
        favorites.remove(path)
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(favorites)).apply()
    }

    fun isFavorite(path: String): Boolean {
        return path in getFavorites()
    }

    fun toggleFavorite(path: String): Boolean {
        return if (isFavorite(path)) {
            removeFavorite(path)
            false
        } else {
            addFavorite(path)
            true
        }
    }

    fun getFavoriteFiles(): List<FileItem> {
        return getFavorites().mapNotNull { path ->
            val file = File(path)
            if (file.exists() && FileItem.isImageFile(file)) {
                FileItem(file, isDirectory = false)
            } else null
        }
    }

    // ========== 最近浏览 ==========

    fun getRecent(): List<String> {
        val json = prefs.getString(KEY_RECENT, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addToRecent(path: String) {
        val recent = getRecent().toMutableList()
        recent.remove(path)
        recent.add(0, path)
        while (recent.size > MAX_RECENT) {
            recent.removeAt(recent.size - 1)
        }
        prefs.edit().putString(KEY_RECENT, gson.toJson(recent)).apply()
    }

    fun clearRecent() {
        prefs.edit().putString(KEY_RECENT, "[]").apply()
    }

    fun getRecentFiles(): List<FileItem> {
        return getRecent().mapNotNull { path ->
            val file = File(path)
            if (file.exists() && FileItem.isImageFile(file)) {
                FileItem(file, isDirectory = false)
            } else null
        }
    }
}
