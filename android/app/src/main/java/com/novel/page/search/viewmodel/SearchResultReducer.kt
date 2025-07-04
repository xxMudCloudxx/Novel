package com.novel.page.search.viewmodel

import com.novel.utils.TimberLogger
import com.novel.core.mvi.MviReducerWithEffect
import com.novel.core.mvi.ReduceResult
import com.novel.core.mvi.MviReducer

/**
 * 搜索结果页面状态转换器
 * 
 * 实现纯函数式状态转换逻辑，替换现有ViewModel手写状态管理
 * 根据当前状态和Intent计算新状态，支持副作用处理
 */
class SearchResultReducer : MviReducer<SearchResultIntent, SearchResultState> {
    
    companion object {
        private const val TAG = "SearchResultReducer"
    }
    
    override fun reduce(currentState: SearchResultState, intent: SearchResultIntent): SearchResultState {
        val result = reduceWithEffect(currentState, intent)
        return result.newState
    }
    
    fun reduceWithEffect(
        currentState: SearchResultState, 
        intent: SearchResultIntent
    ): ReduceResult<SearchResultState, SearchResultEffect> {
        TimberLogger.d(TAG, "处理Intent: ${intent::class.simpleName}")
        
        return when (intent) {
            is SearchResultIntent.UpdateQuery -> {
                // 更新搜索查询内容
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    query = intent.query
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.PerformSearch -> {
                // 开始新的搜索，重置分页状态
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    isLoading = true,
                    error = null,
                    query = intent.query,
                    books = emptyList(), // 清空之前的结果
                    totalResults = 0,
                    hasMore = false,
                    isLoadingMore = false
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.SelectCategory -> {
                // 选择分类并重新搜索
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    selectedCategoryId = intent.categoryId
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.OpenFilterSheet -> {
                // 打开筛选面板
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    isFilterSheetOpen = true
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.CloseFilterSheet -> {
                // 关闭筛选面板
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    isFilterSheetOpen = false
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.UpdateFilters -> {
                // 更新筛选条件
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    filters = intent.filters
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.ApplyFilters -> {
                // 应用筛选条件，关闭面板并重新搜索
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    isFilterSheetOpen = false,
                    isLoading = true,
                    books = emptyList(), // 清空之前的结果
                    totalResults = 0,
                    hasMore = false
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.ClearFilters -> {
                // 清除筛选条件
                val newState = currentState.copy(
                    version = currentState.version + 1,
                    filters = FilterState()
                )
                ReduceResult(newState)
            }
            
            is SearchResultIntent.LoadNextPage -> {
                // 检查是否可以加载更多
                if (currentState.isLoadingMore || !currentState.hasMore) {
                    TimberLogger.d(TAG, "跳过分页加载：isLoadingMore=${currentState.isLoadingMore}, hasMore=${currentState.hasMore}")
                    ReduceResult(currentState)
                } else {
                    // 开始分页加载
                    val newState = currentState.copy(
                        version = currentState.version + 1,
                        isLoadingMore = true
                    )
                    ReduceResult(newState)
                }
            }
            
            is SearchResultIntent.NavigateToDetail -> {
                // 导航到书籍详情
                val effect = SearchResultEffect.NavigateToDetail(intent.bookId)
                ReduceResult(currentState, effect)
            }
            
            is SearchResultIntent.NavigateBack -> {
                // 返回上级页面
                val effect = SearchResultEffect.NavigateBack
                ReduceResult(currentState, effect)
            }
        }
    }
    
    /**
     * 处理搜索成功的状态更新
     * 提供给ViewModel在搜索完成后调用
     */
    fun handleSearchSuccess(
        currentState: SearchResultState,
        books: List<BookInfoRespDto>,
        totalResults: Int,
        hasMore: Boolean,
        isLoadMore: Boolean = false
    ): SearchResultState {
        return if (isLoadMore) {
            // 分页加载成功
            currentState.copy(
                version = currentState.version + 1,
                books = currentState.books + books,
                hasMore = hasMore,
                isLoadingMore = false
            )
        } else {
            // 新搜索成功
            currentState.copy(
                version = currentState.version + 1,
                isLoading = false,
                error = null,
                books = books,
                totalResults = totalResults,
                hasMore = hasMore,
                isLoadingMore = false
            )
        }
    }
    
    /**
     * 处理搜索失败的状态更新
     * 提供给ViewModel在搜索失败后调用
     */
    fun handleSearchError(
        currentState: SearchResultState,
        errorMessage: String,
        isLoadMore: Boolean = false
    ): SearchResultState {
        return if (isLoadMore) {
            // 分页加载失败
            currentState.copy(
                version = currentState.version + 1,
                isLoadingMore = false
            )
        } else {
            // 新搜索失败
            currentState.copy(
                version = currentState.version + 1,
                isLoading = false,
                error = errorMessage,
                isLoadingMore = false
            )
        }
    }
    
    /**
     * 处理分类筛选器加载成功
     * 提供给ViewModel在分类加载完成后调用
     */
    fun handleCategoryFiltersLoaded(
        currentState: SearchResultState,
        categoryFilters: List<CategoryFilter>
    ): SearchResultState {
        return currentState.copy(
            version = currentState.version + 1,
            categoryFilters = categoryFilters
        )
    }
}
