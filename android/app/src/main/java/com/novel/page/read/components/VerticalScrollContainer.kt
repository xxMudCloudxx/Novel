package com.novel.page.read.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelText
import com.novel.page.component.PaperTexture
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 上下滚动容器 - 章节无缝衔接版本，包含纸张纹理，支持边界检测和章节切换
 */
@Composable
fun VerticalScrollContainer(
    pageData: PageData,
    readerSettings: ReaderSettings,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val listState = rememberLazyListState()
    var isLoadingNext by remember { mutableStateOf(false) }
    var isLoadingPrevious by remember { mutableStateOf(false) }
    
    // 检测滚动边界并自动加载章节
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) return@LaunchedEffect
        
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        
        if (visibleItems.isNotEmpty()) {
            val firstVisible = visibleItems.first()
            val lastVisible = visibleItems.last()
            
            // 检测是否滚动到顶部（需要加载上一章）
            if (firstVisible.index == 0 && firstVisible.offset >= -50 && !isLoadingPrevious && pageData.previousChapterData == null) {
                isLoadingPrevious = true
                onChapterChange(FlipDirection.PREVIOUS)
            }
            
            // 检测是否滚动到底部（需要加载下一章）
            val totalItems = layoutInfo.totalItemsCount
            if (lastVisible.index >= totalItems - 1 && !isLoadingNext && pageData.nextChapterData == null) {
                isLoadingNext = true
                onChapterChange(FlipDirection.NEXT)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航信息
        ReaderNavigationInfo(
            chapterName = pageData.chapterName,
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
                verticalArrangement = Arrangement.spacedBy(8.wdp)
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

                // 上一章预加载指示器（如果需要）
                if (pageData.previousChapterData == null && !pageData.hasBookDetailPage) {
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

                // 上一章内容（如果有）
                pageData.previousChapterData?.let { previousChapter ->
                    item(key = "previous_chapter_title_${previousChapter.chapterId}") {
                        NovelText(
                            text = previousChapter.chapterName,
                            fontSize = (readerSettings.fontSize + 4).ssp,
                            fontWeight = FontWeight.Bold,
                            color = readerSettings.textColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.wdp)
                        )
                    }

                    item(key = "previous_chapter_content_${previousChapter.chapterId}") {
                        NovelText(
                            text = HtmlTextUtil.cleanHtml(previousChapter.content),
                            fontSize = readerSettings.fontSize.ssp,
                            color = readerSettings.textColor.copy(alpha = 0.8f),
                            lineHeight = (readerSettings.fontSize * 1.5).ssp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 当前章节标题
                item(key = "current_chapter_title_${pageData.chapterId}") {
                    NovelText(
                        text = pageData.chapterName,
                        fontSize = (readerSettings.fontSize + 4).ssp,
                        fontWeight = FontWeight.Bold,
                        color = readerSettings.textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.wdp)
                    )
                }

                // 当前章节内容
                item(key = "current_chapter_content_${pageData.chapterId}") {
                    NovelText(
                        text = HtmlTextUtil.cleanHtml(pageData.content),
                        fontSize = readerSettings.fontSize.ssp,
                        color = readerSettings.textColor,
                        lineHeight = (readerSettings.fontSize * 1.5).ssp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 下一章内容（如果有）
                pageData.nextChapterData?.let { nextChapter ->
                    item(key = "next_chapter_title_${nextChapter.chapterId}") {
                        NovelText(
                            text = nextChapter.chapterName,
                            fontSize = (readerSettings.fontSize + 4).ssp,
                            fontWeight = FontWeight.Bold,
                            color = readerSettings.textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.wdp)
                        )
                    }

                    item(key = "next_chapter_content_${nextChapter.chapterId}") {
                        NovelText(
                            text = HtmlTextUtil.cleanHtml(nextChapter.content),
                            fontSize = readerSettings.fontSize.ssp,
                            color = readerSettings.textColor,
                            lineHeight = (readerSettings.fontSize * 1.5).ssp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } ?: run {
                    // 如果没有下一章数据，显示加载指示器
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
                            } else {
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
        }

        // 底部页面信息（对于滚动模式，显示章节信息）
        ReaderPageInfo(
            modifier = Modifier.padding(start = 12.wdp, bottom = 3.wdp)
        )
    }
}
