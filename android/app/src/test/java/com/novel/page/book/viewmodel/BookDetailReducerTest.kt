package com.novel.page.book.viewmodel

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * BookDetailReducer单元测试
 * 
 * 验证所有Intent的状态转换逻辑正确性
 */
class BookDetailReducerTest {
    
    private lateinit var reducer: BookDetailReducer
    private lateinit var initialState: BookDetailState
    
    @Before
    fun setUp() {
        reducer = BookDetailReducer()
        initialState = BookDetailState()
    }
    
    @Test
    fun `测试LoadBookDetail Intent - 应该设置加载状态`() {
        // Given
        val intent = BookDetailIntent.LoadBookDetail("123", true)
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isLoading)
        assertNull(result.newState.error)
        assertEquals("123", result.newState.currentBookId)
        assertNull(result.effect)
    }
    
    @Test
    fun `测试RefreshBookDetail Intent - 应该设置加载状态并清除错误`() {
        // Given
        val stateWithError = initialState.copy(error = "网络错误")
        val intent = BookDetailIntent.RefreshBookDetail("123")
        
        // When
        val result = reducer.reduce(stateWithError, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isLoading)
        assertNull(result.newState.error)
        assertEquals("123", result.newState.currentBookId)
    }
    
    @Test
    fun `测试BookInfoLoadSuccess Intent - 应该设置书籍信息并清除加载状态`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val bookInfo = BookDetailState.BookInfo(
            id = "123",
            bookName = "测试书籍",
            authorName = "测试作者",
            bookDesc = "测试描述",
            picUrl = "test.jpg",
            visitCount = 1000L,
            wordCount = 50000,
            categoryName = "玄幻"
        )
        val reviews = listOf(
            BookDetailState.BookReview("1", "好书", 5, "1小时后", "用户1")
        )
        val intent = BookDetailIntent.BookInfoLoadSuccess(bookInfo, reviews)
        
        // When
        val result = reducer.reduce(loadingState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertFalse(result.newState.isLoading)
        assertNull(result.newState.error)
        assertEquals(bookInfo, result.newState.bookInfo)
        assertEquals(reviews, result.newState.reviews)
        assertNull(result.effect)
    }
    
    @Test
    fun `测试LastChapterLoadSuccess Intent - 应该设置最新章节信息`() {
        // Given
        val lastChapter = BookDetailState.LastChapter("第一章", "2024-01-01")
        val intent = BookDetailIntent.LastChapterLoadSuccess(lastChapter)
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertEquals(lastChapter, result.newState.lastChapter)
        assertNull(result.effect)
    }
    
    @Test
    fun `测试LoadFailure Intent - 应该设置错误状态`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val intent = BookDetailIntent.LoadFailure("网络错误")
        
        // When
        val result = reducer.reduce(loadingState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertFalse(result.newState.isLoading)
        assertEquals("网络错误", result.newState.error)
        assertNull(result.effect)
    }
    
    @Test
    fun `测试ToggleDescriptionExpanded Intent - 应该切换展开状态`() {
        // Given - 初始为未展开
        val intent = BookDetailIntent.ToggleDescriptionExpanded
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isDescriptionExpanded)
        
        // 再次切换应该变为false
        val result2 = reducer.reduce(result.newState, intent)
        assertEquals(2L, result2.newState.version)
        assertFalse(result2.newState.isDescriptionExpanded)
    }
    
    @Test
    fun `测试StartReading Intent - 应该产生导航Effect且状态不变`() {
        // Given
        val intent = BookDetailIntent.StartReading("123", "chapter1")
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(initialState, result.newState) // 状态不变
        assertNotNull(result.effect)
        assertTrue(result.effect is BookDetailEffect.NavigateToReader)
        val effect = result.effect as BookDetailEffect.NavigateToReader
        assertEquals("123", effect.bookId)
        assertEquals("chapter1", effect.chapterId)
    }
    
    @Test
    fun `测试AddToBookshelf Intent - 应该更新书架状态并显示Toast`() {
        // Given
        val intent = BookDetailIntent.AddToBookshelf("123")
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isInBookshelf)
        assertNotNull(result.effect)
        assertTrue(result.effect is BookDetailEffect.ShowToast)
        val effect = result.effect as BookDetailEffect.ShowToast
        assertEquals("已添加到书架", effect.message)
    }
    
    @Test
    fun `测试RemoveFromBookshelf Intent - 应该更新书架状态并显示Toast`() {
        // Given
        val stateInBookshelf = initialState.copy(isInBookshelf = true)
        val intent = BookDetailIntent.RemoveFromBookshelf("123")
        
        // When
        val result = reducer.reduce(stateInBookshelf, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertFalse(result.newState.isInBookshelf)
        assertNotNull(result.effect)
        assertTrue(result.effect is BookDetailEffect.ShowToast)
        val effect = result.effect as BookDetailEffect.ShowToast
        assertEquals("已从书架移除", effect.message)
    }
    
    @Test
    fun `测试ShareBook Intent - 应该产生分享Effect且状态不变`() {
        // Given
        val intent = BookDetailIntent.ShareBook("123", "测试书籍")
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(initialState, result.newState) // 状态不变
        assertNotNull(result.effect)
        assertTrue(result.effect is BookDetailEffect.ShareBook)
        val effect = result.effect as BookDetailEffect.ShareBook
        assertEquals("测试书籍", effect.title)
        assertEquals("推荐一本好书：测试书籍", effect.content)
    }
    
    @Test
    fun `测试FollowAuthor Intent - 应该更新关注状态并显示Toast`() {
        // Given
        val intent = BookDetailIntent.FollowAuthor("测试作者")
        
        // When
        val result = reducer.reduce(initialState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isAuthorFollowed)
        assertNotNull(result.effect)
        assertTrue(result.effect is BookDetailEffect.ShowToast)
        val effect = result.effect as BookDetailEffect.ShowToast
        assertEquals("已关注作者：测试作者", effect.message)
    }
    
    @Test
    fun `测试RetryLoading Intent - 应该重置为加载状态`() {
        // Given
        val errorState = initialState.copy(error = "网络错误")
        val intent = BookDetailIntent.RetryLoading("123")
        
        // When
        val result = reducer.reduce(errorState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertTrue(result.newState.isLoading)
        assertNull(result.newState.error)
        assertEquals("123", result.newState.currentBookId)
    }
    
    @Test
    fun `测试ClearError Intent - 应该清除错误状态`() {
        // Given
        val errorState = initialState.copy(error = "网络错误")
        val intent = BookDetailIntent.ClearError
        
        // When
        val result = reducer.reduce(errorState, intent)
        
        // Then
        assertEquals(1L, result.newState.version)
        assertNull(result.newState.error)
    }
    
    @Test
    fun `测试状态版本递增 - 每次状态变更都应该递增版本号`() {
        // Given
        var currentState = initialState
        val intents = listOf(
            BookDetailIntent.LoadBookDetail("123"),
            BookDetailIntent.ToggleDescriptionExpanded,
            BookDetailIntent.AddToBookshelf("123"),
            BookDetailIntent.ClearError
        )
        
        // When & Then
        intents.forEachIndexed { index, intent ->
            val result = reducer.reduce(currentState, intent)
            assertEquals((index + 1).toLong(), result.newState.version)
            currentState = result.newState
        }
    }
} 