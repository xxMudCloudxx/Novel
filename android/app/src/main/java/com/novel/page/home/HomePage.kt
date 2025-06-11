package com.novel.page.home

import android.annotation.SuppressLint
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
import com.novel.page.component.rememberFlipBookAnimationController
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.NavViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.novel.page.component.FlipBookAnimationController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/**
 * 新版首页 - 支持下拉刷新、上拉加载和3D翻书动画
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomePage(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCategory: (Long) -> Unit = {},
    onNavigateToCategoryPage: () -> Unit = {},
    // 接收全局动画控制器
    globalFlipBookController: FlipBookAnimationController? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // 使用传入的全局动画控制器，如果没有则创建本地控制器
    val flipBookController = globalFlipBookController ?: rememberFlipBookAnimationController()
    
    // 下拉刷新状态
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isRefreshing
    )
    
    // 优化：预计算屏幕尺寸，避免重复计算
    val screenSize = remember(configuration, density) {
        Pair(
            configuration.screenWidthDp * density.density,
            configuration.screenHeightDp * density.density
        )
    }
    
    // 监听事件 - 移除书籍跳转，因为现在BookDetailPage在动画中显示
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToBook -> {
                    // 不再跳转，书籍内容在动画中显示
                }
                is HomeEvent.NavigateToCategory -> onNavigateToCategory(event.categoryId)
                is HomeEvent.NavigateToSearch -> {
                    // 直接调用NavViewModel导航到搜索页面
                    NavViewModel.navigateToSearch(event.query)
                }
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
            item(key = "top_bar") {
                HomeTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onAction(HomeAction.OnSearchQueryChange(it)) },
                    onSearchClick = { viewModel.onAction(HomeAction.OnSearchClick) },
                    onCategoryClick = { viewModel.onAction(HomeAction.OnCategoryButtonClick) },
                    modifier = Modifier.padding(horizontal = 15.wdp)
                )
            }
            
            // 2. 分类筛选器
            item(key = "filter_bar") {
                HomeFilterBar(
                    filters = uiState.categoryFilters,
                    selectedFilter = uiState.selectedCategoryFilter,
                    onFilterSelected = { viewModel.onAction(HomeAction.OnCategoryFilterSelected(it)) }
                )
            }
            
            // 3. 榜单面板 - 只在推荐模式下显示，支持3D翻书动画
            if (uiState.isRecommendMode) {
                item(key = "rank_panel") {
                    // 单个榜单面板，支持内部切换和翻书动画
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
                            onBookClick = { bookId, offset, size ->
                                // 榜单点击触发翻书动画
                                coroutineScope.launch {
                                    // 查找对应的书籍信息
                                    val book = uiState.rankBooks.find { it.id == bookId }
                                    
                                    flipBookController.startFlipAnimation(
                                        bookId = bookId.toString(),
                                        imageUrl = book?.picUrl ?: "",
                                        originalPosition = offset,
                                        originalSize = size,
                                        screenWidth = screenSize.first,
                                        screenHeight = screenSize.second
                                    )
                                }
                            },
                            // 传递翻书动画控制器
                            flipBookController = flipBookController
                        )
                    }
                }
            }
            
            // 4. 推荐书籍瀑布流
            item(key = "recommend_grid") {
                if (uiState.isRecommendMode) {
                    // 推荐模式：显示首页推荐数据 - 使用放大透明动画
                    HomeRecommendGrid(
                        homeBooks = uiState.homeRecommendBooks,
                        onBookClick = { viewModel.onAction(HomeAction.OnRecommendBookClick(it)) },
                        onBookClickWithPosition = { bookId, offset, size ->
                            // 推荐流点击触发放大透明动画
                            coroutineScope.launch {
                                // 查找对应书籍的图片URL
                                val book = uiState.homeRecommendBooks.find { it.bookId == bookId }
                                
                                flipBookController.startScaleFadeAnimation(
                                    bookId = bookId.toString(),
                                    imageUrl = book?.picUrl ?: "",
                                    originalPosition = offset,
                                    originalSize = size,
                                    screenWidth = screenSize.first,
                                    screenHeight = screenSize.second
                                )
                            }
                        },
                        onLoadMore = { /* 由上拉监听处理 */ },
                        modifier = Modifier.fillMaxWidth(),
                        fixedHeight = true,  // 在 LazyColumn 中使用固定高度版本
                        flipBookController = flipBookController  // 传递动画控制器
                    )
                } else {
                    // 分类模式：显示搜索结果数据 - 使用放大透明动画
                    HomeRecommendGrid(
                        books = uiState.recommendBooks,
                        onBookClick = { viewModel.onAction(HomeAction.OnRecommendBookClick(it)) },
                        onBookClickWithPosition = { bookId, offset, size ->
                            // 分类推荐点击触发放大透明动画
                            coroutineScope.launch {
                                // 查找对应书籍的图片URL
                                val book = uiState.recommendBooks.find { it.id == bookId }
                                
                                flipBookController.startScaleFadeAnimation(
                                    bookId = bookId.toString(),
                                    imageUrl = book?.picUrl ?: "",
                                    originalPosition = offset,
                                    originalSize = size,
                                    screenWidth = screenSize.first,
                                    screenHeight = screenSize.second
                                )
                            }
                        },
                        onLoadMore = { /* 由上拉监听处理 */ },
                        modifier = Modifier.fillMaxWidth(),
                        fixedHeight = true,  // 在 LazyColumn 中使用固定高度版本
                        flipBookController = flipBookController  // 传递动画控制器
                    )
                }
            }
            
            // 5. 加载更多指示器 - 根据模式显示不同状态
            item(key = "load_more_indicator") {
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
                item(key = "global_loading") {
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
                item(key = "error_card") {
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