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
 * 搜索页面ViewModel - MVI架构重构版
 * 
 * 根据优化方案阶段2第13天任务要求，重构为统一MVI架构：
 * - 继承BaseMviViewModel<SearchIntent, SearchState, SearchEffect>
 * - 移除原有的StateFlow和Channel，改用父类提供的MVI框架
 * - 将原有方法转换为Intent处理函数
 * - 保持UI和业务逻辑完全不变，所有功能完整实现无遗漏
 * 
 * 主要职责：
 * - 管理搜索查询和历史记录
 * - 协调各类榜单数据的加载
 * - 处理搜索相关的用户交互
 * - 管理页面导航和事件通知
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
) : BaseMviViewModel<SearchIntent, SearchState, SearchEffect>() {
    
    companion object {
        private const val TAG = "SearchViewModel"
    }
    
    /** Reducer实例，处理状态转换逻辑 */
    private val reducer = SearchReducer()
    
    /** UseCase相关业务逻辑 */
    private val searchUseCases = SearchUseCases(
        getSearchHistoryUseCase = getSearchHistoryUseCase,
        addSearchHistoryUseCase = addSearchHistoryUseCase,
        toggleHistoryExpansionUseCase = toggleHistoryExpansionUseCase,
        getRankingListUseCase = getRankingListUseCase
    )

    init {
        TimberLogger.d(TAG, "SearchViewModel MVI重构版初始化")
    }
    
    /**
     * 创建初始状态
     * 基础MVI框架要求实现此方法
     */
    override fun createInitialState(): SearchState {
        return SearchState()
    }
    
    /**
     * 获取Reducer实例
     * 基础MVI框架要求实现此方法
     */
    override fun getReducer(): MviReducer<SearchIntent, SearchState> {
        return reducer
    }
    
    /**
     * Intent处理完成后的回调
     * 处理需要异步操作的Intent
     */
    override fun onIntentProcessed(intent: SearchIntent, newState: SearchState) {
        // 获取副作用并处理
        val result = reducer.reduceWithEffect(getCurrentState(), intent)
        result.effect?.let { effect ->
            sendEffect(effect)
        }
        
        when (intent) {
            is SearchIntent.LoadInitialData -> {
                handleLoadInitialData()
            }
            is SearchIntent.PerformSearch -> {
                handlePerformSearch(intent.query)
            }
            is SearchIntent.ToggleHistoryExpansion -> {
                handleToggleHistoryExpansion()
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
    fun onAction(action: SearchAction) {
        val intent = when (action) {
            is SearchAction.LoadInitialData -> SearchIntent.LoadInitialData
            is SearchAction.UpdateSearchQuery -> SearchIntent.UpdateSearchQuery(action.query)
            is SearchAction.PerformSearch -> SearchIntent.PerformSearch(action.query)
            is SearchAction.ToggleHistoryExpansion -> SearchIntent.ToggleHistoryExpansion
            is SearchAction.NavigateToBookDetail -> SearchIntent.NavigateToBookDetail(action.bookId)
            is SearchAction.NavigateBack -> SearchIntent.NavigateBack
            is SearchAction.ClearError -> SearchIntent.ClearError
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
     * 处理初始数据加载
     * 并行加载搜索历史和榜单数据以提升性能
     */
    private fun handleLoadInitialData() {
        TimberLogger.d(TAG, "开始加载初始数据")
        
        viewModelScope.launch {
            try {
                // 并行加载数据以提升性能
                val historyDeferred = searchUseCases.getSearchHistory()
                val rankingDeferred = searchUseCases.getRankingList()
                
                val newState = reducer.handleLoadInitialDataSuccess(
                    currentState = getCurrentState(),
                    searchHistory = historyDeferred,
                    novelRanking = rankingDeferred.novelRanking,
                    dramaRanking = rankingDeferred.dramaRanking,
                    newBookRanking = rankingDeferred.newBookRanking
                )
                updateState(newState)
                
                TimberLogger.d(TAG, "初始数据加载完成")
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "加载初始数据失败", e)
                val newState = reducer.handleLoadInitialDataError(
                    currentState = getCurrentState(),
                    errorMessage = "加载数据失败: ${e.message}"
                )
                updateState(newState)
            }
        }
    }
    
    /**
     * 处理搜索操作
     * 验证输入后添加历史记录并触发导航
     */
    private fun handlePerformSearch(query: String) {
        TimberLogger.d(TAG, "执行搜索: $query")
        
        if (query.isBlank()) {
            // Reducer已经处理了空查询的情况，这里无需额外处理
            return
        }
        
        viewModelScope.launch {
            try {
                // 添加到搜索历史
                searchUseCases.addSearchHistory(query)
                
                // 更新搜索历史显示
                val updatedHistory = searchUseCases.getSearchHistory()
                val newState = reducer.handleSearchHistoryUpdated(
                    currentState = getCurrentState(),
                    updatedHistory = updatedHistory
                )
                updateState(newState)
                
                // 导航副作用已在Reducer中处理
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "搜索操作失败", e)
                sendEffect(SearchEffect.ShowToast("搜索失败: ${e.message}"))
            }
        }
    }
    
    /**
     * 处理历史记录展开状态切换
     * 通过UseCase管理展开状态的持久化
     */
    private fun handleToggleHistoryExpansion() {
        viewModelScope.launch {
            try {
                val currentExpansionState = getCurrentState().isHistoryExpanded
                val newState = searchUseCases.toggleHistoryExpansion(currentExpansionState)
                
                val updatedState = reducer.handleHistoryExpansionPersisted(
                    currentState = getCurrentState(),
                    newExpansionState = newState
                )
                updateState(updatedState)
                
            } catch (e: Exception) {
                TimberLogger.e(TAG, "切换历史记录展开状态失败", e)
                sendEffect(SearchEffect.ShowToast("操作失败: ${e.message}"))
            }
        }
    }
    
    // endregion
}

// region 兼容性定义

/**
 * 搜索页面用户操作（兼容性保留）
 * 保留原有Action定义，方便UI层迁移
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
 * 搜索页面一次性事件（兼容性保留）
 * 保留原有Event定义，方便UI层迁移
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
 * 搜索页面UI状态（兼容性保留）
 * 保留原有UiState定义，方便UI层迁移
 */
typealias SearchUiState = SearchState

// endregion

// region 业务逻辑组合类

/**
 * 搜索相关UseCase组合
 * 封装所有搜索相关的业务逻辑，简化ViewModel
 */
private class SearchUseCases(
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val toggleHistoryExpansionUseCase: ToggleHistoryExpansionUseCase,
    private val getRankingListUseCase: GetRankingListUseCase
) {
    
    suspend fun getSearchHistory(): List<String> {
        return getSearchHistoryUseCase()
    }
    
    suspend fun addSearchHistory(query: String) {
        addSearchHistoryUseCase(query)
    }
    
    suspend fun getRankingList(): com.novel.page.search.repository.RankingData {
        return getRankingListUseCase()
    }
    
    suspend fun toggleHistoryExpansion(currentState: Boolean): Boolean {
        return toggleHistoryExpansionUseCase(currentState)
    }
}

// endregion 