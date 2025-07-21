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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.novel.page.home.component.*
import com.novel.page.home.viewmodel.HomeIntent
import com.novel.page.home.viewmodel.HomeEffect
import com.novel.page.home.viewmodel.HomeViewModel
import com.novel.page.home.skeleton.HomePageSkeleton
import com.novel.page.component.rememberFlipBookAnimationController
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.NavViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.novel.page.component.FlipBookAnimationController
import com.novel.utils.StableCallbacks
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 新版首页 - 支持下拉刷新、上拉加载和3D翻书动画
 * 
 * 采用完整MVI架构：
 * - 直接使用HomeIntent处理用户交互
 * - 监听HomeEffect处理一次性副作用
 * - 订阅HomeState流获取UI状态
 * - 无业务逻辑，纯UI展示和事件转发
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomePage(
    onNavigateToCategory: (Long) -> Unit = StableCallbacks.DefaultNavigateToCategory,
    globalFlipBookController: FlipBookAnimationController? = null
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val uiState by viewModel.screenState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // 使用传入的全局动画控制器，如果没有则创建本地控制器
    val flipBookController = globalFlipBookController ?: rememberFlipBookAnimationController()
    
    // 生命周期感知 - 页面恢复时检查数据
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                super.onStart(owner)
                // 页面恢复可见时检查并恢复数据
                viewModel.sendIntent(HomeIntent.RestoreData)
            }
        })
    }
    
    // 下拉刷新状态
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isRefreshing
    )
    
    // 优化：使用derivedStateOf进行屏幕尺寸计算，避免每次重组都重新计算
    val screenSize by remember {
        derivedStateOf {
            Pair(
                configuration.screenWidthDp * density.density,
                configuration.screenHeightDp * density.density
            )
        }
    }
    
    // 性能优化：使用 StateAdapter 的稳定状态创建方法，避免多个 collectAsState
    val adapter = viewModel.adapter
    val isLoading by adapter.createLoadingState()
    val error by adapter.createErrorState()
    val isSuccess by adapter.createSuccessState()
    
    // 性能优化：使用稳定回调，避免每次重组都创建新Lambda对象
    val navigateToSearch = StableCallbacks.rememberUnitCallback("navigateToSearch") {
        viewModel.sendIntent(HomeIntent.NavigateToSearch(""))
    }
    
    val navigateToCategoryPage = StableCallbacks.rememberUnitCallback("navigateToCategoryPage") {
        viewModel.sendIntent(HomeIntent.NavigateToCategory(1L))
    }
    
    val filterSelected = StableCallbacks.rememberCallback<String>("filterSelected") { filter ->
        viewModel.sendIntent(HomeIntent.SelectCategoryFilter(filter))
    }
    
    val rankTypeSelected = StableCallbacks.rememberCallback<String>("rankTypeSelected") { rankType ->
        viewModel.sendIntent(HomeIntent.SelectRankType(rankType))
    }
    
    val bookClick = StableCallbacks.rememberCallback<Long>("bookClick") { bookId ->
        viewModel.sendIntent(HomeIntent.NavigateToBookDetail(bookId))
    }
    
    val refresh = StableCallbacks.rememberUnitCallback("refresh") {
        viewModel.sendIntent(HomeIntent.RefreshData)
    }
    
    // 稳定的回调函数集合 - 使用稳定的对象封装，避免多个参数传递
    val callbacks = remember(navigateToSearch, navigateToCategoryPage, filterSelected, rankTypeSelected, bookClick, refresh) {
        @Stable
        object {
            val navigateToSearch: () -> Unit = navigateToSearch
            val navigateToCategoryPage: () -> Unit = navigateToCategoryPage  
            val filterSelected: (String) -> Unit = filterSelected
            val rankTypeSelected: (String) -> Unit = rankTypeSelected
            val bookClick: (Long) -> Unit = bookClick
            val refresh: () -> Unit = refresh
        }
    }
    
    // 监听副作用 - 使用MVI Effect系统
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToBook -> {
                    // 不再跳转，书籍内容在动画中显示
                }
                is HomeEffect.NavigateToBookDetail -> {
                    // 不再跳转，书籍内容在动画中显示
                }
                is HomeEffect.NavigateToCategory -> onNavigateToCategory(effect.categoryId)
                is HomeEffect.NavigateToSearch -> {
                    // 直接调用NavViewModel导航到搜索页面
                    NavViewModel.navigateToSearch(effect.query)
                }
                is HomeEffect.NavigateToCategoryPage -> callbacks.navigateToCategoryPage()
                is HomeEffect.NavigateToFullRanking -> {
                    // 导航到完整榜单页面 - 待实现
                }
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is HomeEffect.SendToReactNative -> {
                    // 处理React Native数据发送 - 已经在UseCase层处理
                }
            }
        }
    }
    
    // 优化滚动检测 - 使用derivedStateOf减少重组，并添加防抖
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            
            totalItems > 0 && 
            visibleItems.isNotEmpty() && 
            visibleItems.lastOrNull()?.index == totalItems - 1
        }
    }
    
    // 监听滚动状态，实现上拉加载 - 添加distinctUntilChanged防止每帧触发
    LaunchedEffect(listState) {
        snapshotFlow { isAtBottom }
            .distinctUntilChanged()
            .collectLatest { atBottom ->
                if (atBottom && uiState.canLoadMoreRecommend) {
                    // 触发加载更多
                    if (uiState.isRecommendMode) {
                        viewModel.sendIntent(HomeIntent.LoadMoreHomeRecommend)
                    } else {
                        viewModel.sendIntent(HomeIntent.LoadMoreRecommend)
                    }
                }
            }
    }
    
    // 显示骨架屏或正常内容
    if (uiState.isLoading && uiState.currentRecommendBooks.isEmpty()) {
        HomePageSkeleton()
    } else {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = callbacks.refresh,
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
                    onSearchClick = callbacks.navigateToSearch,
                    onCategoryClick = callbacks.navigateToCategoryPage,
                    modifier = Modifier.padding(horizontal = 15.wdp)
                )
            }
            
            // 2. 分类筛选器
            item(key = "filter_bar") {
                HomeFilterBar(
                    filters = uiState.categoryFilters,
                    selectedFilter = uiState.selectedCategoryFilter,
                    onFilterSelected = callbacks.filterSelected
                )
            }
            
            // 3. 榜单面板 - 只在推荐模式下显示，支持3D翻书动画
            if (uiState.isRecommendMode) {
                item(key = "rank_panel_${uiState.selectedRankType}") {
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
                            onRankTypeSelected = callbacks.rankTypeSelected,
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
            
            // 4. 推荐书籍瀑布流 - 使用统一的 RecommendItem 类型
            item(key = "recommend_grid_${uiState.selectedCategoryFilter}_${uiState.isRecommendMode}") {
                HomeRecommendGrid(
                    recommendItems = uiState.currentRecommendBooks,
                    onBookClick = callbacks.bookClick,
                    onBookClickWithPosition = { bookId, offset, size ->
                        // 推荐流点击触发放大透明动画
                        coroutineScope.launch {
                            // 查找对应书籍的图片URL
                            val item = uiState.currentRecommendBooks.find { it.id == bookId }
                            
                            flipBookController.startScaleFadeAnimation(
                                bookId = bookId.toString(),
                                imageUrl = item?.coverUrl ?: "",
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
            
            // 5. 加载更多指示器 - 使用统一的状态
            item(key = "load_more_indicator_${uiState.canLoadMoreRecommend}") {
                HomeRecommendLoadMoreIndicator(
                    isLoading = !uiState.canLoadMoreRecommend && uiState.currentRecommendBooks.isNotEmpty(),
                    hasMoreData = uiState.canLoadMoreRecommend,
                    totalDataCount = uiState.currentRecommendBooks.size
                )
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
                                onClick = { viewModel.sendIntent(HomeIntent.ClearError) }
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
}