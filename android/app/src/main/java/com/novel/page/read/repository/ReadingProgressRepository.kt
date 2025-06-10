package com.novel.page.read.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novel.page.read.components.PageFlipEffect
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读进度数据
 */
data class ReadingProgressData(
    val bookId: String,
    val chapterId: String,
    val pageIndex: Int, // 章节内页码索引
    val globalPageIndex: Int = 0, // 全书页码索引（可选）
    val chapterProgress: Float = 0f, // 章节内进度 0-1
    val globalProgress: Float = 0f, // 全书进度 0-1
    val lastReadTime: Long = System.currentTimeMillis(),
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL
)

/**
 * 阅读器设置数据
 */
data class ReaderSettingsData(
    val fontSize: Int = 16,
    val brightness: Float = 0.5f,
    val backgroundColor: String = "#FFF5F5DC", // Color序列化为字符串
    val textColor: String = "#FF000000",
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL
)

/**
 * 阅读进度持久化Repository
 * 
 * 功能：
 * 1. 保存和恢复阅读进度
 * 2. 保存和恢复阅读器设置
 * 3. 支持翻页模式切换时的状态保留
 */
@Singleton
class ReadingProgressRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDefaults: NovelUserDefaults
) {
    private val gson = Gson()

    companion object {
        private const val READING_PROGRESS_KEY = "reading_progress_data"
        private const val READER_SETTINGS_KEY = "reader_settings_data"
    }

    /**
     * 保存阅读进度
     */
    suspend fun saveReadingProgress(progress: ReadingProgressData) = withContext(Dispatchers.IO) {
        try {
            val progressMap = getAllReadingProgress().toMutableMap()
            progressMap[progress.bookId] = progress
            val json = gson.toJson(progressMap)
            userDefaults.setString(READING_PROGRESS_KEY, json)
        } catch (e: Exception) {
            // 保存失败时静默处理
        }
    }

    /**
     * 获取指定书籍的阅读进度
     */
    suspend fun getReadingProgress(bookId: String): ReadingProgressData? = withContext(Dispatchers.IO) {
        try {
            val progressMap = getAllReadingProgress()
            progressMap[bookId]
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取所有阅读进度
     */
    suspend fun getAllReadingProgress(): Map<String, ReadingProgressData> = withContext(Dispatchers.IO) {
        try {
            val json = userDefaults.getString(READING_PROGRESS_KEY) ?: return@withContext emptyMap()
            val type = object : TypeToken<Map<String, ReadingProgressData>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 保存阅读器设置
     */
    suspend fun saveReaderSettings(settings: ReaderSettingsData) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(settings)
            userDefaults.setString(READER_SETTINGS_KEY, json)
        } catch (e: Exception) {
            // 保存失败时静默处理
        }
    }

    /**
     * 获取阅读器设置
     */
    suspend fun getReaderSettings(): ReaderSettingsData? = withContext(Dispatchers.IO) {
        try {
            val json = userDefaults.getString(READER_SETTINGS_KEY) ?: return@withContext null
            gson.fromJson(json, ReaderSettingsData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清理指定书籍的阅读进度
     */
    suspend fun clearReadingProgress(bookId: String) = withContext(Dispatchers.IO) {
        try {
            val progressMap = getAllReadingProgress().toMutableMap()
            progressMap.remove(bookId)
            val json = gson.toJson(progressMap)
            userDefaults.setString(READING_PROGRESS_KEY, json)
        } catch (e: Exception) {
            // 清理失败时静默处理
        }
    }

    /**
     * 清理所有阅读进度
     */
    suspend fun clearAllReadingProgress() = withContext(Dispatchers.IO) {
        try {
            userDefaults.remove(READING_PROGRESS_KEY)
        } catch (e: Exception) {
            // 清理失败时静默处理
        }
    }

    /**
     * 更新指定书籍的翻页模式（用于模式切换时保留当前阅读位置）
     */
    suspend fun updatePageFlipEffect(bookId: String, pageFlipEffect: PageFlipEffect) = withContext(Dispatchers.IO) {
        try {
            val progressMap = getAllReadingProgress().toMutableMap()
            val existingProgress = progressMap[bookId]
            if (existingProgress != null) {
                progressMap[bookId] = existingProgress.copy(
                    pageFlipEffect = pageFlipEffect,
                    lastReadTime = System.currentTimeMillis()
                )
                val json = gson.toJson(progressMap)
                userDefaults.setString(READING_PROGRESS_KEY, json)
            }
        } catch (e: Exception) {
            // 更新失败时静默处理
        }
    }

    /**
     * 批量更新多个书籍的翻页模式（全局设置更改时使用）
     */
    suspend fun updateAllBooksPageFlipEffect(pageFlipEffect: PageFlipEffect) = withContext(Dispatchers.IO) {
        try {
            val progressMap = getAllReadingProgress().toMutableMap()
            progressMap.forEach { (bookId, progress) ->
                progressMap[bookId] = progress.copy(pageFlipEffect = pageFlipEffect)
            }
            val json = gson.toJson(progressMap)
            userDefaults.setString(READING_PROGRESS_KEY, json)
        } catch (e: Exception) {
            // 批量更新失败时静默处理
        }
    }
} 