package com.novel.page.read.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.novel.page.component.NovelText
import com.novel.utils.ssp

/**
 * 阅读器导航信息组件
 * 显示在左上角的章节信息和导航按钮
 */
@Composable
fun ReaderNavigationInfo(
    chapterName: String?,     // 当前章节信息
    modifier: Modifier = Modifier
) {
    if (chapterName == null) return
    // 最外层用 Row 或 Column，都可以。这里示例用 Row
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        NovelText(
            text = chapterName,
            color = Color.Gray.copy(alpha = 0.8f),
            fontSize = 10.ssp
        )
    }
}

/**
 * 阅读器页面信息组件
 * 显示在左下角的页码信息
 */
@Composable
fun ReaderPageInfo(
    chapterNum: Int,     // 当前页码（已经是从1开始的）
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val pageInfo = if (totalPages > 1) {
        "$chapterNum / $totalPages"
    } else {
        "1 / 1"
    }

    // 最外层用 Row 或 Column，都可以。这里示例用 Row
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        NovelText(
            text = pageInfo,
            color = Color.Gray.copy(alpha = 0.8f),
            fontSize = 10.ssp
        )
    }
}