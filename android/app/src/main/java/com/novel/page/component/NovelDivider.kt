package com.novel.page.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

/**
 * 默认分割线
 * @param modifier 修饰符
 * @param thickness 分割线厚度
 */
@Composable
fun NovelDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.wdp
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth(),
        thickness = thickness,  // 分割线厚度
        color = NovelColors.NovelDivider  // 颜色设置
    )
}