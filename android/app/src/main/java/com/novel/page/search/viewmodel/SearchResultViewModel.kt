package com.novel.page.search.viewmodel

import com.novel.utils.TimberLogger
import androidx.lifecycle.viewModelScope
import com.novel.core.mvi.BaseMviViewModel
import com.novel.core.mvi.MviReducer
import com.novel.page.search.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索结果页面ViewModel - MVI架构重构版
 * 
 * 根据优化方案阶段2第13天任务要求，重构为统一MVI架构：
 * - 继承BaseMviViewModel<SearchResultIntent, SearchResultState, SearchResultEffect>
 * - 移除原有的BaseViewModel继承和手写状态管理
 * - 将原有方法转换为Intent处理函数
 * - 保持UI和业务逻辑完全不变，所有功能完整实现无遗漏
 * 
 * 主要职责：
 * - 管理搜索结果的获取和展示
 * - 处理分页加载逻辑
 * - 管理筛选条件和分类选择
 * - 协调搜索相关的用户交互
 */
@HiltViewModel
class SearchResultViewModel @Inject constructor(
    /** 搜索书籍用例 */
    private val searchBooksUseCase: SearchBooksUseCase,
    /** 获取分类筛选器用例 */
    private val getCategoryFiltersUseCase: GetCategoryFiltersUseCase
) : BaseMviViewModel<SearchResultIntent, SearchResultState, SearchResultEffect>() {
    
    companion object {
        private const val TAG = "SearchResultViewModel"
    }
    
    /** Reducer实例，处理状态转换逻辑 */
    private val reducer = SearchResultReducer()
    
    /** UseCase相关业务逻辑 */
    private val searchUseCases = SearchResultUseCases(
        searchBooksUseCase = searchBooksUseCase,
        getCategoryFiltersUseCase = getCategoryFiltersUseCase
    )
    
    /** 当前页码（从1开始） */
    private var currentPage = 1
    /** 分页加载状态标识 */
    private var isLoadingMore = false

    init {
        TimberLogger.d(TAG, "SearchResultViewModel MVI重构版初始化")
        // 立即加载分类筛选器
        loadCategoryFilters()
    }
    
    /**
     * 创建初始状态
     * 基础MVI框架要求实现此方法
     */
    override fun createInitialState(): SearchResultState {
        return SearchResultState()
    }
    
    /**
     * 获取Reducer实例
     * 基础MVI框架要求实现此方法
     */
    override fun getReducer(): MviReducer<SearchResultIntent, SearchResultState> {
        return reducer
    }
    
    /**
     * Intent处理完成后的回调
     * 处理需要异步操作的Intent
     */
    override fun onIntentProcessed(intent: SearchResultIntent, newState: SearchResultState) {
        // 获取副作用并处理
        val result = reducer.reduceWithEffect(getCurrentState(), intent)
        result.effect?.let { effect ->
            sendEffect(effect)
        }
        
        when (intent) {
            is SearchResultIntent.PerformSearch -> {
                handlePerformSearch(intent.query)
            }
            is SearchResultIntent.SelectCategory -> {
                handleCategorySelection()
            }
            is SearchResultIntent.ApplyFilters -> {
                handleApplyFilters()
            }
            is SearchResultIntent.LoadNextPage -> {
                handleLoadNextPage()
            }
            else -> {
                // 其他Intent在Reducer中已经完全处理，无需额外操作
            }
        }
    }
    
    /**
     * 提供给UI层的便捷方法
     * 兼容原有的onAction调用方式
     */
    fun onAction(action: SearchResultAction) {
        val intent = when (action) {
            is SearchResultAction.UpdateQuery -> SearchResultIntent.UpdateQuery(action.query)
            is SearchResultAction.PerformSearch -> SearchResultIntent.PerformSearch(action.query)
            is SearchResultAction.SelectCategory -> SearchResultIntent.SelectCategory(action.categoryId)
            is SearchResultAction.OpenFilterSheet -> SearchResultIntent.OpenFilterSheet
            is SearchResultAction.CloseFilterSheet -> SearchResultIntent.CloseFilterSheet
            is SearchResultAction.UpdateFilters -> SearchResultIntent.UpdateFilters(action.filters)
            is SearchResultAction.ApplyFilters -> SearchResultIntent.ApplyFilters
            is SearchResultAction.ClearFilters -> SearchResultIntent.ClearFilters
            is SearchResultAction.LoadNextPage -> SearchResultIntent.LoadNextPage
            is SearchResultAction.NavigateToDetail -> SearchResultIntent.NavigateToDetail(action.bookId)
            is SearchResultAction.NavigateBack -> SearchResultIntent.NavigateBack
        }
        sendIntent(intent)
    }
    
    /**
     * 暴露state和effect给UI层
     * 保持与原有API的兼容性
     */
    val uiState = state
    val events = effect
    
    // region 私有业务逻辑处理方法
    
    /**
     * 处理搜索操作
     * 重置分页状态并执行新搜索
     */
    private fun handlePerformSearch(query: String) {
        TimberLogger.d(TAG, "执行搜索: $query")
        
        if (query.isBlank()) {
            sendEffect(SearchResultEffect.ShowToast("请输入搜索关键词"))
            return
        }
        
        // 重置分页状态
        currentPage = 1
        isLoadingMore = false
        
        performSearch(query, isLoadMore = false)
    }
    
    /**
     * 处理分类选择
     * 选择分类后重新搜索
     */
    private fun handleCategorySelection() {
        val currentState = getCurrentState()
        if (currentState.query.isNotBlank()) {
            // 重置分页状态
            currentPage = 1
            isLoadingMore = false
            
            performSearch(currentState.query, isLoadMore = false)
        }
    }
    
    /**
     * 处理筛选条件应用
     * 应用筛选条件后重新搜索
     */
    private fun handleApplyFilters() {
        val currentState = getCurrentState()
        if (currentState.query.isNotBlank()) {
            // 重置分页状态
            currentPage = 1
            isLoadingMore = false
            
            performSearch(currentState.query, isLoadMore = false)
        }
    }
    
    /**
     * 处理分页加载
     * 加载下一页搜索结果
     */
    private fun handleLoadNextPage() {
        if (isLoadingMore) {
            TimberLogger.d(TAG, "正在加载中，跳过重复请求")
            return
        }
        
        val currentState = getCurrentState()
        if (!currentState.hasMore) {
            TimberLogger.d(TAG, "没有更多数据，跳过加载")
            return
        }
        
        if (currentState.query.isBlank()) {
            TimberLogger.d(TAG, "查询为空，跳过分页加载")
            return
        }
        
        isLoadingMore = true
        currentPage++
        
        performSearch(currentState.query, isLoadMore = true)
    }
    
    /**
     * 执行搜索
     * 统一的搜索逻辑，支持新搜索和分页加载
     */
    private fun performSearch(query: String, isLoadMore: Boolean) {
        TimberLogger.d(TAG, "执行搜索 - 查询: $query, 页码: $currentPage, 分页: $isLoadMore")
        
        viewModelScope.launch {
            try {
                val currentState = getCurrentState()
                val result = searchUseCases.searchBooks(
                    query = query,
                    page = currentPage,
                    categoryId = currentState.selectedCategoryId,
                    filters = currentState.filters
                )
                
                result.fold(
                    onSuccess = { response ->
                        TimberLogger.d(TAG, "搜索成功 - 结果数量: ${response.list.size}, 总数: ${response.total}")
                        
                        val newState = reducer.handleSearchSuccess(
                            currentState = getCurrentState(),
                            books = response.list,
                            totalResults = response.total?.toInt() ?: 0,
                            hasMore = response.list.size >= 20, // 假设每页20条
                            isLoadMore = isLoadMore
                        )
                        updateState(newState)
                        
                        if (isLoadMore) {
                            isLoadingMore = false
                        }
                        
                    },
                    onFailure = { exception ->
                        TimberLogger.e(TAG, "搜索失败", exception)
                        
                        val newState = reducer.handleSearchError(
                            currentState = getCurrentState(),
                            errorMessage = "搜索失败: ${exception.message}",
                            isLoadMore = isLoadMore
                        )
                        updateState(newState)
                        
                        if (isLoadMore) {
                            currentPage-- // 恢复页码
                            isLoadingMore = false
                        }
                        
                        sendEffect(SearchResultEffect.ShowToast("搜索失败: ${exception.message}"))
                    }
                )
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "搜索异常", e)
                
                val newState = reducer.handleSearchError(
                    currentState = getCurrentState(),
                    errorMessage = "搜索异常: ${e.message}",
                    isLoadMore = isLoadMore
                )
                updateState(newState)
                
                if (isLoadMore) {
                    currentPage-- // 恢复页码
                    isLoadingMore = false
                }
                
                sendEffect(SearchResultEffect.ShowToast("搜索异常: ${e.message}"))
            }
        }
    }
    
    /**
     * 加载分类筛选器
     * 在ViewModel初始化时调用
     */
    private fun loadCategoryFilters() {
        viewModelScope.launch {
            try {
                val result = searchUseCases.getCategoryFilters()
                result.fold(
                    onSuccess = { filters ->
                        val newState = reducer.handleCategoryFiltersLoaded(
                            currentState = getCurrentState(),
                            categoryFilters = filters
                        )
                        updateState(newState)
                    },
                    onFailure = {
                        // 使用默认分类
                        val defaultCategories = listOf(
                            CategoryFilter(id = -1, name = "所有"),
                            CategoryFilter(id = 1, name = "武侠玄幻"),
                            CategoryFilter(id = 2, name = "都市言情"),
                            CategoryFilter(id = 3, name = "历史军事"),
                            CategoryFilter(id = 4, name = "游戏竞技"),
                            CategoryFilter(id = 5, name = "科幻灵异"),
                            CategoryFilter(id = 6, name = "其他")
                        )
                        val newState = reducer.handleCategoryFiltersLoaded(
                            currentState = getCurrentState(),
                            categoryFilters = defaultCategories
                        )
                        updateState(newState)
                    }
                )
            } catch (e: Exception) {
                // 忽略分类加载错误，使用默认值
                TimberLogger.e(TAG, "加载分类筛选器失败", e)
            }
        }
    }
    
    // endregion
}

// region 业务逻辑组合类

/**
 * 搜索结果相关UseCase组合
 * 封装所有搜索结果相关的业务逻辑，简化ViewModel
 */
private class SearchResultUseCases(
    private val searchBooksUseCase: SearchBooksUseCase,
    private val getCategoryFiltersUseCase: GetCategoryFiltersUseCase
) {
    
    suspend fun searchBooks(
        query: String,
        page: Int,
        categoryId: Int?,
        filters: FilterState
    ): Result<com.novel.page.search.repository.PageRespDtoBookInfoRespDto> {
        return searchBooksUseCase.execute(
            keyword = query,
            pageNum = page,
            categoryId = categoryId,
            filters = filters,
            pageSize = 20
        )
    }
    
    suspend fun getCategoryFilters(): Result<List<CategoryFilter>> {
        return getCategoryFiltersUseCase.execute()
    }
}

// endregion

// region 原有数据类保持不变

/**
 * 搜索结果页UI状态
 */
data class SearchResultUiState(
    val query: String = "",
    val books: List<BookInfoRespDto> = emptyList(),                     
    val totalResults: Int = 0,
    val hasMore: Boolean = false,
    val isEmpty: Boolean = false,
    val selectedCategoryId: Int? = null,
    val categoryFilters: List<CategoryFilter> = emptyList(),
    val filters: FilterState = FilterState(),
    val isFilterSheetOpen: Boolean = false
)

/**
 * 筛选状态
 */
data class FilterState(
    val updateStatus: UpdateStatus = UpdateStatus.ALL,
    val isVip: VipStatus = VipStatus.ALL,
    val wordCountRange: WordCountRange = WordCountRange.ALL,
    val sortBy: SortBy = SortBy.NULL
)

/**
 * 更新状态
 */
enum class UpdateStatus(val value: Int?, val displayName: String) {
    ALL(null, "全部"),
    FINISHED(1, "已完结"),
    ONGOING(0, "连载中"),
    HALF_YEAR_FINISHED(-1, "半年内完结"),
    THREE_DAYS_UPDATED(-2, "3日内更新"),
    SEVEN_DAYS_UPDATED(-3, "7日内更新"),
    ONE_MONTH_UPDATED(-4, "1月内更新")
}

/**
 * VIP状态
 */
enum class VipStatus(val value: Int?, val displayName: String) {
    ALL(null, "全部"),
    FREE(0, "免费"),
    PAID(1, "付费")
}

/**
 * 字数范围
 */
enum class WordCountRange(val min: Int?, val max: Int?, val displayName: String) {
    ALL(null, null, "全部"),
    UNDER_10W(null, 100000, "10万字以内"),
    W_10_30(100000, 300000, "10-30万"),
    W_30_50(300000, 500000, "30-50万"),
    W_50_100(500000, 1000000, "50-100万"),
    W_100_200(1000000, 2000000, "100-200万"),
    W_200_300(2000000, 3000000, "200-300万"),
    OVER_300W(3000000, null, "300万以上")
}

/**
 * 排序方式
 */
enum class SortBy(val value: String, val displayName: String) {
    NULL("null","默认排序"),
    NEW_UPDATE("last_chapter_update_time desc", "最近更新"),
    HIGH_CLICK("visit_count desc", "点击量"),
    WORD_COUNT("word_count desc", "总字数")
}

/**
 * 分类筛选
 */
data class CategoryFilter(
    val id: Int,
    val name: String?
)

/**
 * 书籍信息响应DTO（简化版本，与API对应）
 */
data class BookInfoRespDto(
    val id: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val picUrl: String?,
    val bookName: String?,
    val authorId: Long?,
    val authorName: String?,
    val bookDesc: String?,
    val bookStatus: Int,
    val visitCount: Long,
    val wordCount: Int,
    val commentCount: Int,
    val firstChapterId: Long?,
    val lastChapterId: Long?,
    val lastChapterName: String?,
    val updateTime: String?
)

/**
 * 搜索结果动作
 */
sealed class SearchResultAction {
    data class UpdateQuery(val query: String) : SearchResultAction()
    data class PerformSearch(val query: String) : SearchResultAction()
    data class SelectCategory(val categoryId: Int?) : SearchResultAction()
    data object OpenFilterSheet : SearchResultAction()
    data object CloseFilterSheet : SearchResultAction()
    data class UpdateFilters(val filters: FilterState) : SearchResultAction()
    data object ApplyFilters : SearchResultAction()
    data object ClearFilters : SearchResultAction()
    data object LoadNextPage : SearchResultAction()
    data class NavigateToDetail(val bookId: String) : SearchResultAction()
    data object NavigateBack : SearchResultAction()
}

/**
 * 搜索结果事件
 */
sealed class SearchResultEvent {
    data class NavigateToDetail(val bookId: String) : SearchResultEvent()
    data object NavigateBack : SearchResultEvent()
}

// endregion 