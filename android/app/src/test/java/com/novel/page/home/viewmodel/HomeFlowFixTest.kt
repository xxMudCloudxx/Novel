package com.novel.page.home.viewmodel

import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Home模块Flow异常处理和加载更多状态修复测试
 * 
 * 验证修复的问题：
 * 1. Flow异常处理不会导致crash
 * 2. 加载更多状态正确显示
 * 3. 数据加载完成后显示"已加载全部"状态
 */
class HomeFlowFixTest {

    private lateinit var reducer: HomeReducer
    private lateinit var initialState: HomeState

    @Before
    fun setUp() {
        reducer = HomeReducer()
        initialState = HomeState()
    }

    @Test
    fun `test CategoryFiltersLoadSuccess updates categoryFilters correctly`() {
        // Given
        val filters = listOf(
            CategoryInfo("0", "推荐"),
            CategoryInfo("1", "玄幻"),
            CategoryInfo("2", "言情"),
            CategoryInfo("3", "都市")
        )
        val intent = HomeIntent.CategoryFiltersLoadSuccess(filters)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertEquals(filters, result.newState.categoryFilters)
        assertFalse(result.newState.categoryFiltersLoading)
        assertEquals(4, result.newState.categoryFilters.size)
        assertEquals("推荐", result.newState.categoryFilters[0].name)
        assertEquals("玄幻", result.newState.categoryFilters[1].name)
    }

    @Test
    fun `test CategoryFiltersLoadFailure handles error correctly`() {
        // Given
        val errorMessage = "网络连接失败"
        val intent = HomeIntent.CategoryFiltersLoadFailure(errorMessage)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertFalse(result.newState.categoryFiltersLoading)
        assertFalse(result.newState.isLoading)
        assertFalse(result.newState.isRefreshing)
        assertEquals(errorMessage, result.newState.error)
    }

    @Test
    fun `test HomeRecommendBooksLoadSuccess with refresh replaces existing books`() {
        // Given
        val existingBooks = listOf(
            createHomeBook(1L, "旧书1"),
            createHomeBook(2L, "旧书2")
        )
        val stateWithBooks = initialState.copy(homeRecommendBooks = existingBooks)
        
        val newBooks = listOf(
            createHomeBook(3L, "新书1"),
            createHomeBook(4L, "新书2")
        )
        val intent = HomeIntent.HomeRecommendBooksLoadSuccess(
            books = newBooks,
            isRefresh = true,
            hasMore = true
        )

        // When
        val result = reducer.reduce(stateWithBooks, intent)

        // Then
        assertEquals(newBooks, result.newState.homeRecommendBooks)
        assertFalse(result.newState.homeRecommendLoading)
        assertTrue(result.newState.hasMoreHomeRecommend)
        assertFalse(result.newState.isRefreshing)
        assertFalse(result.newState.isLoading)
    }

    @Test
    fun `test HomeRecommendBooksLoadSuccess without refresh appends books`() {
        // Given
        val existingBooks = listOf(
            createHomeBook(1L, "旧书1"),
            createHomeBook(2L, "旧书2")
        )
        val stateWithBooks = initialState.copy(homeRecommendBooks = existingBooks)
        
        val newBooks = listOf(
            createHomeBook(3L, "新书1"),
            createHomeBook(4L, "新书2")
        )
        val intent = HomeIntent.HomeRecommendBooksLoadSuccess(
            books = newBooks,
            isRefresh = false,
            hasMore = false  // 没有更多数据了
        )

        // When
        val result = reducer.reduce(stateWithBooks, intent)

        // Then
        assertEquals(4, result.newState.homeRecommendBooks.size)
        assertEquals(existingBooks + newBooks, result.newState.homeRecommendBooks)
        assertFalse(result.newState.homeRecommendLoading)
        assertFalse(result.newState.hasMoreHomeRecommend) // 应该显示已加载全部
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess with loadMore appends correctly`() {
        // Given
        val existingBooks = listOf(
            createSearchBook(1L, "分类书1"),
            createSearchBook(2L, "分类书2")
        )
        val stateWithBooks = initialState.copy(
            recommendBooks = existingBooks,
            recommendPage = 1
        )
        
        val newBooks = listOf(
            createSearchBook(3L, "分类书3"),
            createSearchBook(4L, "分类书4")
        )
        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = newBooks,
            isLoadMore = true,
            hasMore = true,
            totalPages = 3
        )

        // When
        val result = reducer.reduce(stateWithBooks, intent)

        // Then
        assertEquals(4, result.newState.recommendBooks.size)
        assertEquals(existingBooks + newBooks, result.newState.recommendBooks)
        assertFalse(result.newState.recommendLoading)
        assertTrue(result.newState.hasMoreRecommend)
        assertEquals(2, result.newState.recommendPage) // 页码应该增加
        assertEquals(3, result.newState.totalRecommendPages)
    }

    @Test
    fun `test LoadMoreRecommend updates loading state correctly for recommend mode`() {
        // Given
        val stateInRecommendMode = initialState.copy(
            isRecommendMode = true,
            homeRecommendPage = 1
        )
        val intent = HomeIntent.LoadMoreRecommend

        // When
        val result = reducer.reduce(stateInRecommendMode, intent)

        // Then
        assertTrue(result.newState.homeRecommendLoading)
        assertEquals(2, result.newState.homeRecommendPage)
    }

    @Test
    fun `test LoadMoreRecommend updates loading state correctly for category mode`() {
        // Given
        val stateInCategoryMode = initialState.copy(
            isRecommendMode = false,
            recommendPage = 1
        )
        val intent = HomeIntent.LoadMoreRecommend

        // When
        val result = reducer.reduce(stateInCategoryMode, intent)

        // Then
        assertTrue(result.newState.recommendLoading)
        assertEquals(1, result.newState.recommendPage) // 分类模式下页码在Success时才增加
    }

    @Test
    fun `test SelectCategoryFilter switches mode correctly`() {
        // Given - 从分类模式切换到推荐模式
        val stateInCategoryMode = initialState.copy(
            isRecommendMode = false,
            selectedCategoryFilter = "玄幻",
            recommendBooks = listOf(createSearchBook(1L, "测试书")),
            recommendPage = 2
        )
        val intent = HomeIntent.SelectCategoryFilter("推荐")

        // When
        val result = reducer.reduce(stateInCategoryMode, intent)

        // Then
        assertTrue(result.newState.isRecommendMode)
        assertEquals("推荐", result.newState.selectedCategoryFilter)
        assertTrue(result.newState.recommendBooks.isEmpty()) // 应该清空分类推荐书籍
        assertEquals(1, result.newState.recommendPage) // 页码重置
    }

    @Test
    fun `test error handling preserves data integrity`() {
        // Given
        val stateWithData = initialState.copy(
            homeRecommendBooks = listOf(createHomeBook(1L, "测试书")),
            categoryFilters = listOf(CategoryInfo("0", "推荐"), CategoryInfo("1", "玄幻"))
        )
        val intent = HomeIntent.HomeRecommendBooksLoadFailure("网络错误")

        // When
        val result = reducer.reduce(stateWithData, intent)

        // Then
        // 数据应该保持不变
        assertEquals(stateWithData.homeRecommendBooks, result.newState.homeRecommendBooks)
        assertEquals(stateWithData.categoryFilters, result.newState.categoryFilters)
        // 加载状态应该重置
        assertFalse(result.newState.homeRecommendLoading)
        assertFalse(result.newState.isLoading)
        assertFalse(result.newState.isRefreshing)
        // 错误信息应该设置
        assertEquals("网络错误", result.newState.error)
    }

    // Helper methods
    private fun createHomeBook(id: Long, name: String): HomeService.HomeBook {
        return HomeService.HomeBook(
            bookId = id,
            bookName = name,
            authorName = "作者$id",
            picUrl = "http://example.com/cover$id.jpg",
            bookDesc = "描述$id",
            type = 3 // 热门推荐类型
        )
    }

    private fun createSearchBook(id: Long, name: String): SearchService.BookInfo {
        return SearchService.BookInfo(
            id = id,
            categoryId = 1L,
            categoryName = "分类",
            picUrl = "http://example.com/cover$id.jpg",
            bookName = name,
            authorId = id,
            authorName = "作者$id",
            bookDesc = "描述$id",
            bookStatus = 0, // 连载中
            visitCount = 1000L,
            wordCount = 100000,
            commentCount = 10,
            firstChapterId = 1L,
            lastChapterId = 10L,
            lastChapterName = "第十章",
            updateTime = "2025-01-01 10:00:00"
        )
    }
} 