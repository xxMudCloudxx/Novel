package com.novel.page.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.novel.page.home.component.*
import com.novel.page.home.viewmodel.HomeAction
import com.novel.page.home.viewmodel.HomeEvent
import com.novel.page.home.viewmodel.HomeViewModel
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.NavViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 新版首页 - 支持下拉刷新和上拉加载
 */
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCategory: (Long) -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    onNavigateToCategoryPage: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    // 下拉刷新状态
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isRefreshing
    )
    
    // 监听事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToBook -> {
                    // 使用NavViewModel导航到书籍详情页
                    NavViewModel.navigateToBookDetail(event.bookId.toString())
                }
                is HomeEvent.NavigateToCategory -> onNavigateToCategory(event.categoryId)
                is HomeEvent.NavigateToSearch -> onNavigateToSearch(event.query)
                is HomeEvent.NavigateToCategoryPage -> onNavigateToCategoryPage()
                is HomeEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 监听滚动状态，实现上拉加载
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == 
            listState.layoutInfo.totalItemsCount - 1
        }.collectLatest { isAtBottom ->
            if (isAtBottom && listState.layoutInfo.totalItemsCount > 0) {
                // 触发加载更多
                if (uiState.isRecommendMode && uiState.hasMoreHomeRecommend && !uiState.homeRecommendLoading) {
                    viewModel.onAction(HomeAction.LoadMoreHomeRecommend)
                } else if (!uiState.isRecommendMode && uiState.hasMoreRecommend && !uiState.recommendLoading) {
                    viewModel.onAction(HomeAction.LoadMoreRecommend)
                }
            }
        }
    }
    
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.onAction(HomeAction.RefreshRecommend) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(NovelColors.NovelDivider),
            contentPadding = PaddingValues(vertical = 10.wdp),
            verticalArrangement = Arrangement.spacedBy(10.wdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部搜索栏和分类按钮
            item {
                HomeTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onAction(HomeAction.OnSearchQueryChange(it)) },
                    onSearchClick = { viewModel.onAction(HomeAction.OnSearchClick) },
                    onCategoryClick = { viewModel.onAction(HomeAction.OnCategoryButtonClick) },
                    modifier = Modifier.padding(horizontal = 15.wdp)
                )
            }
            
            // 2. 分类筛选器
            item {
                HomeFilterBar(
                    filters = uiState.categoryFilters,
                    selectedFilter = uiState.selectedCategoryFilter,
                    onFilterSelected = { viewModel.onAction(HomeAction.OnCategoryFilterSelected(it)) }
                )
            }
            
            // 3. 榜单面板 - 只在推荐模式下显示，使用单个面板
            if (uiState.isRecommendMode) {
                item {
                    // 单个榜单面板，支持内部切换
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.wdp),
                        contentAlignment = Alignment.Center
                    ) {
                        HomeRankPanel(
                            rankBooks = uiState.rankBooks,
                            selectedRankType = uiState.selectedRankType,
                            onRankTypeSelected = { viewModel.onAction(HomeAction.OnRankTypeSelected(it)) },
                            onBookClick = { viewModel.onAction(HomeAction.OnRankBookClick(it)) }
                        )
                    }
                }
            }
            
            // 4. 推荐书籍瀑布流
            item {
                if (uiState.isRecommendMode) {
                    // 推荐模式：显示首页推荐数据
                    HomeRecommendGrid(
                        homeBooks = uiState.homeRecommendBooks,
                        onBookClick = { viewModel.onAction(HomeAction.OnRecommendBookClick(it)) },
                        onLoadMore = { /* 由上拉监听处理 */ },
                        modifier = Modifier.fillMaxWidth(),
                        fixedHeight = true  // 在 LazyColumn 中使用固定高度版本
                    )
                } else {
                    // 分类模式：显示搜索结果数据
                    HomeRecommendGrid(
                        books = uiState.recommendBooks,
                        onBookClick = { viewModel.onAction(HomeAction.OnRecommendBookClick(it)) },
                        onLoadMore = { /* 由上拉监听处理 */ },
                        modifier = Modifier.fillMaxWidth(),
                        fixedHeight = true  // 在 LazyColumn 中使用固定高度版本
                    )
                }
            }
            
            // 5. 加载更多指示器 - 根据模式显示不同状态
            item {
                if (uiState.isRecommendMode) {
                    // 首页推荐模式
                    HomeRecommendLoadMoreIndicator(
                        isLoading = uiState.homeRecommendLoading,
                        hasMoreData = uiState.hasMoreHomeRecommend,
                        onLoadMore = { viewModel.onAction(HomeAction.LoadMoreHomeRecommend) }
                    )
                } else {
                    // 分类模式
                    HomeRecommendLoadMoreIndicator(
                        isLoading = uiState.recommendLoading,
                        hasMoreData = uiState.hasMoreRecommend,
                        onLoadMore = { viewModel.onAction(HomeAction.LoadMoreRecommend) }
                    )
                }
            }
            
            // 6. 全局加载状态
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.wdp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NovelColors.NovelMain
                        )
                    }
                }
            }
            
            // 7. 错误提示
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.wdp),
                        colors = CardDefaults.cardColors(
                            containerColor = NovelColors.NovelError.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.wdp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "加载失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                            TextButton(
                                onClick = { viewModel.onAction(HomeAction.ClearError) }
                            ) {
                                Text("关闭")
                            }
                        }
                    }
                }
            }
        }
    }
}