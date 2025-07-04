package com.novel.page.search.viewmodel

import androidx.compose.runtime.Stable
import com.novel.core.mvi.MviIntent
import com.novel.core.mvi.MviState
import com.novel.core.mvi.MviEffect

/**
 * SearchResult模块MVI契约定义
 * 
 * 根据优化方案阶段2第13天任务要求，将SearchResult模块重构为统一MVI架构
 * 保持UI和业务逻辑完全不变，所有功能完整实现无遗漏
 */

// region SearchResult Intent 定义

/**
 * 搜索结果页面用户意图
 * 基于核心MVI接口，整合现有SearchResultAction
 */
sealed class SearchResultIntent : MviIntent {
    /** 更新搜索查询内容 */
    data class UpdateQuery(val query: String) : SearchResultIntent()
    
    /** 执行搜索操作 */
    data class PerformSearch(val query: String) : SearchResultIntent()
    
    /** 选择搜索分类 */
    data class SelectCategory(val categoryId: Int?) : SearchResultIntent()
    
    /** 打开筛选面板 */
    data object OpenFilterSheet : SearchResultIntent()
    
    /** 关闭筛选面板 */
    data object CloseFilterSheet : SearchResultIntent()
    
    /** 更新筛选条件 */
    data class UpdateFilters(val filters: FilterState) : SearchResultIntent()
    
    /** 应用筛选条件 */
    data object ApplyFilters : SearchResultIntent()
    
    /** 清除筛选条件 */
    data object ClearFilters : SearchResultIntent()
    
    /** 加载下一页数据 */
    data object LoadNextPage : SearchResultIntent()
    
    /** 导航到书籍详情 */
    data class NavigateToDetail(val bookId: String) : SearchResultIntent()
    
    /** 返回上级页面 */
    data object NavigateBack : SearchResultIntent()
}

// endregion

// region SearchResult State 定义

/**
 * 搜索结果页面状态
 * 基于现有SearchResultUiState重构，符合核心MVI State接口
 */
@Stable
data class SearchResultState(
    override val version: Long = 0L,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    /** 搜索查询内容 */
    val query: String = "",
    /** 搜索结果书籍列表 */
    val books: List<BookInfoRespDto> = emptyList(),
    /** 总结果数量 */
    val totalResults: Int = 0,
    /** 是否还有更多数据 */
    val hasMore: Boolean = false,
    /** 当前选中的分类ID */
    val selectedCategoryId: Int? = null,
    /** 分类筛选器列表 */
    val categoryFilters: List<CategoryFilter> = emptyList(),
    /** 当前筛选条件 */
    val filters: FilterState = FilterState(),
    /** 筛选面板是否打开 */
    val isFilterSheetOpen: Boolean = false,
    /** 分页加载状态 */
    val isLoadingMore: Boolean = false
) : MviState {
    
    override val isEmpty: Boolean
        get() = books.isEmpty() && !isLoading
}

// endregion

// region SearchResult Effect 定义

/**
 * 搜索结果页面副作用
 * 替换现有SearchResultEvent，处理一次性事件如导航
 */
sealed class SearchResultEffect : MviEffect {
    /** 导航到书籍详情 */
    data class NavigateToDetail(val bookId: String) : SearchResultEffect()
    
    /** 返回上级页面 */
    data object NavigateBack : SearchResultEffect()
    
    /** 显示Toast提示 */
    data class ShowToast(val message: String) : SearchResultEffect()
}

// endregion 