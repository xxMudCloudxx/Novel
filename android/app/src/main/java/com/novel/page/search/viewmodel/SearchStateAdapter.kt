package com.novel.page.search.viewmodel

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.novel.page.search.component.SearchRankingItem
import com.novel.core.adapter.StateAdapter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Search状态适配器
 * 
 * 为Search模块提供状态适配功能，方便UI层访问MVI状态的特定部分
 * 继承基础StateAdapter，提供Search模块专用的状态适配功能
 * 
 * 特性：
 * - 继承基础StateAdapter的所有功能
 * - Search模块专用状态访问方法
 * - 细粒度状态订阅，减少不必要的重组
 * - 类型安全的强类型状态访问
 * - UI友好的便利方法
 */
@Stable
class SearchStateAdapter(
    stateFlow: StateFlow<SearchState>
) : StateAdapter<SearchState>(stateFlow) {
    
    // region 搜索相关状态适配
    
    /** 当前搜索查询内容 */
    @Stable
    val searchQuery = mapState { it.searchQuery }
    
    /** 搜索历史记录列表 */
    @Stable
    val searchHistory = mapState { it.searchHistory }
    
    /** 历史记录是否展开显示 */
    @Stable
    val isHistoryExpanded = mapState { it.isHistoryExpanded }
    
    /** 是否有搜索历史 */
    @Stable
    val hasSearchHistory = createConditionFlow { it.searchHistory.isNotEmpty() }
    
    /** 显示的搜索历史（根据展开状态限制数量） */
    @Stable
    val displayedSearchHistory = mapState { state ->
        if (state.isHistoryExpanded) {
            state.searchHistory
        } else {
            state.searchHistory.take(3) // 收起时只显示前3条
        }
    }
    
    // endregion
    
    // region 榜单相关状态适配
    
    /** 小说榜单数据 */
    @Stable
    val novelRanking = mapState { it.novelRanking }
    
    /** 剧本榜单数据 */
    @Stable
    val dramaRanking = mapState { it.dramaRanking }
    
    /** 新书榜单数据 */
    @Stable
    val newBookRanking = mapState { it.newBookRanking }
    
    /** 榜单数据加载状态 */
    @Stable
    val rankingLoading = mapState { it.rankingLoading }
    
    /** 是否有榜单数据 */
    @Stable
    val hasRankingData = createConditionFlow { state ->
        state.novelRanking.isNotEmpty() || 
        state.dramaRanking.isNotEmpty() || 
        state.newBookRanking.isNotEmpty()
    }
    
    /** 所有榜单数据（合并） */
    @Stable
    val allRankingData = mapState { state ->
        persistentListOf<RankingSection>().builder().apply {
            if (state.novelRanking.isNotEmpty()) {
                add(RankingSection("小说榜", state.novelRanking))
            }
            if (state.dramaRanking.isNotEmpty()) {
                add(RankingSection("剧本榜", state.dramaRanking))
            }
            if (state.newBookRanking.isNotEmpty()) {
                add(RankingSection("新书榜", state.newBookRanking))
            }
        }.build()
    }
    
    // endregion
    
    // region Search模块专用便利方法
    
    /** 检查是否需要显示更多历史按钮 */
    fun shouldShowMoreHistoryButton(): Boolean {
        val state = getCurrentSnapshot()
        return state.searchHistory.size > 3 && !state.isHistoryExpanded
    }
    
    /** 检查是否需要显示收起历史按钮 */
    fun shouldShowLessHistoryButton(): Boolean {
        val state = getCurrentSnapshot()
        return state.searchHistory.size > 3 && state.isHistoryExpanded
    }
    
    /** 获取历史记录展开按钮文本 */
    fun getHistoryToggleText(): String {
        val state = getCurrentSnapshot()
        return if (state.isHistoryExpanded) {
            "收起"
        } else {
            "查看更多 (${state.searchHistory.size - 3})"
        }
    }
    
    /** 检查是否可以执行搜索 */
    fun canPerformSearch(): Boolean {
        val state = getCurrentSnapshot()
        return state.searchQuery.isNotBlank() && !state.isLoading
    }
    
    /** 获取搜索提示文本 */
    fun getSearchHint(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "搜索中..."
            state.hasError -> "搜索失败，请重试"
            state.searchHistory.isNotEmpty() -> "搜索小说、作者"
            else -> "发现好看的小说"
        }
    }
    
    /** 获取历史记录数量文本 */
    fun getHistoryCountText(): String {
        val count = getCurrentSnapshot().searchHistory.size
        return when {
            count == 0 -> "暂无搜索历史"
            count <= 3 -> "$count 条搜索历史"
            else -> "共 $count 条搜索历史"
        }
    }
    
    /** 检查是否显示榜单内容 */
    fun shouldShowRankingContent(): Boolean {
        val state = getCurrentSnapshot()
        return !state.isLoading && !state.hasError && (
            state.novelRanking.isNotEmpty() || 
            state.dramaRanking.isNotEmpty() || 
            state.newBookRanking.isNotEmpty()
        )
    }
    
    /** 获取榜单加载状态文本 */
    fun getRankingStatusText(): String {
        val state = getCurrentSnapshot()
        val hasRanking = state.novelRanking.isNotEmpty() || 
                        state.dramaRanking.isNotEmpty() || 
                        state.newBookRanking.isNotEmpty()
        return when {
            state.rankingLoading -> "加载榜单中..."
            state.hasError -> "榜单加载失败"
            hasRanking -> "榜单加载完成"
            else -> "暂无榜单数据"
        }
    }
    
    // endregion
}

/**
 * 榜单分组数据
 */
@Stable
data class RankingSection(
    val title: String,
    val items: ImmutableList<SearchRankingItem>
)

/**
 * StateAdapter工厂方法
 * 简化SearchStateAdapter的创建
 */
fun StateFlow<SearchState>.asSearchAdapter(): SearchStateAdapter {
    return SearchStateAdapter(this)
}

/**
 * 状态组合器
 * 将多个状态组合成UI需要的复合状态
 */
@Stable
data class SearchScreenState(
    val isLoading: Boolean,
    val error: String?,
    val searchQuery: String,
    val displayedHistory: List<String>,
    val rankingSections: List<RankingSection>,
    val canPerformSearch: Boolean,
    val searchHint: String,
    val shouldShowHistoryToggle: Boolean,
    val historyToggleText: String,
    val historyCountText: String,
    val shouldShowRanking: Boolean,
    val rankingStatusText: String
)

/**
 * 将SearchState转换为UI友好的组合状态
 */
fun SearchStateAdapter.toScreenState(): SearchScreenState {
    val snapshot = getCurrentSnapshot()
    return SearchScreenState(
        isLoading = snapshot.isLoading,
        error = snapshot.error,
        searchQuery = snapshot.searchQuery,
        displayedHistory = if (snapshot.isHistoryExpanded) {
            snapshot.searchHistory
        } else {
            snapshot.searchHistory.take(3)
        },
        rankingSections = buildList {
            if (snapshot.novelRanking.isNotEmpty()) {
                add(RankingSection("小说榜", snapshot.novelRanking))
            }
            if (snapshot.dramaRanking.isNotEmpty()) {
                add(RankingSection("剧本榜", snapshot.dramaRanking))
            }
            if (snapshot.newBookRanking.isNotEmpty()) {
                add(RankingSection("新书榜", snapshot.newBookRanking))
            }
        },
        canPerformSearch = canPerformSearch(),
        searchHint = getSearchHint(),
        shouldShowHistoryToggle = shouldShowMoreHistoryButton() || shouldShowLessHistoryButton(),
        historyToggleText = getHistoryToggleText(),
        historyCountText = getHistoryCountText(),
        shouldShowRanking = shouldShowRankingContent(),
        rankingStatusText = getRankingStatusText()
    )
}

/**
 * Search模块状态监听器
 * 提供Search模块特定的状态变更监听
 */
class SearchStateListener(
    private val adapter: SearchStateAdapter
) {
    
    /** 监听搜索查询变更 */
    fun onSearchQueryChanged(action: (String) -> Unit) = adapter.searchQuery.map { query ->
        action(query)
        query
    }
    
    /** 监听历史记录变更 */
    fun onHistoryChanged(action: (ImmutableList<String>) -> Unit) = adapter.searchHistory.map { history ->
        action(history)
        history
    }
    
    /** 监听榜单数据变更 */
    fun onRankingDataChanged(action: (ImmutableList<RankingSection>) -> Unit) = adapter.allRankingData.map { ranking ->
        action(ranking)
        ranking
    }
}

/**
 * 为SearchStateAdapter创建专用监听器
 */
fun SearchStateAdapter.createSearchListener(): SearchStateListener {
    return SearchStateListener(this)
}