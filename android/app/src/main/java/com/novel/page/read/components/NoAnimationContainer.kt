package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.wdp
import com.novel.utils.SwipeBackContainer
import kotlin.math.abs

/**
 * 无动画翻页容器 - 优化版本，修复章节切换问题
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
    var swipeAmount by remember { mutableFloatStateOf(0f) }

    // 检查是否在书籍详情页
    val isOnBookDetailPage = currentPageIndex == -1 && pageData.hasBookDetailPage

    // 主要内容区域
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        swipeDirection = null
                        swipeAmount = 0f
                    },
                    onDragEnd = {
                        swipeDirection?.let { direction ->
                            // 判断是否达到翻页阈值
                            val threshold = size.width * 0.2f
                            if (abs(swipeAmount) >= threshold) {
                                // 根据方向和当前状态判断操作类型
                                when (direction) {
                                    FlipDirection.NEXT -> {
                                        if (isOnBookDetailPage || currentPageIndex < pageData.contentPageCount - 1) {
                                            // 在书籍详情页或章节内翻页
                                            onPageChange(FlipDirection.NEXT)
                                        } else {
                                            // 章节边界，切换到下一章
                                            onChapterChange(FlipDirection.NEXT)
                                        }
                                    }
                                    FlipDirection.PREVIOUS -> {
                                        if (currentPageIndex > 0 || (currentPageIndex == 0 && pageData.hasBookDetailPage)) {
                                            // 章节内翻页或翻到书籍详情页
                                            onPageChange(FlipDirection.PREVIOUS)
                                        } else {
                                            // 章节边界，切换到上一章，或者触发侧滑返回
                                            if (pageData.previousChapterData != null) {
                                                onChapterChange(FlipDirection.PREVIOUS)
                                            } else {
                                                onSwipeBack?.invoke()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        swipeDirection = null
                        swipeAmount = 0f
                    },
                    onDragCancel = { 
                        swipeDirection = null
                        swipeAmount = 0f
                    },
                    onDrag = { _, dragAmount ->
                        val deltaX = dragAmount.x
                        swipeAmount += deltaX
                        
                        // 仅在 swipeDirection 未设置时确定方向，避免拖拽过程中方向改变
                        if (swipeDirection == null && abs(deltaX) > 20) {
                            val newDirection = if (deltaX > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
                            
                            // 检查是否可以在该方向进行操作
                            val canOperate = when (newDirection) {
                                FlipDirection.NEXT -> {
                                    // 向左滑动（下一页/下一章）
                                    when {
                                        isOnBookDetailPage -> true // 从书籍详情页可以翻到内容
                                        currentPageIndex < pageData.contentPageCount - 1 -> true // 章节内还有下一页
                                        pageData.nextChapterData != null -> true // 有下一章
                                        else -> false
                                    }
                                }
                                FlipDirection.PREVIOUS -> {
                                    // 向右滑动（上一页/上一章/返回）
                                    when {
                                        currentPageIndex > 0 -> true // 章节内还有上一页
                                        currentPageIndex == 0 && pageData.hasBookDetailPage -> true // 第一页可以返回书籍详情页
                                        pageData.previousChapterData != null -> true // 有上一章
                                        isOnBookDetailPage -> true // 书籍详情页可以侧滑返回
                                        currentPageIndex == 0 -> true // 第一页可以侧滑返回
                                        else -> false
                                    }
                                }
                            }
                            
                            if (canOperate) {
                                swipeDirection = newDirection
                            }
                        }
                    }
                )
            }
    ) {
        val pageContent = when {
            isOnBookDetailPage -> ""
            currentPageIndex in pageData.pages.indices -> pageData.pages[currentPageIndex]
            else -> ""
        }

        PageContentDisplay(
            page = pageContent,
            chapterName = pageData.chapterName,
            isFirstPage = currentPageIndex == 0,
            isLastPage = currentPageIndex == pageData.pages.size - 1,
            isBookDetailPage = isOnBookDetailPage,
            bookInfo = pageData.bookInfo,
            nextChapterData = pageData.nextChapterData,
            previousChapterData = pageData.previousChapterData,
            readerSettings = readerSettings,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onPageChange = onPageChange,
            showNavigationInfo = true,
            currentPageIndex = if (isOnBookDetailPage) 0 else currentPageIndex + 1,
            totalPages = if (isOnBookDetailPage) 1 else pageData.contentPageCount,
            onClick = onClick
        )
    }
}