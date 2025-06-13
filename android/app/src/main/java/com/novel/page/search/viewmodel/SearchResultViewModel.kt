package com.novel.page.search.viewmodel

import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.component.StateHolderImpl
import com.novel.page.search.usecase.SearchBooksUseCase
import com.novel.page.search.usecase.GetCategoryFiltersUseCase
import com.novel.utils.network.api.front.BookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchBooksUseCase: SearchBooksUseCase,
    private val getCategoryFiltersUseCase: GetCategoryFiltersUseCase,
    private val bookService: BookService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        StateHolderImpl(
            data = SearchResultUiState(),
            isLoading = false,
            error = null
        )
    )
    val uiState: StateFlow<StateHolderImpl<SearchResultUiState>> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchResultEvent>()
    val events: SharedFlow<SearchResultEvent> = _events.asSharedFlow()

    private var currentPage = 1
    private var isLoadingMore = false

    init {
        loadCategoryFilters()
    }

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

    private fun updateQuery(query: String) {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(query = query)
        )
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
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
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(
                                query = query,
                                books = response.list,
                                totalResults = response.total?.toInt() ?: 0,
                                hasMore = response.list.size >= 20,
                                isEmpty = response.list.isEmpty()
                            ),
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "搜索失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }

    private fun selectCategory(categoryId: Int?) {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(selectedCategoryId = categoryId)
        )
        
        // 重新搜索
        if (currentData.query.isNotEmpty()) {
            performSearch(currentData.query)
        }
    }

    private fun openFilterSheet() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(isFilterSheetOpen = true)
        )
    }

    private fun closeFilterSheet() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(isFilterSheetOpen = false)
        )
    }

    private fun updateFilters(filters: FilterState) {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(filters = filters)
        )
    }

    private fun applyFilters() {
        closeFilterSheet()
        val currentData = _uiState.value.data
        if (currentData.query.isNotEmpty()) {
            performSearch(currentData.query)
        }
    }

    private fun clearFilters() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(filters = FilterState())
        )
    }

    private fun loadNextPage() {
        if (isLoadingMore || !_uiState.value.data.hasMore) return
        
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
                        val newBooks = response.list
                        _uiState.value = _uiState.value.copy(
                            data = currentData.copy(
                                books = currentData.books + newBooks,
                                hasMore = newBooks.size >= 20
                            )
                        )
                    },
                    onFailure = { 
                        currentPage-- // 恢复页码
                    }
                )
            } catch (e: Exception) {
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