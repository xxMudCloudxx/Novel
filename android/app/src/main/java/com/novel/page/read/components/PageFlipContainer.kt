package com.novel.page.read.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

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