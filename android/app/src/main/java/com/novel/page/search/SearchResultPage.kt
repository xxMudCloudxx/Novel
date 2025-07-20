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
    val uiState by viewModel.state.collectAsState()
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

    // 处理事件
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchResultEffect.NavigateToDetail -> {
                    NavViewModel.navigateToReader(effect.bookId, null)
                }
                is SearchResultEffect.NavigateBack -> {
                    NavViewModel.navigateBack()
                }
                is SearchResultEffect.ShowToast -> {
                    // TODO: 显示Toast
                }
            }
        }
    }

    // 初始化查询
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty() && uiState.query.isEmpty()) {
            viewModel.sendIntent(SearchResultIntent.UpdateQuery(initialQuery))
            viewModel.sendIntent(SearchResultIntent.PerformSearch(initialQuery))
        }
    }

    // 监听滚动状态，自动加载下一页
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (layoutInfo.totalItemsCount > 0) {
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleItemIndex >= layoutInfo.totalItemsCount - 3 && // 提前3项触发
                        uiState.hasMore &&
                        !uiState.isLoadingMore &&
                        !uiState.isLoading
                    ) {
                        TimberLogger.d("SearchResultPage", "触发分页加载，当前项: $lastVisibleItemIndex，总项: ${layoutInfo.totalItemsCount}")
                        viewModel.sendIntent(SearchResultIntent.LoadNextPage)
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏
        SearchTopBar(
            query = uiState.query,
            onQueryChange = { query ->
                viewModel.sendIntent(SearchResultIntent.UpdateQuery(query))
            },
            onSearchClick = {
                viewModel.sendIntent(SearchResultIntent.PerformSearch(uiState.query))
            },
            onBackClick = {
                viewModel.sendIntent(SearchResultIntent.NavigateBack)
            }
        )

        // 分类筛选
        CategoryFilterRow(
            categories = uiState.categoryFilters,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = { categoryId ->
                viewModel.sendIntent(SearchResultIntent.SelectCategory(categoryId))
            },
            onFilterClick = {
                viewModel.sendIntent(SearchResultIntent.OpenFilterSheet)
            }
        )

        // 结果列表
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading && uiState.books.isEmpty()) {
                SearchResultPageSkeleton()
            } else {
                if (uiState.books.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.wdp)
                    ) {
                        items(items = uiState.books, key = { it.id }) { book ->
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
                        if (uiState.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.wdp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = NovelColors.NovelMain,
                                            modifier = Modifier.size(24.wdp)
                                        )
                                    } else {
                                        NovelText(
                                            text = "点击加载更多...",
                                            fontSize = 14.ssp,
                                            color = NovelColors.NovelTextGray,
                                            modifier = Modifier.clickable {
                                                viewModel.sendIntent(SearchResultIntent.LoadNextPage)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!uiState.isLoading && uiState.isEmpty) {
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

    // 筛选弹窗
    if (uiState.isFilterSheetOpen) {
        SearchFilterBottomSheet(
            filters = uiState.filters,
            onFiltersChange = { filters ->
                viewModel.sendIntent(SearchResultIntent.UpdateFilters(filters))
            },
            onDismiss = {
                viewModel.sendIntent(SearchResultIntent.CloseFilterSheet)
            },
            onClear = {
                viewModel.sendIntent(SearchResultIntent.ClearFilters)
            },
            onApply = {
                viewModel.sendIntent(SearchResultIntent.ApplyFilters)
            }
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
