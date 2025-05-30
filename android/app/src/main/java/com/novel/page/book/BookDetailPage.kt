package com.novel.page.book

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.*
import com.novel.page.book.viewmodel.BookDetailViewModel
import com.novel.page.component.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel
import com.novel.utils.wdp
import kotlinx.coroutines.launch

/**
 * 书籍详情页面
 * @param bookId 书籍ID
 * @param viewModel 视图模型
 */
@Composable
fun BookDetailPage(
    bookId: String,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val thresholdPx = with(LocalDensity.current) { 50.wdp.toPx() }
    val scope = rememberCoroutineScope()
    var lastTrigger by remember { mutableLongStateOf(0L) }
    var handledBack by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

    // 创建一个StateComponent适配器
    val stateComponentAdapter = remember(uiState) {
        object : StateComponent {
            override val viewState: ViewState
                get() = when {
                    uiState.hasError -> ViewState.Error(Exception(uiState.error))
                    uiState.isEmpty -> ViewState.Empty
                    else -> ViewState.Idle
                }

            override fun showViewState(viewState: ViewState) {
                // 不需要实现，因为状态由ViewModel管理
            }

            override fun retry() {
                viewModel.loadBookDetail(bookId)
            }
        }
    }

    // 创建一个LoadingComponent适配器
    val loadingComponentAdapter = remember(uiState) {
        object : LoadingComponent {
            override val loading: Boolean get() = uiState.isLoading
            override val containsCancelable: Boolean get() = false

            override fun showLoading(show: Boolean) {
                // 不需要实现，因为状态由ViewModel管理
            }

            override fun cancelLoading() {
                // 不需要实现
            }
        }
    }

    // 组合LoadingStateComponent
    val loadingStateComponent = remember(loadingComponentAdapter, stateComponentAdapter) {
        object : LoadingStateComponent {
            override val loading: Boolean get() = loadingComponentAdapter.loading
            override val containsCancelable: Boolean get() = loadingComponentAdapter.containsCancelable
            override val viewState: ViewState get() = stateComponentAdapter.viewState

            override fun showLoading(show: Boolean) = loadingComponentAdapter.showLoading(show)
            override fun cancelLoading() = loadingComponentAdapter.cancelLoading()
            override fun showViewState(viewState: ViewState) =
                stateComponentAdapter.showViewState(viewState)

            override fun retry() = stateComponentAdapter.retry()
        }
    }

    LoadingStateComponent(
        component = loadingStateComponent,
        modifier = Modifier
            .background(NovelColors.NovelBookBackground)
            .fillMaxSize()
    ) {
        if (uiState.isSuccess) {
            Box(
                modifier = Modifier
                    .background(NovelColors.NovelBookBackground)
                    .padding(15.wdp)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                NovelColors.NovelBookBackground.copy(alpha = 0.5f),
                                NovelColors.NovelBookBackground
                            ),
                            startX = 0f,
                            endX = dragProgress * 100
                        )
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    handledBack = false // 重置状态
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragProgress += dragAmount
                                    if (!handledBack && dragAmount > thresholdPx &&
                                        System.currentTimeMillis() - lastTrigger > 50000
                                    ) {
                                        lastTrigger = System.currentTimeMillis()
                                        handledBack = true
                                        scope.launch {
                                            NavViewModel.navigateBack()
                                        }
                                    }
                                },
                                onDragEnd = {
                                    handledBack = false // 准备下次触发
                                }
                            )
                        }
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.wdp)
                ) {
                    item {
                        BookCoverSection(bookInfo = uiState.data.bookInfo)
                    }

                    item {
                        BookTitleSection(bookInfo = uiState.data.bookInfo)
                    }

                    item {
                        AuthorSection(bookInfo = uiState.data.bookInfo)
                    }

                    item {
                        BookStatsSection(
                            bookInfo = uiState.data.bookInfo,
                            lastChapter = uiState.data.lastChapter
                        )
                    }

                    item {
                        BookDescriptionSection(
                            description = uiState.data.bookInfo?.bookDesc ?: "",
                            onToggleExpand = { viewModel.toggleDescriptionExpanded() }
                        )
                    }

                    item {
                        BookReviewsSection(reviews = uiState.data.reviews)
                    }
                }
            }
        }
    }
}
