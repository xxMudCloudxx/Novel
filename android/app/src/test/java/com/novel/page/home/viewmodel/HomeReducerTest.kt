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
 * HomeReducer单元测试
 *
 * 验证所有Intent的状态转换逻辑是否正确。
 */
class HomeReducerTest {

    private lateinit var reducer: HomeReducer
    private lateinit var initialState: HomeState

    @Before
    fun setUp() {
        reducer = HomeReducer()
        initialState = HomeState()
    }

    @Test
    fun `test LoadInitialData intent should set loading state`() {
        // Given
        val intent = HomeIntent.LoadInitialData

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertTrue(result.newState.isLoading)
        assertNull(result.newState.error)
        assertEquals(1L, result.newState.version)
    }

    @Test
    fun `test RefreshData intent should set refreshing state`() {
        // Given
        val intent = HomeIntent.RefreshData

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertTrue(result.newState.isRefreshing)
        assertNull(result.newState.error)
    }

    @Test
    fun `test SelectCategoryFilter intent should update filter and mode`() {
        // Given
        val intent = HomeIntent.SelectCategoryFilter("玄幻")

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertEquals("玄幻", result.newState.selectedCategoryFilter)
        assertFalse(result.newState.isRecommendMode)
        assertEquals(1, result.newState.recommendPage)

        // Given
        val recommendIntent = HomeIntent.SelectCategoryFilter("推荐")

        // When
        val recommendResult = reducer.reduce(result.newState, recommendIntent)

        // Then
        assertEquals("推荐", recommendResult.newState.selectedCategoryFilter)
        assertTrue(recommendResult.newState.isRecommendMode)
    }

    @Test
    fun `test SelectRankType intent should update rank type and set loading`() {
        // Given
        val intent = HomeIntent.SelectRankType("更新榜")

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertEquals("更新榜", result.newState.selectedRankType)
        assertTrue(result.newState.rankLoading)
    }

    @Test
    fun `test HomeRecommendBooksLoadSuccess intent should update books and clear loading`() {
        // Given
        val loadingState = initialState.copy(isLoading = true, isRefreshing = true)
        val books = listOf(
            HomeService.HomeBook(type = 2, bookId = 1L, bookName = "Book A", authorName = "Author A", picUrl = "url", bookDesc = "desc"),
            HomeService.HomeBook(type = 3, bookId = 2L, bookName = "Book B", authorName = "Author B", picUrl = "url", bookDesc = "desc")
        )
        val intent = HomeIntent.HomeRecommendBooksLoadSuccess(books, isRefresh = true, hasMore = true)

        // When
        val result = reducer.reduce(loadingState, intent)

        // Then
        assertEquals(books, result.newState.homeRecommendBooks)
        assertFalse(result.newState.isLoading)
        assertFalse(result.newState.isRefreshing)
        assertTrue(result.newState.hasMoreHomeRecommend)
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess intent should add books on load more`() {
        // Given
        val initialBooks = listOf(
            SearchService.BookInfo(id = 1, categoryId = 1, categoryName = "cat", picUrl = "url", bookName = "Initial Book", authorId = 1, authorName = "author", bookDesc = "desc", bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, firstChapterId = 1, lastChapterId = 10, lastChapterName = "ch10", updateTime = "2024-01-01")
        )
        val loadedState = initialState.copy(recommendBooks = initialBooks, recommendPage = 1)
        val moreBooks = listOf(
            SearchService.BookInfo(id = 2, categoryId = 1, categoryName = "cat", picUrl = "url", bookName = "More Book", authorId = 1, authorName = "author", bookDesc = "desc", bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, firstChapterId = 1, lastChapterId = 10, lastChapterName = "ch10", updateTime = "2024-01-01")
        )
        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(moreBooks, isLoadMore = true, hasMore = false, totalPages = 1)

        // When
        val result = reducer.reduce(loadedState, intent)

        // Then
        assertEquals(initialBooks + moreBooks, result.newState.recommendBooks)
        assertEquals(2, result.newState.recommendPage)
        assertFalse(result.newState.hasMoreRecommend)
    }

    @Test
    fun `test RankBooksLoadSuccess intent should update rank books and clear loading`() {
        // Given
        val loadingState = initialState.copy(rankLoading = true)
        val rankBooks = listOf(
            BookService.BookRank(id = 1, categoryId = 1, categoryName = "cat", picUrl = "url", bookName = "Rank Book 1", authorName = "author", bookDesc = "desc", wordCount = 1000, lastChapterName = "ch10", lastChapterUpdateTime = "2024-01-01")
        )
        val intent = HomeIntent.RankBooksLoadSuccess(rankBooks)

        // When
        val result = reducer.reduce(loadingState, intent)

        // Then
        assertEquals(rankBooks, result.newState.rankBooks)
        assertFalse(result.newState.rankLoading)
    }

    @Test
    fun `test LoadFailure intents should set error message`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val intents = listOf(
            HomeIntent.CategoryFiltersLoadFailure("Error 1"),
            HomeIntent.HomeRecommendBooksLoadFailure("Error 2"),
            HomeIntent.CategoryRecommendBooksLoadFailure("Error 3"),
            HomeIntent.RankBooksLoadFailure("Error 4"),
            HomeIntent.CategoriesLoadFailure("Error 5"),
            HomeIntent.BooksLoadFailure("Error 6")
        )

        // When & Then
        intents.forEach { intent ->
            val result = reducer.reduce(loadingState, intent)
            assertNotNull(result.newState.error)
            assertFalse(result.newState.isLoading)
            assertFalse(result.newState.isRefreshing)
        }
    }

    @Test
    fun `test NavigateToBookDetail should produce NavigateToBookDetail effect`() {
        // Given
        val intent = HomeIntent.NavigateToBookDetail(123L)

        // When
        val result = reducer.reduce(initialState, intent)

        // Then
        assertEquals(initialState, result.newState)
        assertTrue(result.effect is HomeEffect.NavigateToBookDetail)
        assertEquals(123L, (result.effect as HomeEffect.NavigateToBookDetail).bookId)
    }

    @Test
    fun `test CategoryFiltersLoadSuccess should update categoryFilters correctly`() {
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
    fun `test LoadMoreRecommend intent should update loading state correctly`() {
        // Given - 在推荐模式下
        val recommendModeState = initialState.copy(isRecommendMode = true, homeRecommendPage = 1)
        val intent = HomeIntent.LoadMoreRecommend

        // When
        val result = reducer.reduce(recommendModeState, intent)

        // Then
        assertTrue(result.newState.homeRecommendLoading)
        assertEquals(2, result.newState.homeRecommendPage)

        // Given - 在分类模式下
        val categoryModeState = initialState.copy(isRecommendMode = false, recommendPage = 1)

        // When
        val categoryResult = reducer.reduce(categoryModeState, intent)

        // Then
        assertTrue(categoryResult.newState.recommendLoading)
        assertEquals(1, categoryResult.newState.recommendPage) // 分类模式下recommendPage不变
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess should append books on load more`() {
        // Given - 已有初始数据的状态
        val initialBooks = listOf(
            SearchService.BookInfo(id = 1, categoryId = 1, categoryName = "cat", picUrl = "url1", bookName = "Book 1", authorId = 1, authorName = "author1", bookDesc = "desc1", bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, firstChapterId = 1, lastChapterId = 10, lastChapterName = "ch10", updateTime = "2024-01-01"),
            SearchService.BookInfo(id = 2, categoryId = 1, categoryName = "cat", picUrl = "url2", bookName = "Book 2", authorId = 2, authorName = "author2", bookDesc = "desc2", bookStatus = 0, visitCount = 200, wordCount = 2000, commentCount = 20, firstChapterId = 1, lastChapterId = 20, lastChapterName = "ch20", updateTime = "2024-01-02")
        )
        val stateWithBooks = initialState.copy(
            recommendBooks = initialBooks,
            recommendPage = 1,
            recommendLoading = true
        )

        val moreBooks = listOf(
            SearchService.BookInfo(id = 3, categoryId = 1, categoryName = "cat", picUrl = "url3", bookName = "Book 3", authorId = 3, authorName = "author3", bookDesc = "desc3", bookStatus = 0, visitCount = 300, wordCount = 3000, commentCount = 30, firstChapterId = 1, lastChapterId = 30, lastChapterName = "ch30", updateTime = "2024-01-03"),
            SearchService.BookInfo(id = 4, categoryId = 1, categoryName = "cat", picUrl = "url4", bookName = "Book 4", authorId = 4, authorName = "author4", bookDesc = "desc4", bookStatus = 0, visitCount = 400, wordCount = 4000, commentCount = 40, firstChapterId = 1, lastChapterId = 40, lastChapterName = "ch40", updateTime = "2024-01-04")
        )

        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = moreBooks,
            isLoadMore = true,
            hasMore = true,
            totalPages = 3
        )

        // When
        val result = reducer.reduce(stateWithBooks, intent)

        // Then
        assertEquals(initialBooks + moreBooks, result.newState.recommendBooks)
        assertEquals(4, result.newState.recommendBooks.size)
        assertEquals(2, result.newState.recommendPage) // 页码应该增加
        assertTrue(result.newState.hasMoreRecommend)
        assertEquals(3, result.newState.totalRecommendPages)
        assertFalse(result.newState.recommendLoading)
        assertFalse(result.newState.isLoading)
        assertFalse(result.newState.isRefreshing)
    }

    @Test
    fun `test HomeRecommendBooksLoadSuccess should handle load more correctly`() {
        // Given - 已有首页推荐数据的状态
        val initialBooks = listOf(
            HomeService.HomeBook(type = 3, bookId = 1L, bookName = "Home Book 1", authorName = "Author 1", picUrl = "url1", bookDesc = "desc1"),
            HomeService.HomeBook(type = 3, bookId = 2L, bookName = "Home Book 2", authorName = "Author 2", picUrl = "url2", bookDesc = "desc2")
        )
        val stateWithBooks = initialState.copy(
            homeRecommendBooks = initialBooks,
            homeRecommendPage = 1,
            homeRecommendLoading = true
        )

        val moreBooks = listOf(
            HomeService.HomeBook(type = 3, bookId = 3L, bookName = "Home Book 3", authorName = "Author 3", picUrl = "url3", bookDesc = "desc3"),
            HomeService.HomeBook(type = 3, bookId = 4L, bookName = "Home Book 4", authorName = "Author 4", picUrl = "url4", bookDesc = "desc4")
        )

        val intent = HomeIntent.HomeRecommendBooksLoadSuccess(
            books = moreBooks,
            isRefresh = false, // 加载更多，不是刷新
            hasMore = false
        )

        // When
        val result = reducer.reduce(stateWithBooks, intent)

        // Then
        assertEquals(initialBooks + moreBooks, result.newState.homeRecommendBooks)
        assertEquals(4, result.newState.homeRecommendBooks.size)
        assertFalse(result.newState.hasMoreHomeRecommend)
        assertFalse(result.newState.homeRecommendLoading)
        assertFalse(result.newState.isLoading)
        assertFalse(result.newState.isRefreshing)
    }

    @Test
    fun `test SelectCategoryFilter should reset page and clear books when switching categories`() {
        // Given - 有推荐书籍数据的状态
        val stateWithData = initialState.copy(
            selectedCategoryFilter = "玄幻",
            isRecommendMode = false,
            recommendBooks = listOf(
                SearchService.BookInfo(id = 1, categoryId = 1, categoryName = "玄幻", picUrl = "url", bookName = "Book 1", authorId = 1, authorName = "author", bookDesc = "desc", bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, firstChapterId = 1, lastChapterId = 10, lastChapterName = "ch10", updateTime = "2024-01-01")
            ),
            recommendPage = 3
        )

        val intent = HomeIntent.SelectCategoryFilter("推荐")

        // When
        val result = reducer.reduce(stateWithData, intent)

        // Then
        assertEquals("推荐", result.newState.selectedCategoryFilter)
        assertTrue(result.newState.isRecommendMode)
        assertTrue(result.newState.recommendBooks.isEmpty()) // 切换到推荐模式时清空分类书籍
        assertEquals(1, result.newState.recommendPage) // 页码重置为1
    }
} 