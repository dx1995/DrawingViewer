package com.drawingviewer.app

import android.content.Context
import android.os.Environment
import java.io.File

// 文件管理器 - 负责扫描文件夹和图片
object FileManager {

    // 默认图纸目录 - 内部存储/图纸
    fun getDefaultDrawingFolder(): File {
        return try {
            val storage = Environment.getExternalStorageDirectory()
            val drawingFolder = File(storage, "图纸")
            if (!drawingFolder.exists()) {
                drawingFolder.mkdirs()
            }
            // 如果创建失败，回退到根目录
            if (!drawingFolder.exists() || !drawingFolder.isDirectory) {
                storage
            } else {
                drawingFolder
            }
        } catch (e: Exception) {
            // 极端情况回退
            File("/sdcard")
        }
    }

    // 获取根目录（内部存储）
    fun getRootFolder(): File {
        return try {
            Environment.getExternalStorageDirectory()
        } catch (e: Exception) {
            File("/sdcard")
        }
    }

    // 获取指定目录下的文件列表
    fun getFilesInFolder(folder: File): List<FileItem> {
        val result = mutableListOf<FileItem>()

        try {
            if (!folder.exists() || !folder.isDirectory) return result

            val files = folder.listFiles() ?: return result

            val folders = mutableListOf<FileItem>()
            val images = mutableListOf<FileItem>()

            for (file in files) {
                try {
                    if (file.isDirectory && !file.name.startsWith(".")) {
                        val childCount = countImagesInFolder(file)
                        folders.add(FileItem(file, isDirectory = true, childCount = childCount))
                    } else if (FileItem.isImageFile(file)) {
                        images.add(FileItem(file, isDirectory = false))
                    }
                } catch (e: Exception) {
                    // 跳过有问题的文件
                }
            }

            folders.sortBy { it.name.lowercase() }
            images.sortBy { it.name.lowercase() }

            result.addAll(folders)
            result.addAll(images)
        } catch (e: Exception) {
            // 返回空列表
        }

        return result
    }

    // 统计文件夹里的图片数量（递归）
    private fun countImagesInFolder(folder: File): Int {
        var count = 0
        try {
            val files = folder.listFiles() ?: return 0
            for (file in files) {
                try {
                    if (file.isDirectory && !file.name.startsWith(".")) {
                        count += countImagesInFolder(file)
                    } else if (FileItem.isImageFile(file)) {
                        count++
                    }
                } catch (e: Exception) {
                    // 跳过
                }
            }
        } catch (e: Exception) {
            // 跳过
        }
        return count
    }

    // 递归获取所有图片
    fun getAllImages(folder: File): List<FileItem> {
        val result = mutableListOf<FileItem>()
        try {
            collectImages(folder, result)
            result.sortBy { it.name.lowercase() }
        } catch (e: Exception) {
            // 返回空
        }
        return result
    }

    private fun collectImages(folder: File, result: MutableList<FileItem>) {
        try {
            val files = folder.listFiles() ?: return
            for (file in files) {
                try {
                    if (file.isDirectory && !file.name.startsWith(".")) {
                        collectImages(file, result)
                    } else if (FileItem.isImageFile(file)) {
                        result.add(FileItem(file, isDirectory = false))
                    }
                } catch (e: Exception) {
                    // 跳过
                }
            }
        } catch (e: Exception) {
            // 跳过
        }
    }

    // 搜索图片
    fun searchImages(folder: File, query: String): List<FileItem> {
        return try {
            val allImages = getAllImages(folder)
            val lowerQuery = query.lowercase()
            allImages.filter { it.name.lowercase().contains(lowerQuery) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 格式化文件大小
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${"%.1f".format(size / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}
