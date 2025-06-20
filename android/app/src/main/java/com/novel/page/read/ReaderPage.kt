package com.novel.page.read

import android.util.Log
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
import androidx.activity.compose.BackHandler
import com.novel.page.component.FlipBookAnimationController
import com.novel.page.component.LoadingStateComponent
import kotlinx.coroutines.launch
import com.novel.page.component.NovelText
import com.novel.page.component.SolidCircleSlider
import com.novel.page.component.ViewState
import com.novel.page.read.components.*
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.repository.ProgressiveCalculationState
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.ReaderViewModel
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.debounceClickable
import com.novel.utils.wdp
import com.novel.utils.ssp
import com.novel.utils.NavViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * 状态信息，通过 CompositionLocal 提供给子组件
 */
data class ReaderInfo(
    val paginationState: ProgressiveCalculationState,
    val pageCountCache: PageCountCacheData?,
    val currentChapter: Chapter?,
    val perChapterPageIndex: Int
)

val LocalReaderInfo = staticCompositionLocalOf<ReaderInfo> {
    error("No ReaderInfo provided")
}

/**
 * 小说阅读器页面
 * @param bookId 书籍ID
 * @param chapterId 章节ID（可选，默认从第一章开始）
 * @param viewModel 阅读器ViewModel
 */
@Composable
fun ReaderPage(
    flipBookController: FlipBookAnimationController? = null,
    bookId: String,
    chapterId: String? = null,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    val coroutineScope = rememberCoroutineScope()

    // 定义返回函数，供多处调用
    val performBack = remember {
        {
            coroutineScope.launch {
                flipBookController?.triggerReverseAnimation()
            }
            NavViewModel.setFlipBookController(null)
        }
    }

    // 系统返回键处理
    BackHandler {
        performBack()
    }

    // 控制面板显示状态
    var showControls by remember { mutableStateOf(false) }

    // 章节目录显示状态
    var showChapterList by remember { mutableStateOf(false) }

    // 设置面板显示状态
    var showSettings by remember { mutableStateOf(false) }

    // 阅读位置恢复状态
    var showProgressRestoredHint by remember { mutableStateOf(false) }

    LaunchedEffect(bookId, chapterId) {
        viewModel.initReader(bookId, chapterId)
        
        // 如果是恢复阅读进度（没有指定章节），显示恢复提示
        if (chapterId == null) {
            delay(1000) // 等待内容加载完成
            showProgressRestoredHint = true
            delay(3000) // 显示3秒后自动隐藏
            showProgressRestoredHint = false
        }
    }

    // 清理controller的DisposableEffect
    DisposableEffect(Unit) {
        onDispose { NavViewModel.setFlipBookController(null) }
    }

    // 将需要传递给子组件的状态打包
    val readerInfo = ReaderInfo(
        paginationState = uiState.paginationState,
        pageCountCache = uiState.pageCountCache,
        currentChapter = uiState.currentChapter,
        perChapterPageIndex = uiState.currentPageIndex
    )

    // 优化：使用 remember 避免重复创建适配器对象，并稳定依赖项
    val loadingStateComponent = remember(
        uiState.hasError,
        uiState.isEmpty,
        uiState.error,
        uiState.isLoading,
        bookId
    ) {
        object : LoadingStateComponent {
            override val loading: Boolean get() = uiState.isLoading
            override val containsCancelable: Boolean get() = false
            override val viewState: ViewState
                get() = when {
                    uiState.hasError -> ViewState.Error(Exception(uiState.error))
                    uiState.isEmpty -> ViewState.Empty
                    else -> ViewState.Idle
                }

            override fun showLoading(show: Boolean) {
                // 状态由ViewModel管理，无需实现
            }

            override fun cancelLoading() {
                // 无需实现
            }

            override fun showViewState(viewState: ViewState) {
                // 状态由ViewModel管理，无需实现
            }

            override fun retry() {
            }
        }
    }

    // 左滑进入阅读器的回调函数
    val handleLeftSwipeToReader: () -> Unit = {
        Log.d("BookDetailPage", "左滑进入阅读器: bookId=$bookId")
        // 把当前的 FlipBookAnimationController 暂存到全局
        NavViewModel.setFlipBookController(flipBookController)
    }
    CompositionLocalProvider(LocalReaderInfo provides readerInfo) {
        LoadingStateComponent(
            component = loadingStateComponent,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = NovelColors.NovelBookBackground,
            flipBookController = flipBookController,
            onLeftSwipeToReader = handleLeftSwipeToReader
        ) {
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
                        // 主阅读内容 - 使用整合的翻页容器以支持新的优化
                        if (uiState.currentPageData != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                IntegratedPageFlipContainer(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    readerSettings = uiState.readerSettings,
                                    onPageChange = { direction ->
                                        // 翻页时关闭所有设置栏
                                        if (showSettings || showChapterList) {
                                            showSettings = false
                                            showChapterList = false
                                        } else {
                                            viewModel.onPageChange(direction)
                                        }
                                    },
                                    onChapterChange = { direction ->
                                        // 章节切换时关闭所有设置栏
                                        if (showSettings || showChapterList) {
                                            showSettings = false
                                            showChapterList = false
                                        } else {
                                            viewModel.onChapterChange(direction)
                                        }
                                    },
                                    onNavigateToReader = { _, _ ->
                                        // 从书籍详情页导航到第一页内容
                                        viewModel.navigateToContent()
                                    },
                                    onSwipeBack = {
                                        // iOS侧滑返回
                                        performBack()
                                    },
                                    onVerticalScrollPageChange = { pageIndex ->
                                        viewModel.updateCurrentPageFromScroll(pageIndex)
                                    },
                                    onClick = {
                                        // 点击时关闭所有设置栏
                                        if (showSettings || showChapterList) {
                                            showSettings = false
                                            showChapterList = false
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
                                        // 保存当前进度
                                        viewModel.saveCurrentReadingProgress()
                                        // 切换章节
                                        viewModel.switchToChapter(chapter.id)
                                        showChapterList = false
                                    },
                                    onDismiss = { showChapterList = false },
                                    modifier = Modifier.clickable(enabled = false) { } // 阻止穿透
                                )
                            }
                        }

                        // —— 再绘制"设置"面板（如果 showSettings == true）——
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

                        // —— 最后再绘制"控制面板" ——
                        // 阅读进度恢复提示
                        AnimatedVisibility(
                            visible = showProgressRestoredHint,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.wdp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = NovelColors.NovelMain.copy(alpha = 0.9f)
                                    ),
                                    shape = RoundedCornerShape(8.wdp)
                                ) {
                                    Text(
                                        text = "已恢复上次阅读位置",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.wdp, vertical = 8.wdp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showControls,  // 只要 showControls，就展示 ReaderControls
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ReaderControls(
                                uiState = uiState,
                                hideProgress = showSettings || showChapterList,  // 有面板时隐藏进度条
                                onBack = performBack,
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 策略模式分发容器
 */
@Composable
private fun IntegratedPageFlipContainer(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
    uiState: ReaderUiState,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    when (readerSettings.pageFlipEffect) {
        PageFlipEffect.NONE -> NoAnimationContainer(
            uiState = uiState,
            readerSettings = readerSettings,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.COVER -> CoverFlipContainer(
            uiState = uiState,
            readerSettings = readerSettings,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.SLIDE -> SlideFlipContainer(
            uiState = uiState,
            readerSettings = readerSettings,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onSwipeBack = onSwipeBack,
            onClick = onClick,
            onSlideIndexChange = { newIndex ->
                // 使用专用的更新方法避免循环更新
                viewModel.updateSlideFlipIndex(newIndex)
            }
        )

        PageFlipEffect.PAGECURL -> PageCurlFlipContainer(
            uiState = uiState,
            readerSettings = readerSettings,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.VERTICAL -> VerticalScrollContainer(
            uiState = uiState,
            readerSettings = readerSettings,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onVerticalScrollPageChange = onVerticalScrollPageChange,
            onClick = onClick
        )
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
            onShowSettings = onShowSettings
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
    hideProgress: Boolean = false,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSeekToProgress: (Float) -> Unit,
    onShowChapterList: () -> Unit,
    onShowSettings: () -> Unit
) {
    val readerInfo = LocalReaderInfo.current
    val paginationState = readerInfo.paginationState
    val totalPages = readerInfo.pageCountCache?.totalPages
        ?: paginationState.estimatedTotalPages.takeIf { it > 0 }
        ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(uiState.readerSettings.backgroundColor.copy(alpha = 1f))
    ) {
        if (!hideProgress) {
            // Pagination progress indicator
            if (paginationState.isCalculating) {
                Text(
                    text = "正在计算总页数...",
                    fontSize = 10.ssp,
                    color = NovelColors.NovelText.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

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
                    progress = uiState.computedReadingProgress,
                    onValueChange = { rawValue ->
                        // 仿照亮度控制，添加步进量化
                        val stepSize = 20f / (totalPages.takeIf { it > 0 } ?: 1)
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
        } else {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.02.wdp),
                color = Color.Gray.copy(alpha = 0.1f),
            )
        }

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
