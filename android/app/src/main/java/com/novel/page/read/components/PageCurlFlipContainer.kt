package com.novel.page.read.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.novel.utils.wdp
import com.novel.utils.SwipeBackContainer

/**
 * PageCurl仿真翻页容器
 *
 * 使用PageCurl库实现真实的书页卷曲翻页效果，并集成纸张纹理
 * 支持章节边界检测和自动章节切换，支持书籍详情页
 * 修复版本：解决边界翻页和章节切换问题，添加书籍详情页支持，增强预加载
 *
 * @param pageData 页面数据
 * @param currentPageIndex 当前页面索引
 * @param readerSettings 阅读器设置
 * @param onPageChange 页面变化回调
 * @param onChapterChange 章节变化回调
 * @param onNavigateToReader 导航到阅读器回调
 * @param onSwipeBack iOS侧滑返回回调
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
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    // 构建虚拟页面序列，支持章节边界和书籍详情页
    val virtualPages = remember(pageData) {
        buildList {
            // 添加上一章的最后一页（如果存在且已预加载）
            if (pageData.previousChapterData != null && pageData.previousChapterData.pages.isNotEmpty()) {
                add(VirtualPage.PreviousChapter(pageData.previousChapterData))
            }
            
            // 添加书籍详情页（如果当前章节支持）
            if (pageData.hasBookDetailPage) {
                add(VirtualPage.BookDetailPage)
            }
            
            // 添加当前章节的所有页面
            pageData.pages.forEachIndexed { index, _ ->
                add(VirtualPage.CurrentChapter(index))
            }
            
            // 添加下一章的第一页（如果存在且已预加载）
            if (pageData.nextChapterData != null && pageData.nextChapterData.pages.isNotEmpty()) {
                add(VirtualPage.NextChapter(pageData.nextChapterData))
            }
        }
    }

    val totalPages = virtualPages.size.coerceAtLeast(1)

    // 计算当前页在虚拟页面序列中的索引
    val virtualCurrentIndex = remember(pageData, currentPageIndex) {
        val previousOffset = if (pageData.previousChapterData?.pages?.isNotEmpty() == true) 1 else 0
        val bookDetailOffset = if (pageData.hasBookDetailPage) 1 else 0
        
        val targetIndex = when {
            currentPageIndex == -1 && pageData.hasBookDetailPage -> {
                // 书籍详情页
                previousOffset
            }
            currentPageIndex >= 0 -> {
                // 正常内容页
                val safeIndex = currentPageIndex.coerceIn(0, pageData.pages.size - 1)
                safeIndex + previousOffset + bookDetailOffset
            }
            else -> previousOffset + bookDetailOffset
        }
        
        targetIndex.coerceIn(0, totalPages - 1)
    }

    // 标记是否正在处理外部变化
    var isHandlingExternalChange by remember { mutableStateOf(false) }

    // PageCurl状态管理
    val pageCurlState = rememberPageCurlState(
        initialCurrent = virtualCurrentIndex
    )

    val currentVirtualPage = virtualPages.getOrNull(pageCurlState.current)
    val isOnBookDetailPage = currentVirtualPage is VirtualPage.BookDetailPage

    // 当在书籍详情页时，使用SwipeBackContainer包裹以支持侧滑返回
    if (isOnBookDetailPage) {
        SwipeBackContainer(
            modifier = Modifier.fillMaxSize(),
            onSwipeComplete = onSwipeBack,
            onLeftSwipeToReader = {
                onPageChange(FlipDirection.NEXT)
            }
        ) {
            // 在SwipeBackContainer中只渲染详情页
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
                showNavigationInfo = false,
                currentPageIndex = 0,
                totalPages = 1,
                onClick = onClick
            )
        }
        return
    }

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
                target = androidx.compose.ui.geometry.Rect(0.25f, 0.0f, 1.0f, 1.0f) // 右侧75%区域
            ),
            backward = com.novel.page.component.pagecurl.config.PageCurlConfig.TargetTapInteraction.Config(
                target = androidx.compose.ui.geometry.Rect(0.0f, 0.0f, 0.75f, 1.0f) // 左侧75%区域
            )
        )
    )

    // 同步外部页面索引变化
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val previousOffset = if (pageData.previousChapterData?.pages?.isNotEmpty() == true) 1 else 0
        val bookDetailOffset = if (pageData.hasBookDetailPage) 1 else 0
        
        val newVirtualIndex = when {
            currentPageIndex == -1 && pageData.hasBookDetailPage -> {
                // 书籍详情页
                previousOffset
            }
            currentPageIndex >= 0 -> {
                // 正常内容页
                val safeIndex = currentPageIndex.coerceIn(0, pageData.pages.size - 1)
                safeIndex + previousOffset + bookDetailOffset
            }
            else -> previousOffset + bookDetailOffset
        }
        
        val targetIndex = newVirtualIndex.coerceIn(0, totalPages - 1)
        if (targetIndex != pageCurlState.current) {
            isHandlingExternalChange = true
            try {
                pageCurlState.snapTo(targetIndex)
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
            val virtualPage = virtualPages.getOrNull(currentVirtualIndex)
            
            when (virtualPage) {
                is VirtualPage.PreviousChapter -> {
                    // 翻到上一章
                    onChapterChange(FlipDirection.PREVIOUS)
                }
                is VirtualPage.NextChapter -> {
                    // 翻到下一章
                    onChapterChange(FlipDirection.NEXT)
                }
                is VirtualPage.BookDetailPage -> {
                    // 翻到书籍详情页
                    onPageChange(FlipDirection.PREVIOUS)
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
                null -> {
                    // 虚拟页面不存在，忽略
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 主要内容区域 - 导航信息现在包含在PageContentDisplay中
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (totalPages > 0) {
                PageCurl(
                    count = totalPages,
                    state = pageCurlState,
                    config = config,
                    modifier = Modifier.fillMaxSize()
                ) { virtualPageIndex ->
                    val virtualPage = virtualPages.getOrNull(virtualPageIndex)
                    
                    if (virtualPage != null) {
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
                                    if (lastPageIndex >= 0 && lastPageIndex < prevChapter.pages.size) {
                                        PageContentDisplay(
                                            page = prevChapter.pages[lastPageIndex],
                                            chapterName = prevChapter.chapterName,
                                            isFirstPage = false,
                                            isLastPage = true,
                                            nextChapterData = pageData,
                                            readerSettings = readerSettings,
                                            onNavigateToReader = onNavigateToReader,
                                            currentPageIndex = lastPageIndex + 1,
                                            totalPages = prevChapter.pages.size,
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
                                            onNavigateToReader = onNavigateToReader,
                                            currentPageIndex = 1,
                                            totalPages = nextChapter.pages.size,
                                            onClick = onClick
                                        )
                                    }
                                }
                                is VirtualPage.BookDetailPage -> {
                                    // 显示书籍详情页
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
                                            onNavigateToReader = onNavigateToReader,
                                            currentPageIndex = pageIndex + 1,
                                            totalPages = pageData.pages.size,
                                            onClick = onClick
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 如果虚拟页面不存在，显示空白页面
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(readerSettings.backgroundColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 虚拟页面类型，用于统一处理章节边界和书籍详情页
 */
private sealed class VirtualPage {
    data class PreviousChapter(val chapterData: PageData) : VirtualPage()
    data class CurrentChapter(val pageIndex: Int) : VirtualPage()
    data class NextChapter(val chapterData: PageData) : VirtualPage()
    data object BookDetailPage : VirtualPage() // 新增：书籍详情页
}