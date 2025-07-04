package com.novel.page.home.viewmodel

import com.novel.utils.network.api.front.SearchService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Home页面分类映射和加载更多功能测试
 * 
 * 验证修复的问题：
 * 1. 分类ID映射正确性
 * 2. 加载更多状态处理
 * 3. 数据显示逻辑
 */
class HomeCategoryMappingTest {

    private lateinit var reducer: HomeReducer
    private lateinit var initialState: HomeState

    @Before
    fun setUp() {
        reducer = HomeReducer()
        initialState = HomeState()
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess with loadMore correctly appends data`() {
        // Given - 初始状态有一些数据
        val existingBooks = listOf(
            createSearchBook(1L, "书籍1"),
            createSearchBook(2L, "书籍2")
        )
        val stateWithData = initialState.copy(
            recommendBooks = existingBooks,
            recommendPage = 1,
            hasMoreRecommend = true
        )
        
        // When - 加载更多成功
        val newBooks = listOf(
            createSearchBook(3L, "书籍3"),
            createSearchBook(4L, "书籍4")
        )
        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = newBooks,
            isLoadMore = true,
            hasMore = true,
            totalPages = 3
        )
        
        val result = reducer.reduce(stateWithData, intent)
        
        // Then - 验证数据正确追加
        assertEquals(4, result.newState.recommendBooks.size)
        assertEquals("书籍1", result.newState.recommendBooks[0].bookName)
        assertEquals("书籍2", result.newState.recommendBooks[1].bookName)
        assertEquals("书籍3", result.newState.recommendBooks[2].bookName)
        assertEquals("书籍4", result.newState.recommendBooks[3].bookName)
        
        // 验证状态正确更新
        assertFalse(result.newState.recommendLoading)
        assertTrue(result.newState.hasMoreRecommend)
        assertEquals(2, result.newState.recommendPage) // 页码应该增加
        assertEquals(3, result.newState.totalRecommendPages)
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess with no more data sets hasMore to false`() {
        // Given - 已有数据的状态
        val existingBooks = listOf(
            createSearchBook(1L, "书籍1"),
            createSearchBook(2L, "书籍2")
        )
        val stateWithData = initialState.copy(
            recommendBooks = existingBooks,
            recommendPage = 2,
            hasMoreRecommend = true
        )
        
        // When - 加载更多，但没有更多数据了
        val newBooks = listOf(
            createSearchBook(3L, "书籍3")
        )
        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = newBooks,
            isLoadMore = true,
            hasMore = false, // 没有更多数据了
            totalPages = 3
        )
        
        val result = reducer.reduce(stateWithData, intent)
        
        // Then - 验证状态正确
        assertEquals(3, result.newState.recommendBooks.size)
        assertFalse(result.newState.recommendLoading)
        assertFalse(result.newState.hasMoreRecommend) // 应该设置为false
        assertEquals(3, result.newState.recommendPage)
    }

    @Test
    fun `test CategoryRecommendBooksLoadSuccess without loadMore replaces data`() {
        // Given - 已有数据的状态
        val existingBooks = listOf(
            createSearchBook(1L, "旧书1"),
            createSearchBook(2L, "旧书2")
        )
        val stateWithData = initialState.copy(
            recommendBooks = existingBooks,
            recommendPage = 2
        )
        
        // When - 非加载更多模式（刷新或重新加载）
        val newBooks = listOf(
            createSearchBook(3L, "新书1"),
            createSearchBook(4L, "新书2")
        )
        val intent = HomeIntent.CategoryRecommendBooksLoadSuccess(
            books = newBooks,
            isLoadMore = false, // 不是加载更多
            hasMore = true,
            totalPages = 2
        )
        
        val result = reducer.reduce(stateWithData, intent)
        
        // Then - 验证数据被替换而不是追加
        assertEquals(2, result.newState.recommendBooks.size)
        assertEquals("新书1", result.newState.recommendBooks[0].bookName)
        assertEquals("新书2", result.newState.recommendBooks[1].bookName)
        
        // 页码应该重置为1
        assertEquals(1, result.newState.recommendPage)
    }

    @Test
    fun `test SelectCategoryFilter clears recommend books when switching categories`() {
        // Given - 分类模式下有推荐书籍数据
        val existingBooks = listOf(
            createSearchBook(1L, "玄幻书1"),
            createSearchBook(2L, "玄幻书2")
        )
        val stateWithData = initialState.copy(
            isRecommendMode = false,
            selectedCategoryFilter = "玄幻奇幻",
            recommendBooks = existingBooks,
            recommendPage = 2
        )
        
        // When - 切换到其他分类
        val intent = HomeIntent.SelectCategoryFilter("都市言情")
        val result = reducer.reduce(stateWithData, intent)
        
        // Then - 推荐书籍应该被清空，页码重置
        assertTrue(result.newState.recommendBooks.isEmpty())
        assertEquals(1, result.newState.recommendPage)
        assertEquals("都市言情", result.newState.selectedCategoryFilter)
        assertFalse(result.newState.isRecommendMode) // 保持分类模式
        assertTrue(result.newState.hasMoreRecommend) // 重置为true
    }

    @Test
    fun `test SelectCategoryFilter switches to recommend mode when selecting 推荐`() {
        // Given - 分类模式状态
        val stateInCategoryMode = initialState.copy(
            isRecommendMode = false,
            selectedCategoryFilter = "玄幻奇幻",
            recommendBooks = listOf(createSearchBook(1L, "测试书"))
        )
        
        // When - 选择"推荐"
        val intent = HomeIntent.SelectCategoryFilter("推荐")
        val result = reducer.reduce(stateInCategoryMode, intent)
        
        // Then - 应该切换到推荐模式
        assertTrue(result.newState.isRecommendMode)
        assertEquals("推荐", result.newState.selectedCategoryFilter)
        assertTrue(result.newState.recommendBooks.isEmpty()) // 清空分类推荐数据
    }

    @Test
    fun `test LoadMoreRecommend updates loading state correctly for category mode`() {
        // Given - 分类模式状态
        val stateInCategoryMode = initialState.copy(
            isRecommendMode = false,
            selectedCategoryFilter = "玄幻奇幻",
            recommendLoading = false,
            hasMoreRecommend = true
        )
        
        // When - 触发加载更多
        val intent = HomeIntent.LoadMoreRecommend
        val result = reducer.reduce(stateInCategoryMode, intent)
        
        // Then - 加载状态应该更新
        assertTrue(result.newState.recommendLoading)
        // 在分类模式下，页码在成功回调时才增加
        assertEquals(stateInCategoryMode.recommendPage, result.newState.recommendPage)
    }

    @Test
    fun `test LoadMoreRecommend updates loading state correctly for recommend mode`() {
        // Given - 推荐模式状态
        val stateInRecommendMode = initialState.copy(
            isRecommendMode = true,
            homeRecommendLoading = false,
            hasMoreHomeRecommend = true,
            homeRecommendPage = 1
        )
        
        // When - 触发加载更多
        val intent = HomeIntent.LoadMoreRecommend
        val result = reducer.reduce(stateInRecommendMode, intent)
        
        // Then - 首页推荐加载状态应该更新
        assertTrue(result.newState.homeRecommendLoading)
        assertEquals(2, result.newState.homeRecommendPage) // 推荐模式下页码立即增加
    }

    @Test
    fun `test error handling preserves existing data and resets loading states`() {
        // Given - 有数据和加载状态的状态
        val stateWithDataAndLoading = initialState.copy(
            recommendBooks = listOf(createSearchBook(1L, "测试书")),
            recommendLoading = true,
            hasMoreRecommend = true
        )
        
        // When - 加载失败
        val intent = HomeIntent.CategoryRecommendBooksLoadFailure("网络错误")
        val result = reducer.reduce(stateWithDataAndLoading, intent)
        
        // Then - 数据应该保持，加载状态重置
        assertEquals(1, result.newState.recommendBooks.size)
        assertFalse(result.newState.recommendLoading)
        assertTrue(result.newState.hasMoreRecommend) // 失败后还可以重试
        assertEquals("网络错误", result.newState.error)
    }

    // Helper method
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
            bookStatus = 0,
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