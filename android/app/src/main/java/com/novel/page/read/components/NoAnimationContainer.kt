package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.novel.page.read.utils.handlePageFlip
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.wdp
import com.novel.utils.SwipeBackContainer
import kotlin.math.abs

/**
 * 无动画翻页容器
 */
@Composable
fun NoAnimationContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var swipeDirection by remember { mutableStateOf<FlipDirection?>(null) }

    // 检查是否在书籍详情页
    val isOnBookDetailPage = currentPageIndex == -1 && pageData.hasBookDetailPage

    // 主要内容区域
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        swipeDirection?.let { direction ->
                            if (isOnBookDetailPage) {
                                when (direction) {
                                    FlipDirection.NEXT -> {
                                        // 从书籍详情页翻到第一页内容
                                        onPageChange(FlipDirection.NEXT)
                                    }
                                    FlipDirection.PREVIOUS -> {
                                        // 从书籍详情页向前滑动，触发iOS侧滑返回
                                        onSwipeBack?.invoke()
                                    }
                                }
                            } else {
                                // 正常页面的翻页逻辑
                                handlePageFlip(
                                    currentPageIndex,
                                    pageData,
                                    direction,
                                    onPageChange,
                                    onChapterChange
                                )
                            }
                        }
                        swipeDirection = null
                    },
                    onDrag = { _, dragAmount ->
                        val totalDragX = dragAmount.x
                        if (abs(totalDragX) > 20) {
                            swipeDirection =
                                if (totalDragX > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
                        }
                    }
                )
            }
            .clickable { onClick() }
    ) {
        if (isOnBookDetailPage) {
            // 显示书籍详情页，使用SwipeBackContainer支持iOS侧滑返回
            SwipeBackContainer(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = readerSettings.backgroundColor,
                onSwipeComplete = onSwipeBack,
                onLeftSwipeToReader = {
                    // 左滑翻到第一页内容
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
                    onNavigateToReader = onNavigateToReader,
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
        } else {
            // 正常页面内容
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部导航信息
                ReaderNavigationInfo(
                    chapterName = pageData.chapterName,
                    modifier = Modifier.padding(start = 12.wdp, top = 12.wdp)
                )

                // 主要内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = 16.wdp, vertical = 10.wdp)
                ) {
                    CurrentPageContent(
                        pageData = pageData,
                        pageIndex = currentPageIndex,
                        readerSettings = readerSettings,
                        isFirstPage = currentPageIndex == 0,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 底部页面信息
                ReaderPageInfo(
                    chapterNum = currentPageIndex + 1, // 显示从1开始的页码
                    totalPages = pageData.pages.size,
                    modifier = Modifier.padding(start = 12.wdp, bottom = 3.wdp)
                )
            }
        }
    }
}