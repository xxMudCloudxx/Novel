package com.novel.page.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.*
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.book.viewmodel.BookDetailViewModel
import com.novel.page.component.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

/**
 * 书籍详情页面 - 集成 iOS 风格背景指示器侧滑返回
 * @param bookId 书籍ID
 * @param fromRank 是否来自榜单（用于识别来源，不影响显示）
 * @param viewModel 视图模型
 */
@Composable
fun BookDetailPage(
    bookId: String,
    fromRank: Boolean = false,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

    // 性能优化：使用 remember 避免重复创建适配器对象
    val stateComponentAdapter = remember(uiState.hasError, uiState.isEmpty, uiState.error) {
        object : StateComponent {
            override val viewState: ViewState
                get() = when {
                    uiState.hasError -> ViewState.Error(Exception(uiState.error))
                    uiState.isEmpty -> ViewState.Empty
                    else -> ViewState.Idle
                }

            override fun showViewState(viewState: ViewState) {
                // 状态由ViewModel管理，无需实现
            }

            override fun retry() {
                viewModel.loadBookDetail(bookId)
            }
        }
    }

    // 性能优化：使用 remember 避免重复创建适配器对象
    val loadingComponentAdapter = remember(uiState.isLoading) {
        object : LoadingComponent {
            override val loading: Boolean get() = uiState.isLoading
            override val containsCancelable: Boolean get() = false

            override fun showLoading(show: Boolean) {
                // 状态由ViewModel管理，无需实现
            }

            override fun cancelLoading() {
                // 无需实现
            }
        }
    }

    // 性能优化：组合LoadingStateComponent，避免重复创建
    val loadingStateComponent = remember(loadingComponentAdapter, stateComponentAdapter) {
        object : LoadingStateComponent {
            override val loading: Boolean get() = loadingComponentAdapter.loading
            override val containsCancelable: Boolean get() = loadingComponentAdapter.containsCancelable
            override val viewState: ViewState get() = stateComponentAdapter.viewState

            override fun showLoading(show: Boolean) = loadingComponentAdapter.showLoading(show)
            override fun cancelLoading() = loadingComponentAdapter.cancelLoading()
            override fun showViewState(viewState: ViewState) = stateComponentAdapter.showViewState(viewState)
            override fun retry() = stateComponentAdapter.retry()
        }
    }

    // 集成新的背景指示器侧滑返回 - 指示器显示在被滑出的背景区域
    LoadingStateComponent(
        component = loadingStateComponent,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.7f) // 浅色背景，与书籍页面形成对比
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
