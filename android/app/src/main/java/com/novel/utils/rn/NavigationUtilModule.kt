package com.novel.utils.rn

import com.facebook.react.bridge.*
import android.os.Handler
import android.os.Looper
import com.novel.ui.theme.ThemeManager
import com.novel.utils.NavViewModel
import com.novel.utils.SettingsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * 导航工具类 - 扩展支持设置功能
 */
class NavigationUtilModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    // 手动获取SettingsUtils实例，因为RN模块无法使用Hilt注入
    private val settingsUtils: SettingsUtils by lazy {
        val application = reactContext.applicationContext as com.novel.MainApplication
        val settingsUtilsField = application.javaClass.getDeclaredField("settingsUtils")
        settingsUtilsField.isAccessible = true
        settingsUtilsField.get(application) as? SettingsUtils
            ?: throw IllegalStateException("SettingsUtils not found in MainApplication")
    }

    // 获取全局主题管理器
    private val themeManager by lazy { ThemeManager.getInstance(reactContext.applicationContext) }

    override fun getName(): String = "NavigationUtil"

    @ReactMethod
    fun goToLogin() {
        // 确保在主线程执行导航
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.navigate("login")
        }
    }

    @ReactMethod
    fun navigateToSettings() {
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.navigate("settings")
        }
    }

    @ReactMethod
    fun navigateBack() {
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.popBackStack()
        }
    }

    /**
     * 清除所有缓存
     */
    @ReactMethod
    fun clearAllCache(callback: Callback) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = settingsUtils.clearAllCache()
                callback.invoke(null, result)
            } catch (e: Exception) {
                callback.invoke(e.message, null)
            }
        }
    }

    /**
     * 计算缓存大小
     */
    @ReactMethod
    fun calculateCacheSize(callback: Callback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val size = settingsUtils.calculateCacheSize()
                val formattedSize = settingsUtils.formatCacheSize(size)
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(null, formattedSize)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(e.message, null)
                }
            }
        }
    }

    /**
     * 切换夜间模式
     */
    @ReactMethod
    fun toggleNightMode(callback: Callback) {
        try {
            val result = settingsUtils.toggleNightMode()
            
            // 获取切换后的模式并发送事件
            val newMode = themeManager.getCurrentThemeMode()
            sendThemeChangeEvent(newMode)
            
            callback.invoke(null, result)
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置夜间模式
     */
    @ReactMethod
    fun setNightMode(mode: String, callback: Callback) {
        try {
            settingsUtils.setNightMode(mode)
            
            // 直接使用主题管理器设置主题，确保原生Android也响应变更
            themeManager.setThemeMode(mode)
            
            // 发送主题变更事件到RN
            sendThemeChangeEvent(mode)
            
            callback.invoke(null, "夜间模式已设置为: $mode")
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 获取当前夜间模式
     */
    @ReactMethod
    fun getCurrentNightMode(callback: Callback) {
        try {
            val mode = themeManager.getCurrentThemeMode()
            callback.invoke(null, mode)
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置是否跟随系统主题
     */
    @ReactMethod
    fun setFollowSystemTheme(follow: Boolean, callback: Callback) {
        try {
            settingsUtils.setFollowSystemTheme(follow)
            callback.invoke(null, "跟随系统主题已设置为: $follow")
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 获取是否跟随系统主题
     */
    @ReactMethod
    fun isFollowSystemTheme(callback: Callback) {
        try {
            val follow = settingsUtils.isFollowSystemTheme()
            callback.invoke(null, follow)
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置自动切换夜间模式
     */
    @ReactMethod
    fun setAutoNightMode(enabled: Boolean, callback: Callback) {
        try {
            settingsUtils.setAutoNightMode(enabled)
            callback.invoke(null, "自动切换夜间模式已设置为: $enabled")
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 获取是否启用自动切换夜间模式
     */
    @ReactMethod
    fun isAutoNightModeEnabled(callback: Callback) {
        try {
            val enabled = settingsUtils.isAutoNightModeEnabled()
            callback.invoke(null, enabled)
        } catch (e: Exception) {
            callback.invoke(e.message, null)
        }
    }

    /**
     * 统一主题切换接口 - 支持RN端调用
     */
    @ReactMethod
    fun changeTheme(theme: String, promise: Promise) {
        try {
            // 使用全局主题管理器来切换主题
            themeManager.setThemeMode(theme)
            
            // 发送主题变更事件到RN端
            sendThemeChangeEvent(theme)
            
            promise.resolve("主题已切换到: $theme")
        } catch (e: Exception) {
            promise.reject("THEME_CHANGE_ERROR", "切换主题失败: ${e.message}", e)
        }
    }

    /**
     * 发送主题变更事件到RN
     */
    private fun sendThemeChangeEvent(theme: String) {
        try {
            val params = Arguments.createMap().apply {
                putString("colorScheme", theme)
            }
            
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("ThemeChanged", params)
                
            println("[NavigationUtilModule] 主题变更事件已发送: $theme")
        } catch (e: Exception) {
            println("[NavigationUtilModule] 发送主题变更事件失败: ${e.message}")
        }
    }
}

// 利用单例持有 ViewModel，避免跨包引用问题
object NavViewModelHolder {
    val navController = NavViewModel.navController
}