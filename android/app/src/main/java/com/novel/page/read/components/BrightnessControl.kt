package com.novel.page.read.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelText
import com.novel.page.component.SolidCircleSlider
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlin.math.roundToInt

/**
 * 亮度控制组件
 */
@Composable
fun BrightnessControl(
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