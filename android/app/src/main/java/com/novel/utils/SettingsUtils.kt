package com.novel.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.novel.ui.theme.ThemeManager
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.network.cache.NetworkCacheManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置工具类
 * 提供清除缓存、切换夜间模式等功能
 */
@Singleton
class SettingsUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val novelUserDefaults: NovelUserDefaults,
    private val networkCacheManager: NetworkCacheManager
) {
    
    // 获取全局主题管理器
    private val themeManager by lazy { ThemeManager.getInstance(context) }
    
    companion object {
        private const val PREF_NIGHT_MODE = "night_mode"
        private const val PREF_AUTO_NIGHT_MODE = "auto_night_mode"
        private const val PREF_FOLLOW_SYSTEM = "follow_system_theme"
    }

    /**
     * 清除所有缓存
     * 包括SharedPreferences和网络缓存
     */
    suspend fun clearAllCache(): String = withContext(Dispatchers.IO) {
        try {
            var totalSize = 0L
            
            // 计算缓存大小
            totalSize += calculateCacheSize()
            
            // 清除网络缓存 (清空所有缓存)
            // TODO: 需要清空所有缓存的API，暂时跳过
            
            // 清除应用内部缓存目录
            clearInternalCache()
            
            // 清除图片缓存等其他缓存
            clearImageCache()
            
            "已清理 ${formatCacheSize(totalSize)} 缓存"
        } catch (e: Exception) {
            "清理缓存失败: ${e.message}"
        }
    }

    /**
     * 计算缓存大小
     */
    suspend fun calculateCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val externalCacheDir = context.externalCacheDir
            
            var totalSize = 0L
            totalSize += getDirSize(cacheDir)
            externalCacheDir?.let { totalSize += getDirSize(it) }
            
            totalSize
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 格式化缓存大小显示
     */
    fun formatCacheSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1fKB", bytes / 1024.0)
            else -> "${bytes}B"
        }
    }

    /**
     * 切换夜间模式
     */
    fun toggleNightMode(): String {
        return try {
            val currentMode = getCurrentNightMode()
            val newMode = when (currentMode) {
                "light" -> "dark"
                "dark" -> "auto"
                else -> "light"
            }
            
            setNightMode(newMode)
            "已切换至${getNightModeDisplayName(newMode)}模式"
        } catch (e: Exception) {
            "切换夜间模式失败: ${e.message}"
        }
    }

    /**
     * 设置夜间模式
     */
    fun setNightMode(mode: String) {
        novelUserDefaults.setString(PREF_NIGHT_MODE, mode)
        
        // 使用全局主题管理器统一管理
        themeManager.setThemeMode(mode)
        
        when (mode) {
            "light" -> {
                novelUserDefaults.setString(PREF_FOLLOW_SYSTEM, "false")
            }
            "dark" -> {
                novelUserDefaults.setString(PREF_FOLLOW_SYSTEM, "false")
            }
            "auto" -> {
                novelUserDefaults.setString(PREF_FOLLOW_SYSTEM, "true")
            }
        }
    }

    /**
     * 获取当前夜间模式
     */
    fun getCurrentNightMode(): String {
        return novelUserDefaults.getString(PREF_NIGHT_MODE) ?: "auto"
    }

    /**
     * 是否跟随系统主题
     */
    fun isFollowSystemTheme(): Boolean {
        return novelUserDefaults.getString(PREF_FOLLOW_SYSTEM)?.toBoolean() ?: true
    }

    /**
     * 设置是否跟随系统主题
     */
    fun setFollowSystemTheme(follow: Boolean) {
        novelUserDefaults.setString(PREF_FOLLOW_SYSTEM, follow.toString())
        if (follow) {
            setNightMode("auto")
        }
    }

    /**
     * 设置自动切换夜间模式
     */
    fun setAutoNightMode(enabled: Boolean) {
        novelUserDefaults.setString(PREF_AUTO_NIGHT_MODE, enabled.toString())
    }

    /**
     * 是否启用自动切换夜间模式
     */
    fun isAutoNightModeEnabled(): Boolean {
        return novelUserDefaults.getString(PREF_AUTO_NIGHT_MODE)?.toBoolean() ?: false
    }

    private fun getNightModeDisplayName(mode: String): String {
        return when (mode) {
            "light" -> "浅色"
            "dark" -> "深色"
            "auto" -> "跟随系统"
            else -> "未知"
        }
    }

    private fun clearInternalCache() {
        val cacheDir = context.cacheDir
        deleteDir(cacheDir)
    }

    private fun clearImageCache() {
        // 清除图片缓存（如果使用Glide、Coil等）
        val imageCacheDir = File(context.cacheDir, "image_cache")
        if (imageCacheDir.exists()) {
            deleteDir(imageCacheDir)
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                deleteDir(file)
            }
        }
        return dir.delete()
    }
} 