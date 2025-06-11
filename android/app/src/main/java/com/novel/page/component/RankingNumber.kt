package com.novel.page.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 排名序号组件
 */
@Composable
fun RankingNumber(
    rank: Int,
    fontSize: TextUnit = 14.ssp,
) {
    val backgroundColor = when (rank) {
        1 -> Color(0xFFFFD700) // 金色
        2 -> Color(0xFFC0C0C0) // 银色
        3 -> Color(0xFFCD7F32) // 铜色
        else -> NovelColors.NovelText
    }

    NovelText(
        text = rank.toString(),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = backgroundColor,
        modifier = Modifier.padding(top = 2.wdp)
    )
}