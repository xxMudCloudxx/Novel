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
import com.novel.utils.NavViewModel
import android.util.Log

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

    // 左滑进入阅读器的回调函数
    val handleLeftSwipeToReader: () -> Unit = {
        Log.d("BookDetailPage", "左滑进入阅读器: bookId=$bookId")
        // 把当前的 FlipBookAnimationController 暂存到全局
        NavViewModel.setFlipBookController(flipBookController)
        onNavigateToReader?.invoke(bookId, null)
    }

    // 根据是否在动画状态使用统一的LoadingStateComponent，但传递不同参数
    LoadingStateComponent(
        component = loadingStateComponent,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.7f),
        flipBookController = flipBookController,
        onLeftSwipeToReader = handleLeftSwipeToReader
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

/**
 * 书籍详情内容组件 - 分离内容渲染逻辑以提升性能
 */
@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onToggleDescription: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.wdp),
        verticalArrangement = Arrangement.spacedBy(16.wdp)
    ) {
        BookCoverSection(bookInfo = uiState.bookInfo)

        BookTitleSection(bookInfo = uiState.bookInfo)

        AuthorSection(bookInfo = uiState.bookInfo)

        BookStatsSection(
            bookInfo = uiState.bookInfo,
            lastChapter = uiState.lastChapter
        )

        BookDescriptionSection(
            description = uiState.bookInfo?.bookDesc ?: "",
            onToggleExpand = onToggleDescription,
            bookId = uiState.bookInfo?.id ?: ""
        )

        BookReviewsSection(reviews = uiState.reviews)
    }
}
