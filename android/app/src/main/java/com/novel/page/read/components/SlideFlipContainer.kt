package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * 平移翻页容器 - ViewPager风格，支持章节切换
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

    // 处理页面变化逻辑
    LaunchedEffect(pagerState.currentPage) {
        val currentGlobalIndex = globalPageIndex
        if (pagerState.currentPage != currentGlobalIndex) {
            val previousChapterSize = pageData.previousChapterData?.pages?.size ?: 0
            val currentChapterSize = pageData.pages.size

            when {
                // 切换到上一章
                pagerState.currentPage < previousChapterSize -> {
                    onChapterChange(FlipDirection.PREVIOUS)
                }
                // 切换到下一章
                pagerState.currentPage >= previousChapterSize + currentChapterSize -> {
                    onChapterChange(FlipDirection.NEXT)
                }
                // 当前章节内翻页
                else -> {
                    val newPageIndex = pagerState.currentPage - previousChapterSize
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

    // 同步外部页面索引变化
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val newGlobalIndex = currentPageIndex + (pageData.previousChapterData?.pages?.size ?: 0)
        if (newGlobalIndex != pagerState.currentPage && newGlobalIndex in 0 until totalPages) {
            pagerState.animateScrollToPage(newGlobalIndex)
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
                    CurrentPageContent(
                        pageData = prevChapter,
                        pageIndex = globalPage,
                        readerSettings = readerSettings,
                        isFirstPage = globalPage == 0,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // 显示当前章节内容
            globalPage < previousChapterSize + currentChapterSize -> {
                val pageIndex = globalPage - previousChapterSize
                CurrentPageContent(
                    pageData = pageData,
                    pageIndex = pageIndex,
                    readerSettings = readerSettings,
                    isFirstPage = pageIndex == 0,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 显示下一章内容
            else -> {
                pageData.nextChapterData?.let { nextChapter ->
                    val pageIndex = globalPage - previousChapterSize - currentChapterSize
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