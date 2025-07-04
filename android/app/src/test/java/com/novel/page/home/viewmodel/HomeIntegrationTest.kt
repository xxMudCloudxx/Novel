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
 * Home模块集成测试
 * 
 * 验证修复后的功能：
 * 1. 分类筛选器数据能正确更新到categoryFilters
 * 2. 上拉加载更多能正确加载新数据
 */
class HomeIntegrationTest {

    private lateinit var reducer: HomeReducer
    private lateinit var initialState: HomeState

    @Before
    fun setUp() {
        reducer = HomeReducer()
        initialState = HomeState()
    }

    @Test
    fun `test complete category filter workflow`() {
        // 1. 初始状态只有默认推荐分类
        assertEquals(1, initialState.categoryFilters.size)
        assertEquals("推荐", initialState.categoryFilters[0].name)

        // 2. 模拟分类筛选器加载成功
        val newFilters = listOf(
            CategoryInfo("0", "推荐"),
            CategoryInfo("1", "玄幻"),
            CategoryInfo("2", "言情"),
            CategoryInfo("3", "都市"),
            CategoryInfo("4", "历史")
        )
        val loadFiltersIntent = HomeIntent.CategoryFiltersLoadSuccess(newFilters)
        val stateAfterLoad = reducer.reduce(initialState, loadFiltersIntent)

        // 3. 验证分类筛选器数据已正确更新
        assertEquals(5, stateAfterLoad.newState.categoryFilters.size)
        assertEquals("推荐", stateAfterLoad.newState.categoryFilters[0].name)
        assertEquals("玄幻", stateAfterLoad.newState.categoryFilters[1].name)
        assertEquals("言情", stateAfterLoad.newState.categoryFilters[2].name)
        assertEquals("都市", stateAfterLoad.newState.categoryFilters[3].name)
        assertEquals("历史", stateAfterLoad.newState.categoryFilters[4].name)
        assertFalse(stateAfterLoad.newState.categoryFiltersLoading)

        // 4. 模拟用户选择玄幻分类
        val selectFilterIntent = HomeIntent.SelectCategoryFilter("玄幻")
        val stateAfterSelect = reducer.reduce(stateAfterLoad.newState, selectFilterIntent)

        // 5. 验证分类切换正确
        assertEquals("玄幻", stateAfterSelect.newState.selectedCategoryFilter)
        assertFalse(stateAfterSelect.newState.isRecommendMode)
        assertEquals(1, stateAfterSelect.newState.recommendPage)
        assertTrue(stateAfterSelect.newState.recommendBooks.isEmpty())
    }

    @Test
    fun `test complete load more workflow for category mode`() {
        // 1. 设置初始状态 - 分类模式，已有一些书籍
        val initialBooks = listOf(
            SearchService.BookInfo(
                id = 1, categoryId = 1, categoryName = "玄幻", picUrl = "url1", 
                bookName = "初始书籍1", authorId = 1, authorName = "作者1", bookDesc = "描述1", 
                bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, 
                firstChapterId = 1, lastChapterId = 10, lastChapterName = "第10章", 
                updateTime = "2024-01-01"
            ),
            SearchService.BookInfo(
                id = 2, categoryId = 1, categoryName = "玄幻", picUrl = "url2", 
                bookName = "初始书籍2", authorId = 2, authorName = "作者2", bookDesc = "描述2", 
                bookStatus = 0, visitCount = 200, wordCount = 2000, commentCount = 20, 
                firstChapterId = 1, lastChapterId = 20, lastChapterName = "第20章", 
                updateTime = "2024-01-02"
            )
        )
        
        val stateWithBooks = initialState.copy(
            selectedCategoryFilter = "玄幻",
            isRecommendMode = false,
            recommendBooks = initialBooks,
            recommendPage = 1,
            hasMoreRecommend = true
        )

        // 2. 模拟触发加载更多
        val loadMoreIntent = HomeIntent.LoadMoreRecommend
        val stateAfterLoadMore = reducer.reduce(stateWithBooks, loadMoreIntent)

        // 3. 验证加载状态正确设置
        assertTrue(stateAfterLoadMore.newState.recommendLoading)
        assertEquals(1, stateAfterLoadMore.newState.recommendPage) // 分类模式下页码不变

        // 4. 模拟加载更多数据成功
        val moreBooks = listOf(
            SearchService.BookInfo(
                id = 3, categoryId = 1, categoryName = "玄幻", picUrl = "url3", 
                bookName = "加载更多书籍1", authorId = 3, authorName = "作者3", bookDesc = "描述3", 
                bookStatus = 0, visitCount = 300, wordCount = 3000, commentCount = 30, 
                firstChapterId = 1, lastChapterId = 30, lastChapterName = "第30章", 
                updateTime = "2024-01-03"
            ),
            SearchService.BookInfo(
                id = 4, categoryId = 1, categoryName = "玄幻", picUrl = "url4", 
                bookName = "加载更多书籍2", authorId = 4, authorName = "作者4", bookDesc = "描述4", 
                bookStatus = 0, visitCount = 400, wordCount = 4000, commentCount = 40, 
                firstChapterId = 1, lastChapterId = 40, lastChapterName = "第40章", 
                updateTime = "2024-01-04"
            )
        )

        val loadSuccessIntent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = moreBooks,
            isLoadMore = true,
            hasMore = true,
            totalPages = 3
        )
        val finalState = reducer.reduce(stateAfterLoadMore.newState, loadSuccessIntent)

        // 5. 验证数据正确追加
        assertEquals(4, finalState.newState.recommendBooks.size)
        assertEquals("初始书籍1", finalState.newState.recommendBooks[0].bookName)
        assertEquals("初始书籍2", finalState.newState.recommendBooks[1].bookName)
        assertEquals("加载更多书籍1", finalState.newState.recommendBooks[2].bookName)
        assertEquals("加载更多书籍2", finalState.newState.recommendBooks[3].bookName)
        assertEquals(2, finalState.newState.recommendPage)
        assertTrue(finalState.newState.hasMoreRecommend)
        assertFalse(finalState.newState.recommendLoading)
    }

    @Test
    fun `test complete load more workflow for recommend mode`() {
        // 1. 设置初始状态 - 推荐模式，已有一些书籍
        val initialBooks = listOf(
            HomeService.HomeBook(
                type = 3, bookId = 1L, bookName = "首页推荐1", 
                authorName = "作者1", picUrl = "url1", bookDesc = "描述1"
            ),
            HomeService.HomeBook(
                type = 3, bookId = 2L, bookName = "首页推荐2", 
                authorName = "作者2", picUrl = "url2", bookDesc = "描述2"
            )
        )
        
        val stateWithBooks = initialState.copy(
            selectedCategoryFilter = "推荐",
            isRecommendMode = true,
            homeRecommendBooks = initialBooks,
            homeRecommendPage = 1,
            hasMoreHomeRecommend = true
        )

        // 2. 模拟触发加载更多
        val loadMoreIntent = HomeIntent.LoadMoreRecommend
        val stateAfterLoadMore = reducer.reduce(stateWithBooks, loadMoreIntent)

        // 3. 验证加载状态正确设置
        assertTrue(stateAfterLoadMore.newState.homeRecommendLoading)
        assertEquals(2, stateAfterLoadMore.newState.homeRecommendPage) // 推荐模式下页码增加

        // 4. 模拟加载更多数据成功
        val moreBooks = listOf(
            HomeService.HomeBook(
                type = 3, bookId = 3L, bookName = "首页推荐3", 
                authorName = "作者3", picUrl = "url3", bookDesc = "描述3"
            ),
            HomeService.HomeBook(
                type = 3, bookId = 4L, bookName = "首页推荐4", 
                authorName = "作者4", picUrl = "url4", bookDesc = "描述4"
            )
        )

        val loadSuccessIntent = HomeIntent.HomeRecommendBooksLoadSuccess(
            books = moreBooks,
            isRefresh = false,
            hasMore = false
        )
        val finalState = reducer.reduce(stateAfterLoadMore.newState, loadSuccessIntent)

        // 5. 验证数据正确追加
        assertEquals(4, finalState.newState.homeRecommendBooks.size)
        assertEquals("首页推荐1", finalState.newState.homeRecommendBooks[0].bookName)
        assertEquals("首页推荐2", finalState.newState.homeRecommendBooks[1].bookName)
        assertEquals("首页推荐3", finalState.newState.homeRecommendBooks[2].bookName)
        assertEquals("首页推荐4", finalState.newState.homeRecommendBooks[3].bookName)
        assertFalse(finalState.newState.hasMoreHomeRecommend)
        assertFalse(finalState.newState.homeRecommendLoading)
    }

    @Test
    fun `test error handling in category filter loading`() {
        // 1. 模拟分类筛选器加载失败
        val loadFailureIntent = HomeIntent.CategoryFiltersLoadFailure("网络错误")
        val stateAfterFailure = reducer.reduce(initialState, loadFailureIntent)

        // 2. 验证错误状态正确设置
        assertFalse(stateAfterFailure.newState.categoryFiltersLoading)
        assertFalse(stateAfterFailure.newState.isLoading)
        assertFalse(stateAfterFailure.newState.isRefreshing)
        assertEquals("网络错误", stateAfterFailure.newState.error)
        // 分类筛选器应该保持默认状态
        assertEquals(1, stateAfterFailure.newState.categoryFilters.size)
        assertEquals("推荐", stateAfterFailure.newState.categoryFilters[0].name)
    }

    @Test
    fun `test switching between recommend and category modes`() {
        // 1. 设置有数据的状态
        val stateWithData = initialState.copy(
            selectedCategoryFilter = "玄幻",
            isRecommendMode = false,
            recommendBooks = listOf(
                SearchService.BookInfo(
                    id = 1, categoryId = 1, categoryName = "玄幻", picUrl = "url", 
                    bookName = "玄幻书籍", authorId = 1, authorName = "作者", bookDesc = "描述", 
                    bookStatus = 0, visitCount = 100, wordCount = 1000, commentCount = 10, 
                    firstChapterId = 1, lastChapterId = 10, lastChapterName = "第10章", 
                    updateTime = "2024-01-01"
                )
            ),
            homeRecommendBooks = listOf(
                HomeService.HomeBook(
                    type = 3, bookId = 1L, bookName = "推荐书籍", 
                    authorName = "作者", picUrl = "url", bookDesc = "描述"
                )
            ),
            recommendPage = 3
        )

        // 2. 切换到推荐模式
        val switchToRecommendIntent = HomeIntent.SelectCategoryFilter("推荐")
        val stateAfterSwitch = reducer.reduce(stateWithData, switchToRecommendIntent)

        // 3. 验证切换正确
        assertEquals("推荐", stateAfterSwitch.newState.selectedCategoryFilter)
        assertTrue(stateAfterSwitch.newState.isRecommendMode)
        assertTrue(stateAfterSwitch.newState.recommendBooks.isEmpty()) // 分类书籍被清空
        assertEquals(1, stateAfterSwitch.newState.recommendPage) // 页码重置
        // 首页推荐书籍保持不变
        assertEquals(1, stateAfterSwitch.newState.homeRecommendBooks.size)
        assertEquals("推荐书籍", stateAfterSwitch.newState.homeRecommendBooks[0].bookName)
    }
} 