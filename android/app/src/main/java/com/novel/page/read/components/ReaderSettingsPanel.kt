package com.novel.page.read.components

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelTheme
import com.novel.utils.ssp
import kotlin.math.roundToInt

/**
 * 阅读器设置数据类
 */
data class ReaderSettings(
    val brightness: Float = 0.5f,          // 亮度 0.0-1.0
    val fontSize: Int = 16,                // 字体大小
    val backgroundColor: Color = Color(0xFFF5F5DC),  // 背景颜色
    val textColor: Color = Color.Black,    // 文字颜色
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.SLIDE  // 翻页效果
)

/**
 * 翻页效果枚举
 */
enum class PageFlipEffect(val displayName: String) {
    REALISTIC("仿真"),
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
 * @param onDismiss 关闭面板回调
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    // 背景主题定义移到Composable函数内
    val backgroundThemes = listOf(
        BackgroundTheme("护眼绿", Color(0xFFCCE8CC), Color(0xFF2E2E2E)),
        BackgroundTheme("经典白", Color(0xFFFFFFFF), Color.Black),
        BackgroundTheme("温暖黄", Color(0xFFF5F5DC), Color(0xFF2E2E2E)),
        BackgroundTheme("夜间黑", Color(0xFF1E1E1E), Color(0xFFE0E0E0)),
        BackgroundTheme("羊皮纸", Color(0xFFF4ECD8), Color(0xFF5D4E37))
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

    // 2. 用 animateFloatAsState 让 slider 的 thumb 在跳动时有一个小过渡
    //    我们把步长控制交给 onValueChange，brightness 本身只有 11 个可能值（0.0, 0.1, 0.2 ...）
    val animatedProgress by animateFloatAsState(
        targetValue = brightness,
        animationSpec = tween(durationMillis = 100) // 100ms 的快速过渡
    )

    LaunchedEffect(brightness) {
        window?.attributes = window?.attributes?.apply {
            screenBrightness = brightness.coerceIn(0f, 1f)
        }
    }

    // 设置面板
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 16.wdp, topEnd = 16.wdp),
        colors = CardDefaults.cardColors(containerColor = NovelColors.NovelBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.wdp),
            verticalArrangement = Arrangement.spacedBy(20.wdp)
        ) {
            // 第一行：亮度调节
            BrightnessControl(
                brightness = brightness,
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
                onThemeChange = { backgroundColor->
                    onSettingsChange(
                        settings.copy(
                            backgroundColor = backgroundColor
                        )
                    )
                }
            )

            // 第四行：翻页效果选择
            PageFlipEffectControl(
                currentEffect = settings.pageFlipEffect,
                onEffectChange = { effect ->
                    onSettingsChange(settings.copy(pageFlipEffect = effect))
                }
            )
        }
    }
}

/**
 * 亮度控制组件
 */
@Composable
private fun BrightnessControl(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit
) {
    val stepCount = 10
    val stepSize = 1f / stepCount

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.wdp)
    ) {
        NovelText("亮度", fontSize = 14.ssp)
        SolidCircleSlider(
            modifier = Modifier.weight(1f),
            progress = brightness,
            onValueChange = { rawValue ->
                // 量化到最近的档位
                val stepped = (rawValue / stepSize).roundToInt() * stepSize
                onBrightnessChange(stepped.coerceIn(0f, 1f))
            },
            // 你可以自己设置 track 颜色、thumb 颜色等
            trackColor = Color(0xFFCBC2AA),
            progressColor = Color(0xFF88896C),
            thumbColor = Color(0xFFF9F5E9),
            trackHeightDp = 24.dp,
            thumbRadiusDp = 16.dp
        )
        NovelText("护眼模式", fontSize = 14.ssp, fontWeight = FontWeight.Bold)
    }
}

@Preview
@Composable
private fun BrightnessControlPreview() {
    NovelTheme {
        BrightnessControl(
            brightness = 0.5f,
            onBrightnessChange = {}
        )
    }
}

/**
 * 翻页效果控制组件
 */
@Composable
private fun PageFlipEffectControl(
    currentEffect: PageFlipEffect,
    onEffectChange: (PageFlipEffect) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "翻页效果",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(20.wdp)
            )

            Text(
                text = "翻页效果",
                fontSize = 14.sp,
                color = NovelColors.NovelText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.wdp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            PageFlipEffect.values().forEach { effect ->
                val isSelected = currentEffect == effect

                Card(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable { onEffectChange(effect) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NovelColors.NovelMain else NovelColors.NovelDivider
                    ),
                    shape = RoundedCornerShape(16.wdp)
                ) {
                    Text(
                        text = effect.displayName,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else NovelColors.NovelText,
                        modifier = Modifier.padding(horizontal = 12.wdp, vertical = 6.wdp)
                    )
                }
            }
        }
    }
} 