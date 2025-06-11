package com.novel.page.search.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.page.search.component.SearchRankingItem
import com.novel.page.search.repository.SearchRepository
import com.novel.page.search.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

/**
 * 搜索页面UI状态
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 搜索相关
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val isHistoryExpanded: Boolean = false,
    
    // 推荐榜单
    val novelRanking: List<SearchRankingItem> = emptyList(),
    val dramaRanking: List<SearchRankingItem> = emptyList(),
    val newBookRanking: List<SearchRankingItem> = emptyList(),

    val rankingLoading: Boolean = false
)

/**
 * 搜索页面用户操作
 */
sealed class SearchAction {
    data object LoadInitialData : SearchAction()
    data class UpdateSearchQuery(val query: String) : SearchAction()
    data class PerformSearch(val query: String) : SearchAction()
    data object ToggleHistoryExpansion : SearchAction()
    data class NavigateToBookDetail(val bookId: Long) : SearchAction()
    data object NavigateBack : SearchAction()
    data object ClearError : SearchAction()
}

/**
 * 搜索页面一次性事件
 */
sealed class SearchEvent {
    data class NavigateToBookDetail(val bookId: Long) : SearchEvent()
    data object NavigateBack : SearchEvent()
    data class ShowToast(val message: String) : SearchEvent()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val toggleHistoryExpansionUseCase: ToggleHistoryExpansionUseCase,
    private val getRankingListUseCase: GetRankingListUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
    }
    
    // UI状态
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // 事件通道
    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()
    
    // 当前状态
    val currentState: SearchUiState get() = _uiState.value
    
    /**
     * 处理用户操作
     */
    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.LoadInitialData -> loadInitialData()
            is SearchAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is SearchAction.PerformSearch -> performSearch(action.query)
            is SearchAction.ToggleHistoryExpansion -> toggleHistoryExpansion()
            is SearchAction.NavigateToBookDetail -> navigateToBookDetail(action.bookId)
            is SearchAction.NavigateBack -> navigateBack()
            is SearchAction.ClearError -> clearError()
        }
    }
    
    /**
     * 更新状态
     */
    private fun updateState(update: (SearchUiState) -> SearchUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    /**
     * 发送事件
     */
    private fun sendEvent(event: SearchEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        Log.d(TAG, "加载初始数据")
        updateState { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // 并行加载数据
                val historyDeferred = getSearchHistoryUseCase()
                val rankingDeferred = getRankingListUseCase()
                
                updateState { currentState ->
                    currentState.copy(
                        isLoading = false,
                        searchHistory = historyDeferred,
                        novelRanking = rankingDeferred.novelRanking,
                        dramaRanking = rankingDeferred.dramaRanking,
                        newBookRanking = rankingDeferred.newBookRanking
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载初始数据失败", e)
                updateState { 
                    it.copy(
                        isLoading = false, 
                        error = "加载数据失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 更新搜索查询
     */
    private fun updateSearchQuery(query: String) {
        Log.d(TAG, "更新搜索查询: $query")
        updateState { it.copy(searchQuery = query) }
    }
    
    /**
     * 执行搜索
     */
    private fun performSearch(query: String) {
        Log.d(TAG, "执行搜索: $query")
        
        if (query.isBlank()) {
            sendEvent(SearchEvent.ShowToast("请输入搜索关键词"))
            return
        }
        
        viewModelScope.launch {
            try {
                // 添加到搜索历史
                addSearchHistoryUseCase(query)
                
                // 更新搜索历史
                val updatedHistory = getSearchHistoryUseCase()
                updateState { it.copy(searchHistory = updatedHistory) }
                
                // TODO: 导航到搜索结果页面
                sendEvent(SearchEvent.ShowToast("搜索功能开发中"))
                
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败", e)
                sendEvent(SearchEvent.ShowToast("搜索失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 切换历史记录展开状态
     */
    private fun toggleHistoryExpansion() {
        Log.d(TAG, "切换历史记录展开状态")
        viewModelScope.launch {
            try {
                val newState = toggleHistoryExpansionUseCase(currentState.isHistoryExpanded)
                updateState { it.copy(isHistoryExpanded = newState) }
            } catch (e: Exception) {
                Log.e(TAG, "切换历史记录展开状态失败", e)
                sendEvent(SearchEvent.ShowToast("操作失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 导航到书籍详情
     */
    private fun navigateToBookDetail(bookId: Long) {
        Log.d(TAG, "导航到书籍详情: $bookId")
        sendEvent(SearchEvent.NavigateToBookDetail(bookId))
    }
    
    /**
     * 返回上一页
     */
    private fun navigateBack() {
        Log.d(TAG, "返回上一页")
        sendEvent(SearchEvent.NavigateBack)
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        Log.d(TAG, "清除错误")
        updateState { it.copy(error = null) }
    }
} 