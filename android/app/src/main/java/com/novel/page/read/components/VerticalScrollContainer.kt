package com.novel.page.read.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelText
import com.novel.page.component.PaperTexture
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
private fun ChapterTitleHeader(title: String, readerSettings: ReaderSettings) {
    NovelText(
        text = title,
        fontSize = (readerSettings.fontSize + 4).ssp,
        fontWeight = FontWeight.Bold,
        color = readerSettings.textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(readerSettings.backgroundColor) // Necessary for sticky headers to not be transparent
            .padding(vertical = 16.wdp)
    )
}

@Composable
private fun ChapterContentText(content: String, readerSettings: ReaderSettings, modifier: Modifier = Modifier) {
    NovelText(
        text = HtmlTextUtil.cleanHtml(content),
        fontSize = readerSettings.fontSize.ssp,
        color = readerSettings.textColor,
        lineHeight = (readerSettings.fontSize * 1.5).ssp,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 上下滚动容器 - 章节无缝衔接版本，包含纸张纹理，支持边界检测和章节切换
 * 优化版：动态更新导航信息，无缝拼接章节内容
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalScrollContainer(
    pageData: PageData,
    readerSettings: ReaderSettings,
    pageCountCache: PageCountCacheData?,
    containerSize: IntSize,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit,
    currentChapterIndex: Int = 0, // 新增：当前章节索引
    totalChapters: Int = 1 // 新增：总章节数
) {
    val listState = rememberLazyListState()
    var isLoadingNext by remember { mutableStateOf(false) }
    var isLoadingPrevious by remember { mutableStateOf(false) }
    
    // 当前显示的章节名称（动态更新）
    var currentDisplayChapterName by remember { mutableStateOf(pageData.chapterName) }

    val contentHeights = remember { mutableMapOf<String, Int>() }

    // Reset loading flags when new data arrives
    LaunchedEffect(pageData.nextChapterData, pageData.previousChapterData) {
        isLoadingNext = false
        isLoadingPrevious = false
    }
    
    // Auto-scroll to new chapter content after it's loaded
    LaunchedEffect(pageData.chapterId) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        
        // Find the index of the current chapter's title item
        val currentChapterKey = "current_chapter_title_${pageData.chapterId}"
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == currentChapterKey }
        
        if (itemInfo != null && itemInfo.offset != 0) {
             // Heuristic to check if we should scroll
             listState.animateScrollToItem(itemInfo.index)
        }
    }

    // 动态更新当前显示的章节名称
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .map { visibleItems ->
                // 找到当前显示区域中的章节标题
                visibleItems.find { item ->
                    val key = item.key as? String
                    key?.contains("chapter_title") == true
                }?.key as? String
            }
            .distinctUntilChanged()
            .collect { titleKey ->
                titleKey?.let { key ->
                    when {
                        key.contains("previous_chapter_title") -> {
                            pageData.previousChapterData?.let { 
                                currentDisplayChapterName = it.chapterName 
                            }
                        }
                        key.contains("current_chapter_title") -> {
                            currentDisplayChapterName = pageData.chapterName
                        }
                        key.contains("next_chapter_title") -> {
                            pageData.nextChapterData?.let { 
                                currentDisplayChapterName = it.chapterName 
                            }
                        }
                    }
                }
            }
    }

    // 实时计算当前页码并通知ViewModel
    LaunchedEffect(listState, containerSize, pageCountCache) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .map {
                val layoutInfo = listState.layoutInfo
                if (layoutInfo.visibleItemsInfo.isEmpty() || containerSize.height == 0 || pageCountCache == null) {
                    return@map -1
                }

                val firstVisible = layoutInfo.visibleItemsInfo.first()
                val chapterKey = firstVisible.key as? String

                if (chapterKey != "current_chapter_content_${pageData.chapterId}") {
                    return@map -1
                }

                val chapterContentHeight = contentHeights[chapterKey] ?: 0
                if (chapterContentHeight == 0) return@map -1

                val chapterRange =
                    pageCountCache.chapterPageRanges.find { it.chapterId == pageData.chapterId }
                val pagesInChapter = chapterRange?.let { it.endPage - it.startPage + 1 } ?: 1
                if (pagesInChapter <= 1) return@map 0 // 如果只有一页，则页码始终为0

                val pageHeight = chapterContentHeight.toFloat() / pagesInChapter.toFloat()
                if (pageHeight <= 0) return@map -1

                val scrollOffsetWithinItem = -firstVisible.offset
                (scrollOffsetWithinItem / pageHeight).toInt().coerceIn(0, pagesInChapter - 1)
            }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect { newPageIndex ->
                onVerticalScrollPageChange(newPageIndex)
            }
    }

    // 检测滚动边界并自动加载章节
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isNotEmpty()) {
            val firstVisible = visibleItems.first()
            val lastVisible = visibleItems.last()

            // 检测是否滚动到顶部（需要加载上一章）
            if (firstVisible.index <= 1 && !isLoadingPrevious && pageData.previousChapterData == null) {
                isLoadingPrevious = true
                onChapterChange(FlipDirection.PREVIOUS)
                // Give time for UI to update before another trigger
                delay(500)
            }

            // 检测是否滚动到底部（需要加载下一章）
            val totalItems = layoutInfo.totalItemsCount
            if (lastVisible.index >= totalItems - 2 && !isLoadingNext && pageData.nextChapterData == null) {
                isLoadingNext = true
                onChapterChange(FlipDirection.NEXT)
                // Give time for UI to update before another trigger
                delay(500)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航信息 - 使用动态更新的章节名称
        ReaderNavigationInfo(
            chapterName = currentDisplayChapterName,
            modifier = Modifier.padding(start = 12.wdp, top = 12.wdp)
        )

        // 主要内容区域
        PaperTexture(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            alpha = 0.04f,
            density = 1.0f,
            seed = pageData.chapterId.hashCode().toLong()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(readerSettings.backgroundColor)
                    .clickable { onClick() }
                    .padding(horizontal = 16.wdp, vertical = 10.wdp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Use dp for arrangement
            ) {
                // 书籍详情页（如果支持）
                if (pageData.hasBookDetailPage) {
                    item(key = "book_detail_page_${pageData.chapterId}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.wdp) // 给书籍详情页一个固定高度
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
                                showNavigationInfo = false,
                                currentPageIndex = 0,
                                totalPages = 1,
                                onClick = onClick
                            )
                        }
                    }
                }

                // 上一章预加载指示器和内容
                if (pageData.previousChapterData == null && !isLoadingPrevious && !pageData.isFirstChapter) {
                    item(key = "load_previous_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.wdp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingPrevious) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.wdp)
                                ) {
                                    CircularProgressIndicator(
                                        color = readerSettings.textColor,
                                        modifier = Modifier.size(20.wdp)
                                    )
                                    NovelText(
                                        text = "加载上一章...",
                                        fontSize = 12.ssp,
                                        color = readerSettings.textColor.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                NovelText(
                                    text = "下拉加载上一章",
                                    fontSize = 12.ssp,
                                    color = readerSettings.textColor.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 上一章内容（如果有）- 无缝拼接
                pageData.previousChapterData?.let { previousChapter ->
                    item(key = "previous_chapter_title_${previousChapter.chapterId}") {
                        ChapterTitleHeader(previousChapter.chapterName, readerSettings)
                    }
                    item(key = "previous_chapter_content_${previousChapter.chapterId}") {
                        ChapterContentText(
                            content = previousChapter.content,
                            readerSettings = readerSettings,
                            modifier = Modifier.padding(bottom = 24.wdp)
                        )
                    }
                }

                // 当前章节内容
                item(key = "current_chapter_title_${pageData.chapterId}") {
                    ChapterTitleHeader(pageData.chapterName, readerSettings)
                }
                item(key = "current_chapter_content_${pageData.chapterId}") {
                    ChapterContentText(
                        content = pageData.content,
                        readerSettings = readerSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.wdp)
                            .onSizeChanged {
                                contentHeights["current_chapter_content_${pageData.chapterId}"] =
                                    it.height
                            }
                    )
                }

                // 下一章内容（如果有）- 无缝拼接
                pageData.nextChapterData?.let { nextChapter ->
                    item(key = "next_chapter_title_${nextChapter.chapterId}") {
                        ChapterTitleHeader(nextChapter.chapterName, readerSettings)
                    }
                    item(key = "next_chapter_content_${nextChapter.chapterId}") {
                        ChapterContentText(
                            content = nextChapter.content,
                            readerSettings = readerSettings,
                            modifier = Modifier.padding(bottom = 24.wdp)
                        )
                    }
                }

                // 下一章预加载指示器
                item(key = "load_next_indicator") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.wdp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingNext) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.wdp)
                            ) {
                                CircularProgressIndicator(
                                    color = readerSettings.textColor,
                                    modifier = Modifier.size(24.wdp)
                                )
                                NovelText(
                                    text = "加载下一章...",
                                    fontSize = 12.ssp,
                                    color = readerSettings.textColor.copy(alpha = 0.7f)
                                )
                            }
                        } else if(pageData.nextChapterData == null && !pageData.isLastPage) {
                            NovelText(
                                text = "上拉加载下一章",
                                fontSize = 12.ssp,
                                color = readerSettings.textColor.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 底部页面信息（对于滚动模式，显示章节信息）
        ReaderPageInfo(
            modifier = Modifier.padding(start = 12.wdp, bottom = 3.wdp),
            currentChapterIndex = currentChapterIndex,
            totalChapters = totalChapters
        )
    }
}
