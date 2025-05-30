package com.novel.page.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.book.components.*
import com.novel.page.book.viewmodel.BookDetailViewModel
import com.novel.page.component.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

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
            override fun showViewState(viewState: ViewState) = stateComponentAdapter.showViewState(viewState)
            override fun retry() = stateComponentAdapter.retry()
        }
    }

    LoadingStateComponent(
        component = loadingStateComponent,
        modifier = Modifier.background(NovelColors.NovelBookBackground).fillMaxSize()
    ) {
        if (uiState.isSuccess) {
            LazyColumn(
                modifier = Modifier
                    .background(NovelColors.NovelBookBackground)
                    .fillMaxSize()
                    .padding(15.wdp),
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
