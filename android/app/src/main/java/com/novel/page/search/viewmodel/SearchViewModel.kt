package com.novel.page.search.viewmodel

import com.novel.utils.TimberLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.page.search.component.SearchRankingItem
import com.novel.page.search.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

/**
 * 搜索页面UI状态
 * 
 * 包含搜索页面所有显示状态：
 * - 搜索查询和历史记录
 * - 各类榜单数据
 * - 加载和错误状态
 */
data class SearchUiState(
    /** 页面加载状态 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    
    // 搜索相关
    /** 当前搜索查询内容 */
    val searchQuery: String = "",
    /** 搜索历史记录列表 */
    val searchHistory: List<String> = emptyList(),
    /** 历史记录是否展开显示 */
    val isHistoryExpanded: Boolean = false,
    
    // 推荐榜单
    /** 小说榜单数据 */
    val novelRanking: List<SearchRankingItem> = emptyList(),
    /** 剧本榜单数据 */
    val dramaRanking: List<SearchRankingItem> = emptyList(),
    /** 新书榜单数据 */
    val newBookRanking: List<SearchRankingItem> = emptyList(),

    /** 榜单数据加载状态 */
    val rankingLoading: Boolean = false
)

/**
 * 搜索页面用户操作
 * 封装所有用户可能的交互行为
 */
sealed class SearchAction {
    /** 加载初始数据 */
    data object LoadInitialData : SearchAction()
    /** 更新搜索查询内容 */
    data class UpdateSearchQuery(val query: String) : SearchAction()
    /** 执行搜索操作 */
    data class PerformSearch(val query: String) : SearchAction()
    /** 切换历史记录展开状态 */
    data object ToggleHistoryExpansion : SearchAction()
    /** 导航到书籍详情页 */
    data class NavigateToBookDetail(val bookId: Long) : SearchAction()
    /** 返回上级页面 */
    data object NavigateBack : SearchAction()
    /** 清除错误状态 */
    data object ClearError : SearchAction()
}

/**
 * 搜索页面一次性事件
 * 用于触发导航和用户提示
 */
sealed class SearchEvent {
    /** 导航到书籍详情页 */
    data class NavigateToBookDetail(val bookId: Long) : SearchEvent()
    /** 导航到搜索结果页 */
    data class NavigateToSearchResult(val query: String) : SearchEvent()
    /** 返回上级页面 */
    data object NavigateBack : SearchEvent()
    /** 显示Toast提示 */
    data class ShowToast(val message: String) : SearchEvent()
}

/**
 * 搜索页面ViewModel
 * 
 * 主要职责：
 * - 管理搜索查询和历史记录
 * - 协调各类榜单数据的加载
 * - 处理搜索相关的用户交互
 * - 管理页面导航和事件通知
 * 
 * 采用UseCase模式分离业务逻辑，保持ViewModel职责单一
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    /** 获取搜索历史用例 */
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    /** 添加搜索历史用例 */
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    /** 切换历史展开状态用例 */
    private val toggleHistoryExpansionUseCase: ToggleHistoryExpansionUseCase,
    /** 获取榜单数据用例 */
    private val getRankingListUseCase: GetRankingListUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
    }
    
    /** UI状态流，响应式更新界面 */
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    /** 事件通道，处理一次性事件 */
    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()
    
    /** 当前状态快照，便于访问 */
    val currentState: SearchUiState get() = _uiState.value
    
    /**
     * 处理用户操作的统一入口
     * 根据操作类型分发到对应的处理方法
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
     * 更新UI状态的辅助方法
     * 确保状态更新的线程安全
     */
    private fun updateState(update: (SearchUiState) -> SearchUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    /**
     * 发送一次性事件
     * 使用协程确保事件发送不阻塞
     */
    private fun sendEvent(event: SearchEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
    
    /**
     * 加载页面初始数据
     * 并行加载搜索历史和榜单数据以提升性能
     */
    private fun loadInitialData() {
        TimberLogger.d(TAG, "开始加载初始数据")
        updateState { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // 并行加载数据以提升性能
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
                
                TimberLogger.d(TAG, "初始数据加载完成")
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载初始数据失败", e)
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
     * 更新搜索查询内容
     * 实时响应用户输入
     */
    private fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
    }
    
    /**
     * 执行搜索操作
     * 验证输入后添加历史记录并导航到结果页
     */
    private fun performSearch(query: String) {
        TimberLogger.d(TAG, "执行搜索: $query")
        
        if (query.isBlank()) {
            sendEvent(SearchEvent.ShowToast("请输入搜索关键词"))
            return
        }
        
        viewModelScope.launch {
            try {
                // 添加到搜索历史
                addSearchHistoryUseCase(query)
                
                // 更新搜索历史显示
                val updatedHistory = getSearchHistoryUseCase()
                updateState { it.copy(searchHistory = updatedHistory) }
                
                // 导航到搜索结果页面
                sendEvent(SearchEvent.NavigateToSearchResult(query.trim()))
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "搜索操作失败", e)
                sendEvent(SearchEvent.ShowToast("搜索失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 切换历史记录展开状态
     * 通过UseCase管理展开状态的持久化
     */
    private fun toggleHistoryExpansion() {
        viewModelScope.launch {
            try {
                val newState = toggleHistoryExpansionUseCase(currentState.isHistoryExpanded)
                updateState { it.copy(isHistoryExpanded = newState) }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "切换历史记录展开状态失败", e)
                sendEvent(SearchEvent.ShowToast("操作失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 导航到书籍详情
     */
    private fun navigateToBookDetail(bookId: Long) {
        TimberLogger.d(TAG, "导航到书籍详情: $bookId")
        sendEvent(SearchEvent.NavigateToBookDetail(bookId))
    }
    
    /**
     * 返回上一页
     */
    private fun navigateBack() {
        TimberLogger.d(TAG, "返回上一页")
        sendEvent(SearchEvent.NavigateBack)
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        TimberLogger.d(TAG, "清除错误")
        updateState { it.copy(error = null) }
    }
} 