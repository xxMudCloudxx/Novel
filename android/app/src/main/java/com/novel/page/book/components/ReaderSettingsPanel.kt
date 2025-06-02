package com.novel.page.book.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 半透明遮罩，点击关闭
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onDismiss() }
        )
        
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
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "阅读设置",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovelColors.NovelText
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.wdp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = NovelColors.NovelTextGray
                        )
                    }
                }
                
                // 第一行：亮度调节
                BrightnessControl(
                    brightness = settings.brightness,
                    onBrightnessChange = { brightness ->
                        onSettingsChange(settings.copy(brightness = brightness))
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
                    currentTextColor = settings.textColor,
                    onThemeChange = { backgroundColor, textColor ->
                        onSettingsChange(
                            settings.copy(
                                backgroundColor = backgroundColor,
                                textColor = textColor
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
}

/**
 * 亮度控制组件
 */
@Composable
private fun BrightnessControl(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "低亮度",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(20.wdp)
            )
            
            Text(
                text = "亮度",
                fontSize = 14.sp,
                color = NovelColors.NovelText,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.wdp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "低亮度",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(16.wdp)
            )
            
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NovelColors.NovelMain,
                    activeTrackColor = NovelColors.NovelMain,
                    inactiveTrackColor = NovelColors.NovelDivider
                )
            )
            
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "高亮度",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(16.wdp)
            )
        }
    }
}

/**
 * 字体大小控制组件
 */
@Composable
private fun FontSizeControl(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "字体",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(20.wdp)
            )
            
            Text(
                text = "字体大小",
                fontSize = 14.sp,
                color = NovelColors.NovelText,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.wdp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            val fontSizes = listOf(12, 14, 16, 18, 20, 22, 24)
            
            fontSizes.forEach { size ->
                val isSelected = fontSize == size
                
                Card(
                    modifier = Modifier
                        .size(40.wdp)
                        .clickable { onFontSizeChange(size) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NovelColors.NovelMain else NovelColors.NovelDivider
                    ),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            fontSize = (size * 0.6).sp,
                            color = if (isSelected) Color.White else NovelColors.NovelText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 背景颜色控制组件
 */
@Composable
private fun BackgroundColorControl(
    backgroundThemes: List<BackgroundTheme>,
    currentBackgroundColor: Color,
    currentTextColor: Color,
    onThemeChange: (Color, Color) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "背景",
                tint = NovelColors.NovelTextGray,
                modifier = Modifier.size(20.wdp)
            )
            
            Text(
                text = "背景颜色",
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
            backgroundThemes.forEach { theme ->
                val isSelected = currentBackgroundColor == theme.backgroundColor
                
                Card(
                    modifier = Modifier
                        .size(50.wdp)
                        .clickable { 
                            onThemeChange(theme.backgroundColor, theme.textColor)
                        },
                    colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                    shape = CircleShape,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, NovelColors.NovelMain)
                    } else null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = NovelColors.NovelMain,
                                modifier = Modifier.size(20.wdp)
                            )
                        }
                    }
                }
            }
        }
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