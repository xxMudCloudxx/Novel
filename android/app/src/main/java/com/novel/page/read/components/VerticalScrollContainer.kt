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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import com.novel.page.read.viewmodel.ReaderUiState

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
    uiState: ReaderUiState,
    readerSettings: ReaderSettings,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val loadedChapters = uiState.loadedChapterData.values.toList()
    
    // 当前显示的章节名称（动态更新）
    var currentDisplayChapterName by remember { mutableStateOf(uiState.currentChapter?.chapterName ?: "") }

    // 动态更新当前显示的章节名称
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .map { visibleItems ->
                // 找到第一个完全可见的章节标题
                visibleItems.firstOrNull {
                    val key = it.key as? String
                    key?.startsWith("chapter_title_") == true
                }?.key as? String
            }
            .distinctUntilChanged()
            .collect { titleKey ->
                titleKey?.let { key ->
                    val chapterId = key.removePrefix("chapter_title_")
                    uiState.loadedChapterData[chapterId]?.let {
                        currentDisplayChapterName = it.chapterName
                    }
                }
            }
    }

    // 检测滚动边界并自动加载章节
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .filter { it.isNotEmpty() }
            .collect { visibleItems ->
                val firstVisible = visibleItems.first()
                val lastVisible = visibleItems.last()
                val totalItems = listState.layoutInfo.totalItemsCount

                // 检测是否滚动到顶部（需要加载上一章）
                if (firstVisible.index <= 1 && !uiState.isSwitchingChapter && !uiState.isFirstChapter) {
                    onChapterChange(FlipDirection.PREVIOUS)
                }

                // 检测是否滚动到底部（需要加载下一章）
                if (lastVisible.index >= totalItems - 2 && !uiState.isSwitchingChapter && !uiState.isLastChapter) {
                    onChapterChange(FlipDirection.NEXT)
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
            seed = uiState.bookId.hashCode().toLong()
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
                // 上一章加载指示器
                if (uiState.isSwitchingChapter && uiState.chapterList.firstOrNull()?.id != uiState.currentChapter?.id) {
                     item(key = "load_previous_indicator") {
                        LoadingItem(readerSettings = readerSettings, text = "加载上一章中...")
                     }
                }

                // 渲染所有已加载的章节
                items(
                    count = loadedChapters.size,
                    key = { index -> "chapter_${loadedChapters[index].chapterId}" }
                ) { index ->
                    val chapterData = loadedChapters[index]
                    Column {
                        ChapterTitleHeader(
                            title = chapterData.chapterName,
                            readerSettings = readerSettings
                        )
                        ChapterContentText(
                            content = chapterData.content,
                            readerSettings = readerSettings,
                            modifier = Modifier.padding(bottom = 24.wdp)
                        )
                    }
                }


                // 下一章预加载指示器
                if (uiState.isSwitchingChapter && uiState.chapterList.lastOrNull()?.id != uiState.currentChapter?.id) {
                    item(key = "load_next_indicator") {
                        LoadingItem(readerSettings = readerSettings, text = "加载下一章中...")
                    }
                }
            }
        }

        // 底部页面信息（对于滚动模式，显示章节信息）
        ReaderPageInfo(
            modifier = Modifier.padding(start = 12.wdp, bottom = 3.wdp),
            currentChapterIndex = uiState.currentChapterIndex,
            totalChapters = uiState.chapterList.size
        )
    }
}

@Composable
private fun LoadingItem(readerSettings: ReaderSettings, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.wdp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            CircularProgressIndicator(
                color = readerSettings.textColor,
                modifier = Modifier.size(24.wdp)
            )
            NovelText(
                text = text,
                fontSize = 12.ssp,
                color = readerSettings.textColor.copy(alpha = 0.7f)
            )
        }
    }
}
