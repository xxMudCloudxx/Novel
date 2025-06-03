package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.zIndex
import com.novel.page.read.utils.handlePageFlip
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.wdp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 覆盖翻页容器 - 优化版本，修复边界处理
 */
@Composable
fun CoverFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onClick: () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var flipDirection by remember { mutableStateOf(FlipDirection.NEXT) }

    // 移除回弹动画，使用简单的状态跟踪
    val isDragging = dragOffset != 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* 不需要特殊处理 */ },
                    onDragEnd = {
                        val threshold = size.width * 0.2f // 降低阈值，更容易触发

                        if (abs(dragOffset) >= threshold) {
                            // 优化的页面切换逻辑
                            when (flipDirection) {
                                FlipDirection.NEXT -> {
                                    if (currentPageIndex < pageData.pages.size - 1) {
                                        // 当前章节内还有下一页
                                        onPageChange(FlipDirection.NEXT)
                                    } else if (pageData.nextChapterData != null) {
                                        // 切换到下一章
                                        onChapterChange(FlipDirection.NEXT)
                                    }
                                }
                                FlipDirection.PREVIOUS -> {
                                    if (currentPageIndex > 0) {
                                        // 当前章节内还有上一页
                                        onPageChange(FlipDirection.PREVIOUS)
                                    } else if (pageData.previousChapterData != null) {
                                        // 切换到上一章
                                        onChapterChange(FlipDirection.PREVIOUS)
                                    }
                                }
                            }
                        }

                        // 立即重置状态，不使用动画
                        dragOffset = 0f
                    },
                    onDrag = { _, dragAmount ->
                        val deltaX = dragAmount.x
                        val maxOffset = size.width * 0.8f // 限制最大拖拽距离
                        
                        // 检查是否可以继续拖拽
                        val canDragNext = currentPageIndex < pageData.pages.size - 1 || pageData.nextChapterData != null
                        val canDragPrevious = currentPageIndex > 0 || pageData.previousChapterData != null
                        
                        val newDirection = if (deltaX > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
                        val canDrag = when (newDirection) {
                            FlipDirection.NEXT -> canDragNext
                            FlipDirection.PREVIOUS -> canDragPrevious
                        }
                        
                        if (canDrag) {
                            dragOffset = (dragOffset + deltaX).coerceIn(-maxOffset, maxOffset)
                            flipDirection = newDirection
                        }
                    }
                )
            }
            .clickable(enabled = !isDragging) { onClick() }
    ) {
        // 计算目标页面信息 - 优化版本
        val targetPageInfo = remember(dragOffset, currentPageIndex, pageData) {
            when {
                dragOffset > 50f -> {
                    // 向右拖拽，显示上一页/上一章
                    when {
                        currentPageIndex > 0 -> {
                            TargetPageInfo(pageData, currentPageIndex - 1, FlipDirection.PREVIOUS)
                        }
                        pageData.previousChapterData != null -> {
                            val lastPageIndex = pageData.previousChapterData.pages.size - 1
                            TargetPageInfo(pageData.previousChapterData, lastPageIndex, FlipDirection.PREVIOUS)
                        }
                        else -> null
                    }
                }
                dragOffset < -50f -> {
                    // 向左拖拽，显示下一页/下一章
                    when {
                        currentPageIndex < pageData.pages.size - 1 -> {
                            TargetPageInfo(pageData, currentPageIndex + 1, FlipDirection.NEXT)
                        }
                        pageData.nextChapterData != null -> {
                            TargetPageInfo(pageData.nextChapterData, 0, FlipDirection.NEXT)
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }

        // 显示背景页（目标页面）
        targetPageInfo?.let { targetInfo ->
            CurrentPageContent(
                pageData = targetInfo.chapterData,
                pageIndex = targetInfo.pageIndex,
                readerSettings = readerSettings,
                isFirstPage = targetInfo.pageIndex == 0,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(-1f)
            )
        }

        // 当前页（覆盖效果）- 移除所有动画和阴影抖动
        CurrentPageContent(
            pageData = pageData,
            pageIndex = currentPageIndex,
            readerSettings = readerSettings,
            isFirstPage = currentPageIndex == 0,
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
        )
    }
}

/**
 * 目标页面信息
 */
private data class TargetPageInfo(
    val chapterData: PageData,
    val pageIndex: Int,
    val direction: FlipDirection
)
