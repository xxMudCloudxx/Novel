package com.novel.page.read

import androidx.collection.emptyLongSet
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelText
import com.novel.page.component.SolidCircleSlider
import com.novel.page.read.components.*
import com.novel.page.read.viewmodel.ReaderViewModel
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.debounceClickable
import com.novel.utils.wdp
import com.novel.utils.ssp
import kotlin.math.roundToInt

/**
 * 小说阅读器页面
 * @param bookId 书籍ID
 * @param chapterId 章节ID（可选，默认从第一章开始）
 * @param viewModel 阅读器ViewModel
 */
@Composable
fun ReaderPage(
    bookId: String,
    chapterId: String? = null,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    // 控制面板显示状态
    var showControls by remember { mutableStateOf(false) }

    // 章节目录显示状态
    var showChapterList by remember { mutableStateOf(false) }

    // 设置面板显示状态
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(bookId, chapterId) {
        viewModel.initReader(bookId, chapterId)
    }

    NovelTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(uiState.readerSettings.backgroundColor)
                .onSizeChanged { size ->
                    if (size != IntSize.Zero) {
                        viewModel.updateContainerSize(size, density)
                    }
                }
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }

                uiState.hasError -> {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = { viewModel.retry() }
                    )
                }

                uiState.isSuccess -> {
                    // 主阅读内容 - 使用新的翻页容器
                    if (uiState.currentPageData != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            PageFlipContainer(
                                pageData = uiState.currentPageData!!,
                                currentPageIndex = uiState.currentPageIndex,
                                flipEffect = uiState.readerSettings.pageFlipEffect,
                                readerSettings = uiState.readerSettings,
                                onPageChange = { direction ->
                                    viewModel.onPageChange(direction)
                                },
                                onChapterChange = { direction ->
                                    viewModel.onChapterChange(direction)
                                },
                                onClick = {
                                    // 如果设置面板显示，点击阅读区域关闭设置
                                    if (showSettings) {
                                        showSettings = false
                                    } else {
                                        showControls = !showControls
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { size ->
                                        // 确保容器尺寸信息及时更新到ViewModel
                                        if (size.width > 0 && size.height > 0) {
                                            viewModel.updateContainerSize(size, density)
                                        }
                                    }
                            )
                        }
                    } else {
                        // 兼容旧的显示方式，当分页数据还未准备好时
                        // 同时更新容器尺寸
                        ReaderContent(
                            uiState = uiState,
                            onClick = {
                                if (showSettings) {
                                    showSettings = false
                                } else {
                                    showControls = !showControls
                                }
                            },
                            onPageChange = { direction ->
                                if (direction > 0) {
                                    viewModel.nextPage()
                                } else {
                                    viewModel.previousPage()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { size ->
                                    // 确保容器尺寸信息及时更新到ViewModel
                                    if (size.width > 0 && size.height > 0) {
                                        viewModel.updateContainerSize(size, density)
                                    }
                                }
                        )
                    }

                    AnimatedVisibility(
                        visible = showChapterList && !showSettings && showControls,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 60.wdp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))  // 让面板从剩余区域向上滑
                            ChapterListPanel(
                                chapters = uiState.chapterList,
                                currentChapterId = uiState.currentChapter?.id ?: "",
                                backgroundColor = uiState.readerSettings.backgroundColor,
                                onChapterSelected = { chapter ->
                                    viewModel.switchToChapter(chapter.id)
                                    showChapterList = false
                                },
                                onDismiss = { showChapterList = false },
                                modifier = Modifier.clickable(enabled = false) { } // 阻止穿透
                            )
                        }
                    }

                    // —— 再绘制“设置”面板（如果 showSettings == true）——
                    AnimatedVisibility(
                        visible = showSettings && !showChapterList && showControls,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 60.wdp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            ReaderSettingsPanel(
                                settings = uiState.readerSettings,
                                onSettingsChange = { settings ->
                                    viewModel.updateSettings(settings)
                                },
                                modifier = Modifier.clickable(enabled = false) { }
                            )
                        }
                    }

                    // —— 最后再绘制“控制面板” ——
                    AnimatedVisibility(
                        visible = showControls,  // 只要 showControls，就展示 ReaderControls
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ReaderControls(
                            uiState = uiState,
                            hideProgress = showSettings || showChapterList,  // 有面板时隐藏进度条
                            onBack = { /* 返回上一页 */ },
                            onPreviousChapter = { viewModel.previousChapter() },
                            onNextChapter = { viewModel.nextChapter() },
                            onSeekToProgress = { progress -> viewModel.seekToProgress(progress) },
                            onShowChapterList = {
                                showChapterList = !showChapterList
                                showSettings = false
                            },
                            onShowSettings = {
                                showSettings = !showSettings
                                showChapterList = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 阅读内容组件（兼容旧版本）
 */
@Composable
private fun ReaderContent(
    uiState: ReaderUiState,
    onClick: () -> Unit,
    onPageChange: (direction: Int) -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.wdp, vertical = 20.wdp),
        verticalArrangement = Arrangement.spacedBy(8.wdp)
    ) {
        // 章节标题
        item {
            Text(
                text = uiState.currentChapter?.chapterName ?: "",
                fontSize = (uiState.readerSettings.fontSize + 4).sp,
                fontWeight = FontWeight.Bold,
                color = uiState.readerSettings.textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.wdp)
            )
        }

        // 章节内容
        item {
            Text(
                text = uiState.bookContent.replace("<br/><br/>", "\n\n").replace("<br/>", "\n"),
                fontSize = uiState.readerSettings.fontSize.sp,
                color = uiState.readerSettings.textColor,
                lineHeight = (uiState.readerSettings.fontSize * 1.5).sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 阅读器控制面板
 */
@Composable
private fun ReaderControls(
    uiState: ReaderUiState,
    onBack: () -> Unit,
    hideProgress: Boolean = false,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSeekToProgress: (Float) -> Unit,
    onShowChapterList: () -> Unit,
    onShowSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部控制栏
        TopBar(
            onBack = onBack,
            backgroundColor = uiState.readerSettings.backgroundColor
        )

        Spacer(modifier = Modifier.weight(1f))

        // 底部控制栏
        BottomControls(
            uiState = uiState,
            hideProgress = hideProgress,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter,
            onSeekToProgress = onSeekToProgress,
            onShowChapterList = onShowChapterList,
            onShowSettings = onShowSettings,
            totalPages = uiState.chapterList.size
        )
    }
}

/**
 * 顶部控制栏
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor.copy(alpha = 1f))
            .padding(4.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = NovelColors.NovelText
            )
        }
    }
}

/**
 * 底部控制栏
 */
@Composable
private fun BottomControls(
    uiState: ReaderUiState,
    onPreviousChapter: () -> Unit,
    hideProgress: Boolean = false,
    onNextChapter: () -> Unit,
    onSeekToProgress: (Float) -> Unit,
    onShowChapterList: () -> Unit,
    onShowSettings: () -> Unit,
    totalPages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(uiState.readerSettings.backgroundColor.copy(alpha = 1f))
    ) {
        if (!hideProgress)
        // 第一行：上一章、进度条、下一章
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.wdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.wdp)
            ) {
                NovelText(
                    "上一章",
                    fontSize = 14.ssp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.debounceClickable(
                        onClick = { onPreviousChapter() }
                    ))

                // 进度条 - 改为SolidCircleSlider
                SolidCircleSlider(
                    progress = uiState.readingProgress,
                    onValueChange = { rawValue ->
                        // 仿照亮度控制，添加步进量化
                        val stepSize = 1f / totalPages
                        val stepped = (rawValue / stepSize).roundToInt() * stepSize
                        val clamped = stepped.coerceIn(0f, 1f)
                        onSeekToProgress(clamped)
                    },
                    modifier = Modifier.weight(1f),
                    trackColor = Color.Gray.copy(alpha = 0.1f),
                    progressColor = Color.Gray.copy(alpha = 0.5f),
                    thumbColor = uiState.readerSettings.backgroundColor,
                    trackHeightDp = 24.dp,
                    thumbRadiusDp = 16.dp
                )

                NovelText(
                    "下一章",
                    fontSize = 14.ssp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.debounceClickable(
                        onClick = { onNextChapter() }
                    ))
            }
        else
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.02.wdp),
                color = Color.Gray.copy(alpha = 0.1f),
            )

        // 第二行：功能按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.wdp)
                .height(60.wdp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = Icons.AutoMirrored.Filled.List,
                text = "目录",
                onClick = onShowChapterList,
            )

            ControlButton(
                icon = Icons.Default.Star,
                text = "夜间",
                onClick = { /* 暂不实现 */ },
            )

            ControlButton(
                icon = Icons.Default.Settings,
                text = "设置",
                onClick = onShowSettings,
            )
        }
    }
}

/**
 * 控制按钮
 */
@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tintColor: Color = NovelColors.NovelText
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tintColor,
            modifier = Modifier.size(24.wdp)
        )
        Text(
            text = text,
            color = tintColor,
            fontSize = 12.sp
        )
    }
}

/**
 * 加载指示器
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NovelColors.NovelMain)
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = NovelColors.NovelError,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.wdp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NovelColors.NovelMain)
        ) {
            Text("重试")
        }
    }
}
