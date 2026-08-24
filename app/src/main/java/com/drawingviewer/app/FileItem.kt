package com.drawingviewer.app

import java.io.File

// 文件项数据类
data class FileItem(
    val file: File,
    val isDirectory: Boolean,
    val childCount: Int = 0
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    val isImage: Boolean get() = isImageFile(file)

    companion object {
        private val imageExtensions = arrayOf("jpg", "jpeg", "png", "bmp", "gif", "webp")

        fun isImageFile(file: File): Boolean {
            if (!file.isFile) return false
            val ext = file.extension.lowercase()
            return ext in imageExtensions
        }
    }
}
