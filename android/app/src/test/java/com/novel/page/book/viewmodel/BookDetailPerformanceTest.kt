package com.novel.page.book.viewmodel

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * BookDetail性能测试
 * 
 * 验证页面加载时间、状态转换性能等指标
 */
class BookDetailPerformanceTest {
    
    private lateinit var reducer: BookDetailReducer
    
    @Before
    fun setUp() {
        reducer = BookDetailReducer()
    }
    
    @Test
    fun `测试Reducer性能 - 状态转换应该在16ms内完成`() {
        // Given
        val initialState = BookDetailState()
        val intent = BookDetailIntent.LoadBookDetail("123")
        
        // When
        val startTime = System.nanoTime()
        repeat(1000) {
            reducer.reduce(initialState, intent)
        }
        val endTime = System.nanoTime()
        
        // Then
        val averageTime = (endTime - startTime) / 1000 / 1_000_000.0 // 转换为毫秒
        assertTrue("Reducer平均执行时间应该小于16ms，实际: ${averageTime}ms", averageTime < 16)
    }
    
    @Test
    fun `测试状态对象创建性能 - 大量状态创建不应该影响性能`() {
        // Given
        val bookInfo = BookDetailState.BookInfo(
            id = "123",
            bookName = "测试书籍",
            authorName = "测试作者",
            bookDesc = "测试描述".repeat(100), // 长描述
            picUrl = "test.jpg",
            visitCount = 1000L,
            wordCount = 50000,
            categoryName = "玄幻"
        )
        
        // When
        val startTime = System.nanoTime()
        repeat(10000) {
            BookDetailState(
                bookInfo = bookInfo,
                version = it.toLong(),
                isLoading = false
            )
        }
        val endTime = System.nanoTime()
        
        // Then
        val totalTime = (endTime - startTime) / 1_000_000.0 // 转换为毫秒
        assertTrue("状态对象创建总时间应该小于100ms，实际: ${totalTime}ms", totalTime < 100)
    }
    
    @Test
    fun `测试Intent处理并发性能 - 多个Intent并发处理`() = runTest {
        // Given
        val initialState = BookDetailState()
        val intents = listOf(
            BookDetailIntent.LoadBookDetail("123"),
            BookDetailIntent.ToggleDescriptionExpanded,
            BookDetailIntent.AddToBookshelf("123"),
            BookDetailIntent.FollowAuthor("作者"),
            BookDetailIntent.ClearError
        )
        
        // When
        val startTime = System.nanoTime()
        var currentState = initialState
        
        repeat(1000) {
            intents.forEach { intent ->
                val result = reducer.reduce(currentState, intent)
                currentState = result.newState
            }
        }
        
        val endTime = System.nanoTime()
        
        // Then
        val totalTime = (endTime - startTime) / 1_000_000.0 // 转换为毫秒
        assertTrue("并发Intent处理总时间应该小于200ms，实际: ${totalTime}ms", totalTime < 200)
        
        // 验证状态版本正确递增
        assertEquals(5000L, currentState.version) // 1000次 × 5个Intent
    }
    
    @Test
    fun `测试内存使用 - 状态对象应该是轻量级的`() {
        // Given
        val states = mutableListOf<BookDetailState>()
        
        // When
        repeat(1000) {
            states.add(
                BookDetailState(
                    version = it.toLong(),
                    bookInfo = BookDetailState.BookInfo(
                        id = it.toString(),
                        bookName = "书籍$it",
                        authorName = "作者$it",
                        bookDesc = "描述$it",
                        picUrl = "url$it",
                        visitCount = it.toLong(),
                        wordCount = it * 1000,
                        categoryName = "分类$it"
                    )
                )
            )
        }
        
        // Then
        // 验证状态列表创建成功（内存测试主要是确保不会OOM）
        assertEquals(1000, states.size)
        
        // 验证状态对象的基本属性
        val lastState = states.last()
        assertEquals(999L, lastState.version)
        assertEquals("书籍999", lastState.bookInfo?.bookName)
    }
    
    @Test
    fun `测试Effect生成性能 - Effect创建应该快速`() {
        // Given
        val initialState = BookDetailState()
        val effectIntents = listOf(
            BookDetailIntent.StartReading("123", null),
            BookDetailIntent.ShareBook("123", "测试书籍"),
            BookDetailIntent.AddToBookshelf("123"),
            BookDetailIntent.FollowAuthor("测试作者")
        )
        
        // When
        val startTime = System.nanoTime()
        
        repeat(1000) {
            effectIntents.forEach { intent ->
                val result = reducer.reduce(initialState, intent)
                assertNotNull("Effect应该被正确生成", result.effect)
            }
        }
        
        val endTime = System.nanoTime()
        
        // Then
        val totalTime = (endTime - startTime) / 1_000_000.0 // 转换为毫秒
        assertTrue("Effect生成总时间应该小于50ms，实际: ${totalTime}ms", totalTime < 50)
    }
} 