package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.wdp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 覆盖翻页容器 - 优化版本，修复边界处理，支持书籍详情页
 */
@Composable
fun CoverFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null, // 新增：iOS侧滑返回回调
    onClick: () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var flipDirection by remember { mutableStateOf(FlipDirection.NEXT) }

    // 移除回弹动画，使用简单的状态跟踪
    val isDragging = dragOffset != 0f

    // 检查是否在书籍详情页
    val isOnBookDetailPage = currentPageIndex == -1 && pageData.hasBookDetailPage

    // 主要内容区域 - 导航信息现在包含在PageContentDisplay中
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* 不需要特殊处理 */ },
                    onDragEnd = {
                        val threshold = size.width * 0.15f // 降低阈值，更容易触发

                        if (abs(dragOffset) >= threshold) {
                            // 根据拖拽方向和当前状态判断操作类型
                            when (flipDirection) {
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
                                        // 章节边界，切换到上一章
                                        onChapterChange(FlipDirection.PREVIOUS)
                                    }
                                }
                            }
                        }

                        // 立即重置状态，不使用动画
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onDrag = { _, dragAmount ->
                        val deltaX = dragAmount.x
                        val maxOffset = size.width * 0.8f // 限制最大拖拽距离

                        val newDirection = if (deltaX > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
                        
                        // 检查是否可以继续拖拽
                        val canDrag = when (newDirection) {
                            FlipDirection.NEXT -> {
                                // 向左拖拽（下一页/下一章）
                                when {
                                    isOnBookDetailPage -> true // 从书籍详情页可以翻到内容
                                    currentPageIndex < pageData.contentPageCount - 1 -> true // 章节内还有下一页
                                    pageData.nextChapterData != null -> true // 有下一章
                                    else -> false
                                }
                            }
                            FlipDirection.PREVIOUS -> {
                                // 向右拖拽（上一页/上一章）
                                when {
                                    currentPageIndex > 0 -> true // 章节内还有上一页
                                    currentPageIndex == 0 && pageData.hasBookDetailPage -> true // 第一页可以返回书籍详情页
                                    pageData.previousChapterData != null -> true // 有上一章
                                    else -> false
                                }
                            }
                        }

                        if (canDrag) {
                            dragOffset = (dragOffset + deltaX).coerceIn(-maxOffset, maxOffset)
                            flipDirection = newDirection
                        } else if (newDirection == FlipDirection.PREVIOUS && deltaX > 0) {
                            // 检查是否应该触发iOS侧滑返回
                            val shouldTriggerSwipeBack = when {
                                isOnBookDetailPage -> true // 在书籍详情页时
                                currentPageIndex == 0 && !pageData.hasBookDetailPage && pageData.previousChapterData == null -> true // 第一页且没有上一章
                                else -> false
                            }
                            
                            if (shouldTriggerSwipeBack) {
                                onSwipeBack?.invoke()
                            }
                        }
                    }
                )
            }
    ) {
        // 计算目标页面信息 - 优化版本，支持书籍详情页
        val targetPageInfo = remember(dragOffset, currentPageIndex, pageData) {
            when {
                dragOffset > 50f -> {
                    // 向右拖拽，显示上一页/上一章/书籍详情页
                    when {
                        currentPageIndex > 0 -> {
                            TargetPageInfo(
                                pageData,
                                currentPageIndex - 1,
                                FlipDirection.PREVIOUS,
                                false
                            )
                        }

                        currentPageIndex == 0 && pageData.hasBookDetailPage -> {
                            TargetPageInfo(pageData, -1, FlipDirection.PREVIOUS, true)
                        }

                        pageData.previousChapterData != null -> {
                            val lastPageIndex = pageData.previousChapterData.contentPageCount - 1
                            TargetPageInfo(
                                pageData.previousChapterData,
                                lastPageIndex,
                                FlipDirection.PREVIOUS,
                                false
                            )
                        }

                        else -> null
                    }
                }

                dragOffset < -50f -> {
                    // 向左拖拽，显示下一页/下一章/内容页
                    when {
                        isOnBookDetailPage -> {
                            TargetPageInfo(pageData, 0, FlipDirection.NEXT, false)
                        }

                        currentPageIndex < pageData.contentPageCount - 1 -> {
                            TargetPageInfo(
                                pageData,
                                currentPageIndex + 1,
                                FlipDirection.NEXT,
                                false
                            )
                        }

                        pageData.nextChapterData != null -> {
                            TargetPageInfo(pageData.nextChapterData, 0, FlipDirection.NEXT, false)
                        }

                        else -> null
                    }
                }

                else -> null
            }
        }

        // 显示背景页（目标页面）
        targetPageInfo?.let { targetInfo ->
            if (targetInfo.isBookDetailPage) {
                // 显示书籍详情页内容
                PageContentDisplay(
                    page = "",
                    chapterName = targetInfo.chapterData.chapterName,
                    isFirstPage = false,
                    isLastPage = false,
                    isBookDetailPage = true,
                    bookInfo = targetInfo.chapterData.bookInfo,
                    nextChapterData = targetInfo.chapterData.nextChapterData,
                    previousChapterData = targetInfo.chapterData.previousChapterData,
                    readerSettings = readerSettings,
                    onSwipeBack = onSwipeBack,
                    onPageChange = { direction ->
                        onPageChange(direction)
                    },
                    showNavigationInfo = false, // 书籍详情页不显示导航信息
                    currentPageIndex = targetInfo.pageIndex,
                    totalPages = targetInfo.chapterData.totalPageCount,
                    onClick = onClick
                )
            } else {
                // 显示正常页面内容
                val pageContent =
                    if (targetInfo.pageIndex >= 0 && targetInfo.pageIndex < targetInfo.chapterData.pages.size) {
                        targetInfo.chapterData.pages[targetInfo.pageIndex]
                    } else ""

                PageContentDisplay(
                    page = pageContent,
                    chapterName = targetInfo.chapterData.chapterName,
                    isFirstPage = targetInfo.pageIndex == 0,
                    isLastPage = targetInfo.pageIndex == targetInfo.chapterData.pages.size - 1,
                    isBookDetailPage = false,
                    bookInfo = null,
                    nextChapterData = targetInfo.chapterData.nextChapterData,
                    previousChapterData = targetInfo.chapterData.previousChapterData,
                    readerSettings = readerSettings,
                    onNavigateToReader = onNavigateToReader,
                    onPageChange = { direction ->
                        onPageChange(direction)
                    },
                    showNavigationInfo = true, // 正常页面显示导航信息
                    currentPageIndex = targetInfo.pageIndex + 1, // 显示从1开始的页码
                    totalPages = targetInfo.chapterData.contentPageCount,
                    onClick = onClick
                )
            }
        }

        // 当前页（覆盖效果）- 移除所有动画和阴影抖动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .then(
                    if (abs(dragOffset) > 10f) {
                        Modifier.shadow(
                            elevation = 6.wdp,
                            shape = RectangleShape
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            if (isOnBookDetailPage) {
                // 当前页是书籍详情页
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
                    onPageChange = { direction ->
                        onPageChange(direction)
                    },
                    showNavigationInfo = false, // 书籍详情页不显示导航信息
                    currentPageIndex = currentPageIndex,
                    totalPages = pageData.totalPageCount,
                    onClick = onClick
                )
            } else {
                // 当前页是正常内容页
                val pageContent =
                    if (currentPageIndex >= 0 && currentPageIndex < pageData.pages.size) {
                        pageData.pages[currentPageIndex]
                    } else ""

                PageContentDisplay(
                    page = pageContent,
                    chapterName = pageData.chapterName,
                    isFirstPage = currentPageIndex == 0,
                    isLastPage = currentPageIndex == pageData.pages.size - 1,
                    isBookDetailPage = false,
                    bookInfo = null,
                    nextChapterData = pageData.nextChapterData,
                    previousChapterData = pageData.previousChapterData,
                    readerSettings = readerSettings,
                    onNavigateToReader = onNavigateToReader,
                    onPageChange = { direction ->
                        onPageChange(direction)
                    },
                    showNavigationInfo = true, // 正常页面显示导航信息
                    currentPageIndex = currentPageIndex + 1, // 显示从1开始的页码
                    totalPages = pageData.contentPageCount,
                    onClick = onClick
                )
            }
        }
    }
}

/**
 * 目标页面信息
 */
private data class TargetPageInfo(
    val chapterData: PageData,
    val pageIndex: Int,
    val direction: FlipDirection,
    val isBookDetailPage: Boolean = false
)
