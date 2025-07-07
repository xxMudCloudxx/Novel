package com.novel.page.read

import com.novel.utils.TimberLogger
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.ReaderIntent
import com.novel.page.read.viewmodel.ReaderViewModel
import com.novel.page.read.viewmodel.ReaderState
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.wdp
import com.novel.utils.ssp
import com.novel.utils.NavViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.toArgb
import com.novel.page.read.viewmodel.PageFlipEffect
import com.novel.page.read.viewmodel.ReaderInfo
import com.novel.page.read.viewmodel.ReaderSettings

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
    val activeController = flipBookController ?: NavViewModel.currentFlipBookController()

    val adapter = viewModel.adapter
    val state by adapter.currentState.collectAsState()
    val isInitialized by adapter.isInitialized.collectAsState(initial = false)
    val showProgressRestoredHint by adapter.mapState { it.showProgressRestoredHint }.collectAsState(initial = false)
    
    val density = LocalDensity.current

    val coroutineScope = rememberCoroutineScope()

    // —— 只做反向动画 + 清理 controller，不做导航 ——
    val reverseOnly = remember {
        {
            coroutineScope.launch {
                activeController?.triggerReverseAnimation()
            }
            NavViewModel.setFlipBookController(null)
        }
    }

    // —— 真正的"后退"：先做动画，再清理 controller，最后导航 ——
    val performBack = remember {
        {
            reverseOnly()
            NavViewModel.navigateBack()
        }
    }

    // 系统返回键处理
    BackHandler {
        performBack()
    }

    LaunchedEffect(bookId, chapterId) {
        TimberLogger.d("ReaderPage", "ReaderPage参数变化: bookId=$bookId, chapterId=$chapterId")
        if (bookId.isNotBlank()) {
            TimberLogger.d("ReaderPage", "开始加载书籍和章节内容")
            viewModel.sendIntent(ReaderIntent.InitReader(bookId, chapterId))
        } else {
            TimberLogger.w("ReaderPage", "书籍ID或章节ID为空，跳过加载")
        }
    }

    LaunchedEffect(state.readerSettings) {
        TimberLogger.d("ReaderPage", "设置状态变化监听触发")
        TimberLogger.d(
            "ReaderPage",
            "当前背景颜色: ${
                String.format(
                    "#%08X",
                    state.readerSettings.backgroundColor.toArgb()
                )
            }"
        )
        TimberLogger.d(
            "ReaderPage",
            "当前文字颜色: ${String.format("#%08X", state.readerSettings.textColor.toArgb())}"
        )
        TimberLogger.d("ReaderPage", "当前字体大小: ${state.readerSettings.fontSize}sp")
        TimberLogger.d("ReaderPage", "当前翻页效果: ${state.readerSettings.pageFlipEffect}")

        // 强制触发更新容器尺寸，确保页数正确刷新
        if (state.containerSize != IntSize.Zero) {
            viewModel.sendIntent(ReaderIntent.UpdateContainerSize(state.containerSize, density))
            TimberLogger.d("ReaderPage", "强制触发容器尺寸更新以刷新页数: ${state.containerSize}")
        }
    }

    // 初始化阅读器并启动页数计算
    LaunchedEffect(bookId, chapterId) {
        viewModel.sendIntent(ReaderIntent.InitReader(bookId, chapterId))

        // 确保在初始化完成后立即启动页数计算
        delay(100) // 等待初始化完成
        if (isInitialized && state.containerSize != IntSize.Zero) {
            // 强制触发页数缓存更新，确保首次打开时页数能正确显示
            viewModel.sendIntent(ReaderIntent.UpdateContainerSize(state.containerSize, density))
        }

        // 如果是恢复阅读进度（没有指定章节），显示恢复提示
        if (chapterId == null) {
            delay(1000) // 等待内容加载完成
            viewModel.sendIntent(ReaderIntent.ShowProgressRestoredHint(true))
            delay(3000) // 显示3秒后自动隐藏
            viewModel.sendIntent(ReaderIntent.ShowProgressRestoredHint(false))
        }
    }

    // 清理controller的DisposableEffect
    DisposableEffect(Unit) {
        onDispose { NavViewModel.setFlipBookController(null) }
    }

    // 将需要传递给子组件的状态打包
    val readerInfo = ReaderInfo(
        paginationState = state.paginationState,
        pageCountCache = state.pageCountCache,
        currentChapter = state.currentChapter,
        perChapterPageIndex = state.currentPageIndex
    )

    // 优化：使用 remember 避免重复创建适配器对象，并稳定依赖项
    val loadingStateComponent = remember(
        state.hasError,
        state.isEmpty,
        state.error,
        state.isLoading,
        bookId
    ) {
        object : LoadingStateComponent {
            override val loading: Boolean get() = state.isLoading
            override val containsCancelable: Boolean get() = false
            override val viewState: ViewState
                get() = when {
                    state.hasError -> ViewState.Error(Exception(state.error))
                    state.isEmpty -> ViewState.Empty
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
                 viewModel.sendIntent(ReaderIntent.Retry)
            }
        }
    }

    // 左滑进入阅读器的回调函数
    val handleLeftSwipeToReader: () -> Unit = {
        TimberLogger.d("BookDetailPage", "左滑进入阅读器: bookId=$bookId")
        // 把当前的 FlipBookAnimationController 暂存到全局
        NavViewModel.setFlipBookController(activeController)
    }
    CompositionLocalProvider(LocalReaderInfo provides readerInfo) {
        LoadingStateComponent(
            component = loadingStateComponent,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = NovelColors.NovelBookBackground,
            flipBookController = activeController,
            onLeftSwipeToReader = handleLeftSwipeToReader
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(state.readerSettings.backgroundColor)
                    .onSizeChanged { size ->
                        if (size != IntSize.Zero) {
                            viewModel.sendIntent(ReaderIntent.UpdateContainerSize(size, density))
                        }
                    }
            ) {
                when {
                    state.isLoading -> {
                        LoadingIndicator()
                    }

                    state.hasError -> {
                        ErrorContent(
                            error = state.error?:"",
                            onRetry = {
                                viewModel.sendIntent(ReaderIntent.Retry)
                            }
                        )
                    }

                    isInitialized -> {
                        // 主阅读内容 - 使用整合的翻页容器以支持新的优化
                        if (state.currentPageData != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                IntegratedPageFlipContainer(
                                    state = state,
                                    onIntent = viewModel::sendIntent,
                                    onSwipeBack = {
                                        // iOS侧滑返回
                                        reverseOnly()
                                    },
                                    onClick = {
                                        viewModel.sendIntent(ReaderIntent.ToggleMenu(!state.isMenuVisible))
                                    }
                                )
                            }
                        } else {
                            // 兼容旧的显示方式，当分页数据还未准备好时
                            // 同时更新容器尺寸
                            ReaderContent(
                                state = state,
                                onClick = {
                                     viewModel.sendIntent(ReaderIntent.ToggleMenu(!state.isMenuVisible))
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { size ->
                                        // 确保容器尺寸信息及时更新到ViewModel
                                        if (size.width > 0 && size.height > 0) {
                                            viewModel.sendIntent(
                                                ReaderIntent.UpdateContainerSize(
                                                    size,
                                                    density
                                                )
                                            )
                                        }
                                    }
                            )
                        }

                        AnimatedVisibility(
                            visible = state.isChapterListVisible,
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
                                    chapters = state.chapterList,
                                    currentChapterId = state.currentChapter?.id ?: "",
                                    backgroundColor = state.readerSettings.backgroundColor,
                                    onChapterSelected = { chapter ->
                                        // 保存当前进度
                                        viewModel.sendIntent(ReaderIntent.SaveProgress())
                                        // 切换章节
                                        viewModel.sendIntent(ReaderIntent.SwitchToChapter(chapter.id))
                                         viewModel.sendIntent(ReaderIntent.ShowChapterList(false))
                                    },
                                    onDismiss = {  viewModel.sendIntent(ReaderIntent.ShowChapterList(false)) },
                                    modifier = Modifier.clickable(enabled = false) { } // 阻止穿透
                                )
                            }
                        }

                        // —— 再绘制"设置"面板（如果 showSettings == true）——
                        AnimatedVisibility(
                            visible = state.isSettingsPanelVisible,
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
                                    settings = state.readerSettings,
                                    onSettingsChange = { settings ->
                                        viewModel.sendIntent(
                                            ReaderIntent.UpdateSettings(settings)
                                        )
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
                                        modifier = Modifier.padding(
                                            horizontal = 16.wdp,
                                            vertical = 8.wdp
                                        )
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = state.isMenuVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ReaderControls(
                                state = state,
                                hideProgress = state.isSettingsPanelVisible || state.isChapterListVisible,
                                onBack = performBack,
                                onIntent = viewModel::sendIntent
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
    state: ReaderState,
    onIntent: (ReaderIntent) -> Unit,
    onSwipeBack: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val onPageChange: (FlipDirection) -> Unit = { direction ->
        if (state.isSettingsPanelVisible || state.isChapterListVisible) {
            onIntent(ReaderIntent.ShowSettingsPanel(false))
            onIntent(ReaderIntent.ShowChapterList(false))
        } else {
            onIntent(ReaderIntent.PageFlip(direction))
        }
    }
    
    val onChapterChange: (FlipDirection) -> Unit = { direction ->
        if (state.isSettingsPanelVisible || state.isChapterListVisible) {
            onIntent(ReaderIntent.ShowSettingsPanel(false))
            onIntent(ReaderIntent.ShowChapterList(false))
        } else {
            when (direction) {
                FlipDirection.NEXT -> onIntent(ReaderIntent.NextChapter)
                FlipDirection.PREVIOUS -> onIntent(ReaderIntent.PreviousChapter)
            }
        }
    }
    
    val onNavigateToReader : () -> Unit = {
        onIntent(ReaderIntent.PageFlip(FlipDirection.NEXT))
    }

    when (state.readerSettings.pageFlipEffect) {
        PageFlipEffect.NONE -> NoAnimationContainer(
            uiState = state,
            readerSettings = state.readerSettings,
            onPageChange = onPageChange,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.COVER -> CoverFlipContainer(
            uiState = state,
            readerSettings = state.readerSettings,
            onPageChange = onPageChange,
            onNavigateToReader = { _, _ -> onNavigateToReader() },
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.SLIDE -> SlideFlipContainer(
            uiState = state,
            readerSettings = state.readerSettings,
            onPageChange = onPageChange,
            onSwipeBack = onSwipeBack,
            onClick = onClick,
            onSlideIndexChange = { newIndex ->
                // 使用专用的更新方法避免循环更新
                onIntent(ReaderIntent.UpdateSlideIndex(newIndex))
            }
        )

        PageFlipEffect.PAGECURL -> PageCurlFlipContainer(
            uiState = state,
            readerSettings = state.readerSettings,
            onPageChange = onPageChange,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )

        PageFlipEffect.VERTICAL -> VerticalScrollContainer(
            uiState = state,
            readerSettings = state.readerSettings,
            onChapterChange = onChapterChange,
            onVerticalScrollPageChange = { pageIndex ->
                 onIntent(ReaderIntent.UpdateScrollPosition(pageIndex))
            },
            onClick = onClick
        )
    }
}

/**
 * 阅读内容组件（兼容旧版本）
 */
@Composable
private fun ReaderContent(
    state: ReaderState,
    onClick: () -> Unit,
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
                text = state.currentChapter?.chapterName ?: "",
                fontSize = (state.readerSettings.fontSize + 4).sp,
                fontWeight = FontWeight.Bold,
                color = state.readerSettings.textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.wdp)
            )
        }

        // 章节内容
        item {
            Text(
                text = state.bookContent.replace("<br/><br/>", "\n\n").replace("<br/>", "\n"),
                fontSize = state.readerSettings.fontSize.sp,
                color = state.readerSettings.textColor,
                lineHeight = (state.readerSettings.fontSize * 1.5).sp,
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
    state: ReaderState,
    onBack: () -> Unit,
    hideProgress: Boolean = false,
    onIntent: (ReaderIntent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部控制栏
        TopBar(
            onBack = onBack,
            readerSettings = state.readerSettings
        )

        Spacer(modifier = Modifier.weight(1f))

        // 底部控制栏
        BottomControls(
            state = state,
            hideProgress = hideProgress,
            onIntent = onIntent
        )
    }
}

/**
 * 顶部控制栏
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    readerSettings: ReaderSettings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(readerSettings.backgroundColor.copy(alpha = 0.95f))
            .padding(4.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = readerSettings.textColor
            )
        }
    }
}

/**
 * 底部控制栏
 */
@Composable
private fun BottomControls(
    state: ReaderState,
    hideProgress: Boolean = false,
    onIntent: (ReaderIntent) -> Unit
) {
    val readerInfo = LocalReaderInfo.current
    val paginationState = readerInfo.paginationState
    val totalPages = readerInfo.pageCountCache?.totalPages
        ?: paginationState.estimatedTotalPages.takeIf { it > 0 }
        ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(state.readerSettings.backgroundColor.copy(alpha = 1f))
    ) {
        if (!hideProgress) {
            // Pagination progress indicator
            if (paginationState.isCalculating) {
                Text(
                    text = "正在计算总页数...",
                    fontSize = 10.ssp,
                    color = state.readerSettings.textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
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
                    color = state.readerSettings.textColor,
                    modifier = Modifier.debounceClickable(
                        onClick = { onIntent(ReaderIntent.PreviousChapter) }
                    ))

                // 进度条 - 改为SolidCircleSlider
                SolidCircleSlider(
                    progress = state.computedReadingProgress,
                    onValueChange = { rawValue ->
                        // 仿照亮度控制，添加步进量化
                        val stepSize = 20f / (totalPages.takeIf { it > 0 } ?: 1)
                        val stepped = (rawValue / stepSize).roundToInt() * stepSize
                        val clamped = stepped.coerceIn(0f, 1f)
                        onIntent(ReaderIntent.SeekToProgress(clamped))
                    },
                    modifier = Modifier.weight(1f),
                    trackColor = Color.Gray.copy(alpha = 0.1f),
                    progressColor = Color.Gray.copy(alpha = 0.5f),
                    thumbColor = state.readerSettings.backgroundColor,
                    trackHeightDp = 24.dp,
                    thumbRadiusDp = 16.dp
                )

                NovelText(
                    "下一章",
                    fontSize = 14.ssp,
                    fontWeight = FontWeight.Bold,
                    color = state.readerSettings.textColor,
                    modifier = Modifier.debounceClickable(
                        onClick = { onIntent(ReaderIntent.NextChapter) }
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
                onClick = { onIntent(ReaderIntent.ShowChapterList(true)) },
                tintColor = state.readerSettings.textColor
            )

            ControlButton(
                icon = Icons.Default.Star,
                text = "夜间",
                onClick = { /* 暂不实现 */ },
                tintColor = state.readerSettings.textColor
            )

            ControlButton(
                icon = Icons.Default.Settings,
                text = "设置",
                onClick = { onIntent(ReaderIntent.ShowSettingsPanel(true)) },
                tintColor = state.readerSettings.textColor
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
    tintColor: Color = Color(0xFF2E2E2E) // 使用安全的默认深灰色，而不是主题色
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
