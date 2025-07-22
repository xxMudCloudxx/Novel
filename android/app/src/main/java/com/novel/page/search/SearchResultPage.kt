package com.novel.page.search

import com.novel.utils.TimberLogger
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import com.novel.page.component.LoadingStateComponent
import com.novel.page.component.ViewState
import com.novel.page.component.NovelText
import com.novel.utils.debounceClickable
import com.novel.page.search.viewmodel.SearchResultIntent
import com.novel.page.search.viewmodel.SearchResultEffect
import com.novel.page.search.viewmodel.SearchResultViewModel
import com.novel.page.search.component.*
import com.novel.page.search.skeleton.SearchResultPageSkeleton
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.ssp
import com.novel.utils.NavViewModel
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp

/**
 * 搜索结果页面 - MVI重构版
 */
@Composable
fun SearchResultPage(
    initialQuery: String,
    globalFlipBookController: com.novel.page.component.FlipBookAnimationController = com.novel.page.component.rememberFlipBookAnimationController()
) {
    val viewModel: SearchResultViewModel = hiltViewModel()
    
    // 性能优化：使用新的@Composable状态访问方法替代collectAsState()
    val adapter = viewModel.adapter
    
    // 使用优化的@Composable状态访问方法
    val isLoading by adapter.mapState { it.isLoading }.collectAsState(initial = false)
    val isLoadingMore by adapter.isLoadingMoreState()
    val hasMore by adapter.hasMoreState()
    val isEmpty by adapter.mapState { it.isEmpty }.collectAsState(initial = true)
    val hasError by adapter.mapState { it.hasError }.collectAsState(initial = false)
    val query by adapter.queryState()
    val books by adapter.booksState()
    val categoryFilters by adapter.categoryFiltersState()
    val selectedCategoryId by adapter.selectedCategoryIdState()
    val filters by adapter.filtersState()
    val isFilterSheetOpen by adapter.isFilterSheetOpenState()
    
    val listState = rememberLazyListState()

    // 准备翻书动画控制器与覆盖层
    val flipBookController = globalFlipBookController
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenSize by remember(configuration, density) {
        derivedStateOf {
            Pair(configuration.screenWidthDp * density.density, configuration.screenHeightDp * density.density)
        }
    }

    // 性能优化：缓存事件处理回调
    val handleNavigateToDetail = remember { { bookId: String ->
        NavViewModel.navigateToReader(bookId, null)
    } }
    
    val handleNavigateBack = remember { {
        NavViewModel.navigateBack()
    } }
    
    val handleShowToast = remember { { message: String ->
        // TODO: 显示Toast
        TimberLogger.d("SearchResultPage", "显示Toast: $message")
    } }
    
    // 处理事件
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchResultEffect.NavigateToDetail -> {
                    handleNavigateToDetail(effect.bookId.toString())
                }
                is SearchResultEffect.NavigateBack -> {
                    handleNavigateBack()
                }
                is SearchResultEffect.ShowToast -> {
                    handleShowToast(effect.message)
                }
            }
        }
    }

    // 性能优化：缓存初始化查询回调
    val handleInitializeQuery = remember(viewModel) { { queryValue: String ->
        viewModel.sendIntent(SearchResultIntent.UpdateQuery(queryValue))
        viewModel.sendIntent(SearchResultIntent.PerformSearch(queryValue))
    } }
    
    // 初始化查询
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty() && query.isEmpty()) {
            handleInitializeQuery(initialQuery)
        }
    }

    // 性能优化：缓存分页加载回调
    val handleLoadNextPage = remember(viewModel) { {
        TimberLogger.d("SearchResultPage", "触发分页加载")
        viewModel.sendIntent(SearchResultIntent.LoadNextPage)
    } }
    
    // 监听滚动状态，自动加载下一页
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (layoutInfo.totalItemsCount > 0) {
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleItemIndex >= layoutInfo.totalItemsCount - 3 && // 提前3项触发
                        hasMore &&
                        !isLoadingMore &&
                        !isLoading
                    ) {
                        TimberLogger.d("SearchResultPage", "当前项: $lastVisibleItemIndex，总项: ${layoutInfo.totalItemsCount}")
                        handleLoadNextPage()
                    }
                }
            }
    }

    // 性能优化：缓存搜索栏回调函数
    val onQueryChange = remember(viewModel) { { queryValue: String ->
        viewModel.sendIntent(SearchResultIntent.UpdateQuery(queryValue))
    } }
    
    val onSearchClick = remember(viewModel, query) { {
        viewModel.sendIntent(SearchResultIntent.PerformSearch(query))
    } }
    
    val onBackClick = remember(viewModel) { {
        viewModel.sendIntent(SearchResultIntent.NavigateBack)
    } }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏
        SearchTopBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearchClick = onSearchClick,
            onBackClick = onBackClick
        )

        // 性能优化：缓存分类筛选回调函数
        val onCategorySelected = remember(viewModel) { { categoryId: Int? ->
            if (categoryId != null) {
                viewModel.sendIntent(SearchResultIntent.SelectCategory(categoryId))
            }
        } }
        
        val onFilterClick = remember(viewModel) { {
            viewModel.sendIntent(SearchResultIntent.OpenFilterSheet)
        } }
        
        // 分类筛选
        CategoryFilterRow(
            categories = categoryFilters,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onFilterClick = onFilterClick
        )

        // 结果列表
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && books.isEmpty()) {
                SearchResultPageSkeleton()
            } else {
                if (books.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.wdp)
                    ) {
                        items(items = books, key = { it.id }) { book ->
                            SearchResultItem(
                                book = book,
                                onClick = {},
                                onClickWithPosition = { b, offset, size ->
                                    coroutineScope.launch {
                                        flipBookController.startScaleFadeAnimation(
                                            bookId = b.id.toString(),
                                            imageUrl = b.picUrl ?: "",
                                            originalPosition = offset,
                                            originalSize = size,
                                            screenWidth = screenSize.first,
                                            screenHeight = screenSize.second
                                        )
                                    }
                                    NavViewModel.setFlipBookController(flipBookController)
                                    NavViewModel.navigateToReader(b.id.toString(), null)
                                }
                            )
                        }

                        // 加载更多指示器
                        if (hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.wdp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = NovelColors.NovelMain,
                                            modifier = Modifier.size(24.wdp)
                                        )
                                    } else {
                                        // 性能优化：缓存加载更多回调
                                        val onLoadMore = remember(viewModel) { {
                                            viewModel.sendIntent(SearchResultIntent.LoadNextPage)
                                        } }
                                        
                                        NovelText(
                                            text = "点击加载更多...",
                                            fontSize = 14.ssp,
                                            color = NovelColors.NovelTextGray,
                                            modifier = Modifier.clickable { onLoadMore() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isLoading && isEmpty) {
                    // 空状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NovelText(
                                text = "🔍",
                                fontSize = 48.ssp,
                                color = NovelColors.NovelTextGray
                            )
                            Spacer(modifier = Modifier.height(16.wdp))
                            NovelText(
                                text = "没有找到相关结果",
                                fontSize = 16.ssp,
                                color = NovelColors.NovelTextGray
                            )
                            Spacer(modifier = Modifier.height(8.wdp))
                            NovelText(
                                text = "试试其他关键词或调整筛选条件",
                                fontSize = 14.ssp,
                                color = NovelColors.NovelTextGray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 性能优化：缓存筛选弹窗回调函数
    val onFiltersChange = remember(viewModel) { { filtersValue: com.novel.page.search.viewmodel.FilterState ->
        viewModel.sendIntent(SearchResultIntent.UpdateFilters(filtersValue))
    } }
    
    val onFilterDismiss = remember(viewModel) { {
        viewModel.sendIntent(SearchResultIntent.CloseFilterSheet)
    } }
    
    val onFilterClear = remember(viewModel) { {
        viewModel.sendIntent(SearchResultIntent.ClearFilters)
    } }
    
    val onFilterApply = remember(viewModel) { {
        viewModel.sendIntent(SearchResultIntent.ApplyFilters)
    } }
    
    // 筛选弹窗
    if (isFilterSheetOpen) {
        SearchFilterBottomSheet(
            filters = filters,
            onFiltersChange = onFiltersChange,
            onDismiss = onFilterDismiss,
            onClear = onFilterClear,
            onApply = onFilterApply
        )
    }

    // 全局动画覆盖层 - 保证动画渲染
    com.novel.page.component.GlobalFlipBookOverlay(controller = flipBookController)
}

/**
 * 分类筛选行
 */
@Composable
private fun CategoryFilterRow(
    categories: ImmutableList<com.novel.page.search.viewmodel.CategoryFilter>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onFilterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.wdp, vertical = 8.wdp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 分类标签
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            TimberLogger.d("CategoryFilterRow", "categories: $categories")
            categories.forEach { category ->
                com.novel.page.search.component.CategoryFilterChip(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = category.name ?: "未知分类",
                    selected = selectedCategoryId == category.id || (selectedCategoryId == null && category.id == -1),
                    onClick = {
                        val targetId = if (category.id == -1) null else category.id
                        onCategorySelected(targetId)
                    }
                )
            }
        }
        // 筛选按钮
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(NovelColors.NovelBackground)
                .size(40.wdp)
                .align(Alignment.CenterEnd)
                .debounceClickable(
                    onClick = onFilterClick
                )
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "筛选",
                tint = NovelColors.NovelMain,
                modifier = Modifier.size(20.wdp)
            )
        }
    }
}
