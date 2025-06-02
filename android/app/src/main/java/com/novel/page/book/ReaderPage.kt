package com.novel.page.book

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.ChapterListPanel
import com.novel.page.book.components.ReaderSettingsPanel
import com.novel.page.book.viewmodel.ReaderViewModel
import com.novel.page.book.viewmodel.ReaderUiState
import com.novel.page.component.BackButton
import com.novel.page.component.LoadingStateComponent
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.wdp
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    
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
                    // 主阅读内容
                    ReaderContent(
                        uiState = uiState,
                        onClick = { showControls = !showControls },
                        onPageChange = { direction ->
                            if (direction > 0) {
                                viewModel.nextPage()
                            } else {
                                viewModel.previousPage()
                            }
                        }
                    )
                    
                    // 控制面板
                    AnimatedVisibility(
                        visible = showControls,
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
                    
                    // 设置面板
                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        ReaderSettingsPanel(
                            settings = uiState.readerSettings,
                            onSettingsChange = { settings -> viewModel.updateSettings(settings) },
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 阅读内容组件
 */
@Composable
private fun ReaderContent(
    uiState: ReaderUiState,
    onClick: () -> Unit,
    onPageChange: (direction: Int) -> Unit
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
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
                imageVector = Icons.Default.ArrowBack,
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
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "上一章",
                    tint = Color.White
                )
            }
            
            // 进度条
            Slider(
                value = uiState.readingProgress,
                onValueChange = onSeekToProgress,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NovelColors.NovelMain,
                    activeTrackColor = NovelColors.NovelMain,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            IconButton(onClick = onNextChapter) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
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