package com.novel.page.read

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.read.components.*
import com.novel.page.read.viewmodel.ReaderViewModel
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
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
                            
                            // 左上角导航信息
                            ReaderNavigationInfo(
                                currentChapter = uiState.currentChapter,
                                currentPageIndex = uiState.currentPageIndex,
                                totalPages = uiState.currentPageData?.pages?.size ?: 0,
                                onBackToHome = { /* 返回首页 */ },
                                onShowChapterList = { 
                                    showChapterList = true
                                    showControls = false
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.wdp)
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
                    
                    // 控制面板
                    AnimatedVisibility(
                        visible = showControls && !showSettings, // 设置面板显示时隐藏控制面板
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ReaderControls(
                            uiState = uiState,
                            onBack = { /* 返回上一页 */ },
                            onPreviousChapter = { viewModel.previousChapter() },
                            onNextChapter = { viewModel.nextChapter() },
                            onSeekToProgress = { progress -> viewModel.seekToProgress(progress) },
                            onShowChapterList = { 
                                showChapterList = true
                                showControls = false
                            },
                            onShowSettings = { 
                                showSettings = true
                                showControls = false
                            }
                        )
                    }
                    
                    // 章节目录侧滑面板
                    AnimatedVisibility(
                        visible = showChapterList,
                        enter = slideInHorizontally(initialOffsetX = { -it }),
                        exit = slideOutHorizontally(targetOffsetX = { -it })
                    ) {
                        ChapterListPanel(
                            chapters = uiState.chapterList,
                            currentChapterId = uiState.currentChapter?.id ?: "",
                            onChapterSelected = { chapter ->
                                viewModel.switchToChapter(chapter.id)
                                showChapterList = false
                            },
                            onDismiss = { showChapterList = false }
                        )
                    }
                    
                    // 设置面板 - 添加外部点击关闭功能
                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 半透明遮罩，点击关闭设置面板
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    .clickable { showSettings = false }
                            )
                            
                            // 设置面板内容
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                ReaderSettingsPanel(
                                    settings = uiState.readerSettings,
                                    onSettingsChange = { settings -> viewModel.updateSettings(settings) },
                                    modifier = Modifier.clickable(enabled = false) { } // 阻止点击事件穿透
                                )
                            }
                        }
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
        TopBar(onBack = onBack)
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 底部控制栏
        BottomControls(
            uiState = uiState,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter,
            onSeekToProgress = onSeekToProgress,
            onShowChapterList = onShowChapterList,
            onShowSettings = onShowSettings
        )
    }
}

/**
 * 顶部控制栏
 */
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
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
    onNextChapter: () -> Unit,
    onSeekToProgress: (Float) -> Unit,
    onShowChapterList: () -> Unit,
    onShowSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.wdp)
    ) {
        // 第一行：上一章、进度条、下一章
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.wdp)
        ) {
            IconButton(onClick = onPreviousChapter) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一章",
                    tint = Color.White
                )
            }
            
            // 进度条
            Slider(
                value = uiState.readingProgress,
                onValueChange = { rawValue ->
                    // 仿照亮度控制，添加步进量化
                    val stepCount = 100  // 100个步进档位，提供更精细的控制
                    val stepSize = 1f / stepCount
                    val stepped = (rawValue / stepSize).roundToInt() * stepSize
                    val clamped = stepped.coerceIn(0f, 1f)
                    onSeekToProgress(clamped)
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NovelColors.NovelMain,
                    activeTrackColor = NovelColors.NovelMain,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            IconButton(onClick = onNextChapter) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一章",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.wdp))
        
        // 第二行：功能按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton(
                icon = Icons.Default.List,
                text = "目录",
                onClick = onShowChapterList
            )
            
            ControlButton(
                icon = Icons.Default.Star,
                text = "夜间",
                onClick = { /* 暂不实现 */ }
            )
            
            ControlButton(
                icon = Icons.Default.Settings,
                text = "设置",
                onClick = onShowSettings
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
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(24.wdp)
        )
        Spacer(modifier = Modifier.height(4.wdp))
        Text(
            text = text,
            color = Color.White,
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

/**
 * 阅读器导航信息组件
 * 显示在左上角的章节信息和导航按钮
 */
@Composable
private fun ReaderNavigationInfo(
    currentChapter: Chapter?,
    currentPageIndex: Int,
    totalPages: Int,
    onBackToHome: () -> Unit,
    onShowChapterList: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 显示/隐藏状态
    var isVisible by remember { mutableStateOf(true) }
    
    // 自动隐藏逻辑：3秒后自动隐藏
    LaunchedEffect(currentPageIndex) {
        isVisible = true
        kotlinx.coroutines.delay(3000)
        isVisible = false
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .clickable { isVisible = !isVisible }, // 点击切换显示状态
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(8.wdp)
        ) {
            Row(
                modifier = Modifier.padding(8.wdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.wdp)
            ) {
                // 返回首页按钮
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier.size(24.wdp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "返回首页",
                        tint = Color.White,
                        modifier = Modifier.size(16.wdp)
                    )
                }
                
                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.wdp)
                        .height(20.wdp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                
                // 章节目录按钮
                IconButton(
                    onClick = onShowChapterList,
                    modifier = Modifier.size(24.wdp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "章节目录",
                        tint = Color.White,
                        modifier = Modifier.size(16.wdp)
                    )
                }
                
                // 章节信息
                Column(
                    modifier = Modifier.widthIn(max = 200.wdp)
                ) {
                    // 章节标题
                    Text(
                        text = currentChapter?.chapterName ?: "加载中...",
                        color = Color.White,
                        fontSize = 12.ssp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 页面进度
                    if (totalPages > 0) {
                        Text(
                            text = "${currentPageIndex + 1}/$totalPages",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.ssp
                        )
                    }
                }
            }
        }
    }
} 