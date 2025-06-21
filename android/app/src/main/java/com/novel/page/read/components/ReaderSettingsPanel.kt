package com.novel.page.read.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.novel.utils.wdp
import kotlin.math.roundToInt

/**
 * 阅读器设置数据类
 */
data class ReaderSettings(
    val brightness: Float = 0.5f,          // 亮度 0.0-1.0
    val fontSize: Int = 16,                // 字体大小
    val backgroundColor: Color = Color(0xFFF5F5DC),  // 背景颜色（温暖黄）
    val textColor: Color = Color(0xFF2E2E2E),    // 文字颜色（深灰）
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL  // 翻页效果
) {
    companion object {
        /**
         * 获取默认设置
         */
        fun getDefault(): ReaderSettings {
            return ReaderSettings(
                brightness = 0.5f,
                fontSize = 16,
                backgroundColor = Color(0xFFF5F5DC), // 温暖黄背景
                textColor = Color(0xFF2E2E2E),       // 深灰文字
                pageFlipEffect = PageFlipEffect.PAGECURL
            )
        }
    }
}

/**
 * 翻页效果枚举
 */
enum class PageFlipEffect(val displayName: String) {
    PAGECURL("书卷"),
    COVER("覆盖"),
    SLIDE("平移"),
    VERTICAL("上下"),
    NONE("无动画")
}

/**
 * 预设背景颜色
 */
data class BackgroundTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * 阅读器设置面板
 * @param settings 当前设置
 * @param onSettingsChange 设置变更回调
 * @param modifier 自定义修饰符
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    modifier: Modifier = Modifier
) {

    Log.d("ReaderSettingsPanel", "settings: $settings")
    // 背景主题定义移到Composable函数内
    val backgroundThemes = listOf(
        BackgroundTheme("护眼绿", Color(0xFFCCE8CC), Color(0xFF2E2E2E)),  // 浅绿背景 + 深灰文字
        BackgroundTheme("经典白", Color(0xFFFFFFFF), Color(0xFF2E2E2E)),  // 白色背景 + 深灰文字
        BackgroundTheme("温暖黄", Color(0xFFF5F5DC), Color(0xFF2E2E2E)),  // 米黄背景 + 深灰文字 
        BackgroundTheme("夜间黑", Color(0xFF1E1E1E), Color(0xFFE0E0E0)),  // 深黑背景 + 浅灰文字
        BackgroundTheme("羊皮纸", Color(0xFFF4ECD8), Color(0xFF5D4E37))   // 羊皮纸背景 + 棕色文字
    )

    // 1. 把外层的 brightness 存成一个 State，仍然是 0f..1f 之间离散档位
    var brightness by remember { mutableFloatStateOf(settings.brightness) }
    val activity = LocalContext.current as? ComponentActivity
    val window = activity?.window

    LaunchedEffect(brightness) {
        window?.attributes = window?.attributes?.apply {
            screenBrightness = brightness.coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(brightness) {
        window?.attributes = window?.attributes?.apply {
            screenBrightness = brightness.coerceIn(0f, 1f)
        }
    }

    // 设置面板
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = settings.backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(20.wdp),
            verticalArrangement = Arrangement.spacedBy(20.wdp)
        ) {
            // 第一行：亮度调节
            BrightnessControl(
                brightness = brightness,
                backgroundColor = settings.backgroundColor,
                onBrightnessChange = { raw ->
                    // 同样把 raw 量化到最近的档位
                    val stepCount = 10
                    val stepSize = 1f / stepCount
                    val stepped = (raw / stepSize).roundToInt() * stepSize
                    val clamped = stepped.coerceIn(0f, 1f)
                    // 把量化后的离散值直接存回外层 brightness
                    brightness = clamped
                    // 同步回传给 ReaderSettingsPanel 的 settings
                    onSettingsChange(settings.copy(brightness = clamped))
                }
            )

            // 第二行：字体大小调节
            FontSizeControl(
                fontSize = settings.fontSize,
                onFontSizeChange = { fontSize ->
                    onSettingsChange(settings.copy(fontSize = fontSize))
                }
            )

            // 第三行：背景颜色选择
            BackgroundColorControl(
                backgroundThemes = backgroundThemes,
                currentBackgroundColor = settings.backgroundColor,
                onThemeChange = { theme ->
                    onSettingsChange(
                        settings.copy(
                            backgroundColor = theme.backgroundColor,
                            textColor = theme.textColor
                        )
                    )
                }
            )

            // 第四行：翻页效果选择
            PageFlipEffectControl(
                currentEffect = settings.pageFlipEffect,
                backgroundColor = settings.backgroundColor,
                onEffectChange = { effect ->
                    onSettingsChange(settings.copy(pageFlipEffect = effect))
                }
            )
        }
    }
}