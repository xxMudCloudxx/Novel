package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    onClick: () -> Unit
) {
    var swipeDirection by remember { mutableStateOf<FlipDirection?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        swipeDirection?.let { direction ->
                            handlePageFlip(
                                currentPageIndex,
                                pageData,
                                direction,
                                onPageChange,
                                onChapterChange
                            )
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
        CurrentPageContent(
            pageData = pageData,
            pageIndex = currentPageIndex,
            readerSettings = readerSettings,
            isFirstPage = currentPageIndex == 0,
            modifier = Modifier.fillMaxSize()
        )
    }
}