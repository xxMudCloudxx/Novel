package com.novel.rn.settings.usecase

import com.novel.core.domain.BaseUseCase
import com.novel.rn.settings.SettingsUtils
import com.novel.ui.theme.ThemeManager
import com.novel.utils.TimberLogger
import javax.inject.Inject

/**
 * 更新设置UseCase
 * 
 * 功能：
 * - 主题模式设置更新
 * - 自动切换配置修改
 * - 时间设置更新
 * - 跟随系统主题设置
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsUtils: SettingsUtils,
    private val themeManager: ThemeManager
) : BaseUseCase<UpdateSettingsUseCase.UpdateParams, UpdateSettingsUseCase.UpdateResult>() {

    companion object {
        private const val TAG = "UpdateSettingsUseCase"
    }

    sealed class UpdateParams {
        data class ThemeMode(val mode: String) : UpdateParams()
        data class FollowSystemTheme(val follow: Boolean) : UpdateParams()
        data class AutoNightMode(val enabled: Boolean) : UpdateParams()
        data class NightModeTime(val startTime: String, val endTime: String) : UpdateParams()
        object ToggleTheme : UpdateParams()
    }

    data class UpdateResult(
        val message: String,
        val newThemeMode: String? = null,
        val newActualTheme: String? = null
    )

    override suspend fun execute(parameters: UpdateParams): UpdateResult {
        TimberLogger.d(TAG, "开始更新设置: ${parameters::class.simpleName}")
        
        try {
            when (parameters) {
                is UpdateParams.ThemeMode -> {
                    settingsUtils.setNightMode(parameters.mode)
                    themeManager.setThemeMode(parameters.mode)
                    
                    val actualTheme = themeManager.getCurrentActualThemeMode()
                    val result = UpdateResult(
                        message = "主题已切换到: ${getThemeDisplayName(parameters.mode)}",
                        newThemeMode = parameters.mode,
                        newActualTheme = actualTheme
                    )
                    
                    TimberLogger.d(TAG, "主题模式更新成功: ${parameters.mode} -> $actualTheme")
                    return result
                }
                
                is UpdateParams.FollowSystemTheme -> {
                    settingsUtils.setFollowSystemTheme(parameters.follow)
                    
                    val newMode = if (parameters.follow) "auto" else themeManager.getCurrentThemeMode()
                    val actualTheme = themeManager.getCurrentActualThemeMode()
                    
                    val result = UpdateResult(
                        message = "跟随系统主题已${if (parameters.follow) "开启" else "关闭"}",
                        newThemeMode = newMode,
                        newActualTheme = actualTheme
                    )
                    
                    TimberLogger.d(TAG, "跟随系统主题更新成功: ${parameters.follow}")
                    return result
                }
                
                is UpdateParams.AutoNightMode -> {
                    settingsUtils.setAutoNightMode(parameters.enabled)
                    
                    val result = UpdateResult(
                        message = "自动切换夜间模式已${if (parameters.enabled) "开启" else "关闭"}"
                    )
                    
                    TimberLogger.d(TAG, "自动夜间模式更新成功: ${parameters.enabled}")
                    return result
                }
                
                is UpdateParams.NightModeTime -> {
                    settingsUtils.setNightModeTime(parameters.startTime, parameters.endTime)
                    
                    val result = UpdateResult(
                        message = "夜间模式时间已设置为: ${parameters.startTime} - ${parameters.endTime}"
                    )
                    
                    TimberLogger.d(TAG, "夜间模式时间更新成功: ${parameters.startTime} - ${parameters.endTime}")
                    return result
                }
                
                is UpdateParams.ToggleTheme -> {
                    val resultMessage = settingsUtils.toggleNightMode()
                    val newThemeMode = themeManager.getCurrentThemeMode()
                    val newActualTheme = themeManager.getCurrentActualThemeMode()
                    
                    val result = UpdateResult(
                        message = resultMessage,
                        newThemeMode = newThemeMode,
                        newActualTheme = newActualTheme
                    )
                    
                    TimberLogger.d(TAG, "主题切换成功: $newThemeMode -> $newActualTheme")
                    return result
                }
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "更新设置失败: ${parameters::class.simpleName}", e)
            throw e
        }
    }
    
    private fun getThemeDisplayName(mode: String): String {
        return when (mode) {
            "light" -> "浅色"
            "dark" -> "深色"
            "auto" -> "跟随系统"
            else -> "未知"
        }
    }
} 