package com.novel.page.read.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import com.novel.page.read.repository.PageCountCacheData
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
    pageCountCache: PageCountCacheData?,
    containerSize: IntSize,
    onPageChange: (direction: FlipDirection) -> Unit,
    onChapterChange: (direction: FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null, // 新增：iOS侧滑返回回调
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        when (flipEffect) {
            PageFlipEffect.VERTICAL -> {
                VerticalScrollContainer(
                    pageData = pageData,
                    readerSettings = readerSettings,
                    pageCountCache = pageCountCache,
                    containerSize = containerSize,
                    onChapterChange = onChapterChange,
                    onNavigateToReader = onNavigateToReader,
                    onSwipeBack = onSwipeBack,
                    onVerticalScrollPageChange = onVerticalScrollPageChange,
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
                    onNavigateToReader = onNavigateToReader,
                    onSwipeBack = onSwipeBack,
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
                    onNavigateToReader = onNavigateToReader,
                    onSwipeBack = onSwipeBack,
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
                    onSwipeBack = onSwipeBack,
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
                    onNavigateToReader = onNavigateToReader,
                    onSwipeBack = onSwipeBack,
                    onClick = onClick
                )
            }
        }
    }
}