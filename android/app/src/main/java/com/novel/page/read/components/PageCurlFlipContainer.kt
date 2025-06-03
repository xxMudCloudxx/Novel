package com.novel.page.read.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.novel.page.component.PaperTexture
import com.novel.page.component.pagecurl.config.rememberPageCurlConfig
import com.novel.page.component.pagecurl.page.ExperimentalPageCurlApi
import com.novel.page.component.pagecurl.page.PageCurl
import com.novel.page.component.pagecurl.page.rememberPageCurlState
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * PageCurl仿真翻页容器
 *
 * 使用PageCurl库实现真实的书页卷曲翻页效果，并集成纸张纹理
 * 支持章节边界检测和自动章节切换
 * 修复版本：解决边界翻页和章节切换问题
 *
 * @param pageData 页面数据
 * @param currentPageIndex 当前页面索引
 * @param readerSettings 阅读器设置
 * @param onPageChange 页面变化回调
 * @param onChapterChange 章节变化回调
 * @param onClick 点击回调
 */
@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PageCurlFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onClick: () -> Unit
) {
    // 构建虚拟页面序列，支持章节边界
    val virtualPages = remember(pageData) {
        buildList {
            // 添加上一章的最后一页（如果存在）
            if (pageData.previousChapterData != null && pageData.previousChapterData.pages.isNotEmpty()) {
                add(VirtualPage.PreviousChapter(pageData.previousChapterData))
            }
            
            // 添加当前章节的所有页面
            pageData.pages.forEachIndexed { index, _ ->
                add(VirtualPage.CurrentChapter(index))
            }
            
            // 添加下一章的第一页（如果存在）
            if (pageData.nextChapterData != null && pageData.nextChapterData.pages.isNotEmpty()) {
                add(VirtualPage.NextChapter(pageData.nextChapterData))
            }
        }
    }

    val totalPages = virtualPages.size

    // 计算当前页在虚拟页面序列中的索引
    val virtualCurrentIndex = remember(pageData, currentPageIndex) {
        val previousOffset = if (pageData.previousChapterData?.pages?.isNotEmpty() == true) 1 else 0
        val targetIndex = currentPageIndex + previousOffset
        targetIndex.coerceIn(0, totalPages - 1)
    }

    // 标记是否正在处理外部变化
    var isHandlingExternalChange by remember { mutableStateOf(false) }

    // PageCurl状态管理
    val pageCurlState = rememberPageCurlState(
        initialCurrent = virtualCurrentIndex
    )

    // PageCurl配置 - 优化拖拽和点击区域
    val config = rememberPageCurlConfig(
        backPageColor = readerSettings.backgroundColor,
        backPageContentAlpha = 0.15f,
        shadowColor = if (readerSettings.backgroundColor.luminance() > 0.5f) Color.Black else Color.White,
        shadowAlpha = 0.25f,
        shadowRadius = 12.dp,
        dragForwardEnabled = true,
        dragBackwardEnabled = true,
        tapForwardEnabled = true,
        tapBackwardEnabled = true,
        // 扩大点击区域，改善边界操作体验
        tapInteraction = com.novel.page.component.pagecurl.config.PageCurlConfig.TargetTapInteraction(
            forward = com.novel.page.component.pagecurl.config.PageCurlConfig.TargetTapInteraction.Config(
                target = androidx.compose.ui.geometry.Rect(0.3f, 0.0f, 1.0f, 1.0f) // 右侧70%区域
            ),
            backward = com.novel.page.component.pagecurl.config.PageCurlConfig.TargetTapInteraction.Config(
                target = androidx.compose.ui.geometry.Rect(0.0f, 0.0f, 0.7f, 1.0f) // 左侧70%区域
            )
        )
    )

    // 同步外部页面索引变化
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val newVirtualIndex = currentPageIndex + (if (pageData.previousChapterData?.pages?.isNotEmpty() == true) 1 else 0)
        if (newVirtualIndex != pageCurlState.current && newVirtualIndex in 0 until totalPages) {
            isHandlingExternalChange = true
            try {
                pageCurlState.snapTo(newVirtualIndex)
            } finally {
                isHandlingExternalChange = false
            }
        }
    }

    // 监听PageCurl状态变化并处理边界情况 - 优化版本
    LaunchedEffect(pageCurlState.current) {
        if (isHandlingExternalChange) return@LaunchedEffect
        
        val currentVirtualIndex = pageCurlState.current
        if (currentVirtualIndex != virtualCurrentIndex && currentVirtualIndex in 0 until totalPages) {
            val virtualPage = virtualPages[currentVirtualIndex]
            
            when (virtualPage) {
                is VirtualPage.PreviousChapter -> {
                    // 翻到上一章
                    onChapterChange(FlipDirection.PREVIOUS)
                }
                is VirtualPage.NextChapter -> {
                    // 翻到下一章
                    onChapterChange(FlipDirection.NEXT)
                }
                is VirtualPage.CurrentChapter -> {
                    // 当前章节内翻页
                    val newPageIndex = virtualPage.pageIndex
                    if (newPageIndex != currentPageIndex && newPageIndex in 0 until pageData.pages.size) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (totalPages > 0) {
            PageCurl(
                count = totalPages,
                state = pageCurlState,
                config = config,
                modifier = Modifier.fillMaxSize()
            ) { virtualPageIndex ->
                if (virtualPageIndex in virtualPages.indices) {
                    val virtualPage = virtualPages[virtualPageIndex]
                    
                    // 渲染每一页内容，包含纸张纹理
                    PaperTexture(
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.04f,
                        density = 1.2f,
                        seed = virtualPageIndex.toLong() * 42L
                    ) {
                        when (virtualPage) {
                            is VirtualPage.PreviousChapter -> {
                                val prevChapter = virtualPage.chapterData
                                val lastPageIndex = prevChapter.pages.size - 1
                                if (lastPageIndex >= 0) {
                                    PageContentDisplay(
                                        page = prevChapter.pages[lastPageIndex],
                                        chapterName = prevChapter.chapterName,
                                        isFirstPage = false,
                                        isLastPage = true,
                                        nextChapterData = pageData,
                                        readerSettings = readerSettings,
                                        onClick = onClick
                                    )
                                }
                            }
                            is VirtualPage.NextChapter -> {
                                val nextChapter = virtualPage.chapterData
                                if (nextChapter.pages.isNotEmpty()) {
                                    PageContentDisplay(
                                        page = nextChapter.pages[0],
                                        chapterName = nextChapter.chapterName,
                                        isFirstPage = true,
                                        isLastPage = false,
                                        previousChapterData = pageData,
                                        readerSettings = readerSettings,
                                        onClick = onClick
                                    )
                                }
                            }
                            is VirtualPage.CurrentChapter -> {
                                val pageIndex = virtualPage.pageIndex
                                if (pageIndex in 0 until pageData.pages.size) {
                                    PageContentDisplay(
                                        page = pageData.pages[pageIndex],
                                        chapterName = pageData.chapterName,
                                        isFirstPage = pageIndex == 0,
                                        isLastPage = pageIndex == pageData.pages.size - 1,
                                        nextChapterData = if (pageIndex == pageData.pages.size - 1) pageData.nextChapterData else null,
                                        previousChapterData = if (pageIndex == 0) pageData.previousChapterData else null,
                                        readerSettings = readerSettings,
                                        onClick = onClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 虚拟页面类型，用于统一处理章节边界
 */
private sealed class VirtualPage {
    data class PreviousChapter(val chapterData: PageData) : VirtualPage()
    data class CurrentChapter(val pageIndex: Int) : VirtualPage()
    data class NextChapter(val chapterData: PageData) : VirtualPage()
}