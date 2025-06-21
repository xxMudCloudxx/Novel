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
import com.novel.utils.ReactNativeBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import kotlinx.coroutines.delay
import com.novel.page.home.dao.toEntity
import com.novel.repository.CachedBookRepository

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
    data object LoadInitialData : HomeAction()
    data object RefreshData : HomeAction()
    data class UpdateSearchQuery(val query: String) : HomeAction()
    data class SelectCategoryFilter(val categoryName: String) : HomeAction()
    data object LoadMoreRecommend : HomeAction()
    data object LoadMoreHomeRecommend : HomeAction()
    data class SelectRankType(val rankType: String) : HomeAction()
    data class NavigateToSearch(val query: String) : HomeAction()
    data class NavigateToBookDetail(val bookId: Long) : HomeAction()
    data class NavigateToCategory(val categoryId: Long) : HomeAction()
    data class NavigateToFullRanking(val rankType: String) : HomeAction()
    data object RestoreData : HomeAction()
    
    // 兼容旧版本的动作
    @Deprecated("Use RefreshData instead")
    data object Refresh : HomeAction()
    @Deprecated("Use UpdateSearchQuery instead")
    data class OnSearchQueryChange(val query: String) : HomeAction()
    @Deprecated("Use NavigateToBookDetail instead")
    data class OnRankBookClick(val bookId: Long) : HomeAction()
    @Deprecated("Use NavigateToBookDetail instead")
    data class OnRecommendBookClick(val bookId: Long) : HomeAction()
    @Deprecated("Use SelectCategoryFilter instead")
    data class OnCategoryFilterSelected(val filter: String) : HomeAction()
    @Deprecated("Use SelectRankType instead")
    data class OnRankTypeSelected(val rankType: String) : HomeAction()
    @Deprecated("Use NavigateToSearch instead")
    data object OnSearchClick : HomeAction()
    @Deprecated("Use NavigateToCategory instead")
    data object OnCategoryButtonClick : HomeAction()
    @Deprecated("Use LoadMoreRecommend instead")
    data object LoadMoreRecommendByCategory : HomeAction()
    @Deprecated("Use RefreshData instead")
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
    data class NavigateToBookDetail(val bookId: Long) : HomeEvent()
    data class NavigateToFullRanking(val rankType: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val homeService: HomeService,
    private val searchService: SearchService,
    private val cachedBookRepository: CachedBookRepository
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
    
    // 预加载缓存 - 分类筛选器数据缓存
    private val categoryRecommendCache = mutableMapOf<String, List<SearchService.BookInfo>>()
    private val categoryLoadingSet = mutableSetOf<String>()
    
    // 预加载缓存 - 榜单数据缓存  
    private val rankBooksCache = mutableMapOf<String, List<BookService.BookRank>>()
    private val rankLoadingSet = mutableSetOf<String>()
    
    // 榜单类型列表，用于确定前一页和后一页
    private val rankTypes = listOf(
        HomeRepository.RANK_TYPE_VISIT,
        HomeRepository.RANK_TYPE_UPDATE, 
        HomeRepository.RANK_TYPE_NEWEST
    )
    
    init {
        // 初始化时加载数据
        onAction(HomeAction.LoadInitialData)
        
        // 发送测试数据到RN（确保RN能接收到数据）
        viewModelScope.launch {
            delay(2000) // 等待RN初始化完成
            ReactNativeBridge.sendTestUserDataToRN()
            delay(500)
            ReactNativeBridge.sendTestRecommendBooksToRN()
        }
    }
    
    /**
     * 处理用户操作
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.LoadInitialData -> {
                loadInitialData()
            }
            is HomeAction.RefreshData -> {
                refreshData()
            }
            is HomeAction.UpdateSearchQuery -> {
                updateState { it.copy(searchQuery = action.query) }
            }
            is HomeAction.SelectCategoryFilter -> {
                selectCategoryFilter(action.categoryName)
            }
            is HomeAction.LoadMoreRecommend -> {
                if (currentState.isRecommendMode) {
                    loadMoreHomeRecommend()
                } else {
                    loadMoreRecommend()
                }
            }
            is HomeAction.SelectRankType -> {
                selectRankType(action.rankType)
            }
            is HomeAction.NavigateToSearch -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToSearch(action.query))
                }
            }
            is HomeAction.NavigateToBookDetail -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToBookDetail(action.bookId))
                }
            }
            is HomeAction.NavigateToCategory -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToCategory(action.categoryId))
                }
            }
            is HomeAction.NavigateToFullRanking -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToFullRanking(action.rankType))
                }
            }
            is HomeAction.RestoreData -> {
                // 新增：页面恢复时的数据检查和恢复
                restoreDataIfNeeded()
            }
            is HomeAction.LoadMoreHomeRecommend -> {
                loadMoreHomeRecommend()
            }
            
            // 兼容旧版本的动作处理
            is HomeAction.Refresh -> refreshData()
            is HomeAction.OnSearchQueryChange -> updateState { it.copy(searchQuery = action.query) }
            is HomeAction.OnRankBookClick -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToBookDetail(action.bookId))
                }
            }
            is HomeAction.OnRecommendBookClick -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToBookDetail(action.bookId))
                }
            }
            is HomeAction.OnCategoryFilterSelected -> selectCategoryFilter(action.filter)
            is HomeAction.OnRankTypeSelected -> selectRankType(action.rankType)
            is HomeAction.OnSearchClick -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToSearch(currentState.searchQuery))
                }
            }
            is HomeAction.OnCategoryButtonClick -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToCategory(0))
                }
            }
            is HomeAction.LoadMoreRecommendByCategory -> {
                if (!currentState.isRecommendMode) {
                    loadMoreRecommend()
                }
            }
            is HomeAction.RefreshRecommend -> refreshData()
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
                
                // 延迟触发预加载，避免初始加载时网络压力过大
                launch {
                    delay(1000) // 等待主要数据加载完成
                    preloadAdjacentRankTypes(currentState.selectedRankType)
                    preloadAdjacentCategories(currentState.selectedCategoryFilter)
                }
                
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
     * 加载分类筛选器数据 - 使用缓存策略
     */
    private fun loadCategoryFilters() {
        viewModelScope.launch {
            updateState { it.copy(categoryFiltersLoading = true) }
            
            try {
                homeRepository.getBookCategories(strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST)
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
     * 加载首页推荐书籍 - 改进的分页策略，使用缓存优先
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
                    val homeBooks = homeRepository.getHomeBooks(
                        strategy = if (isRefresh) com.novel.utils.network.cache.CacheStrategy.NETWORK_ONLY 
                                 else com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                    )
                    
                    cachedHomeBooks = homeBooks
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
                
                // 修复：当获取到的数据为0时，强制重新获取网络数据
                if (currentBooks.isEmpty() && !isRefresh) {
                    Log.w(TAG, "首页推荐数据为空，尝试从网络获取")
                    // 使用网络优先策略重新获取
                    val networkBooks = homeRepository.getHomeBooks(
                        strategy = com.novel.utils.network.cache.CacheStrategy.NETWORK_ONLY
                    )
                    
                    if (networkBooks.isNotEmpty()) {
                        cachedHomeBooks = networkBooks
                        val newCurrentBooks = networkBooks.take(RECOMMEND_PAGE_SIZE)
                        val newHasMore = RECOMMEND_PAGE_SIZE < networkBooks.size
                        
                        updateState { 
                            it.copy(
                                homeRecommendBooks = newCurrentBooks,
                                homeRecommendLoading = false,
                                hasMoreHomeRecommend = newHasMore,
                                homeRecommendPage = 1,
                                isRefreshing = false
                            ) 
                        }
                        Log.d(TAG, "从网络获取首页推荐数据成功：${newCurrentBooks.size}本")
                        return@launch
                    }
                }
                
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
     * 根据分类加载推荐书籍 - 使用缓存策略
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
                
                // 使用CachedBookRepository进行搜索，它已经实现了缓存策略
                val booksData = cachedBookRepository.searchBooks(
                    categoryId = categoryId,
                    pageNum = 1,
                    pageSize = RECOMMEND_PAGE_SIZE,
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                
                val hasMore = booksData.list.size >= RECOMMEND_PAGE_SIZE
                
                // 修复：当获取到的数据为0时，强制重新获取网络数据
                if (booksData.list.isEmpty()) {
                    Log.w(TAG, "分类推荐数据为空，尝试从网络获取")
                    // 使用网络优先策略重新获取
                    val networkBooksData = cachedBookRepository.searchBooks(
                        categoryId = categoryId,
                        pageNum = 1,
                        pageSize = RECOMMEND_PAGE_SIZE,
                        strategy = com.novel.utils.network.cache.CacheStrategy.NETWORK_ONLY
                    )
                    
                    if (networkBooksData.list.isNotEmpty()) {
                        val networkHasMore = networkBooksData.list.size >= RECOMMEND_PAGE_SIZE
                        
                        // 更新缓存
                        categoryRecommendCache[categoryName] = networkBooksData.list
                        
                        updateState { 
                            it.copy(
                                recommendBooks = networkBooksData.list,
                                recommendLoading = false,
                                hasMoreRecommend = networkHasMore,
                                totalRecommendPages = networkBooksData.pages.toInt(),
                                recommendPage = 1,
                                isRefreshing = false
                            ) 
                        }
                        Log.d(TAG, "从网络获取分类推荐数据成功：${networkBooksData.list.size}本")
                        return@launch
                    }
                }
                
                // 更新缓存
                categoryRecommendCache[categoryName] = booksData.list
                
                updateState { 
                    it.copy(
                        recommendBooks = booksData.list,
                        recommendLoading = false,
                        hasMoreRecommend = hasMore,
                        totalRecommendPages = booksData.pages.toInt(),
                        recommendPage = 1,
                        isRefreshing = false // 确保停止刷新状态
                    ) 
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
     * 加载更多推荐书籍 - 使用缓存策略
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
                
                // 使用CachedBookRepository进行搜索，它已经实现了缓存策略
                val booksData = cachedBookRepository.searchBooks(
                    categoryId = categoryId,
                    pageNum = nextPage,
                    pageSize = RECOMMEND_PAGE_SIZE,
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                
                val newBooks = booksData.list
                val hasMore = newBooks.size >= RECOMMEND_PAGE_SIZE
                
                updateState { 
                    it.copy(
                        recommendBooks = it.recommendBooks + newBooks,
                        recommendLoading = false,
                        hasMoreRecommend = hasMore,
                        recommendPage = nextPage
                    ) 
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
                val rankBooks = homeRepository.getRankBooks(rankType)
                
                // 修复：当获取到的榜单数据为0时，强制重新获取网络数据
                if (rankBooks.isEmpty()) {
                    Log.w(TAG, "榜单数据为空，尝试从网络获取")
                    // 使用网络优先策略重新获取
                    val networkRankBooks = homeRepository.getRankBooks(
                        rankType,
                        strategy = com.novel.utils.network.cache.CacheStrategy.NETWORK_ONLY
                    )
                    
                    if (networkRankBooks.isNotEmpty()) {
                        // 更新缓存
                        rankBooksCache[rankType] = networkRankBooks
                        
                        updateState { 
                            it.copy(
                                rankBooks = networkRankBooks,
                                rankLoading = false
                            ) 
                        }
                        Log.d(TAG, "从网络获取$rankType 数据成功，共${networkRankBooks.size}本书")
                        return@launch
                    }
                }
                
                // 更新缓存
                rankBooksCache[rankType] = rankBooks
                
                updateState { 
                    it.copy(
                        rankBooks = rankBooks,
                        rankLoading = false
                    ) 
                }
                Log.d(TAG, "$rankType 数据加载完成，共${rankBooks.size}本书")
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
     * 选择分类筛选器 - 支持预加载
     */
    private fun selectCategoryFilter(filter: String) {
        if (filter == currentState.selectedCategoryFilter) return
        
        updateState { it.copy(selectedCategoryFilter = filter) }
        loadRecommendByCategoryFromCacheOrNetwork(filter)
        
        // 触发相邻分类的预加载
        preloadAdjacentCategories(filter)
    }
    
    /**
     * 选择榜单类型 - 支持预加载
     */
    private fun selectRankType(rankType: String) {
        if (rankType == currentState.selectedRankType) return
        
        updateState { it.copy(selectedRankType = rankType) }
        loadRankBooksFromCacheOrNetwork(rankType)
        
        // 触发相邻榜单的预加载
        preloadAdjacentRankTypes(rankType)
    }
    
    /**
     * 刷新数据
     */
    private fun refreshData() {
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, error = null) }
            
            try {
                // 清理过期缓存
                clearOldCaches()
                
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
                .collect { apiCategories ->
                    // 转换为Entity
                    val categories = apiCategories.map { category ->
                        HomeCategoryEntity(
                            id = category.id,
                            name = category.name,
                            iconUrl = null,
                            sortOrder = 0
                        )
                    }
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
                    // 转换为Entity
                    val carouselEntities = carousel.map { it.toEntity("carousel") }
                    val hotEntities = hot.map { it.toEntity("hot") }
                    val newEntities = new.map { it.toEntity("new") }
                    val vipEntities = vip.map { it.toEntity("vip") }
                    
                    updateState { 
                        it.copy(
                            carouselBooks = carouselEntities,
                            hotBooks = hotEntities,
                            newBooks = newEntities,
                            vipBooks = vipEntities,
                            booksLoading = false,
                            isLoading = false
                        ) 
                    }
                    Log.d(TAG, "书籍数据加载完成：轮播${carouselEntities.size}，热门${hotEntities.size}，最新${newEntities.size}，VIP${vipEntities.size}")
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
                    loadRecommendByCategoryFromCacheOrNetwork(currentState.selectedCategoryFilter)
                }
            } finally {
                updateState { it.copy(isRefreshing = false) }
            }
        }
    }
    
    // region 预加载相关方法
    
    /**
     * 预加载相邻分类的数据
     */
    private fun preloadAdjacentCategories(currentFilter: String) {
        viewModelScope.launch {
            val filters = currentState.categoryFilters
            val currentIndex = filters.indexOfFirst { it.name == currentFilter }
            
            if (currentIndex == -1) return@launch
            
            // 预加载前一个分类
            if (currentIndex > 0) {
                val prevFilter = filters[currentIndex - 1].name
                preloadCategoryData(prevFilter)
            }
            
            // 预加载后一个分类
            if (currentIndex < filters.size - 1) {
                val nextFilter = filters[currentIndex + 1].name
                preloadCategoryData(nextFilter)
            }
        }
    }
    
    /**
     * 预加载指定分类的数据 - 使用缓存策略
     */
    private fun preloadCategoryData(categoryName: String) {
        // 如果是推荐分类或已经在加载中或已有缓存，则跳过
        if (categoryName == "推荐" || 
            categoryLoadingSet.contains(categoryName) ||
            categoryRecommendCache.containsKey(categoryName)) {
            return
        }
        
        viewModelScope.launch {
            categoryLoadingSet.add(categoryName)
            
            try {
                val categoryId = currentState.categoryFilters
                    .find { it.name == categoryName }?.id?.toIntOrNull() ?: 1
                
                // 使用CachedBookRepository进行搜索，它已经实现了缓存策略
                val booksData = cachedBookRepository.searchBooks(
                    categoryId = categoryId,
                    pageNum = 1,
                    pageSize = RECOMMEND_PAGE_SIZE,
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                
                categoryRecommendCache[categoryName] = booksData.list
                Log.d(TAG, "预加载分类 $categoryName 数据成功，共${booksData.list.size}本书")
                
            } catch (e: Exception) {
                Log.e(TAG, "预加载分类 $categoryName 数据失败", e)
            } finally {
                categoryLoadingSet.remove(categoryName)
            }
        }
    }
    
    /**
     * 预加载相邻榜单类型的数据
     */
    private fun preloadAdjacentRankTypes(currentRankType: String) {
        viewModelScope.launch {
            val currentIndex = rankTypes.indexOf(currentRankType)
            
            if (currentIndex == -1) return@launch
            
            // 预加载前一个榜单
            if (currentIndex > 0) {
                val prevRankType = rankTypes[currentIndex - 1]
                preloadRankData(prevRankType)
            }
            
            // 预加载后一个榜单
            if (currentIndex < rankTypes.size - 1) {
                val nextRankType = rankTypes[currentIndex + 1]
                preloadRankData(nextRankType)
            }
        }
    }
    
    /**
     * 预加载指定榜单类型的数据
     */
    private fun preloadRankData(rankType: String) {
        // 如果已经在加载中或已有缓存，则跳过
        if (rankLoadingSet.contains(rankType) || rankBooksCache.containsKey(rankType)) {
            return
        }
        
        viewModelScope.launch {
            rankLoadingSet.add(rankType)
            
            try {
                val rankBooks = homeRepository.getRankBooks(rankType)
                rankBooksCache[rankType] = rankBooks
                Log.d(TAG, "预加载榜单 $rankType 数据成功，共${rankBooks.size}本书")
            } catch (e: Exception) {
                Log.e(TAG, "预加载榜单 $rankType 数据失败", e)
            } finally {
                rankLoadingSet.remove(rankType)
            }
        }
    }
    
    /**
     * 从缓存或网络加载榜单数据
     */
    private fun loadRankBooksFromCacheOrNetwork(rankType: String) {
        // 如果缓存中有数据，立即使用
        rankBooksCache[rankType]?.let { cachedBooks ->
            updateState { 
                it.copy(
                    rankBooks = cachedBooks,
                    rankLoading = false,
                    selectedRankType = rankType
                ) 
            }
            Log.d(TAG, "使用缓存的榜单数据：$rankType，共${cachedBooks.size}本书")
            return
        }
        
        // 缓存中没有数据，从网络加载
        loadRankBooks(rankType)
    }
    
    /**
     * 从缓存或网络加载分类推荐数据
     */
    private fun loadRecommendByCategoryFromCacheOrNetwork(categoryName: String) {
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
        
        // 如果缓存中有数据，立即使用
        categoryRecommendCache[categoryName]?.let { cachedBooks ->
            updateState { 
                it.copy(
                    recommendBooks = cachedBooks,
                    recommendLoading = false,
                    isRecommendMode = false,
                    recommendPage = 1,
                    hasMoreRecommend = cachedBooks.size >= RECOMMEND_PAGE_SIZE,
                    isRefreshing = false
                ) 
            }
            Log.d(TAG, "使用缓存的分类数据：$categoryName，共${cachedBooks.size}本书")
            return
        }
        
        // 缓存中没有数据，从网络加载
        loadRecommendByCategory(categoryName)
    }
    
    /**
     * 清理过期的缓存数据
     */
    private fun clearOldCaches() {
        // 限制分类缓存数量，保留最近使用的10个
        if (categoryRecommendCache.size > 4) {
            val keysToRemove = categoryRecommendCache.keys.take(categoryRecommendCache.size - 4)
            keysToRemove.forEach { categoryRecommendCache.remove(it) }
        }
        
        // 榜单缓存相对较少，暂不清理
        Log.d(TAG, "缓存清理完成，分类缓存剩余：${categoryRecommendCache.size}个")
    }
    
    /**
     * 页面恢复时检查并恢复数据
     */
    private fun restoreDataIfNeeded() {
        viewModelScope.launch {
            // 检查榜单数据是否为空
            if (currentState.rankBooks.isEmpty()) {
                Log.d(TAG, "检测到榜单数据为空，尝试恢复数据")
                
                // 先尝试从内存缓存恢复
                val currentRankType = currentState.selectedRankType
                if (rankBooksCache.containsKey(currentRankType)) {
                    rankBooksCache[currentRankType]?.let { cachedBooks ->
                        updateState { 
                            it.copy(
                                rankBooks = cachedBooks,
                                rankLoading = false
                            ) 
                        }
                        Log.d(TAG, "从内存缓存恢复榜单数据：$currentRankType，共${cachedBooks.size}本书")
                        return@launch
                    }
                }
                
                // 内存缓存为空，从持久化缓存或网络加载
                loadRankBooksFromCacheOrNetwork(currentRankType)
            }
            
            // 检查分类数据是否为空
            if (currentState.categoryFilters.size <= 1) { // 只有"推荐"分类
                Log.d(TAG, "检测到分类数据不完整，重新加载")
                loadCategoryFilters()
            }
            
            // 检查推荐数据是否为空
            if (currentState.isRecommendMode && currentState.homeRecommendBooks.isEmpty()) {
                Log.d(TAG, "检测到推荐数据为空，重新加载")
                loadHomeRecommendBooks()
            }
        }
    }
    
    // endregion
} 