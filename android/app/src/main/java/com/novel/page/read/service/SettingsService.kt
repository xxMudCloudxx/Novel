package com.novel.page.read.service

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.utils.ReaderLogTags
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置服务类
 * 
 * 负责阅读器设置的持久化存储和读取：
 * 1. 翻页效果设置保存和恢复
 * 2. 字体大小设置管理
 * 3. 屏幕亮度设置控制
 * 4. 背景颜色和文字颜色的序列化存储
 * 5. 设置数据的验证和容错处理
 */
@Singleton
class SettingsService @Inject constructor(
    private val userDefaults: NovelUserDefaults
) {
    companion object {
        private const val TAG = ReaderLogTags.SETTINGS_SERVICE
    }
    
    /**
     * 加载阅读器设置
     * 
     * 从UserDefaults中恢复所有设置项，包含完整的容错处理
     * @return 完整的阅读器设置对象
     */
    fun loadSettings(): ReaderSettings {
        Log.d(TAG, "开始加载阅读器设置")
        
        try {
            val defaultSettings = ReaderSettings.getDefault()
            Log.d(TAG, "默认设置: 字体=${defaultSettings.fontSize}sp, 背景色=${colorToHex(defaultSettings.backgroundColor)}, 文字色=${colorToHex(defaultSettings.textColor)}")
            
            var newSettings = defaultSettings

            // 1. 加载翻页效果设置
            userDefaults.get<String>(NovelUserDefaultsKey.PAGE_FLIP_EFFECT)?.let { savedEffect ->
                try {
                    val pageFlipEffect = PageFlipEffect.valueOf(savedEffect)
                    newSettings = newSettings.copy(pageFlipEffect = pageFlipEffect)
                    Log.d(TAG, "翻页效果设置加载成功: $savedEffect")
                } catch (e: Exception) {
                    Log.e(TAG, "翻页效果设置解析失败: $savedEffect, 使用默认值", e)
                }
            } ?: run {
                Log.d(TAG, "翻页效果设置未找到，使用默认值: ${defaultSettings.pageFlipEffect}")
            }

            // 2. 加载字体大小设置
            userDefaults.get<Int>(NovelUserDefaultsKey.FONT_SIZE)?.let { fontSize ->
                if (fontSize in 12..44) {
                    newSettings = newSettings.copy(fontSize = fontSize)
                    Log.d(TAG, "字体大小设置加载成功: ${fontSize}sp")
                } else {
                    Log.w(TAG, "字体大小超出范围: ${fontSize}sp, 使用默认值: ${defaultSettings.fontSize}sp")
                }
            } ?: run {
                Log.d(TAG, "字体大小设置未找到，使用默认值: ${defaultSettings.fontSize}sp")
            }

            // 3. 加载亮度设置
            userDefaults.get<Float>(NovelUserDefaultsKey.BRIGHTNESS)?.let { brightness ->
                if (brightness in 0f..1f) {
                    newSettings = newSettings.copy(brightness = brightness)
                    Log.d(TAG, "亮度设置加载成功: ${(brightness * 100).toInt()}%")
                } else {
                    Log.w(TAG, "亮度值超出范围: $brightness, 使用默认值: ${(defaultSettings.brightness * 100).toInt()}%")
                }
            } ?: run {
                Log.d(TAG, "亮度设置未找到，使用默认值: ${(defaultSettings.brightness * 100).toInt()}%")
            }

            // 4. 加载背景颜色设置
            userDefaults.get<String>(NovelUserDefaultsKey.BACKGROUND_COLOR)?.let { colorString ->
                Log.d(TAG, "尝试加载背景颜色: $colorString")
                try {
                    if (colorString.isNotBlank() && colorString.startsWith("#") && colorString.length >= 7) {
                        val colorLong = if (colorString.length == 7) {
                            "FF${colorString.substring(1)}".toLong(16)
                        } else {
                            colorString.substring(1).toLong(16)
                        }
                        val backgroundColor = Color(colorLong.toULong())
                        newSettings = newSettings.copy(backgroundColor = backgroundColor)
                        Log.d(TAG, "背景颜色设置加载成功: $colorString -> ${colorToHex(backgroundColor)}")
                    } else {
                        Log.w(TAG, "背景颜色格式无效: $colorString, 使用默认值: ${colorToHex(defaultSettings.backgroundColor)}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "背景颜色解析失败: $colorString, 使用默认值", e)
                }
            } ?: run {
                Log.d(TAG, "背景颜色设置未找到，使用默认值: ${colorToHex(defaultSettings.backgroundColor)}")
            }

            // 5. 加载文字颜色设置
            userDefaults.get<String>(NovelUserDefaultsKey.TEXT_COLOR)?.let { colorString ->
                Log.d(TAG, "尝试加载文字颜色: $colorString")
                try {
                    if (colorString.isNotBlank() && colorString.startsWith("#") && colorString.length >= 7) {
                        val colorLong = if (colorString.length == 7) {
                            "FF${colorString.substring(1)}".toLong(16)
                        } else {
                            colorString.substring(1).toLong(16)
                        }
                        val textColor = Color(colorLong.toULong())
                        newSettings = newSettings.copy(textColor = textColor)
                        Log.d(TAG, "文字颜色设置加载成功: $colorString -> ${colorToHex(textColor)}")
                    } else {
                        Log.w(TAG, "文字颜色格式无效: $colorString, 使用默认值: ${colorToHex(defaultSettings.textColor)}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "文字颜色解析失败: $colorString, 使用默认值", e)
                }
            } ?: run {
                Log.d(TAG, "文字颜色设置未找到，使用默认值: ${colorToHex(defaultSettings.textColor)}")
            }

            val validatedSettings = validateAndFixColors(newSettings)

            Log.d(TAG, "设置加载完成: 字体=${validatedSettings.fontSize}sp, 亮度=${(validatedSettings.brightness * 100).toInt()}%, 背景色=${colorToHex(validatedSettings.backgroundColor)}, 文字色=${colorToHex(validatedSettings.textColor)}, 翻页效果=${validatedSettings.pageFlipEffect}")
            return validatedSettings
            
        } catch (e: Exception) {
            Log.e(TAG, "设置加载失败，使用默认设置", e)
            val defaultSettings = ReaderSettings.getDefault()
            Log.d(TAG, "返回默认设置: 字体=${defaultSettings.fontSize}sp, 背景色=${colorToHex(defaultSettings.backgroundColor)}")
            return defaultSettings
        }
    }

    /**
     * 保存阅读器设置
     * 
     * 将所有设置项序列化保存到UserDefaults
     * @param settings 要保存的设置对象
     */
    fun saveSettings(settings: ReaderSettings) {
        Log.d(TAG, "开始保存阅读器设置")
        Log.d(TAG, "保存设置: 字体=${settings.fontSize}sp, 亮度=${(settings.brightness * 100).toInt()}%, 背景色=${colorToHex(settings.backgroundColor)}, 文字色=${colorToHex(settings.textColor)}, 翻页效果=${settings.pageFlipEffect}")
        
        try {
            // 1. 保存翻页效果
            userDefaults.set(settings.pageFlipEffect.name, NovelUserDefaultsKey.PAGE_FLIP_EFFECT)
            Log.d(TAG, "翻页效果保存成功: ${settings.pageFlipEffect.name}")
            
            // 2. 保存字体大小
            userDefaults.set(settings.fontSize, NovelUserDefaultsKey.FONT_SIZE)
            Log.d(TAG, "字体大小保存成功: ${settings.fontSize}sp")
            
            // 3. 保存亮度
            userDefaults.set(settings.brightness, NovelUserDefaultsKey.BRIGHTNESS)
            Log.d(TAG, "亮度保存成功: ${(settings.brightness * 100).toInt()}%")

            // 4. 保存背景颜色
            try {
                val backgroundColorInt = settings.backgroundColor.toArgb()
                val backgroundColorString = String.format("#%08X", backgroundColorInt)
                userDefaults.set(backgroundColorString, NovelUserDefaultsKey.BACKGROUND_COLOR)
                Log.d(TAG, "背景颜色保存成功: ${colorToHex(settings.backgroundColor)} -> $backgroundColorString")
            } catch (e: Exception) {
                Log.e(TAG, "背景颜色保存失败，使用默认值", e)
                userDefaults.set("#FFF5F5DC", NovelUserDefaultsKey.BACKGROUND_COLOR)
            }

            // 5. 保存文字颜色
            try {
                val textColorInt = settings.textColor.toArgb()
                val textColorString = String.format("#%08X", textColorInt)
                userDefaults.set(textColorString, NovelUserDefaultsKey.TEXT_COLOR)
                Log.d(TAG, "文字颜色保存成功: ${colorToHex(settings.textColor)} -> $textColorString")
            } catch (e: Exception) {
                Log.e(TAG, "文字颜色保存失败，使用默认值", e)
                userDefaults.set("#FF2E2E2E", NovelUserDefaultsKey.TEXT_COLOR)
            }
            
            Log.d(TAG, "所有设置保存完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "设置保存失败", e)
        }
    }

    /**
     * 验证并修复颜色设置
     * 确保背景色和文字色有效且对比度充足
     */
    private fun validateAndFixColors(settings: ReaderSettings): ReaderSettings {
        var newBg = settings.backgroundColor
        var newText = settings.textColor
        val default = ReaderSettings.getDefault()

        // 1. 修复无效的透明颜色
        if (newBg.alpha < 0.1f) {
            Log.w(TAG, "检测到无效的背景颜色 (alpha=${newBg.alpha})，重置为默认值。")
            newBg = default.backgroundColor
        }
        if (newText.alpha < 0.1f) {
            Log.w(TAG, "检测到无效的文字颜色 (alpha=${newText.alpha})，重置为默认值。")
            newText = default.textColor
        }

        // 2. 确保对比度充足
        val contrast = ColorUtils.calculateContrast(newText.toArgb(), newBg.toArgb())
        Log.d(TAG, "颜色对比度检查: ${String.format("%.2f", contrast)}")
        if (contrast < 2.5f) {
            Log.w(TAG, "颜色对比度不足 (${String.format("%.2f", contrast)})，重置文字颜色。")
            // 基于背景亮度选择高对比度的文字颜色
            newText = if (newBg.luminance() > 0.5f) Color(0xFF2E2E2E) else Color.White
            Log.d(TAG, "根据背景亮度 (${newBg.luminance()})，已将文字颜色调整为: ${colorToHex(newText)}")
        }
        
        return settings.copy(backgroundColor = newBg, textColor = newText)
    }
    
    /**
     * 将Color对象转换为十六进制字符串，用于日志显示
     */
    private fun colorToHex(color: Color): String {
        return try {
            String.format("#%08X", color.toArgb())
        } catch (e: Exception) {
            "INVALID_COLOR"
        }
    }
}