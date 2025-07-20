package com.novel.page.search.viewmodel

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.novel.core.adapter.StateAdapter
import kotlinx.collections.immutable.ImmutableList

/**
 * SearchResult状态适配器
 * 
 * 为SearchResult模块提供状态适配功能，方便UI层访问MVI状态的特定部分
 * 继承基础StateAdapter，提供SearchResult模块专用的状态适配功能
 * 
 * 特性：
 * - 继承基础StateAdapter的所有功能
 * - SearchResult模块专用状态访问方法
 * - 细粒度状态订阅，减少不必要的重组
 * - 类型安全的强类型状态访问
 * - UI友好的便利方法
 */
@Stable
class SearchResultStateAdapter(
    stateFlow: StateFlow<SearchResultState>
) : StateAdapter<SearchResultState>(stateFlow) {
    
    // region 搜索相关状态适配
    
    /** 搜索查询内容 */
    val query = mapState { it.query }
    
    /** 搜索结果书籍列表 */
    val books = mapState { it.books }
    
    /** 总结果数量 */
    val totalResults = mapState { it.totalResults }
    
    /** 是否还有更多数据 */
    val hasMore = mapState { it.hasMore }
    
    /** 分页加载状态 */
    val isLoadingMore = mapState { it.isLoadingMore }
    
    /** 是否有搜索结果 */
    val hasResults = createConditionFlow { it.books.isNotEmpty() }
    
    // endregion
    
    // region 筛选相关状态适配
    
    /** 当前选中的分类ID */
    val selectedCategoryId = mapState { it.selectedCategoryId }
    
    /** 分类筛选器列表 */
    val categoryFilters = mapState { it.categoryFilters }
    
    /** 当前筛选条件 */
    val filters = mapState { it.filters }
    
    /** 筛选面板是否打开 */
    val isFilterSheetOpen = mapState { it.isFilterSheetOpen }
    
    /** 是否应用了筛选条件 */
    val hasActiveFilters = createConditionFlow { state ->
        with(state.filters) {
            updateStatus != UpdateStatus.ALL ||
            isVip != VipStatus.ALL ||
            wordCountRange != WordCountRange.ALL ||
            sortBy != SortBy.NULL
        } || state.selectedCategoryId != null
    }
    
    /** 筛选条件摘要文本 */
    val filterSummary = mapState { state ->
        buildList {
            state.selectedCategoryId?.let { categoryId ->
                val categoryName = state.categoryFilters
                    .find { it.id == categoryId }?.name ?: "分类$categoryId"
                add(categoryName)
            }
            
            with(state.filters) {
                if (updateStatus != UpdateStatus.ALL) {
                    add(updateStatus.displayName)
                }
                if (isVip != VipStatus.ALL) {
                    add(isVip.displayName)
                }
                if (wordCountRange != WordCountRange.ALL) {
                    add(wordCountRange.displayName)
                }
                if (sortBy != SortBy.NULL) {
                    add(sortBy.displayName)
                }
            }
        }.joinToString("、")
    }
    
    // endregion
    
    // region SearchResult模块专用便利方法
    
    /** 检查是否可以加载更多 */
    fun canLoadMore(): Boolean {
        val state = getCurrentSnapshot()
        return state.hasMore && !state.isLoadingMore && !state.isLoading
    }
    
    /** 检查是否可以执行搜索 */
    fun canPerformSearch(): Boolean {
        val state = getCurrentSnapshot()
        return state.query.isNotBlank() && !state.isLoading
    }
    
    /** 获取结果统计文本 */
    fun getResultsText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading && state.books.isEmpty() -> "搜索中..."
            state.hasError -> "搜索失败"
            state.books.isEmpty() -> "暂无结果"
            state.totalResults > 0 -> "共找到 ${state.totalResults} 本小说"
            else -> "已显示 ${state.books.size} 本小说"
        }
    }
    
    /** 获取分页状态文本 */
    fun getLoadMoreText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoadingMore -> "加载中..."
            !state.hasMore -> "已加载全部"
            state.hasError -> "加载失败，点击重试"
            else -> "点击加载更多"
        }
    }
    
    /** 获取筛选按钮文本 */
    fun getFilterButtonText(): String {
        val summary = getCurrentSnapshot().let { state ->
            buildList {
                state.selectedCategoryId?.let { categoryId ->
                    val categoryName = state.categoryFilters
                        .find { it.id == categoryId }?.name ?: "分类$categoryId"
                    add(categoryName)
                }
                
                with(state.filters) {
                    if (updateStatus != UpdateStatus.ALL) {
                        add(updateStatus.displayName)
                    }
                    if (isVip != VipStatus.ALL) {
                        add(isVip.displayName)
                    }
                    if (wordCountRange != WordCountRange.ALL) {
                        add(wordCountRange.displayName)
                    }
                    if (sortBy != SortBy.NULL) {
                        add(sortBy.displayName)
                    }
                }
            }.joinToString("、")
        }
        return if (summary.isBlank()) {
            "筛选"
        } else {
            "筛选 ($summary)"
        }
    }
    
    /** 获取当前选中分类名称 */
    fun getSelectedCategoryName(): String? {
        val state = getCurrentSnapshot()
        return state.selectedCategoryId?.let { categoryId ->
            state.categoryFilters.find { it.id == categoryId }?.name
        }
    }
    
    /** 检查特定分类是否被选中 */
    fun isCategorySelected(categoryId: Int): Boolean {
        return getCurrentSnapshot().selectedCategoryId == categoryId
    }
    
    /** 获取分页信息 */
    fun getPaginationInfo(): PaginationInfo {
        val state = getCurrentSnapshot()
        return PaginationInfo(
            currentCount = state.books.size,
            totalCount = state.totalResults,
            hasMore = state.hasMore,
            isLoadingMore = state.isLoadingMore
        )
    }
    
    /** 获取搜索状态摘要 */
    fun getSearchStatusSummary(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "搜索中"
            state.hasError -> "搜索失败"
            state.books.isEmpty() -> "无结果"
            state.isLoadingMore -> "加载更多中"
            else -> "搜索完成"
        }
    }
    
    /** 检查是否显示空状态 */
    fun shouldShowEmptyState(): Boolean {
        val state = getCurrentSnapshot()
        return !state.isLoading && state.books.isEmpty() && !state.hasError
    }
    
    /** 检查是否显示加载更多按钮 */
    fun shouldShowLoadMoreButton(): Boolean {
        val state = getCurrentSnapshot()
        return state.books.isNotEmpty() && state.hasMore && !state.isLoadingMore
    }
    
    /** 获取筛选状态摘要 */
    fun getFilterStatusSummary(): String {
        val activeFiltersCount = with(getCurrentSnapshot().filters) {
            var count = 0
            if (updateStatus != UpdateStatus.ALL) count++
            if (isVip != VipStatus.ALL) count++
            if (wordCountRange != WordCountRange.ALL) count++
            if (sortBy != SortBy.NULL) count++
            count
        }
        
        val categorySelected = getCurrentSnapshot().selectedCategoryId != null
        val totalActive = activeFiltersCount + if (categorySelected) 1 else 0
        
        return when (totalActive) {
            0 -> "无筛选"
            1 -> "1个筛选条件"
            else -> "${totalActive}个筛选条件"
        }
    }
    
    /** 检查是否显示榜单内容 */
    fun shouldShowRankingContent(): Boolean {
        val state = getCurrentSnapshot()
        return !state.isLoading && !state.hasError && with(state.filters) {
            updateStatus != UpdateStatus.ALL ||
            isVip != VipStatus.ALL ||
            wordCountRange != WordCountRange.ALL ||
            sortBy != SortBy.NULL
        } || state.selectedCategoryId != null
    }
    
    // endregion
}

/**
 * 分页信息
 */
@Stable
data class PaginationInfo(
    val currentCount: Int,
    val totalCount: Int,
    val hasMore: Boolean,
    val isLoadingMore: Boolean
)

/**
 * StateAdapter工厂方法
 * 简化SearchResultStateAdapter的创建
 */
fun StateFlow<SearchResultState>.asSearchResultAdapter(): SearchResultStateAdapter {
    return SearchResultStateAdapter(this)
}

/**
 * 状态组合器
 * 将多个状态组合成UI需要的复合状态
 */
@Stable
data class SearchResultScreenState(
    val isLoading: Boolean,
    val error: String?,
    val query: String,
    val books: ImmutableList<BookInfoRespDto>,
    val hasResults: Boolean,
    val resultsText: String,
    val hasActiveFilters: Boolean,
    val filterButtonText: String,
    val canLoadMore: Boolean,
    val loadMoreText: String,
    val isFilterSheetOpen: Boolean,
    val categoryFilters: ImmutableList<CategoryFilter>,
    val currentFilters: FilterState,
    val paginationInfo: PaginationInfo,
    val searchStatusSummary: String,
    val shouldShowEmptyState: Boolean,
    val shouldShowLoadMoreButton: Boolean,
    val filterStatusSummary: String
)

/**
 * 将SearchResultState转换为UI友好的组合状态
 */
fun SearchResultStateAdapter.toScreenState(): SearchResultScreenState {
    val snapshot = getCurrentSnapshot()
    return SearchResultScreenState(
        isLoading = snapshot.isLoading,
        error = snapshot.error,
        query = snapshot.query,
        books = snapshot.books,
        hasResults = snapshot.books.isNotEmpty(),
        resultsText = getResultsText(),
        hasActiveFilters = with(snapshot.filters) {
            updateStatus != UpdateStatus.ALL ||
            isVip != VipStatus.ALL ||
            wordCountRange != WordCountRange.ALL ||
            sortBy != SortBy.NULL
        } || snapshot.selectedCategoryId != null,
        filterButtonText = getFilterButtonText(),
        canLoadMore = canLoadMore(),
        loadMoreText = getLoadMoreText(),
        isFilterSheetOpen = snapshot.isFilterSheetOpen,
        categoryFilters = snapshot.categoryFilters,
        currentFilters = snapshot.filters,
        paginationInfo = getPaginationInfo(),
        searchStatusSummary = getSearchStatusSummary(),
        shouldShowEmptyState = shouldShowEmptyState(),
        shouldShowLoadMoreButton = shouldShowLoadMoreButton(),
        filterStatusSummary = getFilterStatusSummary()
    )
}

/**
 * 筛选条件建造者
 * 简化筛选条件的构建
 */
@Stable
class FilterBuilder {
    private var updateStatus: UpdateStatus = UpdateStatus.ALL
    private var isVip: VipStatus = VipStatus.ALL
    private var wordCountRange: WordCountRange = WordCountRange.ALL
    private var sortBy: SortBy = SortBy.NULL
    
    fun updateStatus(status: UpdateStatus) = apply { this.updateStatus = status }
    fun vipStatus(status: VipStatus) = apply { this.isVip = status }
    fun wordCountRange(range: WordCountRange) = apply { this.wordCountRange = range }
    fun sortBy(sort: SortBy) = apply { this.sortBy = sort }
    
    fun build(): FilterState = FilterState(
        updateStatus = updateStatus,
        isVip = isVip,
        wordCountRange = wordCountRange,
        sortBy = sortBy
    )
}

/**
 * 筛选条件建造者工厂方法
 */
fun buildFilter(block: FilterBuilder.() -> Unit): FilterState {
    return FilterBuilder().apply(block).build()
}

/**
 * SearchResult模块状态监听器
 * 提供SearchResult模块特定的状态变更监听
 */
class SearchResultStateListener(
    private val adapter: SearchResultStateAdapter
) {
    
    /** 监听搜索结果变更 */
    fun onBooksChanged(action: (ImmutableList<BookInfoRespDto>) -> Unit) = adapter.books.map { books ->
        action(books)
        books
    }
    
    /** 监听筛选条件变更 */
    fun onFiltersChanged(action: (FilterState) -> Unit) = adapter.filters.map { filters ->
        action(filters)
        filters
    }
    
    /** 监听分页状态变更 */
    fun onPaginationChanged(action: (PaginationInfo) -> Unit) = adapter.mapState { state ->
        adapter.getPaginationInfo()
    }.map { pagination ->
        action(pagination)
        pagination
    }
    
    /** 监听分类选择变更 */
    fun onCategoryChanged(action: (Int?) -> Unit) = adapter.selectedCategoryId.map { categoryId ->
        action(categoryId)
        categoryId
    }
}

/**
 * 为SearchResultStateAdapter创建专用监听器
 */
fun SearchResultStateAdapter.createSearchResultListener(): SearchResultStateListener {
    return SearchResultStateListener(this)
}