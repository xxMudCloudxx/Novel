package com.novel.page.search.viewmodel

import com.novel.page.search.component.SearchRankingItem
import org.junit.Test
import org.junit.Assert.*

/**
 * SearchReducer单元测试
 * 
 * 测试覆盖：
 * - 所有SearchIntent状态转换逻辑
 * - 副作用Effect正确触发
 * - 历史记录展开逻辑
 * - 榜单数据加载处理
 * - 搜索查询状态管理
 * - 错误状态处理
 */
class SearchReducerTest {

    private val reducer = SearchReducer()

    // region 测试数据
    
    private val initialState = SearchState()
    
    private val testSearchHistory = listOf("测试小说", "玄幻小说", "都市言情")
    
    private val testNovelRanking = listOf(
        SearchRankingItem(1, "测试小说1", "测试作者1", 1),
        SearchRankingItem(2, "测试小说2", "测试作者2", 2),
        SearchRankingItem(3, "测试小说3", "测试作者3", 3)
    )
    
    private val testDramaRanking = listOf(
        SearchRankingItem(4, "热门短剧1", "短剧作者1", 1),
        SearchRankingItem(5, "热门短剧2", "短剧作者2", 2)
    )
    
    private val testNewBookRanking = listOf(
        SearchRankingItem(6, "新书推荐1", "新人作者1", 1),
        SearchRankingItem(7, "新书推荐2", "新人作者2", 2)
    )
    
    // endregion

    // region LoadInitialData Intent测试

    @Test
    fun `LoadInitialData应该设置加载状态并清除错误`() {
        // Given
        val stateWithError = initialState.copy(error = "之前的错误")
        val intent = SearchIntent.LoadInitialData

        // When
        val result = reducer.reduceWithEffect(stateWithError, intent)

        // Then
        with(result.newState) {
            assertTrue("应该设置为加载状态", isLoading)
            assertNull("应该清除错误", error)
            assertEquals("版本应该递增", stateWithError.version + 1, version)
        }
        assertNull("LoadInitialData不应产生副作用", result.effect)
    }

    // endregion

    // region UpdateSearchQuery Intent测试

    @Test
    fun `UpdateSearchQuery应该正确更新搜索查询内容`() {
        // Given
        val query = "测试搜索关键词"
        val intent = SearchIntent.UpdateSearchQuery(query)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        with(result.newState) {
            assertEquals("应该更新搜索查询内容", query, searchQuery)
            assertEquals("版本应该递增", initialState.version + 1, version)
            assertFalse("不应影响加载状态", isLoading)
        }
        assertNull("UpdateSearchQuery不应产生副作用", result.effect)
    }

    @Test
    fun `UpdateSearchQuery应该支持空字符串`() {
        // Given
        val stateWithQuery = initialState.copy(searchQuery = "之前的查询")
        val intent = SearchIntent.UpdateSearchQuery("")

        // When
        val result = reducer.reduceWithEffect(stateWithQuery, intent)

        // Then
        assertEquals("应该支持清空查询", "", result.newState.searchQuery)
    }

    // endregion

    // region PerformSearch Intent测试

    @Test
    fun `PerformSearch应该在查询为空时触发Toast副作用`() {
        // Given
        val intent = SearchIntent.PerformSearch("")

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发Toast副作用", result.effect is SearchEffect.ShowToast)
        assertEquals("Toast消息应该正确", "请输入搜索关键词", (result.effect as SearchEffect.ShowToast).message)
    }

    @Test
    fun `PerformSearch应该在查询为空白字符时触发Toast副作用`() {
        // Given
        val intent = SearchIntent.PerformSearch("   ")

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发Toast副作用", result.effect is SearchEffect.ShowToast)
    }

    @Test
    fun `PerformSearch应该在有效查询时触发导航副作用`() {
        // Given
        val query = "测试小说"
        val intent = SearchIntent.PerformSearch(query)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发导航副作用", result.effect is SearchEffect.NavigateToSearchResult)
        assertEquals("导航查询应该正确", query, (result.effect as SearchEffect.NavigateToSearchResult).query)
    }

    @Test
    fun `PerformSearch应该自动清理查询的空白字符`() {
        // Given
        val query = "  测试小说  "
        val intent = SearchIntent.PerformSearch(query)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        val effect = result.effect as SearchEffect.NavigateToSearchResult
        assertEquals("应该清理空白字符", "测试小说", effect.query)
    }

    // endregion

    // region ToggleHistoryExpansion Intent测试

    @Test
    fun `ToggleHistoryExpansion应该正确切换展开状态从false到true`() {
        // Given
        val stateCollapsed = initialState.copy(isHistoryExpanded = false)
        val intent = SearchIntent.ToggleHistoryExpansion

        // When
        val result = reducer.reduceWithEffect(stateCollapsed, intent)

        // Then
        with(result.newState) {
            assertTrue("应该切换到展开状态", isHistoryExpanded)
            assertEquals("版本应该递增", stateCollapsed.version + 1, version)
        }
        assertNull("ToggleHistoryExpansion不应产生副作用", result.effect)
    }

    @Test
    fun `ToggleHistoryExpansion应该正确切换展开状态从true到false`() {
        // Given
        val stateExpanded = initialState.copy(isHistoryExpanded = true)
        val intent = SearchIntent.ToggleHistoryExpansion

        // When
        val result = reducer.reduceWithEffect(stateExpanded, intent)

        // Then
        with(result.newState) {
            assertFalse("应该切换到收起状态", isHistoryExpanded)
            assertEquals("版本应该递增", stateExpanded.version + 1, version)
        }
    }

    // endregion

    // region NavigateToBookDetail Intent测试

    @Test
    fun `NavigateToBookDetail应该触发导航副作用`() {
        // Given
        val bookId = 12345L
        val intent = SearchIntent.NavigateToBookDetail(bookId)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发导航副作用", result.effect is SearchEffect.NavigateToBookDetail)
        assertEquals("书籍ID应该正确", bookId, (result.effect as SearchEffect.NavigateToBookDetail).bookId)
    }

    // endregion

    // region NavigateBack Intent测试

    @Test
    fun `NavigateBack应该触发返回副作用`() {
        // Given
        val intent = SearchIntent.NavigateBack

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发返回副作用", result.effect is SearchEffect.NavigateBack)
    }

    // endregion

    // region ClearError Intent测试

    @Test
    fun `ClearError应该清除错误状态`() {
        // Given
        val stateWithError = initialState.copy(error = "测试错误消息")
        val intent = SearchIntent.ClearError

        // When
        val result = reducer.reduceWithEffect(stateWithError, intent)

        // Then
        with(result.newState) {
            assertNull("应该清除错误", error)
            assertEquals("版本应该递增", stateWithError.version + 1, version)
        }
        assertNull("ClearError不应产生副作用", result.effect)
    }

    // endregion

    // region 业务逻辑处理方法测试

    @Test
    fun `handleLoadInitialDataSuccess应该正确更新所有榜单数据`() {
        // Given
        val loadingState = initialState.copy(isLoading = true, rankingLoading = true)

        // When
        val result = reducer.handleLoadInitialDataSuccess(
            currentState = loadingState,
            searchHistory = testSearchHistory,
            novelRanking = testNovelRanking,
            dramaRanking = testDramaRanking,
            newBookRanking = testNewBookRanking
        )

        // Then
        with(result) {
            assertFalse("加载状态应该设置为false", isLoading)
            assertFalse("榜单加载状态应该设置为false", rankingLoading)
            assertNull("错误应该清除", error)
            assertEquals("搜索历史应该正确设置", testSearchHistory, searchHistory)
            assertEquals("小说榜单应该正确设置", testNovelRanking, novelRanking)
            assertEquals("短剧榜单应该正确设置", testDramaRanking, dramaRanking)
            assertEquals("新书榜单应该正确设置", testNewBookRanking, newBookRanking)
            assertEquals("版本应该递增", loadingState.version + 1, version)
        }
    }

    @Test
    fun `handleLoadInitialDataError应该正确设置错误状态`() {
        // Given
        val loadingState = initialState.copy(isLoading = true, rankingLoading = true)
        val errorMessage = "加载失败：网络连接错误"

        // When
        val result = reducer.handleLoadInitialDataError(
            currentState = loadingState,
            errorMessage = errorMessage
        )

        // Then
        with(result) {
            assertFalse("加载状态应该设置为false", isLoading)
            assertFalse("榜单加载状态应该设置为false", rankingLoading)
            assertEquals("错误消息应该正确设置", errorMessage, error)
            assertEquals("版本应该递增", loadingState.version + 1, version)
        }
    }

    @Test
    fun `handleSearchHistoryUpdated应该正确更新搜索历史`() {
        // Given
        val stateWithOldHistory = initialState.copy(searchHistory = listOf("旧历史"))
        val newHistory = listOf("新历史1", "新历史2", "新历史3")

        // When
        val result = reducer.handleSearchHistoryUpdated(
            currentState = stateWithOldHistory,
            updatedHistory = newHistory
        )

        // Then
        with(result) {
            assertEquals("搜索历史应该更新", newHistory, searchHistory)
            assertEquals("版本应该递增", stateWithOldHistory.version + 1, version)
        }
    }

    @Test
    fun `handleHistoryExpansionPersisted应该正确更新展开状态`() {
        // Given
        val stateCollapsed = initialState.copy(isHistoryExpanded = false)
        val newExpansionState = true

        // When
        val result = reducer.handleHistoryExpansionPersisted(
            currentState = stateCollapsed,
            newExpansionState = newExpansionState
        )

        // Then
        with(result) {
            assertEquals("展开状态应该更新", newExpansionState, isHistoryExpanded)
            assertEquals("版本应该递增", stateCollapsed.version + 1, version)
        }
    }

    // endregion

    // region 状态一致性测试

    @Test
    fun `所有状态转换都应该递增版本号`() {
        val intents = listOf(
            SearchIntent.LoadInitialData,
            SearchIntent.UpdateSearchQuery("测试"),
            SearchIntent.ToggleHistoryExpansion,
            SearchIntent.ClearError
        )

        intents.forEach { intent ->
            val result = reducer.reduceWithEffect(initialState, intent)
            assertEquals(
                "Intent ${intent::class.simpleName} 应该递增版本号",
                initialState.version + 1,
                result.newState.version
            )
        }
    }

    @Test
    fun `状态应该保持不可变性`() {
        // Given
        val intent = SearchIntent.UpdateSearchQuery("测试查询")

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertNotSame("应该返回新的状态实例", initialState, result.newState)
        assertEquals("原始状态不应被修改", "", initialState.searchQuery)
        assertEquals("新状态应该包含更新", "测试查询", result.newState.searchQuery)
    }

    // endregion
} 