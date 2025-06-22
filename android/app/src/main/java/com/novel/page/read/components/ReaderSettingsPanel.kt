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
 * 
 * 封装阅读器的所有可配置设置项，支持个性化阅读体验
 * 所有设置都会自动持久化到本地存储
 * 
 * @param brightness 屏幕亮度值，范围0.0-1.0，0.0最暗，1.0最亮
 * @param fontSize 阅读字体大小，范围12-44，单位sp
 * @param backgroundColor 阅读背景颜色，支持多种预设主题
 * @param textColor 文字颜色，会根据背景色自动适配对比度
 * @param pageFlipEffect 翻页动画效果，支持多种翻页模式
 */
data class ReaderSettings(
    /** 屏幕亮度值 - 范围0.0-1.0，影响整个屏幕亮度 */
    val brightness: Float = 0.5f,
    
    /** 阅读字体大小 - 范围12-44sp，影响文字显示大小 */
    val fontSize: Int = 16,
    
    /** 阅读背景颜色 - 默认温暖黄色，护眼舒适 */
    val backgroundColor: Color = Color(0xFFF5F5DC),
    
    /** 文字颜色 - 默认深灰色，与背景形成良好对比 */
    val textColor: Color = Color(0xFF2E2E2E),
    
    /** 翻页动画效果 - 默认仿真书本翻页效果 */
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL
) {
    companion object {
        /**
         * 获取默认阅读器设置
         * 
         * 提供经过优化的默认配置，确保最佳的阅读体验：
         * - 适中的亮度避免眼部疲劳
         * - 标准字体大小适合大多数用户
         * - 温暖背景色减少蓝光刺激
         * - 高对比度文字色保证清晰度
         * - 仿真翻页效果提升沉浸感
         * 
         * @return 默认的ReaderSettings实例
         */
        fun getDefault(): ReaderSettings {
            return ReaderSettings(
                brightness = 0.5f,                    // 中等亮度，平衡护眼与可读性
                fontSize = 16,                        // 标准字体大小，适合大多数设备
                backgroundColor = Color(0xFFF5F5DC),  // 温暖米黄色，护眼舒适
                textColor = Color(0xFF2E2E2E),        // 深灰色文字，清晰易读
                pageFlipEffect = PageFlipEffect.PAGECURL  // 仿真翻页，增强沉浸感
            )
        }
    }
}

/**
 * 翻页动画效果枚举类
 * 
 * 定义阅读器支持的各种翻页动画效果，每种效果都有不同的视觉体验和性能特征
 * 
 * @param displayName 在设置界面显示的中文名称
 */
enum class PageFlipEffect(val displayName: String) {
    /** 仿真书本翻页 - 模拟真实书本的卷曲翻页效果，最具沉浸感 */
    PAGECURL("书卷"),
    
    /** 覆盖式翻页 - 新页面从上方覆盖当前页面，简洁流畅 */
    COVER("覆盖"),
    
    /** 平移式翻页 - 页面左右滑动切换，类似于现代应用的标准交互 */
    SLIDE("平移"),
    
    /** 垂直滚动 - 连续的上下滚动阅读，适合长篇内容 */
    VERTICAL("上下"),
    
    /** 无动画翻页 - 直接切换页面，性能最优，适合低端设备 */
    NONE("无动画")
}

/**
 * 背景主题配置类
 * 
 * 预定义的阅读背景主题，每个主题都包含优化搭配的背景色和文字色
 * 确保在不同光线环境下都有良好的可读性和舒适度
 * 
 * @param name 主题名称，显示在设置界面
 * @param backgroundColor 背景颜色，影响整个阅读区域
 * @param textColor 文字颜色，与背景色形成适当对比度
 */
data class BackgroundTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * 阅读器设置面板组件
 * 
 * 提供完整的阅读器个性化设置界面，包括：
 * - 亮度调节：支持10档离散亮度设置，即时生效
 * - 字体大小：12-44sp范围调节，适配不同用户需求
 * - 背景主题：5种预设主题，优化不同环境下的阅读体验
 * - 翻页效果：多种动画模式，平衡视觉效果与性能
 * 
 * 所有设置变更都会实时预览并自动保存到本地
 * 
 * @param settings 当前阅读器设置状态
 * @param onSettingsChange 设置变更回调函数，传递新的设置对象
 * @param modifier 额外的修饰符，用于自定义布局和样式
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    // 预定义的背景主题列表，经过护眼性和可读性优化
    val backgroundThemes = listOf(
        BackgroundTheme("护眼绿", Color(0xFFCCE8CC), Color(0xFF2E2E2E)),  // 护眼绿色，减缓视觉疲劳
        BackgroundTheme("经典白", Color(0xFFFFFFFF), Color(0xFF2E2E2E)),  // 经典白色，适合明亮环境
        BackgroundTheme("温暖黄", Color(0xFFF5F5DC), Color(0xFF2E2E2E)),  // 温暖米黄，最佳护眼效果
        BackgroundTheme("夜间黑", Color(0xFF1E1E1E), Color(0xFFE0E0E0)),  // 夜间模式，适合暗光环境
        BackgroundTheme("羊皮纸", Color(0xFFF4ECD8), Color(0xFF5D4E37))   // 复古羊皮纸，仿古书籍质感
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