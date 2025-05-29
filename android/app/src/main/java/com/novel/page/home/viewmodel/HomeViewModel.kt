package com.novel.page.home.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.page.home.dao.HomeRepository
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

/**
 * 分类数据
 */
data class CategoryInfo(
    val id: String,
    val name: String
)

/**
 * 首页UI状态
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    
    // 分类数据
    val categories: List<HomeCategoryEntity> = emptyList(),
    val categoryLoading: Boolean = false,
    
    // 书籍推荐数据
    val carouselBooks: List<HomeBookEntity> = emptyList(),
    val hotBooks: List<HomeBookEntity> = emptyList(),
    val newBooks: List<HomeBookEntity> = emptyList(),
    val vipBooks: List<HomeBookEntity> = emptyList(),
    
    val booksLoading: Boolean = false,
    
    // 搜索相关
    val searchQuery: String = "",
    
    // 分类筛选器状态
    val selectedCategoryFilter: String = "推荐",
    val categoryFilters: List<CategoryInfo> = listOf(
        CategoryInfo("0", "推荐")
    ),
    val categoryFiltersLoading: Boolean = false,
    
    // 榜单状态
    val selectedRankType: String = HomeRepository.RANK_TYPE_VISIT,
    val rankBooks: List<BookService.BookRank> = emptyList(),
    val rankLoading: Boolean = false,
    
    // 推荐书籍状态 - 支持两种数据源
    val recommendBooks: List<SearchService.BookInfo> = emptyList(), // 分类搜索结果
    val homeRecommendBooks: List<HomeService.HomeBook> = emptyList(), // 首页推荐
    val recommendLoading: Boolean = false,
    val hasMoreRecommend: Boolean = true,
    val recommendPage: Int = 1,
    val totalRecommendPages: Int = 1,
    
    // 首页推荐分页状态
    val homeRecommendLoading: Boolean = false,
    val hasMoreHomeRecommend: Boolean = true,
    val homeRecommendPage: Int = 1,
    
    // 当前显示模式
    val isRecommendMode: Boolean = true // true=推荐模式，false=分类模式
)

/**
 * 首页用户操作
 */
sealed class HomeAction {
    data object Refresh : HomeAction()
    data object LoadInitialData : HomeAction()
    data class OnSearchQueryChange(val query: String) : HomeAction()
    data class OnRankBookClick(val bookId: Long) : HomeAction()
    data class OnRecommendBookClick(val bookId: Long) : HomeAction()
    data class OnCategoryFilterSelected(val filter: String) : HomeAction()
    data class OnRankTypeSelected(val rankType: String) : HomeAction()
    data object OnSearchClick : HomeAction()
    data object OnCategoryButtonClick : HomeAction()
    data object LoadMoreRecommend : HomeAction()
    data object LoadMoreHomeRecommend : HomeAction()
    data object RefreshRecommend : HomeAction()
    data object ClearError : HomeAction()
}

/**
 * 首页一次性事件
 */
sealed class HomeEvent {
    data class NavigateToBook(val bookId: Long) : HomeEvent()
    data class NavigateToCategory(val categoryId: Long) : HomeEvent()
    data class NavigateToSearch(val query: String = "") : HomeEvent()
    data object NavigateToCategoryPage : HomeEvent()
    data class ShowToast(val message: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeService: HomeService,
    private val searchService: SearchService
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val RECOMMEND_PAGE_SIZE = 8
    }
    
    // UI状态
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // 事件通道
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()
    
    // 当前状态
    val currentState: HomeUiState get() = _uiState.value
    
    // 缓存全量首页推荐数据
    private var cachedHomeBooks: List<HomeService.HomeBook> = emptyList()
    
    init {
        // 初始化时加载数据
        onAction(HomeAction.LoadInitialData)
    }
    
    /**
     * 处理用户操作
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.LoadInitialData -> loadInitialData()
            is HomeAction.Refresh -> refreshData()
            is HomeAction.OnSearchQueryChange -> updateSearchQuery(action.query)
            is HomeAction.OnRankBookClick -> navigateToRankBook(action.bookId)
            is HomeAction.OnRecommendBookClick -> navigateToRecommendBook(action.bookId)
            is HomeAction.OnCategoryFilterSelected -> selectCategoryFilter(action.filter)
            is HomeAction.OnRankTypeSelected -> selectRankType(action.rankType)
            is HomeAction.OnSearchClick -> navigateToSearch()
            is HomeAction.OnCategoryButtonClick -> navigateToCategoryPage()
            is HomeAction.LoadMoreRecommend -> loadMoreRecommend()
            is HomeAction.LoadMoreHomeRecommend -> loadMoreHomeRecommend()
            is HomeAction.RefreshRecommend -> refreshRecommend()
            is HomeAction.ClearError -> clearError()
        }
    }
    
    /**
     * 更新状态
     */
    private fun updateState(update: (HomeUiState) -> HomeUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    /**
     * 发送事件
     */
    private fun sendEvent(event: HomeEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            try {
                // 并发加载所有数据
                launch { loadCategoryFilters() }
                launch { loadCategories() }
                launch { loadBooks() }
                launch { loadRankBooks(currentState.selectedRankType) }
                launch { loadHomeRecommendBooks() }
                
                updateState { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "加载初始数据失败", e)
                updateState { 
                    it.copy(
                        isLoading = false, 
                        error = "加载数据失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 加载分类筛选器数据 - 使用Repository
     */
    private fun loadCategoryFilters() {
        viewModelScope.launch {
            updateState { it.copy(categoryFiltersLoading = true) }
            
            try {
                homeRepository.getBookCategories()
                    .catch { e ->
                        Log.e(TAG, "加载分类筛选器失败", e)
                        updateState { 
                            it.copy(
                                categoryFiltersLoading = false,
                                error = "加载分类失败：${e.localizedMessage}"
                            ) 
                        }
                    }
                    .collect { categories ->
                        val filters = mutableListOf<CategoryInfo>().apply {
                            add(CategoryInfo("0", "推荐"))
                            addAll(categories.map { CategoryInfo(it.id.toString(), it.name) })
                        }
                        
                        updateState { 
                            it.copy(
                                categoryFilters = filters,
                                categoryFiltersLoading = false
                            ) 
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "加载分类筛选器异常", e)
                updateState { 
                    it.copy(
                        categoryFiltersLoading = false,
                        error = "加载分类失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 加载首页推荐书籍 - 改进的分页策略
     */
    private fun loadHomeRecommendBooks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            updateState { 
                it.copy(
                    homeRecommendLoading = true,
                    error = if (isRefresh) null else it.error
                ) 
            }
            
            try {
                // 如果是刷新或者缓存为空，重新获取数据
                if (isRefresh || cachedHomeBooks.isEmpty()) {
                    val response = homeService.getHomeBooksBlocking()
                    
                    if (response.ok == true && response.data != null) {
                        cachedHomeBooks = response.data
                    } else {
                        throw Exception(response.message ?: "获取推荐书籍失败")
                    }
                }
                
                // 基于缓存数据进行分页
                val currentPage = if (isRefresh) 1 else currentState.homeRecommendPage
                val startIndex = (currentPage - 1) * RECOMMEND_PAGE_SIZE
                val endIndex = startIndex + RECOMMEND_PAGE_SIZE
                
                val currentBooks = if (isRefresh) {
                    // 刷新时重置为第一页
                    cachedHomeBooks.take(RECOMMEND_PAGE_SIZE)
                } else {
                    // 加载更多时累积显示
                    cachedHomeBooks.take(endIndex)
                }
                
                val hasMore = endIndex < cachedHomeBooks.size
                
                updateState { 
                    it.copy(
                        homeRecommendBooks = currentBooks,
                        homeRecommendLoading = false,
                        hasMoreHomeRecommend = hasMore,
                        homeRecommendPage = currentPage,
                        isRefreshing = false
                    ) 
                }
                
                Log.d(TAG, "首页推荐数据加载完成：当前显示${currentBooks.size}本，总共${cachedHomeBooks.size}本，hasMore=$hasMore")
            } catch (e: Exception) {
                Log.e(TAG, "加载首页推荐书籍失败", e)
                updateState { 
                    it.copy(
                        homeRecommendLoading = false,
                        isRefreshing = false,
                        error = "加载推荐书籍失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 根据分类加载推荐书籍
     */
    private fun loadRecommendByCategory(categoryName: String) {
        if (categoryName == "推荐") {
            updateState { 
                it.copy(
                    isRecommendMode = true,
                    recommendBooks = emptyList(),
                    recommendPage = 1
                ) 
            }
            loadHomeRecommendBooks()
            return
        }
        
        viewModelScope.launch {
            updateState { 
                it.copy(
                    recommendLoading = true,
                    isRecommendMode = false,
                    recommendBooks = emptyList(),
                    recommendPage = 1
                ) 
            }
            
            try {
                val categoryId = currentState.categoryFilters
                    .find { it.name == categoryName }?.id?.toIntOrNull() ?: 1
                
                val response = searchService.searchBooksBlocking(
                    SearchService.SearchRequest(
                        categoryId = categoryId,
                        pageNum = 1,
                        pageSize = RECOMMEND_PAGE_SIZE
                    )
                )
                
                if (response.ok == true && response.data != null) {
                    val hasMore = response.data.list.size >= RECOMMEND_PAGE_SIZE
                    updateState { 
                        it.copy(
                            recommendBooks = response.data.list,
                            recommendLoading = false,
                            hasMoreRecommend = hasMore,
                            totalRecommendPages = response.data.pages.toInt(),
                            recommendPage = 1,
                            isRefreshing = false // 确保停止刷新状态
                        ) 
                    }
                } else {
                    throw Exception(response.message ?: "搜索书籍失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "根据分类加载推荐书籍失败", e)
                updateState { 
                    it.copy(
                        recommendLoading = false,
                        isRefreshing = false, // 确保停止刷新状态
                        error = "加载分类书籍失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 加载更多推荐书籍
     */
    private fun loadMoreRecommend() {
        if (currentState.isRecommendMode || currentState.recommendLoading || !currentState.hasMoreRecommend) {
            return
        }
        
        viewModelScope.launch {
            updateState { it.copy(recommendLoading = true) }
            
            try {
                val categoryId = currentState.categoryFilters
                    .find { it.name == currentState.selectedCategoryFilter }?.id?.toIntOrNull() ?: 1
                
                val nextPage = currentState.recommendPage + 1
                val response = searchService.searchBooksBlocking(
                    SearchService.SearchRequest(
                        categoryId = categoryId,
                        pageNum = nextPage,
                        pageSize = RECOMMEND_PAGE_SIZE
                    )
                )
                
                if (response.ok == true && response.data != null) {
                    val newBooks = response.data.list
                    val hasMore = newBooks.size >= RECOMMEND_PAGE_SIZE
                    
                    updateState { 
                        it.copy(
                            recommendBooks = it.recommendBooks + newBooks,
                            recommendLoading = false,
                            hasMoreRecommend = hasMore,
                            recommendPage = nextPage
                        ) 
                    }
                } else {
                    throw Exception(response.message ?: "加载更多失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载更多推荐书籍失败", e)
                updateState { 
                    it.copy(
                        recommendLoading = false,
                        error = "加载更多失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 加载榜单书籍 - 使用Repository
     */
    private fun loadRankBooks(rankType: String = HomeRepository.RANK_TYPE_VISIT) {
        viewModelScope.launch {
            updateState { it.copy(rankLoading = true) }
            
            try {
                homeRepository.getRankBooks(rankType)
                    .catch { e ->
                        Log.e(TAG, "加载榜单书籍失败", e)
                        updateState { 
                            it.copy(
                                rankLoading = false,
                                error = "加载榜单失败：${e.localizedMessage}"
                            ) 
                        }
                    }
                    .collect { rankBooks ->
                        updateState { 
                            it.copy(
                                rankBooks = rankBooks,
                                rankLoading = false
                            ) 
                        }
                        Log.d(TAG, "$rankType 数据加载完成，共${rankBooks.size}本书")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "加载榜单书籍异常", e)
                updateState { 
                    it.copy(
                        rankLoading = false,
                        error = "加载榜单失败：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 选择分类筛选器
     */
    private fun selectCategoryFilter(filter: String) {
        if (filter == currentState.selectedCategoryFilter) return
        
        updateState { it.copy(selectedCategoryFilter = filter) }
        loadRecommendByCategory(filter)
    }
    
    /**
     * 选择榜单类型
     */
    private fun selectRankType(rankType: String) {
        if (rankType == currentState.selectedRankType) return
        
        updateState { it.copy(selectedRankType = rankType) }
        loadRankBooks(rankType)
    }
    
    /**
     * 刷新数据
     */
    private fun refreshData() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, error = null) }
            
            try {
                // 强制刷新所有数据
                homeRepository.refreshAllData()
                
                // 重新加载UI数据
                launch { loadCategories(forceRefresh = true) }
                launch { loadBooks(forceRefresh = true) }
                
                sendEvent(HomeEvent.ShowToast("刷新成功"))
                
            } catch (e: Exception) {
                Log.e(TAG, "刷新数据失败", e)
                updateState { 
                    it.copy(error = "刷新失败：${e.localizedMessage}") 
                }
                sendEvent(HomeEvent.ShowToast("刷新失败"))
            } finally {
                updateState { it.copy(isRefreshing = false) }
            }
        }
    }
    
    /**
     * 加载分类数据
     */
    private fun loadCategories(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            updateState { it.copy(categoryLoading = true) }
            
            homeRepository.getCategories(forceRefresh)
                .catch { e ->
                    Log.e(TAG, "加载分类失败", e)
                    updateState { it.copy(categoryLoading = false) }
                }
                .collect { categories ->
                    updateState { 
                        it.copy(
                            categories = categories,
                            categoryLoading = false
                        ) 
                    }
                    Log.d(TAG, "分类数据加载完成，共${categories.size}个")
                }
        }
    }
    
    /**
     * 加载书籍推荐数据
     */
    private fun loadBooks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            updateState { it.copy(booksLoading = true) }
            
            try {
                // 并发加载不同类型的书籍
                val carouselFlow = homeRepository.getCarouselBooks(forceRefresh)
                val hotFlow = homeRepository.getHotBooks(forceRefresh)
                val newFlow = homeRepository.getNewBooks(forceRefresh)
                val vipFlow = homeRepository.getVipBooks(forceRefresh)
                
                // 合并所有Flow
                combine(carouselFlow, hotFlow, newFlow, vipFlow) { carousel, hot, new, vip ->
                    updateState { 
                        it.copy(
                            carouselBooks = carousel,
                            hotBooks = hot,
                            newBooks = new,
                            vipBooks = vip,
                            booksLoading = false,
                            isLoading = false
                        ) 
                    }
                    Log.d(TAG, "书籍数据加载完成：轮播${carousel.size}，热门${hot.size}，最新${new.size}，VIP${vip.size}")
                }.catch { e ->
                    Log.e(TAG, "加载书籍数据失败", e)
                    updateState { 
                        it.copy(
                            booksLoading = false,
                            isLoading = false,
                            error = "加载书籍失败：${e.localizedMessage}"
                        ) 
                    }
                }.collect()
                
            } catch (e: Exception) {
                Log.e(TAG, "加载书籍数据异常", e)
                updateState { 
                    it.copy(
                        booksLoading = false,
                        isLoading = false,
                        error = "加载书籍异常：${e.localizedMessage}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 更新搜索查询
     */
    private fun updateSearchQuery(query: String) {
        updateState { 
            it.copy(searchQuery = query) 
        }
    }
    
    /**
     * 导航到搜索页面
     */
    private fun navigateToSearch() {
        val query = currentState.searchQuery
        Log.d(TAG, "导航到搜索页面，查询：$query")
        sendEvent(HomeEvent.NavigateToSearch(query))
    }
    
    /**
     * 清除错误状态
     */
    private fun clearError() {
        updateState { it.copy(error = null) }
    }
    
    /**
     * 导航到分类页面
     */
    private fun navigateToCategoryPage() {
        Log.d(TAG, "点击分类按钮")
        sendEvent(HomeEvent.NavigateToCategoryPage)
    }
    
    /**
     * 导航到榜单书籍详情
     */
    private fun navigateToRankBook(bookId: Long) {
        Log.d(TAG, "点击榜单书籍：$bookId")
        sendEvent(HomeEvent.NavigateToBook(bookId))
    }
    
    /**
     * 导航到推荐书籍详情
     */
    private fun navigateToRecommendBook(bookId: Long) {
        Log.d(TAG, "点击推荐书籍：$bookId")
        sendEvent(HomeEvent.NavigateToBook(bookId))
    }
    
    /**
     * 加载更多首页推荐书籍 - 改进版
     */
    private fun loadMoreHomeRecommend() {
        if (currentState.homeRecommendLoading || !currentState.hasMoreHomeRecommend) {
            return
        }
        
        viewModelScope.launch {
            updateState { 
                it.copy(
                    homeRecommendLoading = true,
                    homeRecommendPage = it.homeRecommendPage + 1
                ) 
            }
            
            try {
                val nextPage = currentState.homeRecommendPage
                val startIndex = (nextPage - 1) * RECOMMEND_PAGE_SIZE
                val endIndex = startIndex + RECOMMEND_PAGE_SIZE
                
                // 基于缓存数据累积显示
                val updatedBooks = cachedHomeBooks.take(endIndex)
                val hasMore = endIndex < cachedHomeBooks.size
                
                updateState { 
                    it.copy(
                        homeRecommendBooks = updatedBooks,
                        homeRecommendLoading = false,
                        hasMoreHomeRecommend = hasMore
                    ) 
                }
                
                Log.d(TAG, "加载更多首页推荐：当前显示${updatedBooks.size}本，总共${cachedHomeBooks.size}本，hasMore=$hasMore")
            } catch (e: Exception) {
                Log.e(TAG, "加载更多首页推荐书籍失败", e)
                updateState { 
                    it.copy(
                        homeRecommendLoading = false,
                        homeRecommendPage = it.homeRecommendPage - 1 // 回退页码
                    ) 
                }
            }
        }
    }
    
    /**
     * 刷新推荐数据
     */
    private fun refreshRecommend() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, error = null) }
            
            try {
                if (currentState.isRecommendMode) {
                    // 刷新首页推荐
                    updateState { 
                        it.copy(
                            homeRecommendBooks = emptyList(),
                            homeRecommendPage = 1,
                            hasMoreHomeRecommend = true
                        ) 
                    }
                    loadHomeRecommendBooks(isRefresh = true)
                } else {
                    // 刷新分类推荐
                    updateState { 
                        it.copy(
                            recommendBooks = emptyList(),
                            recommendPage = 1,
                            hasMoreRecommend = true
                        ) 
                    }
                    loadRecommendByCategory(currentState.selectedCategoryFilter)
                }
            } finally {
                updateState { it.copy(isRefreshing = false) }
            }
        }
    }
} 