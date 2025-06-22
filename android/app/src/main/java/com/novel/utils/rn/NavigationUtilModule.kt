package com.novel.utils.rn

import android.util.Log
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
 * React Native导航工具模块
 * 
 * 核心功能：
 * - 提供RN端到Android原生页面的导航能力
 * - 统一管理应用设置和缓存操作
 * - 跨端主题同步和管理
 * - 设备事件通信桥梁
 * 
 * 设计特点：
 * - 线程安全的导航操作
 * - 异步缓存操作避免阻塞UI
 * - 统一的错误处理和回调机制
 * - 主题状态实时同步
 */
class NavigationUtilModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "NavigationUtilModule"
    }

    /** 设置工具实例，通过反射获取避免循环依赖 */
    private val settingsUtils: SettingsUtils by lazy {
        try {
            val application = reactContext.applicationContext as com.novel.MainApplication
            val settingsUtilsField = application.javaClass.getDeclaredField("settingsUtils")
            settingsUtilsField.isAccessible = true
            settingsUtilsField.get(application) as? SettingsUtils
                ?: throw IllegalStateException("SettingsUtils not found in MainApplication")
        } catch (e: Exception) {
            Log.e(TAG, "获取SettingsUtils失败", e)
            throw e
        }
    }

    /** 全局主题管理器实例 */
    private val themeManager by lazy { 
        Log.d(TAG, "初始化ThemeManager")
        ThemeManager.getInstance(reactContext.applicationContext) 
    }

    override fun getName(): String = "NavigationUtil"

    /**
     * 导航到登录页面
     * 确保在主线程执行导航操作
     */
    @ReactMethod
    fun goToLogin() {
        Log.d(TAG, "导航到登录页面")
        // 确保在主线程执行导航
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.navigate("login")
        }
    }

    /**
     * 导航到设置页面
     */
    @ReactMethod
    fun navigateToSettings() {
        Log.d(TAG, "导航到设置页面")
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.navigate("settings")
        }
    }

    /**
     * 返回上一页
     */
    @ReactMethod
    fun navigateBack() {
        Log.d(TAG, "返回上一页")
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.popBackStack()
        }
    }

    /**
     * 清除所有缓存
     * 
     * 异步执行，避免阻塞UI线程
     * 操作完成后通过回调返回结果
     */
    @ReactMethod
    fun clearAllCache(callback: Callback) {
        Log.d(TAG, "开始清除所有缓存")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = settingsUtils.clearAllCache()
                Log.d(TAG, "缓存清除完成: $result")
                callback.invoke(null, result)
            } catch (e: Exception) {
                Log.e(TAG, "清除缓存失败", e)
                callback.invoke(e.message, null)
            }
        }
    }

    /**
     * 计算缓存大小
     * 
     * 在IO线程执行计算，主线程回调结果
     * 自动格式化缓存大小为可读格式
     */
    @ReactMethod
    fun calculateCacheSize(callback: Callback) {
        Log.d(TAG, "开始计算缓存大小")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val size = settingsUtils.calculateCacheSize()
                val formattedSize = settingsUtils.formatCacheSize(size)
                Log.d(TAG, "缓存大小计算完成: $formattedSize")
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(null, formattedSize)
                }
            } catch (e: Exception) {
                Log.e(TAG, "计算缓存大小失败", e)
                Handler(Looper.getMainLooper()).post {
                    callback.invoke(e.message, null)
                }
            }
        }
    }

    /**
     * 切换夜间模式
     * 
     * 自动在浅色和深色主题间切换
     * 完成后发送主题变更事件到RN端
     */
    @ReactMethod
    fun toggleNightMode(callback: Callback) {
        Log.d(TAG, "切换夜间模式")
        try {
            val result = settingsUtils.toggleNightMode()
            
            // 获取切换后的模式并发送事件
            val newMode = themeManager.getCurrentThemeMode()
            sendThemeChangeEvent(newMode)
            
            Log.d(TAG, "夜间模式切换完成: $newMode")
            callback.invoke(null, result)
        } catch (e: Exception) {
            Log.e(TAG, "切换夜间模式失败", e)
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置夜间模式
     * 
     * @param mode 主题模式: "light", "dark", "auto"
     * 支持手动设置和跟随系统模式
     */
    @ReactMethod
    fun setNightMode(mode: String, callback: Callback) {
        Log.d(TAG, "设置夜间模式: $mode")
        try {
            settingsUtils.setNightMode(mode)
            
            // 直接使用主题管理器设置主题，确保原生Android也响应变更
            themeManager.setThemeMode(mode)
            
            // 发送主题变更事件到RN
            sendThemeChangeEvent(mode)
            
            callback.invoke(null, "夜间模式已设置为: $mode")
        } catch (e: Exception) {
            Log.e(TAG, "设置夜间模式失败", e)
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
            Log.w(TAG, "获取当前夜间模式失败", e)
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置是否跟随系统主题
     * 
     * @param follow true=跟随系统，false=手动设置
     */
    @ReactMethod
    fun setFollowSystemTheme(follow: Boolean, callback: Callback) {
        Log.d(TAG, "设置跟随系统主题: $follow")
        try {
            settingsUtils.setFollowSystemTheme(follow)
            callback.invoke(null, "跟随系统主题已设置为: $follow")
        } catch (e: Exception) {
            Log.e(TAG, "设置跟随系统主题失败", e)
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
            Log.w(TAG, "获取跟随系统主题状态失败", e)
            callback.invoke(e.message, null)
        }
    }

    /**
     * 设置自动切换夜间模式
     * 
     * 支持根据时间自动切换主题
     */
    @ReactMethod
    fun setAutoNightMode(enabled: Boolean, callback: Callback) {
        Log.d(TAG, "设置自动切换夜间模式: $enabled")
        try {
            settingsUtils.setAutoNightMode(enabled)
            callback.invoke(null, "自动切换夜间模式已设置为: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "设置自动切换夜间模式失败", e)
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
            Log.w(TAG, "获取自动切换夜间模式状态失败", e)
            callback.invoke(e.message, null)
        }
    }

    /**
     * 统一主题切换接口
     * 
     * Promise形式的异步接口，支持RN端的async/await调用
     */
    @ReactMethod
    fun changeTheme(theme: String, promise: Promise) {
        Log.d(TAG, "统一主题切换: $theme")
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
                
            Log.d("NavigationUtilModule", "主题变更事件已发送: $theme")
        } catch (e: Exception) {
            Log.e("NavigationUtilModule", "发送主题变更事件失败", e)
        }
    }
}

// 利用单例持有 ViewModel，避免跨包引用问题
object NavViewModelHolder {
    val navController = NavViewModel.navController
}