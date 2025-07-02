package com.novel.page.search.viewmodel

import com.novel.utils.TimberLogger
import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.component.StateHolderImpl
import com.novel.page.search.usecase.SearchBooksUseCase
import com.novel.page.search.usecase.GetCategoryFiltersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索结果页面ViewModel
 * 
 * 主要职责：
 * - 管理搜索结果的加载和分页
 * - 处理搜索过滤条件和分类选择
 * - 管理筛选面板的显示状态
 * - 协调搜索相关的用户交互
 * 
 * 继承BaseViewModel获得统一的加载状态管理能力
 */
@HiltViewModel
class SearchResultViewModel @Inject constructor(
    /** 搜索书籍用例，处理搜索请求 */
    private val searchBooksUseCase: SearchBooksUseCase,
    /** 获取分类筛选项用例 */
    private val getCategoryFiltersUseCase: GetCategoryFiltersUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "SearchResultViewModel"
    }

    /** UI状态流，包装搜索结果数据和加载状态 */
    private val _uiState = MutableStateFlow(
        StateHolderImpl(
            data = SearchResultUiState(),
            isLoading = false,
            error = null
        )
    )
    val uiState: StateFlow<StateHolderImpl<SearchResultUiState>> = _uiState.asStateFlow()

    /** 事件流，处理一次性事件如导航和提示 */
    private val _events = MutableSharedFlow<SearchResultEvent>()
    val events: SharedFlow<SearchResultEvent> = _events.asSharedFlow()

    /** 当前页码，用于分页加载 */
    private var currentPage = 1
    /** 分页加载状态锁，防止重复请求 */
    private var isLoadingMore = false

    init {
        TimberLogger.d(TAG, "SearchResultViewModel初始化")
        // 预加载分类筛选配置
        loadCategoryFilters()
    }

    /**
     * 处理用户操作的统一入口
     * 根据操作类型分发到对应的处理方法
     */
    fun onAction(action: SearchResultAction) {
        when (action) {
            is SearchResultAction.UpdateQuery -> updateQuery(action.query)
            is SearchResultAction.PerformSearch -> performSearch(action.query)
            is SearchResultAction.SelectCategory -> selectCategory(action.categoryId)
            is SearchResultAction.OpenFilterSheet -> openFilterSheet()
            is SearchResultAction.CloseFilterSheet -> closeFilterSheet()
            is SearchResultAction.UpdateFilters -> updateFilters(action.filters)
            is SearchResultAction.ApplyFilters -> applyFilters()
            is SearchResultAction.ClearFilters -> clearFilters()
            is SearchResultAction.LoadNextPage -> loadNextPage()
            is SearchResultAction.NavigateToDetail -> navigateToDetail(action.bookId)
            is SearchResultAction.NavigateBack -> navigateBack()
        }
    }

    /**
     * 更新搜索查询内容
     * 实时更新UI状态，不触发搜索
     */
    private fun updateQuery(query: String) {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(query = query)
        )
    }

    /**
     * 执行搜索操作
     * 重置分页状态，发起新的搜索请求
     */
    private fun performSearch(query: String) {
        TimberLogger.d(TAG, "执行搜索: $query")
        viewModelScope.launch {
            try {
                // 重置分页状态
                currentPage = 1
                isLoadingMore = false
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentData = _uiState.value.data
                val result = searchBooksUseCase.execute(
                    keyword = query,
                    categoryId = currentData.selectedCategoryId,
                    filters = currentData.filters,
                    pageNum = currentPage,
                    pageSize = 20
                )
                
                result.fold(
                    onSuccess = { response ->
                        TimberLogger.d(TAG, "搜索成功，获得${response.list.size}条结果")
                        val loadedFilters = _uiState.value.data.categoryFilters
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(
                                query = query,
                                books = response.list,
                                totalResults = response.total?.toInt() ?: 0,
                                hasMore = response.list.size >= 20,
                                isEmpty = response.list.isEmpty(),
                                categoryFilters = loadedFilters
                            ),
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        TimberLogger.e(TAG, "搜索失败", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "搜索失败"
                        )
                    }
                )
            } catch (e: Exception) {
                TimberLogger.e(TAG, "搜索异常", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }

    /**
     * 选择搜索分类
     * 更新分类状态并重新搜索
     */
    private fun selectCategory(categoryId: Int?) {
        TimberLogger.d(TAG, "选择分类: $categoryId")
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(selectedCategoryId = categoryId)
        )
        
        // 如果有搜索内容则重新搜索
        if (currentData.query.isNotEmpty()) {
            performSearch(currentData.query)
        }
    }

    /**
     * 打开筛选面板
     * 更新面板显示状态
     */
    private fun openFilterSheet() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(isFilterSheetOpen = true)
        )
    }

    /**
     * 关闭筛选面板
     * 隐藏面板但不重置筛选条件
     */
    private fun closeFilterSheet() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(isFilterSheetOpen = false)
        )
    }

    /**
     * 更新筛选条件
     * 实时更新筛选状态，不触发搜索
     */
    private fun updateFilters(filters: FilterState) {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(filters = filters)
        )
    }

    /**
     * 应用筛选条件
     * 关闭面板并重新搜索
     */
    private fun applyFilters() {
        TimberLogger.d(TAG, "应用筛选条件")
        closeFilterSheet()
        val currentData = _uiState.value.data
        if (currentData.query.isNotEmpty()) {
            performSearch(currentData.query)
        }
    }

    /**
     * 清除所有筛选条件
     * 重置为默认筛选状态
     */
    private fun clearFilters() {
        TimberLogger.d(TAG, "清除筛选条件")
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(filters = FilterState())
        )
    }

    /**
     * 加载下一页数据
     * 支持分页加载，防止重复请求
     */
    private fun loadNextPage() {
        if (isLoadingMore || !_uiState.value.data.hasMore) {
            TimberLogger.d(TAG, "跳过分页加载：isLoadingMore=$isLoadingMore, hasMore=${_uiState.value.data.hasMore}")
            return
        }
        
        TimberLogger.d(TAG, "加载第${currentPage + 1}页数据")
        viewModelScope.launch {
            try {
                isLoadingMore = true
                currentPage++
                
                val currentData = _uiState.value.data
                val result = searchBooksUseCase.execute(
                    keyword = currentData.query,
                    categoryId = currentData.selectedCategoryId,
                    filters = currentData.filters,
                    pageNum = currentPage,
                    pageSize = 20
                )
                
                result.fold(
                    onSuccess = { response ->
                        TimberLogger.d(TAG, "分页加载成功，新增${response.list.size}条结果")
                        val loadedFilters = _uiState.value.data.categoryFilters
                        val newBooks = response.list
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(
                                books = currentData.books + newBooks,
                                hasMore = newBooks.size >= 20,
                                categoryFilters = loadedFilters
                            )
                        )
                    },
                    onFailure = { exception ->
                        TimberLogger.e(TAG, "分页加载失败", exception)
                        currentPage-- // 恢复页码
                    }
                )
            } catch (e: Exception) {
                TimberLogger.e(TAG, "分页加载异常", e)
                currentPage-- // 恢复页码
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun navigateToDetail(bookId: String) {
        viewModelScope.launch {
            _events.emit(SearchResultEvent.NavigateToDetail(bookId))
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.emit(SearchResultEvent.NavigateBack)
        }
    }

    private fun loadCategoryFilters() {
        viewModelScope.launch {
            try {
                val result = getCategoryFiltersUseCase.execute()
                result.fold(
                    onSuccess = { filters ->
                        val currentData = _uiState.value.data
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(categoryFilters = filters)
                        )
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
                        val currentData = _uiState.value.data
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(categoryFilters = defaultCategories)
                        )
                    }
                )
            } catch (e: Exception) {
                // 忽略分类加载错误，使用默认值
            }
        }
    }
}

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