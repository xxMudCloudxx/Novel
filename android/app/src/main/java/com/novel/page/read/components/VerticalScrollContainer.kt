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
 * 上下滚动容器 - 章节无缝衔接版本，包含纸张纹理
 */
@Composable
fun VerticalScrollContainer(
    pageData: PageData,
    readerSettings: ReaderSettings,
    onChapterChange: (FlipDirection) -> Unit,
    onClick: () -> Unit
) {
    val listState = rememberLazyListState()
    var isLoadingNext by remember { mutableStateOf(false) }
    var isLoadingPrevious by remember { mutableStateOf(false) }

    PaperTexture(
        modifier = Modifier.fillMaxSize(),
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

                // 章节分隔线
                item(key = "previous_divider_${previousChapter.chapterId}") {
                    Spacer(modifier = Modifier.height(24.wdp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = readerSettings.textColor.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.wdp))
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

            // 章节分隔线
            item(key = "current_chapter_divider_${pageData.chapterId}") {
                Spacer(modifier = Modifier.height(24.wdp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = readerSettings.textColor.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(24.wdp))
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

                // 下一章分隔线
                item(key = "next_chapter_divider_${nextChapter.chapterId}") {
                    Spacer(modifier = Modifier.height(24.wdp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = readerSettings.textColor.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.wdp))
                }
            } ?: run {
                // 如果没有下一章数据，显示加载指示器
                item(key = "load_next_indicator") {
                    LaunchedEffect(Unit) {
                        if (!isLoadingNext) {
                            isLoadingNext = true
                            onChapterChange(FlipDirection.NEXT)
                        }
                    }

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
                                text = "加载下一章...",
                                fontSize = 12.ssp,
                                color = readerSettings.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 预加载上一章的指示器（放在顶部，触发预加载）
            if (pageData.previousChapterData == null) {
                item(key = "load_previous_indicator") {
                    LaunchedEffect(Unit) {
                        if (!isLoadingPrevious) {
                            isLoadingPrevious = true
                            onChapterChange(FlipDirection.PREVIOUS)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.wdp),
                        contentAlignment = Alignment.Center
                    ) {
                        NovelText(
                            text = "正在加载上一章...",
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
