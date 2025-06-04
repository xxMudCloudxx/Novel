package com.novel.page.read.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import com.novel.page.component.NovelText
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 翻页容器组件
 * 根据不同的翻页效果实现对应的动画和交互
 */
@Composable
fun PageFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    flipEffect: PageFlipEffect,
    readerSettings: ReaderSettings,
    onPageChange: (direction: FlipDirection) -> Unit,
    onChapterChange: (direction: FlipDirection) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    Column {
        ReaderNavigationInfoSimple(
            chapterName = pageData.chapterName,
            modifier = Modifier
                .padding(start = 12.wdp, top = 12.wdp) // 根据实际需要微调位置
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
        ) {
            when (flipEffect) {
                PageFlipEffect.VERTICAL -> {
                    VerticalScrollContainer(
                        pageData = pageData,
                        readerSettings = readerSettings,
                        onChapterChange = onChapterChange,
                        onClick = onClick
                    )
                }

                PageFlipEffect.PAGECURL -> {
                    PageCurlFlipContainer(
                        pageData = pageData,
                        currentPageIndex = currentPageIndex,
                        readerSettings = readerSettings,
                        onPageChange = onPageChange,
                        onChapterChange = onChapterChange,
                        onClick = onClick
                    )
                }

                PageFlipEffect.COVER -> {
                    CoverFlipContainer(
                        pageData = pageData,
                        currentPageIndex = currentPageIndex,
                        readerSettings = readerSettings,
                        onPageChange = onPageChange,
                        onChapterChange = onChapterChange,
                        onClick = onClick
                    )
                }

                PageFlipEffect.SLIDE -> {
                    SlideFlipContainer(
                        pageData = pageData,
                        currentPageIndex = currentPageIndex,
                        readerSettings = readerSettings,
                        onPageChange = onPageChange,
                        onChapterChange = onChapterChange,
                        onClick = onClick
                    )
                }

                PageFlipEffect.NONE -> {
                    NoAnimationContainer(
                        pageData = pageData,
                        currentPageIndex = currentPageIndex,
                        readerSettings = readerSettings,
                        onPageChange = onPageChange,
                        onChapterChange = onChapterChange,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

/**
 * 阅读器导航信息组件
 * 显示在左上角的章节信息和导航按钮
 */
@Composable
fun ReaderNavigationInfoSimple(
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
            fontSize = 14.ssp
        )
    }
}