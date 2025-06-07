package com.novel.page.read.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.novel.page.component.NovelText
import com.novel.page.read.LocalReaderInfo
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
 * 显示在左下角的页码信息 - 支持全局页码和计算中状态
 */
@Composable
fun ReaderPageInfo(
    modifier: Modifier = Modifier
) {
    val readerInfo = LocalReaderInfo.current
    val isCalculating = readerInfo.paginationState.isCalculating

    val totalPages = if (readerInfo.pageCountCache != null) {
        readerInfo.pageCountCache.totalPages
    } else {
        readerInfo.paginationState.estimatedTotalPages
    }

    val currentPage = if (readerInfo.pageCountCache != null && readerInfo.currentChapter != null) {
        val chapterRange = readerInfo.pageCountCache.chapterPageRanges.find { it.chapterId == readerInfo.currentChapter.id }
        if (chapterRange != null) {
            (chapterRange.startPage + readerInfo.perChapterPageIndex + 1).coerceAtLeast(1)
        } else {
            1
        }
    } else {
        1
    }

    val pageInfo = when {
        isCalculating && totalPages > 0 -> "$currentPage / $totalPages..."
        isCalculating -> "页数计算中..."
        totalPages > 0 -> "$currentPage / $totalPages"
        else -> "" // Don't show anything if not ready
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