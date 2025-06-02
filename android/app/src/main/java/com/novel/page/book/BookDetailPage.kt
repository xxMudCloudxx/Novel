package com.novel.page.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.*
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.book.viewmodel.BookDetailViewModel
import com.novel.page.component.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import kotlinx.coroutines.launch
import com.novel.utils.SwipeBackContainer

/**
 * 书籍详情页面 - 集成 iOS 风格背景指示器侧滑返回
 * 支持3D翻书动画状态下的特殊侧滑处理
 * @param bookId 书籍ID
 * @param fromRank 是否来自榜单（用于识别来源，不影响显示）
 * @param viewModel 视图模型
 * @param flipBookController 翻书动画控制器（可选）
 */
@Composable
fun BookDetailPage(
    bookId: String,
    fromRank: Boolean = false,
    viewModel: BookDetailViewModel = hiltViewModel(),
    flipBookController: FlipBookAnimationController? = null
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

    // 根据是否在3D翻书动画状态选择不同的容器
    if (isInAnimationMode) {
        // 在3D翻书动画状态下，使用带指示器的侧滑返回容器
        SwipeBackContainer(
            modifier = Modifier
                .fillMaxSize()
                .background(NovelColors.NovelBookBackground),
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
                    onToggleDescription = { viewModel.toggleDescriptionExpanded() }
                )
            }
        }
    } else {
        // 正常状态下使用原有的LoadingStateComponent
        LoadingStateComponent(
            component = loadingStateComponent,
            modifier = Modifier.fillMaxSize(),
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
                        onToggleDescription = { viewModel.toggleDescriptionExpanded() }
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
    onToggleDescription: () -> Unit
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
                onToggleExpand = onToggleDescription
            )
        }
        
        item(key = "reviews") {
            BookReviewsSection(reviews = uiState.reviews)
        }
    }
}
