package com.novel.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelText
import com.novel.page.search.viewmodel.SearchResultAction
import com.novel.page.search.viewmodel.SearchResultEffect
import com.novel.page.search.viewmodel.SearchResultViewModel
import com.novel.page.search.viewmodel.CategoryFilter
import com.novel.page.search.component.SearchResultItem
import com.novel.page.search.component.SearchTopBar
import com.novel.page.search.component.SearchFilterBottomSheet
import com.novel.page.search.skeleton.SearchResultPageSkeleton
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel
import com.novel.utils.wdp
import com.novel.utils.ssp

/**
 * 搜索结果页面 - MVI重构版
 */
@Composable
fun SearchResultPage(
    initialQuery: String,
    viewModel: SearchResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 处理事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchResultEffect.NavigateToDetail -> {
                    NavViewModel.navigateToReader(event.bookId, null)
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
            viewModel.onAction(SearchResultAction.UpdateQuery(initialQuery))
            viewModel.onAction(SearchResultAction.PerformSearch(initialQuery))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏
        SearchTopBar(
            query = uiState.query,
            onQueryChange = { query ->
                viewModel.onAction(SearchResultAction.UpdateQuery(query))
            },
            onSearchClick = {
                viewModel.onAction(SearchResultAction.PerformSearch(uiState.query))
            },
            onBackClick = {
                viewModel.onAction(SearchResultAction.NavigateBack)
            }
        )

        // 分类筛选
        CategoryFilterRow(
            categories = uiState.categoryFilters,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = { categoryId ->
                viewModel.onAction(SearchResultAction.SelectCategory(categoryId))
            },
            onFilterClick = {
                viewModel.onAction(SearchResultAction.OpenFilterSheet)
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
                                onClick = {
                                    viewModel.onAction(SearchResultAction.NavigateToDetail(book.id.toString()))
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
                                                viewModel.onAction(SearchResultAction.LoadNextPage)
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
                viewModel.onAction(SearchResultAction.UpdateFilters(filters))
            },
            onDismiss = {
                viewModel.onAction(SearchResultAction.CloseFilterSheet)
            },
            onClear = {
                viewModel.onAction(SearchResultAction.ClearFilters)
            },
            onApply = {
                viewModel.onAction(SearchResultAction.ApplyFilters)
            }
        )
    }
}

/**
 * 分类筛选行
 */
@Composable
private fun CategoryFilterRow(
    categories: List<CategoryFilter>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onFilterClick: () -> Unit
) {
    // 简单实现，避免复杂依赖
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.wdp, vertical = 8.wdp),
        horizontalArrangement = Arrangement.spacedBy(8.wdp)
    ) {
        categories.take(5).forEach { category ->
            val isSelected = selectedCategoryId == category.id
            NovelText(
                text = category.name ?: "",
                fontSize = 14.ssp,
                color = if (isSelected) NovelColors.NovelMain else NovelColors.NovelTextGray,
                modifier = Modifier
                    .clickable { onCategorySelected(category.id) }
                    .padding(horizontal = 8.wdp, vertical = 4.wdp)
            )
        }
        
        NovelText(
            text = "筛选",
            fontSize = 14.ssp,
            color = NovelColors.NovelMain,
            modifier = Modifier
                .clickable { onFilterClick() }
                .padding(horizontal = 8.wdp, vertical = 4.wdp)
        )
    }
}
