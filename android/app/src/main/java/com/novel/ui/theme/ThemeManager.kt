package com.novel.ui.theme

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局主题管理器
 * 统一管理Android原生的主题状态，确保所有组件都能响应主题变更
 * 支持主题状态持久化缓存
 */
class ThemeManager private constructor(private val context: Context) : ViewModel() {
    
    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_FOLLOW_SYSTEM = "follow_system_theme"
        
        @Volatile
        private var INSTANCE: ThemeManager? = null
        
        private val viewModelStore = ViewModelStore()
        private val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = Companion.viewModelStore
        }
        
        fun getInstance(context: Context? = null): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    requireNotNull(context) { "Context必须在首次调用时提供" }
                    val factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ThemeManager(context.applicationContext) as T
                        }
                    }
                    val provider = ViewModelProvider(viewModelStoreOwner, factory)
                    val instance = provider[ThemeManager::class.java]
                    INSTANCE = instance
                    instance
                }
            }
        }
        
        /**
         * 初始化主题管理器
         */
        fun initialize(application: Application) {
            val instance = getInstance(application)
            instance.restoreThemeFromCache()
        }
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _followSystemTheme = MutableStateFlow(true)
    val followSystemTheme: StateFlow<Boolean> = _followSystemTheme.asStateFlow()
    
    /**
     * 从缓存恢复主题设置
     */
    private fun restoreThemeFromCache() {
        try {
            val savedThemeMode = sharedPreferences.getString(KEY_THEME_MODE, "auto") ?: "auto"
            val savedIsDarkMode = sharedPreferences.getBoolean(KEY_IS_DARK_MODE, false)
            val savedFollowSystem = sharedPreferences.getBoolean(KEY_FOLLOW_SYSTEM, true)
            
            // 恢复状态
            _isDarkMode.value = savedIsDarkMode
            _followSystemTheme.value = savedFollowSystem
            
            // 应用主题设置，但不重复保存到缓存
            applyThemeMode(savedThemeMode, saveToCache = false)
            
            println("[ThemeManager] 已从缓存恢复主题: mode=$savedThemeMode, isDark=$savedIsDarkMode, followSystem=$savedFollowSystem")
        } catch (e: Exception) {
            println("[ThemeManager] 恢复主题缓存失败: ${e.message}")
            // 失败时使用默认设置
            setThemeMode("auto")
        }
    }
    
    /**
     * 保存主题设置到缓存
     */
    private fun saveThemeToCache() {
        try {
            sharedPreferences.edit { // 使用KTX扩展函数替代with
                putString(KEY_THEME_MODE, getCurrentThemeMode())
                putBoolean(KEY_IS_DARK_MODE, _isDarkMode.value)
                putBoolean(KEY_FOLLOW_SYSTEM, _followSystemTheme.value)
            }
            println("[ThemeManager] 主题设置已保存到缓存")
        } catch (e: Exception) {
            println("[ThemeManager] 保存主题缓存失败: ${e.message}")
        }
    }
    
    /**
     * 应用主题模式
     */
    private fun applyThemeMode(mode: String, saveToCache: Boolean = true) {
        when (mode) {
            "light" -> {
                _isDarkMode.value = false
                _followSystemTheme.value = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "dark" -> {
                _isDarkMode.value = true
                _followSystemTheme.value = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "auto" -> {
                _followSystemTheme.value = true
                _isDarkMode.value = false // 系统会自动处理
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
        
        if (saveToCache) {
            saveThemeToCache()
        }
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: String) {
        applyThemeMode(mode, saveToCache = true)
    }
    
    /**
     * 切换深色模式
     */
    fun toggleDarkMode() {
        val newMode = if (_isDarkMode.value) "light" else "dark"
        setThemeMode(newMode)
    }
    
    /**
     * 获取当前主题模式字符串
     */
    fun getCurrentThemeMode(): String {
        return when {
            _followSystemTheme.value -> "auto"
            _isDarkMode.value -> "dark"
            else -> "light"
        }
    }
    
    /**
     * 清除主题缓存
     */
    fun clearThemeCache() {
        try {
            sharedPreferences.edit().clear().apply()
            println("[ThemeManager] 主题缓存已清除")
        } catch (e: Exception) {
            println("[ThemeManager] 清除主题缓存失败: ${e.message}")
        }
    }
}

/**
 * 全局主题状态提供者
 * 在Compose中使用，自动响应主题变更
 */
object GlobalThemeProvider {
    private lateinit var themeManager: ThemeManager
    
    fun initialize(context: Context) {
        themeManager = ThemeManager.getInstance(context)
    }
    
    val isDarkMode: StateFlow<Boolean> 
        get() = themeManager.isDarkMode
        
    val followSystemTheme: StateFlow<Boolean> 
        get() = themeManager.followSystemTheme
    
    fun setThemeMode(mode: String) = themeManager.setThemeMode(mode)
    fun toggleDarkMode() = themeManager.toggleDarkMode()
    fun getCurrentThemeMode() = themeManager.getCurrentThemeMode()
    fun clearThemeCache() = themeManager.clearThemeCache()
}