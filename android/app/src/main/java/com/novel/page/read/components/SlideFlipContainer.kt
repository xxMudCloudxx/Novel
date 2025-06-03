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
    onClick: () -> Unit
) {
    // 计算总页面数，包括当前章节和相邻章节
    val totalPages = remember(pageData) {
        var total = pageData.pages.size
        if (pageData.previousChapterData != null) total += pageData.previousChapterData.pages.size
        if (pageData.nextChapterData != null) total += pageData.nextChapterData.pages.size
        total
    }

    // 计算当前页面在整个虚拟页面序列中的位置
    val globalPageIndex = remember(pageData, currentPageIndex) {
        var index = currentPageIndex
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

    // 处理页面变化逻辑 - 优化版本
    LaunchedEffect(pagerState.currentPage) {
        // 如果正在处理外部变化，跳过此次处理
        if (isHandlingExternalChange) return@LaunchedEffect
        
        val currentGlobalIndex = globalPageIndex
        if (pagerState.currentPage != currentGlobalIndex) {
            val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
            val currentChapterSize = pageData.pages.size
            val newPageIndex = pagerState.currentPage - previousChapterSize

            when {
                // 切换到上一章
                pagerState.currentPage < previousChapterSize -> {
                    onChapterChange(FlipDirection.PREVIOUS)
                    return@LaunchedEffect
                }
                // 切换到下一章
                pagerState.currentPage >= previousChapterSize + currentChapterSize -> {
                    onChapterChange(FlipDirection.NEXT)
                    return@LaunchedEffect
                }
                // 当前章节内翻页 - 增加边界检查
                newPageIndex in 0 until currentChapterSize && newPageIndex != currentPageIndex -> {
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

    // 同步外部页面索引变化 - 优化版本
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val newGlobalIndex = currentPageIndex + (pageData.previousChapterData?.pages?.size ?: 0)
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

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) { globalPage ->
        val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
        val currentChapterSize = pageData.pages.size

        when {
            // 显示上一章内容
            globalPage < previousChapterSize -> {
                pageData.previousChapterData?.let { prevChapter ->
                    if (prevChapter.pages.isNotEmpty()) {
                        CurrentPageContent(
                            pageData = prevChapter,
                            pageIndex = globalPage,
                            readerSettings = readerSettings,
                            isFirstPage = globalPage == 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            // 显示当前章节内容
            globalPage < previousChapterSize + currentChapterSize -> {
                val pageIndex = globalPage - previousChapterSize
                if (pageIndex in 0 until pageData.pages.size) {
                    CurrentPageContent(
                        pageData = pageData,
                        pageIndex = pageIndex,
                        readerSettings = readerSettings,
                        isFirstPage = pageIndex == 0,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // 显示下一章内容
            else -> {
                pageData.nextChapterData?.let { nextChapter ->
                    val pageIndex = globalPage - previousChapterSize - currentChapterSize
                    if (pageIndex in 0 until nextChapter.pages.size) {
                        CurrentPageContent(
                            pageData = nextChapter,
                            pageIndex = pageIndex,
                            readerSettings = readerSettings,
                            isFirstPage = pageIndex == 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}