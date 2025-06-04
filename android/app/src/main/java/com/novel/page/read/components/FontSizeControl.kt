package com.novel.page.read.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 优化后：不在组件内部再存一份 state，只依据外层传入的 fontSize 来渲染并判断可点范围
 */
@Composable
fun FontSizeControl(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit
) {
    // 预定义的字号列表（可根据需求增减）
    val fontSizes = listOf(
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        40,
        41,
        42,
        43,
        44
    )

    // 找到当前 fontSize 在 fontSizes 列表里的索引
    // 如果传进来的 fontSize 不在列表里，就把索引当作 -1（之后会把它视作"可以递增到第一个"）
    val currentIndex = fontSizes.indexOf(fontSize).coerceAtLeast(0)

    // 是否能减小（大于最小一档）
    val canDecrease = currentIndex > 0
    // 是否能增大（小于最大一档）
    val canIncrease = currentIndex < fontSizes.size - 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.wdp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 左侧"字号"标签
        NovelText(
            text = "字号",
            fontSize = 14.ssp,
            color = NovelColors.NovelText
        )

        // —— 减号按钮 ——
        Card(
            modifier = Modifier
                .size(width = 48.wdp, height = 32.wdp)
                .debounceClickable(
                    intervalMillis = 200,
                    enabled = canDecrease,
                    onClick = {
                        if (canDecrease) {
                            val newSize = fontSizes[currentIndex - 1]
                            onFontSizeChange(newSize)
                        }
                    }
                ),
            shape = RoundedCornerShape(16.wdp),
            colors = CardDefaults.cardColors(
                containerColor = if (canDecrease) Color.Gray.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NovelText(
                    text = "A－",
                    fontSize = 14.ssp,
                    color = if (canDecrease) Color.Black else NovelColors.NovelTextGray.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // —— 中间显示当前字体大小的数字 ——
        // 这里直接展示参数 fontSize，而不再读组件内部的任何 state。
        NovelText(
            text = fontSize.toString(),
            fontSize = 12.ssp,
            color = NovelColors.NovelText,
            fontWeight = FontWeight.Medium
        )

        // —— 加号按钮 ——
        Card(
            modifier = Modifier
                .size(width = 48.wdp, height = 32.wdp)
                .debounceClickable(
                    intervalMillis = 200,
                    enabled = canIncrease,
                    onClick = {
                        if (canIncrease) {
                            val newSize = fontSizes[currentIndex + 1]
                            onFontSizeChange(newSize)
                        }
                    }
                ),
            shape = RoundedCornerShape(16.wdp),
            colors = CardDefaults.cardColors(
                containerColor = if (canIncrease) Color.Gray.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NovelText(
                    text = "A＋",
                    fontSize = 14.ssp,
                    color = if (canIncrease) Color.Black else NovelColors.NovelTextGray.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}