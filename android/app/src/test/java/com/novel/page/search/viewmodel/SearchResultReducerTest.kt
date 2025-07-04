package com.novel.page.search.viewmodel

import org.junit.Test
import org.junit.Assert.*

/**
 * SearchResultReducer单元测试
 * 
 * 测试覆盖：
 * - 所有SearchResultIntent状态转换逻辑
 * - 分页加载机制
 * - 筛选条件处理
 * - 分类选择逻辑
 * - 搜索成功/失败状态处理
 */
class SearchResultReducerTest {

    private val reducer = SearchResultReducer()

    // region 测试数据
    
    private val initialState = SearchResultState()
    
    private val testBooks = listOf(
        BookInfoRespDto(
            id = 1L,
            categoryId = 1L,
            categoryName = "玄幻小说",
            picUrl = "https://example.com/book1.jpg",
            bookName = "测试小说1",
            authorId = 1L,
            authorName = "测试作者1",
            bookDesc = "测试描述1",
            bookStatus = 1,
            visitCount = 1000L,
            wordCount = 100000,
            commentCount = 50,
            firstChapterId = 1L,
            lastChapterId = 10L,
            lastChapterName = "第十章",
            updateTime = "2023-12-01"
        ),
        BookInfoRespDto(
            id = 2L,
            categoryId = 2L,
            categoryName = "都市言情",
            picUrl = "https://example.com/book2.jpg",
            bookName = "测试小说2",
            authorId = 2L,
            authorName = "测试作者2",
            bookDesc = "测试描述2",
            bookStatus = 0,
            visitCount = 2000L,
            wordCount = 200000,
            commentCount = 80,
            firstChapterId = 11L,
            lastChapterId = 25L,
            lastChapterName = "第二十五章",
            updateTime = "2023-12-02"
        )
    )
    
    private val testCategoryFilters = listOf(
        CategoryFilter(id = -1, name = "所有"),
        CategoryFilter(id = 1, name = "武侠玄幻"),
        CategoryFilter(id = 2, name = "都市言情"),
        CategoryFilter(id = 3, name = "历史军事")
    )
    
    private val testFilters = FilterState(
        updateStatus = UpdateStatus.ONGOING,
        isVip = VipStatus.FREE,
        wordCountRange = WordCountRange.W_10_30,
        sortBy = SortBy.NEW_UPDATE
    )
    
    // endregion

    // region UpdateQuery Intent测试

    @Test
    fun `UpdateQuery应该正确更新查询内容`() {
        // Given
        val query = "测试搜索"
        val intent = SearchResultIntent.UpdateQuery(query)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        with(result.newState) {
            assertEquals("应该更新查询内容", query, this.query)
            assertEquals("版本应该递增", initialState.version + 1, version)
        }
        assertNull("UpdateQuery不应产生副作用", result.effect)
    }

    // endregion

    // region PerformSearch Intent测试

    @Test
    fun `PerformSearch应该重置状态并开始加载`() {
        // Given
        val stateWithData = initialState.copy(
            books = testBooks,
            totalResults = 100,
            hasMore = true,
            error = "之前的错误"
        )
        val query = "新搜索"
        val intent = SearchResultIntent.PerformSearch(query)

        // When
        val result = reducer.reduceWithEffect(stateWithData, intent)

        // Then
        with(result.newState) {
            assertEquals("应该更新查询", query, this.query)
            assertTrue("应该开始加载", isLoading)
            assertNull("应该清除错误", error)
            assertTrue("应该清空书籍列表", books.isEmpty())
            assertEquals("应该重置总结果数", 0, totalResults)
            assertFalse("应该重置hasMore", hasMore)
            assertFalse("应该重置分页加载状态", isLoadingMore)
            assertEquals("版本应该递增", stateWithData.version + 1, version)
        }
        assertNull("PerformSearch不应产生副作用", result.effect)
    }

    // endregion

    // region SelectCategory Intent测试

    @Test
    fun `SelectCategory应该正确更新选中的分类`() {
        // Given
        val categoryId = 2
        val intent = SearchResultIntent.SelectCategory(categoryId)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        with(result.newState) {
            assertEquals("应该更新选中分类", categoryId, selectedCategoryId)
            assertEquals("版本应该递增", initialState.version + 1, version)
        }
        assertNull("SelectCategory不应产生副作用", result.effect)
    }

    @Test
    fun `SelectCategory应该支持null分类（全部）`() {
        // Given
        val stateWithCategory = initialState.copy(selectedCategoryId = 1)
        val intent = SearchResultIntent.SelectCategory(null)

        // When
        val result = reducer.reduceWithEffect(stateWithCategory, intent)

        // Then
        assertNull("应该支持设置为null", result.newState.selectedCategoryId)
    }

    // endregion

    // region Filter相关Intent测试

    @Test
    fun `OpenFilterSheet应该打开筛选面板`() {
        // Given
        val intent = SearchResultIntent.OpenFilterSheet

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        with(result.newState) {
            assertTrue("应该打开筛选面板", isFilterSheetOpen)
            assertEquals("版本应该递增", initialState.version + 1, version)
        }
        assertNull("OpenFilterSheet不应产生副作用", result.effect)
    }

    @Test
    fun `CloseFilterSheet应该关闭筛选面板`() {
        // Given
        val stateWithOpenSheet = initialState.copy(isFilterSheetOpen = true)
        val intent = SearchResultIntent.CloseFilterSheet

        // When
        val result = reducer.reduceWithEffect(stateWithOpenSheet, intent)

        // Then
        with(result.newState) {
            assertFalse("应该关闭筛选面板", isFilterSheetOpen)
            assertEquals("版本应该递增", stateWithOpenSheet.version + 1, version)
        }
        assertNull("CloseFilterSheet不应产生副作用", result.effect)
    }

    @Test
    fun `UpdateFilters应该正确更新筛选条件`() {
        // Given
        val intent = SearchResultIntent.UpdateFilters(testFilters)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        with(result.newState) {
            assertEquals("应该更新筛选条件", testFilters, filters)
            assertEquals("版本应该递增", initialState.version + 1, version)
        }
        assertNull("UpdateFilters不应产生副作用", result.effect)
    }

    @Test
    fun `ApplyFilters应该关闭面板并重新搜索`() {
        // Given
        val stateWithOpenSheet = initialState.copy(
            isFilterSheetOpen = true,
            books = testBooks,
            hasMore = true
        )
        val intent = SearchResultIntent.ApplyFilters

        // When
        val result = reducer.reduceWithEffect(stateWithOpenSheet, intent)

        // Then
        with(result.newState) {
            assertFalse("应该关闭筛选面板", isFilterSheetOpen)
            assertTrue("应该开始加载", isLoading)
            assertTrue("应该清空书籍列表", books.isEmpty())
            assertEquals("应该重置总结果数", 0, totalResults)
            assertFalse("应该重置hasMore", hasMore)
            assertEquals("版本应该递增", stateWithOpenSheet.version + 1, version)
        }
        assertNull("ApplyFilters不应产生副作用", result.effect)
    }

    @Test
    fun `ClearFilters应该重置为默认筛选条件`() {
        // Given
        val stateWithFilters = initialState.copy(filters = testFilters)
        val intent = SearchResultIntent.ClearFilters

        // When
        val result = reducer.reduceWithEffect(stateWithFilters, intent)

        // Then
        with(result.newState) {
            assertEquals("应该重置为默认筛选", FilterState(), filters)
            assertEquals("版本应该递增", stateWithFilters.version + 1, version)
        }
        assertNull("ClearFilters不应产生副作用", result.effect)
    }

    // endregion

    // region LoadNextPage Intent测试

    @Test
    fun `LoadNextPage应该在可以加载更多时设置分页加载状态`() {
        // Given
        val stateCanLoadMore = initialState.copy(
            hasMore = true,
            isLoadingMore = false
        )
        val intent = SearchResultIntent.LoadNextPage

        // When
        val result = reducer.reduceWithEffect(stateCanLoadMore, intent)

        // Then
        with(result.newState) {
            assertTrue("应该设置分页加载状态", isLoadingMore)
            assertEquals("版本应该递增", stateCanLoadMore.version + 1, version)
        }
        assertNull("LoadNextPage不应产生副作用", result.effect)
    }

    @Test
    fun `LoadNextPage应该在正在分页加载时跳过操作`() {
        // Given
        val stateAlreadyLoading = initialState.copy(
            hasMore = true,
            isLoadingMore = true
        )
        val intent = SearchResultIntent.LoadNextPage

        // When
        val result = reducer.reduceWithEffect(stateAlreadyLoading, intent)

        // Then
        assertEquals("状态不应变化", stateAlreadyLoading, result.newState)
        assertNull("不应产生副作用", result.effect)
    }

    @Test
    fun `LoadNextPage应该在没有更多数据时跳过操作`() {
        // Given
        val stateNoMore = initialState.copy(
            hasMore = false,
            isLoadingMore = false
        )
        val intent = SearchResultIntent.LoadNextPage

        // When
        val result = reducer.reduceWithEffect(stateNoMore, intent)

        // Then
        assertEquals("状态不应变化", stateNoMore, result.newState)
        assertNull("不应产生副作用", result.effect)
    }

    // endregion

    // region Navigation Intent测试

    @Test
    fun `NavigateToDetail应该触发导航副作用`() {
        // Given
        val bookId = "12345"
        val intent = SearchResultIntent.NavigateToDetail(bookId)

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发导航副作用", result.effect is SearchResultEffect.NavigateToDetail)
        assertEquals("书籍ID应该正确", bookId, (result.effect as SearchResultEffect.NavigateToDetail).bookId)
    }

    @Test
    fun `NavigateBack应该触发返回副作用`() {
        // Given
        val intent = SearchResultIntent.NavigateBack

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertEquals("状态不应变化", initialState, result.newState)
        assertTrue("应该触发返回副作用", result.effect is SearchResultEffect.NavigateBack)
    }

    // endregion

    // region 业务逻辑处理方法测试

    @Test
    fun `handleSearchSuccess应该正确处理新搜索成功`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val totalResults = 50
        val hasMore = true

        // When
        val result = reducer.handleSearchSuccess(
            currentState = loadingState,
            books = testBooks,
            totalResults = totalResults,
            hasMore = hasMore,
            isLoadMore = false
        )

        // Then
        with(result) {
            assertFalse("加载状态应该设置为false", isLoading)
            assertNull("错误应该清除", error)
            assertEquals("书籍列表应该正确设置", testBooks, books)
            assertEquals("总结果数应该正确设置", totalResults, this.totalResults)
            assertEquals("hasMore应该正确设置", hasMore, this.hasMore)
            assertFalse("分页加载状态应该为false", isLoadingMore)
            assertEquals("版本应该递增", loadingState.version + 1, version)
        }
    }

    @Test
    fun `handleSearchSuccess应该正确处理分页加载成功`() {
        // Given
        val stateWithData = initialState.copy(
            books = listOf(testBooks[0]),
            isLoadingMore = true
        )
        val newBooks = listOf(testBooks[1])
        val hasMore = false

        // When
        val result = reducer.handleSearchSuccess(
            currentState = stateWithData,
            books = newBooks,
            totalResults = 2,
            hasMore = hasMore,
            isLoadMore = true
        )

        // Then
        with(result) {
            assertEquals("书籍应该合并", testBooks, books)
            assertEquals("hasMore应该更新", hasMore, this.hasMore)
            assertFalse("分页加载状态应该为false", isLoadingMore)
            assertEquals("版本应该递增", stateWithData.version + 1, version)
        }
    }

    @Test
    fun `handleSearchError应该正确处理新搜索失败`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val errorMessage = "搜索失败：网络错误"

        // When
        val result = reducer.handleSearchError(
            currentState = loadingState,
            errorMessage = errorMessage,
            isLoadMore = false
        )

        // Then
        with(result) {
            assertFalse("加载状态应该设置为false", isLoading)
            assertEquals("错误消息应该正确设置", errorMessage, error)
            assertFalse("分页加载状态应该为false", isLoadingMore)
            assertEquals("版本应该递增", loadingState.version + 1, version)
        }
    }

    @Test
    fun `handleSearchError应该正确处理分页加载失败`() {
        // Given
        val stateLoadingMore = initialState.copy(
            books = testBooks,
            isLoadingMore = true
        )
        val errorMessage = "分页加载失败"

        // When
        val result = reducer.handleSearchError(
            currentState = stateLoadingMore,
            errorMessage = errorMessage,
            isLoadMore = true
        )

        // Then
        with(result) {
            assertEquals("书籍列表应该保持不变", testBooks, books)
            assertFalse("分页加载状态应该设置为false", isLoadingMore)
            assertEquals("版本应该递增", stateLoadingMore.version + 1, version)
        }
    }

    @Test
    fun `handleCategoryFiltersLoaded应该正确设置分类筛选器`() {
        // Given
        val emptyState = initialState

        // When
        val result = reducer.handleCategoryFiltersLoaded(
            currentState = emptyState,
            categoryFilters = testCategoryFilters
        )

        // Then
        with(result) {
            assertEquals("分类筛选器应该正确设置", testCategoryFilters, categoryFilters)
            assertEquals("版本应该递增", emptyState.version + 1, version)
        }
    }

    // endregion

    // region 状态一致性测试

    @Test
    fun `所有状态转换都应该递增版本号`() {
        val intents = listOf(
            SearchResultIntent.UpdateQuery("测试"),
            SearchResultIntent.PerformSearch("测试"),
            SearchResultIntent.SelectCategory(1),
            SearchResultIntent.OpenFilterSheet,
            SearchResultIntent.CloseFilterSheet,
            SearchResultIntent.UpdateFilters(FilterState()),
            SearchResultIntent.ApplyFilters,
            SearchResultIntent.ClearFilters,
            SearchResultIntent.LoadNextPage
        )

        intents.forEach { intent ->
            val result = reducer.reduceWithEffect(initialState, intent)
            // 除了有条件跳过的LoadNextPage，其他都应该递增版本
            if (intent is SearchResultIntent.LoadNextPage && !initialState.hasMore) {
                assertEquals("LoadNextPage在hasMore=false时应该跳过", initialState, result.newState)
            } else {
                assertEquals(
                    "Intent ${intent::class.simpleName} 应该递增版本号",
                    initialState.version + 1,
                    result.newState.version
                )
            }
        }
    }

    @Test
    fun `状态应该保持不可变性`() {
        // Given
        val intent = SearchResultIntent.UpdateQuery("新查询")

        // When
        val result = reducer.reduceWithEffect(initialState, intent)

        // Then
        assertNotSame("应该返回新的状态实例", initialState, result.newState)
        assertEquals("原始状态不应被修改", "", initialState.query)
        assertEquals("新状态应该包含更新", "新查询", result.newState.query)
    }

    // endregion

    // region 边界条件测试

    @Test
    fun `应该正确处理空搜索结果`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)

        // When
        val result = reducer.handleSearchSuccess(
            currentState = loadingState,
            books = emptyList(),
            totalResults = 0,
            hasMore = false,
            isLoadMore = false
        )

        // Then
        with(result) {
            assertTrue("书籍列表为空", books.isEmpty())
            assertEquals("总结果数为0", 0, totalResults)
            assertFalse("没有更多数据", hasMore)
            assertTrue("isEmpty应该为true", isEmpty)
        }
    }

    @Test
    fun `LoadNextPage在边界条件下应该正确跳过`() {
        val testCases = listOf(
            "正在加载更多" to initialState.copy(hasMore = true, isLoadingMore = true),
            "没有更多数据" to initialState.copy(hasMore = false, isLoadingMore = false),
            "同时满足两个条件" to initialState.copy(hasMore = false, isLoadingMore = true)
        )

        testCases.forEach { (description, state) ->
            val result = reducer.reduceWithEffect(state, SearchResultIntent.LoadNextPage)
            assertEquals("$description 时应该跳过操作", state, result.newState)
        }
    }

    // endregion
} 