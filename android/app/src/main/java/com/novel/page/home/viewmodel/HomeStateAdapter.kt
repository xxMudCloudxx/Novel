package com.novel.page.home.viewmodel

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService
import com.novel.core.adapter.StateAdapter

/**
 * Home状态适配器
 * 
 * 为Home模块提供状态适配功能，方便UI层访问MVI状态的特定部分
 * 继承基础StateAdapter，提供Home模块专用的状态适配功能
 * 
 * 特性：
 * - 继承基础StateAdapter的所有功能
 * - Home模块专用状态访问方法
 * - 细粒度状态订阅，减少不必要的重组
 * - 类型安全的强类型状态访问
 * - UI友好的便利方法
 * - 向后兼容HomeUiState格式
 */
@Stable
class HomeStateAdapter(
    stateFlow: StateFlow<HomeState>
) : StateAdapter<HomeState>(stateFlow) {
    
    // region 基础状态适配
    
    /** 下拉刷新状态 */
    val isRefreshing = mapState { it.isRefreshing }
    
    /** 搜索查询内容 */
    val searchQuery = mapState { it.searchQuery }
    
    // endregion
    
    // region 分类相关状态适配
    
    /** 书籍分类列表 */
    val categories = mapState { it.categories }
    
    /** 分类数据加载状态 */
    val categoryLoading = mapState { it.categoryLoading }
    
    /** 分类筛选器列表 */
    val categoryFilters = mapState { it.categoryFilters }
    
    /** 当前选中的分类筛选器 */
    val selectedCategoryFilter = mapState { it.selectedCategoryFilter }
    
    /** 分类筛选器加载状态 */
    val categoryFiltersLoading = mapState { it.categoryFiltersLoading }
    
    /** 是否有分类数据 */
    val hasCategories = createConditionFlow { it.categories.isNotEmpty() }
    
    // endregion
    
    // region 书籍推荐相关状态适配
    
    /** 轮播图书籍列表 */
    val carouselBooks = mapState { it.carouselBooks }
    
    /** 热门书籍列表 */
    val hotBooks = mapState { it.hotBooks }
    
    /** 最新书籍列表 */
    val newBooks = mapState { it.newBooks }
    
    /** VIP书籍列表 */
    val vipBooks = mapState { it.vipBooks }
    
    /** 书籍数据加载状态 */
    val booksLoading = mapState { it.booksLoading }
    
    /** 是否有轮播图书籍 */
    val hasCarouselBooks = createConditionFlow { it.carouselBooks.isNotEmpty() }
    
    /** 是否有热门书籍 */
    val hasHotBooks = createConditionFlow { it.hotBooks.isNotEmpty() }
    
    /** 是否有最新书籍 */
    val hasNewBooks = createConditionFlow { it.newBooks.isNotEmpty() }
    
    /** 是否有VIP书籍 */
    val hasVipBooks = createConditionFlow { it.vipBooks.isNotEmpty() }
    
    // endregion
    
    // region 榜单相关状态适配
    
    /** 当前选中的榜单类型 */
    val selectedRankType = mapState { it.selectedRankType }
    
    /** 榜单书籍列表 */
    val rankBooks = mapState { it.rankBooks }
    
    /** 榜单加载状态 */
    val rankLoading = mapState { it.rankLoading }
    
    /** 是否有榜单数据 */
    val hasRankBooks = createConditionFlow { it.rankBooks.isNotEmpty() }
    
    // endregion
    
    // region 推荐书籍相关状态适配
    
    /** 分类推荐书籍列表 */
    val recommendBooks = mapState { it.recommendBooks }
    
    /** 首页推荐书籍列表 */
    val homeRecommendBooks = mapState { it.homeRecommendBooks }
    
    /** 推荐书籍加载状态 */
    val recommendLoading = mapState { it.recommendLoading }
    
    /** 是否还有更多推荐数据 */
    val hasMoreRecommend = mapState { it.hasMoreRecommend }
    
    /** 当前推荐页码 */
    val recommendPage = mapState { it.recommendPage }
    
    /** 显示模式（推荐模式/分类模式） */
    val isRecommendMode = mapState { it.isRecommendMode }
    
    /** 首页推荐加载状态 */
    val homeRecommendLoading = mapState { it.homeRecommendLoading }
    
    /** 是否还有更多首页推荐 */
    val hasMoreHomeRecommend = mapState { it.hasMoreHomeRecommend }
    
    /** 首页推荐页码 */
    val homeRecommendPage = mapState { it.homeRecommendPage }
    
    /** 是否有推荐书籍数据 */
    val hasRecommendBooks = createConditionFlow { state ->
        state.recommendBooks.isNotEmpty() || state.homeRecommendBooks.isNotEmpty()
    }
    
    /** 当前显示的推荐书籍数量 */
    val currentRecommendCount = mapState { state ->
        if (state.isRecommendMode) {
            state.homeRecommendBooks.size
        } else {
            state.recommendBooks.size
        }
    }
    
    // endregion
    
    // region Home模块专用便利方法
    
    /** 检查是否可以执行搜索 */
    fun canPerformSearch(): Boolean {
        val state = getCurrentSnapshot()
        return state.searchQuery.isNotBlank() && !state.isLoading
    }
    
    /** 获取搜索提示文本 */
    fun getSearchHint(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "加载中..."
            state.hasError -> "搜索失败，请重试"
            else -> "搜索您喜欢的小说"
        }
    }
    
    /** 检查是否可以加载更多推荐 */
    fun canLoadMoreRecommend(): Boolean {
        val state = getCurrentSnapshot()
        return when {
            state.isRecommendMode -> state.hasMoreHomeRecommend && !state.homeRecommendLoading
            else -> state.hasMoreRecommend && !state.recommendLoading
        }
    }
    
    /** 获取加载更多文本 */
    fun getLoadMoreText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isRecommendMode && state.homeRecommendLoading -> "加载中..."
            !state.isRecommendMode && state.recommendLoading -> "加载中..."
            !canLoadMoreRecommend() -> "已加载全部"
            state.hasError -> "加载失败，点击重试"
            else -> "点击加载更多"
        }
    }
    
    /** 获取分类筛选器状态文本 */
    fun getCategoryFilterStatusText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.categoryFiltersLoading -> "加载分类中..."
            state.categoryFilters.isEmpty() -> "暂无分类"
            else -> "共${state.categoryFilters.size}个分类"
        }
    }
    
    /** 获取榜单状态文本 */
    fun getRankingStatusText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.rankLoading -> "加载榜单中..."
            state.rankBooks.isEmpty() -> "暂无榜单"
            else -> "${state.selectedRankType} - ${state.rankBooks.size}本书"
        }
    }
    
    /** 检查特定分类是否被选中 */
    fun isCategorySelected(categoryName: String): Boolean {
        return getCurrentSnapshot().selectedCategoryFilter == categoryName
    }
    
    /** 检查特定榜单类型是否被选中 */
    fun isRankTypeSelected(rankType: String): Boolean {
        return getCurrentSnapshot().selectedRankType == rankType
    }
    
    /** 获取首页状态摘要 */
    fun getHomeStatusSummary(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "加载中"
            state.hasError -> "加载失败"
            state.isRefreshing -> "刷新中"
            state.isEmpty -> "暂无数据"
            else -> "加载完成"
        }
    }
    
    /** 检查是否显示空状态 */
    fun shouldShowEmptyState(): Boolean {
        val state = getCurrentSnapshot()
        return !state.isLoading && !state.hasError && 
               state.categories.isEmpty() && 
               state.carouselBooks.isEmpty() && 
               state.hotBooks.isEmpty() && 
               state.newBooks.isEmpty() && 
               state.vipBooks.isEmpty()
    }
    
    /** 检查是否显示加载更多按钮 */
    fun shouldShowLoadMoreButton(): Boolean {
        val state = getCurrentSnapshot()
        return canLoadMoreRecommend() && (state.recommendBooks.isNotEmpty() || state.homeRecommendBooks.isNotEmpty())
    }
    
    /** 获取推荐模式文本 */
    fun getRecommendModeText(): String {
        val state = getCurrentSnapshot()
        return if (state.isRecommendMode) {
            "首页推荐"
        } else {
            "分类推荐 - ${state.selectedCategoryFilter}"
        }
    }
    
    // endregion
    
    // region 向后兼容方法
    
    /**
     * 将HomeState转换为HomeUiState
     * 保持与原有UI层的兼容性
     */
    fun toHomeUiState(): HomeUiState {
        val state = getCurrentSnapshot()
        return HomeUiState(
            // 基础状态
            version = state.version,
            isLoading = state.isLoading,
            error = state.error,
            isRefreshing = state.isRefreshing,
            
            // 分类数据相关
            categories = state.categories,
            categoryLoading = state.categoryLoading,
            
            // 书籍推荐数据相关
            carouselBooks = state.carouselBooks,
            hotBooks = state.hotBooks,
            newBooks = state.newBooks,
            vipBooks = state.vipBooks,
            booksLoading = state.booksLoading,
            
            // 搜索相关
            searchQuery = state.searchQuery,
            
            // 分类筛选器状态
            selectedCategoryFilter = state.selectedCategoryFilter,
            categoryFilters = state.categoryFilters,
            categoryFiltersLoading = state.categoryFiltersLoading,
            
            // 榜单状态
            selectedRankType = state.selectedRankType,
            rankBooks = state.rankBooks,
            rankLoading = state.rankLoading,
            
            // 推荐书籍状态
            recommendBooks = state.recommendBooks,
            homeRecommendBooks = state.homeRecommendBooks,
            recommendLoading = state.recommendLoading,
            hasMoreRecommend = state.hasMoreRecommend,
            recommendPage = state.recommendPage,
            totalRecommendPages = state.totalRecommendPages,
            
            // 首页推荐分页状态
            homeRecommendLoading = state.homeRecommendLoading,
            hasMoreHomeRecommend = state.hasMoreHomeRecommend,
            homeRecommendPage = state.homeRecommendPage,
            
            // 显示模式控制
            isRecommendMode = state.isRecommendMode
        )
    }
    
    // endregion
}

/**
 * StateAdapter工厂方法
 * 简化HomeStateAdapter的创建
 */
fun StateFlow<HomeState>.asHomeAdapter(): HomeStateAdapter {
    return HomeStateAdapter(this)
}

/**
 * 状态组合器
 * 将多个状态组合成UI需要的复合状态
 */
@Stable
data class HomeScreenState(
    val isLoading: Boolean,
    val error: String?,
    val isRefreshing: Boolean,
    val searchQuery: String,
    val categories: List<HomeCategoryEntity>,
    val selectedCategoryFilter: String,
    val carouselBooks: List<HomeBookEntity>,
    val hotBooks: List<HomeBookEntity>,
    val newBooks: List<HomeBookEntity>,
    val vipBooks: List<HomeBookEntity>,
    val rankBooks: List<BookService.BookRank>,
    val selectedRankType: String,
    val currentRecommendBooks: List<Any>, // 根据模式显示不同类型的书籍
    val canPerformSearch: Boolean,
    val searchHint: String,
    val canLoadMoreRecommend: Boolean,
    val loadMoreText: String,
    val homeStatusSummary: String,
    val shouldShowEmptyState: Boolean,
    val shouldShowLoadMoreButton: Boolean,
    val recommendModeText: String
)

/**
 * 将HomeState转换为UI友好的组合状态
 */
fun HomeStateAdapter.toScreenState(): HomeScreenState {
    val snapshot = getCurrentSnapshot()
    return HomeScreenState(
        isLoading = snapshot.isLoading,
        error = snapshot.error,
        isRefreshing = snapshot.isRefreshing,
        searchQuery = snapshot.searchQuery,
        categories = snapshot.categories,
        selectedCategoryFilter = snapshot.selectedCategoryFilter,
        carouselBooks = snapshot.carouselBooks,
        hotBooks = snapshot.hotBooks,
        newBooks = snapshot.newBooks,
        vipBooks = snapshot.vipBooks,
        rankBooks = snapshot.rankBooks,
        selectedRankType = snapshot.selectedRankType,
        currentRecommendBooks = if (snapshot.isRecommendMode) {
            snapshot.homeRecommendBooks
        } else {
            snapshot.recommendBooks
        },
        canPerformSearch = canPerformSearch(),
        searchHint = getSearchHint(),
        canLoadMoreRecommend = canLoadMoreRecommend(),
        loadMoreText = getLoadMoreText(),
        homeStatusSummary = getHomeStatusSummary(),
        shouldShowEmptyState = shouldShowEmptyState(),
        shouldShowLoadMoreButton = shouldShowLoadMoreButton(),
        recommendModeText = getRecommendModeText()
    )
}

/**
 * Home模块状态监听器
 * 提供Home模块特定的状态变更监听
 */
class HomeStateListener(
    private val adapter: HomeStateAdapter
) {
    
    /** 监听搜索查询变更 */
    fun onSearchQueryChanged(action: (String) -> Unit) = adapter.searchQuery.map { query ->
        action(query)
        query
    }
    
    /** 监听分类筛选器变更 */
    fun onCategoryFilterChanged(action: (String) -> Unit) = adapter.selectedCategoryFilter.map { filter ->
        action(filter)
        filter
    }
    
    /** 监听榜单类型变更 */
    fun onRankTypeChanged(action: (String) -> Unit) = adapter.selectedRankType.map { rankType ->
        action(rankType)
        rankType
    }
    
    /** 监听推荐书籍变更 */
    fun onRecommendBooksChanged(action: (Boolean, Int) -> Unit) = adapter.combineState { state ->
        Pair(state.isRecommendMode, if (state.isRecommendMode) state.homeRecommendBooks.size else state.recommendBooks.size)
    }.map { (isRecommendMode, count) ->
        action(isRecommendMode, count)
        Pair(isRecommendMode, count)
    }
}

/**
 * 为HomeStateAdapter创建专用监听器
 */
fun HomeStateAdapter.createHomeListener(): HomeStateListener {
    return HomeStateListener(this)
}

/**
 * 原有的HomeUiState数据类（保持兼容性）
 */
data class HomeUiState(
    // 基础状态
    val version: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    
    // 分类数据相关
    val categories: List<HomeCategoryEntity> = emptyList(),
    val categoryLoading: Boolean = false,
    
    // 书籍推荐数据相关
    val carouselBooks: List<HomeBookEntity> = emptyList(),
    val hotBooks: List<HomeBookEntity> = emptyList(),
    val newBooks: List<HomeBookEntity> = emptyList(),
    val vipBooks: List<HomeBookEntity> = emptyList(),
    val booksLoading: Boolean = false,
    
    // 搜索相关
    val searchQuery: String = "",
    
    // 分类筛选器状态
    val selectedCategoryFilter: String = "推荐",
    val categoryFilters: List<CategoryInfo> = listOf(CategoryInfo("0", "推荐")),
    val categoryFiltersLoading: Boolean = false,
    
    // 榜单状态
    val selectedRankType: String = "点击榜",
    val rankBooks: List<BookService.BookRank> = emptyList(),
    val rankLoading: Boolean = false,
    
    // 推荐书籍状态
    val recommendBooks: List<SearchService.BookInfo> = emptyList(),
    val homeRecommendBooks: List<HomeService.HomeBook> = emptyList(),
    val recommendLoading: Boolean = false,
    val hasMoreRecommend: Boolean = true,
    val recommendPage: Int = 1,
    val totalRecommendPages: Int = 1,
    
    // 首页推荐分页状态
    val homeRecommendLoading: Boolean = false,
    val hasMoreHomeRecommend: Boolean = true,
    val homeRecommendPage: Int = 1,
    
    // 显示模式控制
    val isRecommendMode: Boolean = true
)

/**
 * Home页面的Action定义（保持原有定义以确保兼容性）
 */
sealed class HomeAction {
    data object LoadInitialData : HomeAction()
    data object RefreshData : HomeAction()
    data class UpdateSearchQuery(val query: String) : HomeAction()
    data class SelectCategoryFilter(val categoryName: String) : HomeAction()
    data class SelectRankType(val rankType: String) : HomeAction()
    data object LoadMoreRecommend : HomeAction()
    data object LoadMoreHomeRecommend : HomeAction()
    data class NavigateToSearch(val query: String) : HomeAction()
    data class NavigateToBookDetail(val bookId: Long) : HomeAction()
    data class NavigateToCategory(val categoryId: Long) : HomeAction()
    data class NavigateToFullRanking(val rankType: String) : HomeAction()
    data object RestoreData : HomeAction()
    data object ClearError : HomeAction()
}

/**
 * Home页面的Event定义（保持原有定义以确保兼容性）
 */
sealed class HomeEvent {
    data class NavigateToBook(val bookId: Long) : HomeEvent()
    data class NavigateToCategory(val categoryId: Long) : HomeEvent()
    data class NavigateToSearch(val query: String = "") : HomeEvent()
    data object NavigateToCategoryPage : HomeEvent()
    data class ShowToast(val message: String) : HomeEvent()
    data class NavigateToBookDetail(val bookId: Long) : HomeEvent()
    data class NavigateToFullRanking(val rankType: String) : HomeEvent()
    data class SendToReactNative(val data: Any) : HomeEvent()
}