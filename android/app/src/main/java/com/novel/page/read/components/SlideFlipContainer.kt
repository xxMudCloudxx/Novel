package com.novel.page.read.components

import androidx.compose.foundation.clickable
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

/**
 * 平移翻页容器 - ViewPager风格，支持章节切换
 * 修复版本：解决章节切换时多翻一页的问题，修复页面索引计算逻辑，增强预加载支持
 */
@Composable
fun SlideFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    // 重新计算页面尺寸和顺序
    val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
    val bookDetailPageSize = if (pageData.hasBookDetailPage) 1 else 0
    val currentChapterSize = pageData.pages.size
    val nextChapterSize = pageData.nextChapterData?.pages?.size ?: 0

    val totalPages = remember(pageData) {
        previousChapterSize + bookDetailPageSize + currentChapterSize + nextChapterSize
    }

    // 正确计算当前页面在虚拟序列中的全局索引
    val globalPageIndex = remember(pageData, currentPageIndex) {
        if (currentPageIndex == -1 && pageData.hasBookDetailPage) {
            previousChapterSize // 书籍详情页的索引
        } else {
            val safeCurrentIndex = currentPageIndex.coerceAtLeast(0)
            previousChapterSize + bookDetailPageSize + safeCurrentIndex // 正常内容页索引
        }
    }

    val pagerState = rememberPagerState(
        initialPage = globalPageIndex.coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )

    var isHandlingExternalChange by remember { mutableStateOf(false) }

    // 当在书籍详情页时，使用SwipeBackContainer包裹以支持侧滑返回
    if (currentPageIndex == -1 && pageData.hasBookDetailPage) {
        SwipeBackContainer(
            modifier = Modifier.fillMaxSize(),
            onSwipeComplete = onSwipeBack,
            onLeftSwipeToReader = {
                onPageChange(FlipDirection.NEXT)
            }
        ) {
            PageContentDisplay(
                page = "", 
                chapterName = pageData.chapterName, 
                isFirstPage = false, 
                isLastPage = false, 
                isBookDetailPage = true,
                bookInfo = pageData.bookInfo,
                nextChapterData = pageData.nextChapterData,
                previousChapterData = pageData.previousChapterData,
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
    LaunchedEffect(globalPageIndex) {
        val targetPage = globalPageIndex.coerceIn(0, totalPages - 1)
        if (pagerState.currentPage != targetPage) {
            isHandlingExternalChange = true
            try {
                pagerState.scrollToPage(targetPage)
            } finally {
                isHandlingExternalChange = false
            }
        }
    }

    // 从Pager到ViewModel的同步 - 使用settledPage确保只在滚动结束后触发
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                if (isHandlingExternalChange || settledPage == globalPageIndex) return@collect

                val direction = if (settledPage > globalPageIndex) FlipDirection.NEXT else FlipDirection.PREVIOUS

                when {
                    // 切换到上一章
                    settledPage < previousChapterSize -> {
                        onChapterChange(FlipDirection.PREVIOUS)
                    }
                    // 切换到书籍详情页
                    bookDetailPageSize == 1 && settledPage == previousChapterSize -> {
                        onPageChange(FlipDirection.PREVIOUS)
                    }
                    // 切换到下一章
                    settledPage >= previousChapterSize + bookDetailPageSize + currentChapterSize -> {
                        onChapterChange(FlipDirection.NEXT)
                    }
                    // 当前章节内翻页
                    else -> {
                        onPageChange(direction)
                                }
        }
    }
}

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().clickable { onClick() }
    ) { globalPage ->
        // 确保globalPage在有效范围内
        val validGlobalPage = globalPage.coerceIn(0, totalPages - 1)
        
        when {
            // 上一章内容
            validGlobalPage < previousChapterSize -> {
                pageData.previousChapterData?.let { prevChapter ->
                    val pageIndex = validGlobalPage
                    if (pageIndex < prevChapter.pages.size) {
                        PageContentDisplay(
                            page = prevChapter.pages[pageIndex],
                            chapterName = prevChapter.chapterName,
                            isFirstPage = pageIndex == 0, 
                            isLastPage = pageIndex == prevChapter.pages.size - 1,
                            nextChapterData = pageData,
                            readerSettings = readerSettings,
                            onPageChange = { onPageChange(it) },
                            currentPageIndex = pageIndex + 1, 
                            totalPages = prevChapter.pages.size,
                            onClick = onClick
                        )
                    }
                }
            }
            // 书籍详情页
            validGlobalPage < previousChapterSize + bookDetailPageSize -> {
                PageContentDisplay(
                    page = "", 
                    chapterName = pageData.chapterName, 
                    isFirstPage = false, 
                    isLastPage = false, 
                    isBookDetailPage = true,
                    bookInfo = pageData.bookInfo,
                    nextChapterData = pageData.nextChapterData,
                    previousChapterData = pageData.previousChapterData,
                    readerSettings = readerSettings, 
                    onSwipeBack = onSwipeBack,
                    onPageChange = { onPageChange(it) },
                    showNavigationInfo = false, 
                    currentPageIndex = 0, 
                    totalPages = 1,
                    onClick = onClick
                )
            }
            // 当前章节内容
            validGlobalPage < previousChapterSize + bookDetailPageSize + currentChapterSize -> {
                val pageIndex = validGlobalPage - previousChapterSize - bookDetailPageSize
                if (pageIndex < pageData.pages.size) {
                    PageContentDisplay(
                        page = pageData.pages[pageIndex],
                        chapterName = pageData.chapterName,
                        isFirstPage = pageIndex == 0, 
                        isLastPage = pageIndex == pageData.pages.size - 1,
                        nextChapterData = pageData.nextChapterData,
                        previousChapterData = pageData.previousChapterData,
                        readerSettings = readerSettings,
                        onPageChange = { onPageChange(it) },
                        currentPageIndex = pageIndex + 1, 
                        totalPages = pageData.pages.size,
                        onClick = onClick
                    )
                }
            }
            // 下一章内容
            else -> {
                pageData.nextChapterData?.let { nextChapter ->
                    val pageIndex = validGlobalPage - previousChapterSize - bookDetailPageSize - currentChapterSize
                    if (pageIndex < nextChapter.pages.size) {
                        PageContentDisplay(
                            page = nextChapter.pages[pageIndex],
                            chapterName = nextChapter.chapterName,
                            isFirstPage = pageIndex == 0, 
                            isLastPage = pageIndex == nextChapter.pages.size - 1,
                            previousChapterData = pageData,
                            readerSettings = readerSettings,
                            onPageChange = { onPageChange(it) },
                            currentPageIndex = pageIndex + 1, 
                            totalPages = nextChapter.pages.size,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}