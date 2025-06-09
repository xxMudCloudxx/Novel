package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.SwipeBackContainer
import kotlinx.coroutines.flow.distinctUntilChanged
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.page.read.viewmodel.VirtualPage

/**
 * 平移翻页容器 - ViewPager风格，支持章节切换
 * 修复版本：解决章节切换时多翻一页的问题，修复页面索引计算逻辑，增强预加载支持
 */
@Composable
fun SlideFlipContainer(
    uiState: ReaderUiState,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val virtualPages = uiState.virtualPages
    val virtualPageIndex = uiState.virtualPageIndex
    val loadedChapters = uiState.loadedChapterData

    if (virtualPages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().clickable { onClick() })
        return
    }

    val totalPages = virtualPages.size
    val currentVirtualPage = virtualPages.getOrNull(virtualPageIndex)

    val pagerState = rememberPagerState(
        initialPage = virtualPageIndex,
        pageCount = { totalPages }
    )
    
    val isOnBookDetailPage = currentVirtualPage is VirtualPage.BookDetailPage

    // 当在书籍详情页时，使用SwipeBackContainer包裹以支持侧滑返回
    if (isOnBookDetailPage) {
        SwipeBackContainer(
            modifier = Modifier.fillMaxSize(),
            onSwipeComplete = onSwipeBack,
            onLeftSwipeToReader = {
                onPageChange(FlipDirection.NEXT)
            }
        ) {
            val bookInfo = loadedChapters[uiState.currentChapter?.id]?.bookInfo
            PageContentDisplay(
                page = "",
                chapterName = uiState.currentChapter?.chapterName ?: "",
                isFirstPage = false,
                isLastPage = false,
                isBookDetailPage = true,
                bookInfo = bookInfo,
                nextChapterData = uiState.nextChapterData,
                previousChapterData = uiState.previousChapterData,
                readerSettings = readerSettings,
                onSwipeBack = onSwipeBack,
                onPageChange = { onPageChange(it) },
                showNavigationInfo = false,
                currentPageIndex = 0,
                totalPages = 1,
                onClick = onClick
            )
        }
        return
    }

    // 从ViewModel到Pager的同步
    LaunchedEffect(virtualPageIndex) {
        if (pagerState.currentPage != virtualPageIndex) {
            pagerState.scrollToPage(virtualPageIndex)
        }
    }

    // 从Pager到ViewModel的同步 - 使用settledPage确保只在滚动结束后触发
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                if (settledPage != virtualPageIndex) {
                    val direction = if (settledPage > virtualPageIndex) FlipDirection.NEXT else FlipDirection.PREVIOUS
                    onPageChange(direction)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().clickable { onClick() }
    ) { pageIndex ->
        val virtualPage = virtualPages.getOrNull(pageIndex)

        when (virtualPage) {
            is VirtualPage.BookDetailPage -> {
                val bookInfo = loadedChapters[uiState.currentChapter?.id]?.bookInfo
                PageContentDisplay(
                    page = "",
                    chapterName = uiState.currentChapter?.chapterName ?: "",
                    isFirstPage = false,
                    isLastPage = false,
                    isBookDetailPage = true,
                    bookInfo = bookInfo,
                    nextChapterData = uiState.nextChapterData,
                    previousChapterData = uiState.previousChapterData,
                    readerSettings = readerSettings,
                    onSwipeBack = onSwipeBack,
                    onPageChange = { onPageChange(it) },
                    showNavigationInfo = false,
                    currentPageIndex = 0,
                    totalPages = 1,
                    onClick = onClick
                )
            }
            is VirtualPage.ContentPage -> {
                val chapterData = loadedChapters[virtualPage.chapterId]
                if (chapterData != null) {
                    PageContentDisplay(
                        page = chapterData.pages[virtualPage.pageIndex],
                        chapterName = chapterData.chapterName,
                        isFirstPage = virtualPage.pageIndex == 0,
                        isLastPage = virtualPage.pageIndex == chapterData.pages.size - 1,
                        nextChapterData = if (virtualPage.pageIndex == chapterData.pages.size - 1) uiState.nextChapterData else null,
                        previousChapterData = if (virtualPage.pageIndex == 0) uiState.previousChapterData else null,
                        readerSettings = readerSettings,
                        onPageChange = { onPageChange(it) },
                        currentPageIndex = virtualPage.pageIndex + 1,
                        totalPages = chapterData.pages.size,
                        onClick = onClick
                    )
                }
            }
            is VirtualPage.ChapterSection -> {
                // Chapter section not supported in this flip mode
                Box(modifier = Modifier.fillMaxSize())
            }
            null -> {
                // Placeholder for loading or error
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}