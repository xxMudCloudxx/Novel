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
import androidx.compose.ui.Modifier
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * 平移翻页容器 - ViewPager风格，支持章节切换
 * 修复版本：解决章节切换时多翻一页的问题
 */
@Composable
fun SlideFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onSwipeBack: (() -> Unit)? = null, // 新增：iOS侧滑返回回调
    onClick: () -> Unit
) {
    // 计算总页面数，包括当前章节、相邻章节和书籍详情页
    val totalPages = remember(pageData) {
        var total = pageData.pages.size
        if (pageData.hasBookDetailPage) total += 1 // 书籍详情页
        if (pageData.previousChapterData != null) total += pageData.previousChapterData.pages.size
        if (pageData.nextChapterData != null) total += pageData.nextChapterData.pages.size
        total
    }

    // 计算当前页面在整个虚拟页面序列中的位置
    val globalPageIndex = remember(pageData, currentPageIndex) {
        var index = currentPageIndex
        if (pageData.hasBookDetailPage) {
            index += 1 // 书籍详情页占用一个位置
        }
        if (pageData.previousChapterData != null) {
            index += pageData.previousChapterData.pages.size
        }
        index
    }

    val pagerState = rememberPagerState(
        initialPage = globalPageIndex,
        pageCount = { totalPages }
    )

    // 标记当前是否正在处理外部页面变化，避免重复触发
    var isHandlingExternalChange by remember { mutableStateOf(false) }

    // 检查是否在书籍详情页
    val isOnBookDetailPage = remember(currentPageIndex, pageData.hasBookDetailPage) {
        currentPageIndex == -1 && pageData.hasBookDetailPage
    }

    // 处理页面变化逻辑 - 支持书籍详情页和iOS侧滑
    LaunchedEffect(pagerState.currentPage) {
        // 如果正在处理外部变化，跳过此次处理
        if (isHandlingExternalChange) return@LaunchedEffect

        if (pagerState.currentPage != globalPageIndex) {
            val bookDetailPageSize = if (pageData.hasBookDetailPage) 1 else 0
            val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
            val currentChapterSize = pageData.pages.size

            when {
                // 切换到书籍详情页
                pagerState.currentPage < bookDetailPageSize -> {
                    onPageChange(FlipDirection.PREVIOUS) // 切换到书籍详情页
                    return@LaunchedEffect
                }
                // 切换到上一章
                pagerState.currentPage < bookDetailPageSize + previousChapterSize -> {
                    onChapterChange(FlipDirection.PREVIOUS)
                    return@LaunchedEffect
                }
                // 切换到下一章
                pagerState.currentPage >= bookDetailPageSize + previousChapterSize + currentChapterSize -> {
                    onChapterChange(FlipDirection.NEXT)
                    return@LaunchedEffect
                }
                // 当前章节内翻页
                else -> {
                    val newPageIndex =
                        pagerState.currentPage - bookDetailPageSize - previousChapterSize
                    if (newPageIndex in 0 until currentChapterSize && newPageIndex != currentPageIndex) {
                        val direction = if (newPageIndex > currentPageIndex) {
                            FlipDirection.NEXT
                        } else {
                            FlipDirection.PREVIOUS
                        }
                        onPageChange(direction)
                    }
                }
            }
        }
    }

    // 同步外部页面索引变化 - 支持书籍详情页
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val bookDetailPageSize = if (pageData.hasBookDetailPage) 1 else 0
        val newGlobalIndex = currentPageIndex + bookDetailPageSize + (pageData.previousChapterData?.pages?.size ?: 0)
        if (newGlobalIndex != pagerState.currentPage && newGlobalIndex in 0 until totalPages) {
            // 标记正在处理外部变化
            isHandlingExternalChange = true
            try {
                pagerState.animateScrollToPage(newGlobalIndex)
            } finally {
                // 延迟重置标记，确保动画完成后再允许处理页面变化
                isHandlingExternalChange = false
            }
        }
    }

    // 主要内容区域 - 导航信息现在包含在PageContentDisplay中
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) { globalPage ->
            val bookDetailPageSize = if (pageData.hasBookDetailPage) 1 else 0
            val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
            val currentChapterSize = pageData.pages.size

            when {
                // 显示书籍详情页
                globalPage < bookDetailPageSize -> {
                    if (pageData.hasBookDetailPage && pageData.bookInfo != null) {
                        PageContentDisplay(
                            page = "",
                            chapterName = "",
                            isFirstPage = false,
                            isLastPage = false,
                            isBookDetailPage = true,
                            bookInfo = pageData.bookInfo,
                            nextChapterData = pageData.nextChapterData,
                            previousChapterData = pageData.previousChapterData,
                            readerSettings = readerSettings,
                            onSwipeBack = onSwipeBack,
                            onPageChange = { direction -> 
                                onPageChange(direction)
                            },
                            showNavigationInfo = false, // 书籍详情页不显示导航信息
                            currentPageIndex = 0,
                            totalPages = 1,
                            onClick = onClick
                        )
                    }
                }
                // 显示上一章内容
                globalPage < bookDetailPageSize + previousChapterSize -> {
                    pageData.previousChapterData?.let { prevChapter ->
                        val pageIndex = globalPage - bookDetailPageSize
                        if (pageIndex in 0 until prevChapter.pages.size) {
                            PageContentDisplay(
                                page = prevChapter.pages[pageIndex],
                                chapterName = prevChapter.chapterName,
                                isFirstPage = pageIndex == 0,
                                isLastPage = pageIndex == prevChapter.pages.size - 1,
                                isBookDetailPage = false,
                                bookInfo = null,
                                nextChapterData = pageData,
                                previousChapterData = prevChapter.previousChapterData,
                                readerSettings = readerSettings,
                                onPageChange = { direction -> 
                                    onPageChange(direction)
                                },
                                showNavigationInfo = true,
                                currentPageIndex = pageIndex + 1,
                                totalPages = prevChapter.pages.size,
                                onClick = onClick
                            )
                        }
                    }
                }
                // 显示当前章节内容
                globalPage < bookDetailPageSize + previousChapterSize + currentChapterSize -> {
                    val pageIndex = globalPage - bookDetailPageSize - previousChapterSize
                    if (pageIndex in 0 until pageData.pages.size) {
                        PageContentDisplay(
                            page = pageData.pages[pageIndex],
                            chapterName = pageData.chapterName,
                            isFirstPage = pageIndex == 0,
                            isLastPage = pageIndex == pageData.pages.size - 1,
                            isBookDetailPage = false,
                            bookInfo = null,
                            nextChapterData = pageData.nextChapterData,
                            previousChapterData = pageData.previousChapterData,
                            readerSettings = readerSettings,
                            onPageChange = { direction -> 
                                onPageChange(direction)
                            },
                            showNavigationInfo = true,
                            currentPageIndex = pageIndex + 1,
                            totalPages = pageData.pages.size,
                            onClick = onClick
                        )
                    }
                }
                // 显示下一章内容
                else -> {
                    pageData.nextChapterData?.let { nextChapter ->
                        val pageIndex = globalPage - bookDetailPageSize - previousChapterSize - currentChapterSize
                        if (pageIndex in 0 until nextChapter.pages.size) {
                            PageContentDisplay(
                                page = nextChapter.pages[pageIndex],
                                chapterName = nextChapter.chapterName,
                                isFirstPage = pageIndex == 0,
                                isLastPage = pageIndex == nextChapter.pages.size - 1,
                                isBookDetailPage = false,
                                bookInfo = null,
                                nextChapterData = nextChapter.nextChapterData,
                                previousChapterData = pageData,
                                readerSettings = readerSettings,
                                onPageChange = { direction -> 
                                    onPageChange(direction)
                                },
                                showNavigationInfo = true,
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