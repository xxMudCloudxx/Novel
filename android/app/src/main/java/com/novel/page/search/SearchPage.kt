package com.novel.page.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.LoadingStateComponent
import com.novel.page.component.ViewState
import com.novel.page.search.viewmodel.SearchAction
import com.novel.page.search.viewmodel.SearchEvent
import com.novel.page.search.viewmodel.SearchViewModel
import com.novel.page.search.component.SearchHistorySection
import com.novel.page.search.component.RankingSection
import com.novel.page.search.component.SearchTopBar
import com.novel.page.search.skeleton.SearchPageSkeleton
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel
import com.novel.utils.wdp

/**
 * 搜索页面
 * @param onNavigateBack 返回回调
 * @param onNavigateToBookDetail 导航到书籍详情
 * @param viewModel 搜索页面ViewModel
 */
@Composable
fun SearchPage(
    onNavigateBack: () -> Unit,
    onNavigateToBookDetail: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 处理一次性事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.NavigateToBookDetail -> {
                    onNavigateToBookDetail(event.bookId)
                }

                is SearchEvent.NavigateToSearchResult ->              // ⬅️ 新增
                    NavViewModel.navigateToSearchResult(event.query)  //   调用全局导航

                is SearchEvent.NavigateBack -> {
                    onNavigateBack()
                }

                is SearchEvent.ShowToast -> {
                    // TODO: 显示Toast
                }
            }
        }
    }

    // 初始化数据
    LaunchedEffect(Unit) {
        viewModel.onAction(SearchAction.LoadInitialData)
    }

    // 适配器对象，用于LoadingStateComponent
    val loadingStateComponent = remember(
        uiState.isLoading,
        uiState.error
    ) {
        object : LoadingStateComponent {
            override val loading: Boolean get() = uiState.isLoading
            override val containsCancelable: Boolean get() = false
            override val viewState: ViewState
                get() = when {
                    uiState.error != null -> ViewState.Error(Exception(uiState.error))
                    else -> ViewState.Idle
                }

            override fun showLoading(show: Boolean) {}
            override fun cancelLoading() {}
            override fun showViewState(viewState: ViewState) {}
            override fun retry() {
                viewModel.onAction(SearchAction.LoadInitialData)
            }
        }
    }

    LoadingStateComponent(
        component = loadingStateComponent,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.7f)
    ) {
        // 显示骨架屏或正常内容
        if (uiState.isLoading) {
            SearchPageSkeleton()
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 顶部搜索栏
            SearchTopBar(
                query = uiState.searchQuery,
                onQueryChange = { query ->
                    viewModel.onAction(SearchAction.UpdateSearchQuery(query))
                },
                onBackClick = {
                    viewModel.onAction(SearchAction.NavigateBack)
                },
                onSearchClick = {
                    if (uiState.searchQuery.isNotBlank()) {
                        viewModel.onAction(SearchAction.PerformSearch(uiState.searchQuery))
                    }
                }
            )

            // 主要内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 搜索历史记录
                if (uiState.searchHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.wdp))
                    SearchHistorySection(
                        history = uiState.searchHistory,
                        isExpanded = uiState.isHistoryExpanded,
                        onHistoryClick = { query ->
                            viewModel.onAction(SearchAction.UpdateSearchQuery(query))
                            viewModel.onAction(SearchAction.PerformSearch(query))
                        },
                        onToggleExpansion = {
                            viewModel.onAction(SearchAction.ToggleHistoryExpansion)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.wdp))

                // 推荐榜单区域
                RankingSection(
                    novelRanking = uiState.novelRanking,
                    dramaRanking = uiState.dramaRanking,
                    newBookRanking = uiState.newBookRanking,
                    onRankingItemClick = { bookId ->
                        viewModel.onAction(SearchAction.NavigateToBookDetail(bookId))
                    },
                    onViewFullRanking = { rankingType ->
                        val rankingItems = when (rankingType) {
                            "点击榜" -> uiState.novelRanking
                            "推荐榜" -> uiState.dramaRanking
                            "新书榜" -> uiState.newBookRanking
                            else -> emptyList()
                        }
                        NavViewModel.navigateToFullRanking(rankingType, rankingItems)
                    }
                )
                Spacer(modifier = Modifier.height(24.wdp))
            }
        }
        }
    }
}