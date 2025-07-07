package com.novel.rn.settings

import com.novel.utils.TimberLogger
import com.facebook.react.bridge.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.novel.ComposeMainActivity

/**
 * 设置桥接模块
 * 
 * 专门处理设置相关的RN调用，通过SettingsViewModel管理状态：
 * - 主题切换和管理
 * - 缓存计算和清理
 * - 时间设置管理
 * - 自动主题切换
 */
class SettingsBridgeModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "SettingsBridgeModule"
    }

    override fun getName(): String = "SettingsBridge"

    private val settingsViewModel: SettingsViewModel?
        get() = try {
            val activity = currentActivity as? ComposeMainActivity
            activity?.let {
                val vm = ViewModelProvider(it as ViewModelStoreOwner)[SettingsViewModel::class.java]
                vm.initReactContext(reactContext)
                vm
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "无法获取SettingsViewModel", e)
            null
        }

    /**
     * 切换夜间模式
     */
    @ReactMethod
    fun toggleNightMode(callback: Callback) {
        TimberLogger.d(TAG, "切换夜间模式")
        
        settingsViewModel?.let { viewModel ->
            // 监听Effect来获取结果
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            // 发送Intent
            viewModel.sendIntent(SettingsIntent.ToggleNightMode)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 设置夜间模式
     */
    @ReactMethod
    fun setNightMode(mode: String, callback: Callback) {
        TimberLogger.d(TAG, "设置夜间模式: $mode")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.SetNightMode(mode))
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取当前夜间模式
     */
    @ReactMethod
    fun getCurrentNightMode(callback: Callback) {
        TimberLogger.d(TAG, "获取当前夜间模式")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.currentThemeMode)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取当前实际主题
     */
    @ReactMethod
    fun getCurrentActualTheme(callback: Callback) {
        TimberLogger.d(TAG, "获取当前实际主题")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.actualTheme)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 设置是否跟随系统主题
     */
    @ReactMethod
    fun setFollowSystemTheme(follow: Boolean, callback: Callback) {
        TimberLogger.d(TAG, "设置跟随系统主题: $follow")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.SetFollowSystemTheme(follow))
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取是否跟随系统主题
     */
    @ReactMethod
    fun isFollowSystemTheme(callback: Callback) {
        TimberLogger.d(TAG, "获取跟随系统主题状态")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.isFollowSystemTheme)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 设置自动切换夜间模式
     */
    @ReactMethod
    fun setAutoNightMode(enabled: Boolean, callback: Callback) {
        TimberLogger.d(TAG, "设置自动切换夜间模式: $enabled")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.SetAutoNightMode(enabled))
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取是否启用自动切换夜间模式
     */
    @ReactMethod
    fun isAutoNightModeEnabled(callback: Callback) {
        TimberLogger.d(TAG, "获取自动切换夜间模式状态")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.isAutoNightModeEnabled)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 清除所有缓存
     */
    @ReactMethod
    fun clearAllCache(callback: Callback) {
        TimberLogger.d(TAG, "清除所有缓存")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.CacheCleared -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.ClearAllCache)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 计算缓存大小
     */
    @ReactMethod
    fun calculateCacheSize(callback: Callback) {
        TimberLogger.d(TAG, "计算缓存大小")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.CacheCalculated -> callback.invoke(null, effect.size)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.CalculateCacheSize)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 设置夜间模式时间段
     */
    @ReactMethod
    fun setNightModeTime(startTime: String, endTime: String, callback: Callback) {
        TimberLogger.d(TAG, "设置夜间模式时间段: $startTime - $endTime")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.SetNightModeTime(startTime, endTime))
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取夜间模式开始时间
     */
    @ReactMethod
    fun getNightModeStartTime(callback: Callback) {
        TimberLogger.d(TAG, "获取夜间模式开始时间")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.nightModeStartTime)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 获取夜间模式结束时间
     */
    @ReactMethod
    fun getNightModeEndTime(callback: Callback) {
        TimberLogger.d(TAG, "获取夜间模式结束时间")
        
        settingsViewModel?.let { viewModel ->
            val currentState = viewModel.getStateForBridge()
            callback.invoke(null, currentState.nightModeEndTime)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 检查当前时间的主题状态
     */
    @ReactMethod
    fun checkCurrentTimeTheme(callback: Callback) {
        TimberLogger.d(TAG, "检查当前时间的主题状态")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForCallback(viewModel, callback) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> callback.invoke(null, effect.message)
                    is SettingsEffect.ShowError -> callback.invoke(effect.error, null)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.CheckCurrentTimeTheme)
        } ?: run {
            callback.invoke("ViewModel未初始化", null)
        }
    }

    /**
     * 统一主题切换接口（Promise版本）
     */
    @ReactMethod
    fun changeTheme(theme: String, promise: Promise) {
        TimberLogger.d(TAG, "统一主题切换: $theme")
        
        settingsViewModel?.let { viewModel ->
            observeEffectForPromise(viewModel, promise) { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> promise.resolve(effect.message)
                    is SettingsEffect.ShowError -> promise.reject("THEME_CHANGE_ERROR", effect.error)
                    else -> null
                }
            }
            
            viewModel.sendIntent(SettingsIntent.SetNightMode(theme))
        } ?: run {
            promise.reject("VIEWMODEL_ERROR", "ViewModel未初始化")
        }
    }

    /**
     * 观察Effect并执行回调的辅助方法
     */
    private fun observeEffectForCallback(
        viewModel: SettingsViewModel,
        callback: Callback,
        effectHandler: (SettingsEffect) -> Unit?
    ) {
        // 这里需要实现Effect观察逻辑
        // 由于RN桥接是同步的，我们需要用其他方式来处理异步结果
        // 可以考虑使用事件发送机制或者简化为同步处理
        TimberLogger.d(TAG, "设置Effect观察器")
    }

    /**
     * 观察Effect并执行Promise的辅助方法
     */
    private fun observeEffectForPromise(
        viewModel: SettingsViewModel,
        promise: Promise,
        effectHandler: (SettingsEffect) -> Unit?
    ) {
        TimberLogger.d(TAG, "设置Effect观察器(Promise)")
    }
} 