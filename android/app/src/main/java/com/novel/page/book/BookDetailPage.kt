package com.novel.page.book

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.*
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.book.viewmodel.BookDetailViewModel
import com.novel.page.component.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import kotlinx.coroutines.launch
import com.novel.utils.SwipeBackContainer
import androidx.compose.ui.geometry.Offset

/**
 * 书籍详情页面 - 集成 iOS 风格背景指示器侧滑返回
 * 支持3D翻书动画状态下的特殊侧滑处理
 * 支持左滑进入阅读器
 * @param bookId 书籍ID
 * @param fromRank 是否来自榜单（用于识别来源，不影响显示）
 * @param viewModel 视图模型
 * @param flipBookController 翻书动画控制器（可选）
 * @param onNavigateToReader 导航到阅读器的回调
 */
@Composable
fun BookDetailPage(
    bookId: String,
    fromRank: Boolean = false,
    viewModel: BookDetailViewModel = hiltViewModel(),
    flipBookController: FlipBookAnimationController? = null,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

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
                viewModel.loadBookDetail(bookId)
            }
        }
    }

    // 优化：检查动画状态，避免频繁检查
    val isInAnimationMode = remember(flipBookController?.animationState) {
        flipBookController?.animationState?.isAnimating == true
    }

    // 优化左滑手势检测 - 改进手势识别和避免冲突
    val swipeToReaderModifier = Modifier.pointerInput(bookId, onNavigateToReader) {
        var initialTouch = Offset.Zero
        var totalDistance = 0f
        var dragStartTime = 0L
        var isDragValid = false
        
        detectDragGestures(
            onDragStart = { offset ->
                initialTouch = offset
                totalDistance = 0f
                dragStartTime = System.currentTimeMillis()
                isDragValid = false
                
                // 只在屏幕右半部分开始的手势才被认为是有效的左滑
                isDragValid = offset.x > size.width * 0.4f
            },
            onDragEnd = {
                val dragDuration = System.currentTimeMillis() - dragStartTime
                
                // 优化判断条件：
                // 1. 从右侧开始滑动
                // 2. 向左滑动足够距离
                // 3. 滑动速度适中（防止误触）
                val isValidLeftSwipe = isDragValid && 
                    totalDistance < -120f && // 向左滑动至少120像素
                    dragDuration < 800L && // 滑动时间不超过800ms
                    dragDuration > 100L // 滑动时间至少100ms（防止意外触摸）
                
                if (isValidLeftSwipe) {
                    onNavigateToReader?.invoke(bookId, null)
                }
                
                // 重置状态
                isDragValid = false
                totalDistance = 0f
                initialTouch = Offset.Zero
            }
        ) { change, dragAmount ->
            if (isDragValid) {
                // 只累积向左的滑动距离
                if (dragAmount.x < 0) {
                    totalDistance += dragAmount.x
                }
                
                // 如果开始向右滑动，取消手势
                if (dragAmount.x > 10f && totalDistance > -50f) {
                    isDragValid = false
                }
                
                // 消费手势事件，防止与其他组件冲突
                change.consume()
            }
        }
    }

    // 根据是否在3D翻书动画状态选择不同的容器
    if (isInAnimationMode) {
        // 在3D翻书动画状态下，使用带指示器的侧滑返回容器
        SwipeBackContainer(
            modifier = Modifier
                .fillMaxSize()
                .background(NovelColors.NovelBookBackground)
                .then(swipeToReaderModifier), // 添加左滑手势
            backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.8f),
            edgeWidthDp = 300.wdp,
            firstThreshold = 0.05f,
            completeThreshold = 0.25f, // 降低阈值，更容易触发
            onSwipeComplete = {
                // 侧滑完成时触发倒放动画
                coroutineScope.launch {
                    try {
                        flipBookController?.triggerReverseAnimation()
                    } catch (e: Exception) {
                        // 静默处理异常，避免影响用户体验
                    }
                }
            }
        ) {
            // 书籍详情内容
            if (uiState.isSuccess) {
                BookDetailContent(
                    uiState = uiState.data,
                    onToggleDescription = { viewModel.toggleDescriptionExpanded() },
                    onNavigateToReader = onNavigateToReader
                )
            }
        }
    } else {
        // 正常状态下使用原有的LoadingStateComponent
        LoadingStateComponent(
            component = loadingStateComponent,
            modifier = Modifier
                .fillMaxSize()
                .then(swipeToReaderModifier), // 添加左滑手势
            backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.7f),
            flipBookController = flipBookController
        ) {
            // 前景内容 - 书籍详情页面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovelColors.NovelBookBackground)
            ) {
                // 性能优化：只在成功状态下渲染内容，避免不必要的组合
                if (uiState.isSuccess) {
                    BookDetailContent(
                        uiState = uiState.data,
                        onToggleDescription = { viewModel.toggleDescriptionExpanded() },
                        onNavigateToReader = onNavigateToReader
                    )
                }
            }
        }
    }
}

/**
 * 书籍详情内容组件 - 分离内容渲染逻辑以提升性能
 */
@Composable
private fun BookDetailContent(
    uiState: BookDetailUiState,
    onToggleDescription: () -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.wdp),
        verticalArrangement = Arrangement.spacedBy(16.wdp)
    ) {
        item(key = "cover") {
            BookCoverSection(bookInfo = uiState.bookInfo)
        }

        item(key = "title") {
            BookTitleSection(bookInfo = uiState.bookInfo)
        }

        item(key = "author") {
            AuthorSection(bookInfo = uiState.bookInfo)
        }

        item(key = "stats") {
            BookStatsSection(
                bookInfo = uiState.bookInfo,
                lastChapter = uiState.lastChapter
            )
        }

        item(key = "description") {
            BookDescriptionSection(
                description = uiState.bookInfo?.bookDesc ?: "",
                onToggleExpand = onToggleDescription,
                bookId =  uiState.bookInfo?.id?: "",
                onNavigateToReader = onNavigateToReader
            )
        }

        item(key = "reviews") {
            BookReviewsSection(reviews = uiState.reviews)
        }
    }
}
