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
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.page.read.viewmodel.VirtualPage
import com.novel.utils.wdp
import com.novel.utils.SwipeBackContainer
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * PageCurl仿真翻页容器
 *
 * 使用PageCurl库实现真实的书页卷曲翻页效果，并集成纸张纹理
 * 支持章节边界检测和自动章节切换，支持书籍详情页
 * 修复版本：解决边界翻页和章节切换问题，添加书籍详情页支持，增强预加载
 *
 * @param uiState 全局UI状态，包含虚拟页面列表和加载的数据
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
    uiState: ReaderUiState,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val virtualPages = uiState.virtualPages
    val virtualPageIndex = uiState.virtualPageIndex
    val loadedChapters = uiState.loadedChapterData

    if (virtualPages.isEmpty()) {
        // 如果虚拟页面为空（例如，初始加载时），显示一个空白或加载指示器
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(readerSettings.backgroundColor)
        )
        return
    }


    val totalPages = virtualPages.size
    val currentVirtualPage = virtualPages.getOrNull(virtualPageIndex)
    val isOnBookDetailPage = currentVirtualPage is VirtualPage.BookDetailPage

    // PageCurl状态管理
    val pageCurlState = rememberPageCurlState(
        initialCurrent = virtualPageIndex
    )

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
            val bookInfo = (uiState.currentPageData?.bookInfo
                ?: loadedChapters[uiState.currentChapter?.id]?.bookInfo)

            PageContentDisplay(
                page = "",
                chapterName = uiState.currentChapter?.chapterName ?: "",
                isFirstPage = false,
                isLastPage = false,
                isBookDetailPage = true,
                bookInfo = bookInfo,
                nextChapterData = uiState.nextChapterData,
                previousChapterData = uiState.previousChapterData,
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
    LaunchedEffect(virtualPageIndex) {
        if (virtualPageIndex != pageCurlState.current) {
            pageCurlState.snapTo(virtualPageIndex)
        }
    }

    // 监听PageCurl状态变化并通知ViewModel
    LaunchedEffect(pageCurlState) {
        snapshotFlow { pageCurlState.current }
            .distinctUntilChanged()
            .filter { it != virtualPageIndex } // 只在用户手动翻页时通知
            .collect { currentPage ->
                val direction = if (currentPage > virtualPageIndex) FlipDirection.NEXT else FlipDirection.PREVIOUS
                onPageChange(direction)
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
                ) { pageIdx ->
                    val virtualPage = virtualPages.getOrNull(pageIdx)
                    
                    if (virtualPage != null) {
                        // 渲染每一页内容，包含纸张纹理
                        PaperTexture(
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.04f,
                            density = 1.2f,
                            seed = pageIdx.toLong() * 42L
                        ) {
                            when (virtualPage) {
                                is VirtualPage.BookDetailPage -> {
                                    val bookInfo = (uiState.currentPageData?.bookInfo
                                        ?: loadedChapters[uiState.currentChapter?.id]?.bookInfo)
                                    // 显示书籍详情页
                                    PageContentDisplay(
                                        page = "",
                                        chapterName = uiState.currentChapter?.chapterName ?: "",
                                        isFirstPage = false,
                                        isLastPage = false,
                                        isBookDetailPage = true,
                                        bookInfo = bookInfo,
                                        nextChapterData = uiState.nextChapterData,
                                        previousChapterData = uiState.previousChapterData,
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
                                is VirtualPage.ContentPage -> {
                                    val chapterData = loadedChapters[virtualPage.chapterId]
                                    if (chapterData != null && virtualPage.pageIndex in chapterData.pages.indices) {
                                        PageContentDisplay(
                                            page = chapterData.pages[virtualPage.pageIndex],
                                            chapterName = chapterData.chapterName,
                                            isFirstPage = virtualPage.pageIndex == 0,
                                            isLastPage = virtualPage.pageIndex == chapterData.pages.size - 1,
                                            nextChapterData = if (virtualPage.pageIndex == chapterData.pages.size - 1) uiState.nextChapterData else null,
                                            previousChapterData = if (virtualPage.pageIndex == 0) uiState.previousChapterData else null,
                                            readerSettings = readerSettings,
                                            onNavigateToReader = onNavigateToReader,
                                            currentPageIndex = virtualPage.pageIndex + 1,
                                            totalPages = chapterData.pages.size,
                                            onClick = onClick
                                        )
                                    }
                                }
                                is VirtualPage.ChapterSection -> {
                                    // Chapter section not supported in this flip mode
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(readerSettings.backgroundColor)
                                    )
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
private sealed class VirtualPageLegacy {
    data class PreviousChapter(val chapterData: PageData) : VirtualPageLegacy()
    data class CurrentChapter(val pageIndex: Int) : VirtualPageLegacy()
    data class NextChapter(val chapterData: PageData) : VirtualPageLegacy()
    data object BookDetailPage : VirtualPageLegacy() // 新增：书籍详情页
}