package com.novel.page.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.novel.core.mvi.BaseMviViewModel
import com.novel.core.mvi.MviReducer
import com.novel.page.home.dao.HomeRepository
import com.novel.page.home.usecase.*
import com.novel.page.component.StateHolderImpl
import com.novel.utils.TimberLogger
import com.novel.utils.network.cache.CacheStrategy
import com.novel.utils.network.repository.CachedBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页ViewModel - MVI重构版本
 * 
 * 采用统一的MVI架构模式：
 * - 继承BaseMviViewModel，获得完整的MVI支持
 * - 使用Intent处理所有用户交互和系统事件
 * - 通过Reducer进行纯函数状态转换
 * - 使用Effect处理一次性副作用
 * - 业务逻辑完全委托给UseCase层（特别是HomeCompositeUseCase）
 * 
 * 功能特性：
 * - 首页数据的加载和缓存
 * - 分类筛选和推荐书籍展示
 * - 榜单数据的异步获取
 * - 搜索功能集成
 * - React Native数据同步
 * - 下拉刷新和加载更多
 * - 错误处理和重试机制
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    /** 主要数据仓库 */
    private val homeRepository: HomeRepository,
    /** 缓存书籍数据仓库 */
    private val cachedBookRepository: CachedBookRepository
) : BaseMviViewModel<HomeIntent, HomeState, HomeEffect>() {
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val RECOMMEND_PAGE_SIZE = 8
    }
    
    // 手动创建UseCase实例，避免Hilt泛型问题
    private val homeCompositeUseCase: HomeCompositeUseCase by lazy {
        HomeCompositeUseCase(homeRepository, cachedBookRepository, com.novel.core.domain.ComposeUseCase())
    }
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase by lazy {
        GetHomeCategoriesUseCase(homeRepository)
    }
    private val getHomeRecommendBooksUseCase: GetHomeRecommendBooksUseCase by lazy {
        GetHomeRecommendBooksUseCase(homeRepository)
    }
    private val getRankingBooksUseCase: GetRankingBooksUseCase by lazy {
        GetRankingBooksUseCase(homeRepository)
    }
    private val refreshHomeDataUseCase: RefreshHomeDataUseCase by lazy {
        RefreshHomeDataUseCase(homeRepository)
    }
    private val sendReactNativeDataUseCase: SendReactNativeDataUseCase by lazy {
        SendReactNativeDataUseCase()
    }
    private val getCategoryRecommendBooksUseCase: GetCategoryRecommendBooksUseCase by lazy {
        GetCategoryRecommendBooksUseCase(cachedBookRepository)
    }
    private val homeStatusCheckUseCase: HomeStatusCheckUseCase by lazy {
        HomeStatusCheckUseCase(homeRepository)
    }
    
    // 缓存全量首页推荐数据
    private var cachedHomeBooks: List<com.novel.utils.network.api.front.HomeService.HomeBook> = emptyList()
    
    /** 新的StateAdapter实例 */
    val adapter = HomeStateAdapter(state)
    
    /** 兼容性属性：UI状态流，适配原有的UI层期望格式 */
    val uiState: StateFlow<HomeUiState> = state.map { mviState ->
        adapter.toHomeUiState()
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = HomeUiState()
    )

    /** 兼容性属性：当前状态 */
    val currentState: HomeUiState get() = uiState.value
    
    init {
        // 初始化时加载数据
        sendIntent(HomeIntent.LoadInitialData)
        
        // 发送测试数据到RN（延迟执行，确保RN能接收到数据）
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            sendReactNativeDataUseCase(Unit)
        }
    }
    
    /**
     * 创建初始状态
     */
    override fun createInitialState(): HomeState {
        return HomeState()
    }
    
    /**
     * 获取Reducer实例
     */
    override fun getReducer(): MviReducer<HomeIntent, HomeState> {
        // 返回一个适配器，将MviReducerWithEffect适配为MviReducer
        val effectReducer = HomeReducer()
        return object : MviReducer<HomeIntent, HomeState> {
            override fun reduce(currentState: HomeState, intent: HomeIntent): HomeState {
                val result = effectReducer.reduce(currentState, intent)
                // 在这里处理副作用
                result.effect?.let { effect ->
                    sendEffect(effect)
                }
                return result.newState
            }
        }
    }

    /**
     * Intent处理完成后的回调
     * 在这里处理需要调用UseCase的Intent
     */
    override fun onIntentProcessed(intent: HomeIntent, newState: HomeState) {
        when (intent) {
            is HomeIntent.LoadInitialData -> {
                loadInitialData()
            }
            is HomeIntent.RefreshData -> {
                refreshData()
            }
            is HomeIntent.SelectCategoryFilter -> {
                selectCategoryFilter(intent.categoryName)
            }
            is HomeIntent.SelectRankType -> {
                selectRankType(intent.rankType)
            }
            is HomeIntent.LoadMoreRecommend -> {
                val currentState = getCurrentState()
                if (currentState.isRecommendMode) {
                    loadMoreHomeRecommend()
                } else {
                    loadMoreRecommend()
                }
            }
            is HomeIntent.LoadMoreHomeRecommend -> {
                loadMoreHomeRecommend()
            }
            is HomeIntent.RestoreData -> {
                restoreDataIfNeeded()
            }
            else -> {
                // 其他Intent由Reducer处理，无需额外操作
        }
    }
    }
    
    // ========== 私有方法 - 业务逻辑处理 ==========
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "开始加载初始数据")
                
                val result = homeCompositeUseCase(
                    HomeCompositeUseCase.Params(loadInitialData = true)
                )
                
                if (result.isSuccess) {
                    // 发送各种成功Intent
                    sendIntent(HomeIntent.CategoryFiltersLoadSuccess(result.categoryFilters))
                    sendIntent(HomeIntent.CategoriesLoadSuccess(result.categories))
                    sendIntent(HomeIntent.BooksLoadSuccess(
                        carouselBooks = result.carouselBooks,
                        hotBooks = result.hotBooks,
                        newBooks = result.newBooks,
                        vipBooks = result.vipBooks
                    ))
                    sendIntent(HomeIntent.RankBooksLoadSuccess(result.rankBooks))
                    sendIntent(HomeIntent.HomeRecommendBooksLoadSuccess(
                        books = result.homeRecommendBooks,
                        isRefresh = true,
                        hasMore = result.hasMoreRecommend
                    ))
                    
                    // 缓存首页推荐数据
                    cachedHomeBooks = result.homeRecommendBooks
                    
                    TimberLogger.d(TAG, "初始数据加载完成")
                } else {
                    sendIntent(HomeIntent.BooksLoadFailure(result.errorMessage ?: "加载失败"))
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载初始数据异常", e)
                sendIntent(HomeIntent.BooksLoadFailure(e.message ?: "未知错误"))
            }
        }
    }
    
    /**
     * 刷新数据
     */
    private fun refreshData() {
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "开始刷新数据")
                
                val result = homeCompositeUseCase(
                    HomeCompositeUseCase.Params(refreshData = true)
                )
                
                if (result.isSuccess) {
                    // 更新缓存并发送成功Intent
                    cachedHomeBooks = result.homeRecommendBooks
                    
                    sendIntent(HomeIntent.CategoryFiltersLoadSuccess(result.categoryFilters))
                    sendIntent(HomeIntent.CategoriesLoadSuccess(result.categories))
                    sendIntent(HomeIntent.BooksLoadSuccess(
                        carouselBooks = result.carouselBooks,
                        hotBooks = result.hotBooks,
                        newBooks = result.newBooks,
                        vipBooks = result.vipBooks
                    ))
                    sendIntent(HomeIntent.HomeRecommendBooksLoadSuccess(
                        books = result.homeRecommendBooks,
                        isRefresh = true,
                        hasMore = result.hasMoreRecommend
                    ))
                    sendIntent(HomeIntent.RefreshComplete)
                    
                    sendEffect(HomeEffect.ShowToast("刷新成功"))
                    TimberLogger.d(TAG, "数据刷新完成")
                } else {
                    sendIntent(HomeIntent.BooksLoadFailure(result.errorMessage ?: "刷新失败"))
                    sendEffect(HomeEffect.ShowToast("刷新失败"))
                    }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "刷新数据异常", e)
                sendIntent(HomeIntent.BooksLoadFailure(e.message ?: "刷新失败"))
                sendEffect(HomeEffect.ShowToast("刷新失败"))
            }
        }
    }
    
    /**
     * 选择分类筛选器
     */
    private fun selectCategoryFilter(categoryName: String) {
        val currentState = getCurrentState()
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "选择分类筛选器: $categoryName")
                
                val result = homeCompositeUseCase(
                    HomeCompositeUseCase.Params(
                        categoryFilter = categoryName,
                        categoryFilters = currentState.categoryFilters,
                        currentPage = 1
                    )
                )
                
                if (result.isSuccess) {
                    if (categoryName == "推荐") {
                        sendIntent(HomeIntent.HomeRecommendBooksLoadSuccess(
                            books = result.homeRecommendBooks,
                            isRefresh = true,
                            hasMore = result.hasMoreRecommend
                        ))
                } else {
                        sendIntent(HomeIntent.CategoryRecommendBooksLoadSuccess(
                            books = result.recommendBooks,
                            isLoadMore = false,
                            hasMore = result.hasMoreRecommend,
                            totalPages = result.totalPages
                        ))
                        }
                } else {
                    sendIntent(HomeIntent.CategoryRecommendBooksLoadFailure(result.errorMessage ?: "加载分类数据失败"))
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "选择分类筛选器异常", e)
                sendIntent(HomeIntent.CategoryRecommendBooksLoadFailure(e.message ?: "未知错误"))
            }
        }
    }
    
    /**
     * 选择榜单类型
     */
    private fun selectRankType(rankType: String) {
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "选择榜单类型: $rankType")
                
                val result = homeCompositeUseCase(
                    HomeCompositeUseCase.Params(rankType = rankType)
                    )
                    
                if (result.isSuccess) {
                    sendIntent(HomeIntent.RankBooksLoadSuccess(result.rankBooks))
                } else {
                    sendIntent(HomeIntent.RankBooksLoadFailure(result.errorMessage ?: "加载榜单数据失败"))
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "选择榜单类型异常", e)
                sendIntent(HomeIntent.RankBooksLoadFailure(e.message ?: "未知错误"))
            }
        }
    }
    
    /**
     * 加载更多推荐内容
     */
    private fun loadMoreRecommend() {
        val currentState = getCurrentState()
        
        // 检查是否已经在加载或没有更多数据
//        if (currentState.recommendLoading) {
//            TimberLogger.d(TAG, "加载更多推荐内容已在进行中，跳过重复请求")
//            return
//        }
        
        if (!currentState.hasMoreRecommend) {
            TimberLogger.d(TAG, "没有更多推荐内容可加载")
            return
        }
        
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "开始加载更多推荐内容 - 当前页: ${currentState.recommendPage}")
                
                val nextPage = currentState.recommendPage + 1
                val categoryId = getCurrentCategoryId(currentState.selectedCategoryFilter)
                
                TimberLogger.d(TAG, "请求参数 - categoryId: $categoryId, pageNum: $nextPage, pageSize: $RECOMMEND_PAGE_SIZE")
                
                val result = getCategoryRecommendBooksUseCase(
                    GetCategoryRecommendBooksUseCase.Params(
                    categoryId = categoryId,
                    pageNum = nextPage,
                    pageSize = RECOMMEND_PAGE_SIZE,
                        strategy = CacheStrategy.CACHE_FIRST
                    )
                )
                
                TimberLogger.d(TAG, "加载更多推荐内容成功 - 页码: $nextPage, 获取数量: ${result.list.size}, 总页数: ${result.pages}")
                
                sendIntent(HomeIntent.CategoryRecommendBooksLoadSuccess(
                    books = result.list,
                    isLoadMore = true,
                    hasMore = result.list.size >= RECOMMEND_PAGE_SIZE,
                    totalPages = result.pages.toInt()
                ))
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载更多推荐内容异常", e)
                sendIntent(HomeIntent.CategoryRecommendBooksLoadFailure(e.message ?: "加载更多失败"))
            }
        }
    }
    
    /**
     * 加载更多首页推荐内容
     */
    private fun loadMoreHomeRecommend() {
        val currentState = getCurrentState()
        
        // 检查是否已经在加载或没有更多数据
//        if (currentState.homeRecommendLoading) {
//            TimberLogger.d(TAG, "加载更多首页推荐内容已在进行中，跳过重复请求")
//            return
//        }
        
        if (!currentState.hasMoreHomeRecommend) {
            TimberLogger.d(TAG, "没有更多首页推荐内容可加载")
            return
        }
        
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "加载更多首页推荐内容 - 当前页: ${currentState.homeRecommendPage}")
                
                // 如果缓存数据为空，先加载数据
                if (cachedHomeBooks.isEmpty()) {
                    val homeBooks = getHomeRecommendBooksUseCase(GetHomeRecommendBooksUseCase.Params())
                    cachedHomeBooks = homeBooks
                }
                
                // 基于缓存数据进行分页 - 计算下一页数据
                val nextPage = currentState.homeRecommendPage + 1
                val startIndex = (nextPage - 1) * RECOMMEND_PAGE_SIZE
                val endIndex = startIndex + RECOMMEND_PAGE_SIZE
                
                val moreBooks = cachedHomeBooks.drop(startIndex).take(RECOMMEND_PAGE_SIZE)
                val hasMore = endIndex < cachedHomeBooks.size
                
                TimberLogger.d(TAG, "加载更多首页推荐内容计算 - 下一页: $nextPage, startIndex: $startIndex, endIndex: $endIndex, 缓存总数: ${cachedHomeBooks.size}")
                TimberLogger.d(TAG, "加载更多首页推荐内容成功 - 页码: $nextPage, 获取数量: ${moreBooks.size}, 剩余: $hasMore")
                
                sendIntent(HomeIntent.HomeRecommendBooksLoadSuccess(
                    books = moreBooks,
                    isRefresh = false,
                    hasMore = hasMore
                ))
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载更多首页推荐内容异常", e)
                sendIntent(HomeIntent.HomeRecommendBooksLoadFailure(e.message ?: "加载更多失败"))
            }
        }
    }
    
    /**
     * 恢复数据（页面重新获得焦点时）
     */
    private fun restoreDataIfNeeded() {
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "检查并恢复数据")
                
                val statusResult = homeStatusCheckUseCase(
                    HomeStatusCheckUseCase.Params()
                )
                
                val currentState = getCurrentState()
                
                // 检查并恢复榜单数据
                if (currentState.rankBooks.isEmpty()) {
                    selectRankType(currentState.selectedRankType)
    }
    
                // 检查并恢复分类数据
                if (currentState.categoryFilters.size <= 1) {
                    loadCategoryFilters()
                }
                
                // 检查并恢复推荐数据
                if (currentState.isRecommendMode && currentState.homeRecommendBooks.isEmpty()) {
                    loadHomeRecommendBooks()
                    }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "恢复数据异常", e)
                }
        }
    }
    
    /**
     * 加载分类筛选器数据
     */
    private fun loadCategoryFilters() {
        viewModelScope.launch {
            try {
                getHomeCategoriesUseCase(GetHomeCategoriesUseCase.Params())
                    .collect { filters ->
                        sendIntent(HomeIntent.CategoryFiltersLoadSuccess(filters))
                    }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载分类筛选器异常", e)
                sendIntent(HomeIntent.CategoryFiltersLoadFailure(e.message ?: "加载分类失败"))
            }
        }
    }
    
    /**
     * 加载首页推荐书籍
     */
    private fun loadHomeRecommendBooks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (isRefresh || cachedHomeBooks.isEmpty()) {
                    val strategy = if (isRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
                    val homeBooks = getHomeRecommendBooksUseCase(GetHomeRecommendBooksUseCase.Params(strategy))
                    cachedHomeBooks = homeBooks
                }
                
                // 基于缓存数据进行分页
                val currentPage = if (isRefresh) 1 else getCurrentState().homeRecommendPage
                val startIndex = (currentPage - 1) * RECOMMEND_PAGE_SIZE
                val endIndex = startIndex + RECOMMEND_PAGE_SIZE
                
                val currentBooks = if (isRefresh) {
                    cachedHomeBooks.take(RECOMMEND_PAGE_SIZE)
                } else {
                    cachedHomeBooks.take(endIndex)
                }
                
                val hasMore = endIndex < cachedHomeBooks.size
                
                sendIntent(HomeIntent.HomeRecommendBooksLoadSuccess(
                    books = currentBooks,
                    isRefresh = isRefresh,
                    hasMore = hasMore
                ))
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载首页推荐书籍异常", e)
                sendIntent(HomeIntent.HomeRecommendBooksLoadFailure(e.message ?: "加载推荐书籍失败"))
            }
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 获取当前分类ID
     */
    private fun getCurrentCategoryId(categoryName: String): Int {
        val currentState = getCurrentState()
        
        // 打印调试信息
        TimberLogger.d(TAG, "获取分类ID - 分类名称: $categoryName")
        TimberLogger.d(TAG, "可用分类筛选器: ${currentState.categoryFilters.map { "${it.name}(${it.id})" }}")
        
        // 特殊处理推荐模式
        if (categoryName == "推荐") {
            TimberLogger.d(TAG, "推荐模式，返回categoryId: 0")
            return 0
        }
        
        // 查找对应的分类ID
        val categoryInfo = currentState.categoryFilters.find { it.name == categoryName }
        val categoryId = categoryInfo?.id?.toIntOrNull() ?: run {
            // 如果找不到，尝试用分类名称创建一个映射
            val mappedId = when (categoryName) {
                "玄幻奇幻" -> 1
                "武侠仙侠" -> 2
                "都市言情" -> 3
                "历史军情" -> 4
                "科幻灵异" -> 5
                "网游竞技" -> 6
                else -> 1 // 默认返回1
            }
            TimberLogger.w(TAG, "未找到分类 '$categoryName' 的ID，使用映射值: $mappedId")
            mappedId
        }
        
        TimberLogger.d(TAG, "分类 '$categoryName' 对应ID: $categoryId")
        return categoryId
    }
} 