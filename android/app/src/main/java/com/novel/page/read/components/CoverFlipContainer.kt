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
 * 覆盖翻页容器 - 优化版本，移除回弹动画
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
                        val threshold = size.width * 0.25f // 降低阈值，更容易触发

                        if (abs(dragOffset) >= threshold) {
                            // 直接处理页面切换，无额外动画
                            handlePageFlip(
                                currentPageIndex,
                                pageData,
                                flipDirection,
                                onPageChange,
                                onChapterChange,
                            )
                        }

                        // 立即重置状态，不使用动画
                        dragOffset = 0f
                    },
                    onDrag = { _, dragAmount ->
                        val deltaX = dragAmount.x
                        val maxOffset = size.width * 0.8f // 限制最大拖拽距离

                        dragOffset = (dragOffset + deltaX).coerceIn(-maxOffset, maxOffset)
                        flipDirection = if (deltaX > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
                    }
                )
            }
            .clickable(enabled = !isDragging) { onClick() }
    ) {
        // 背景页（目标页面内容）
        val targetPageIndex = when {
            dragOffset > 0 && currentPageIndex > 0 -> currentPageIndex - 1
            dragOffset > 0 && currentPageIndex == 0 && pageData.previousChapterData != null ->
                pageData.previousChapterData.pages.size - 1
            dragOffset < 0 && currentPageIndex < pageData.pages.size - 1 -> currentPageIndex + 1
            dragOffset < 0 && currentPageIndex == pageData.pages.size - 1 && pageData.nextChapterData != null -> 0
            else -> currentPageIndex
        }

        val targetDirection = if (dragOffset > 0) FlipDirection.PREVIOUS else FlipDirection.NEXT
        val hasTargetPage = when (targetDirection) {
            FlipDirection.NEXT -> currentPageIndex < pageData.pages.size - 1 || pageData.nextChapterData != null
            FlipDirection.PREVIOUS -> currentPageIndex > 0 || pageData.previousChapterData != null
        }

        // 显示背景页
        if (hasTargetPage && abs(dragOffset) > 10f) {
            val targetChapterData = when {
                targetPageIndex in pageData.pages.indices -> pageData
                targetDirection == FlipDirection.NEXT -> pageData.nextChapterData
                targetDirection == FlipDirection.PREVIOUS -> pageData.previousChapterData
                else -> null
            }

            targetChapterData?.let { chapterData ->
                val finalPageIndex = when {
                    chapterData == pageData -> targetPageIndex
                    targetDirection == FlipDirection.NEXT -> 0
                    else -> chapterData.pages.size - 1
                }

                CurrentPageContent(
                    pageData = chapterData,
                    pageIndex = finalPageIndex,
                    readerSettings = readerSettings,
                    isFirstPage = finalPageIndex == 0,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(-1f)
                )
            }
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
