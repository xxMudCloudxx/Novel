package com.novel.page.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

/**
 * 实现主按钮（登录按钮）
 * @param content 按钮内容，可以是文本或任意组件
 * @param modifier 修饰符
 * @param onClick 点击事件
 */
@Composable
fun NovelMainButton(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    round: Dp = 24.wdp,
    enabldeClicke: Boolean = true,
    colors: List<Color> = listOf(NovelColors.NovelMain, NovelColors.NovelMainLight)
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = colors
                ),
                shape = RoundedCornerShape(round)
            )
            .clickable(
                enabled = enabldeClicke,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 实现弱化按钮（注册按钮）
 * @param content 按钮内容，可以是文本或任意组件
 * @param modifier 修饰符
 * @param onClick 点击事件
 */
@Composable
fun NovelWeakenButton(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    round: Dp = 24.wdp,
    color: Color = NovelColors.NovelSecondaryBackground
) {
    Box(
        modifier = modifier
            .background(
                color = color,
                shape = RoundedCornerShape(round)
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}