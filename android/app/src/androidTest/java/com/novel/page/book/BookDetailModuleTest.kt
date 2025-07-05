package com.novel.page.book

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novel.ui.theme.NovelTheme
import com.novel.page.book.viewmodel.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Book模块综合测试
 * 
 * 包含UI测试、集成测试和性能测试
 */
@RunWith(AndroidJUnit4::class)
class BookDetailModuleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================================
    // UI测试
    // ===========================================

    @Test
    fun bookDetailPage_基本渲染_成功() {
        composeTestRule.setContent {
            NovelTheme {
                BookDetailPage(
                    bookId = "1",
                    onNavigateToReader = { _, _ -> }
                )
            }
        }

        // 验证页面渲染成功
        composeTestRule.onRoot().assertExists()
        composeTestRule.waitForIdle()
    }

    @Test
    fun bookDetailPage_UI交互_基础验证() {
        var readerClicked = false
        
        composeTestRule.setContent {
            NovelTheme {
                BookDetailPage(
                    bookId = "1",
                    onNavigateToReader = { _, _ -> readerClicked = true }
                )
            }
        }

        // 等待UI渲染完成
        composeTestRule.waitForIdle()
        
        // 验证基础交互
        composeTestRule.onRoot().assertExists()
    }

    // ===========================================
    // 集成测试
    // ===========================================

    @Test
    fun bookDetailState_计算属性_正确性() {
        // 测试初始状态
        val initialState = BookDetailState()
        
        assertFalse(initialState.isSuccess)
        assertTrue(initialState.isEmpty)
        assertNull(initialState.bookInfo)
        assertNull(initialState.error)
        assertEquals(0L, initialState.version)

        // 测试加载状态
        val loadingState = BookDetailState(
            isLoading = true,
            currentBookId = "1"
        )
        
        assertTrue(loadingState.isLoading)
        assertFalse(loadingState.isSuccess)
        assertFalse(loadingState.isEmpty)
        assertEquals("1", loadingState.currentBookId)

        // 测试成功状态
        val successState = BookDetailState(
            bookInfo = BookDetailState.BookInfo(
                id = "1",
                bookName = "测试书籍",
                authorName = "测试作者",
                bookDesc = "测试描述",
                picUrl = "https://example.com/cover.jpg",
                visitCount = 1000L,
                wordCount = 100000,
                categoryName = "测试分类"
            ),
            isLoading = false,
            error = null
        )
        
        assertTrue(successState.isSuccess)
        assertFalse(successState.isEmpty)
        assertNotNull(successState.bookInfo)
        assertEquals("测试书籍", successState.bookInfo?.bookName)
    }

    @Test
    fun bookDetailReducer_状态转换_基本验证() {
        val reducer = BookDetailReducer()
        var currentState = BookDetailState()

        // 测试加载书籍详情
        val loadResult = reducer.reduce(
            currentState,
            BookDetailIntent.LoadBookDetail("1")
        )
        currentState = loadResult.newState
        assertTrue(currentState.isLoading)
        assertEquals("1", currentState.currentBookId)
        assertEquals(1L, currentState.version)

        // 测试切换描述展开状态
        val toggleResult = reducer.reduce(
            currentState,
            BookDetailIntent.ToggleDescriptionExpanded
        )
        currentState = toggleResult.newState
        assertTrue(currentState.isDescriptionExpanded)
        assertEquals(2L, currentState.version)

        // 测试清除错误
        val errorState = currentState.copy(error = "测试错误")
        val clearErrorResult = reducer.reduce(
            errorState,
            BookDetailIntent.ClearError
        )
        val clearedState = clearErrorResult.newState
        assertNull(clearedState.error)
    }

    // ===========================================
    // 性能测试
    // ===========================================

    @Test
    fun bookDetailState_更新性能_基准测试() {
        var state = BookDetailState()
        
        val time = measureTimeMillis {
            repeat(100) { index ->
                state = state.copy(
                    version = state.version + 1,
                    currentBookId = "book_$index",
                    isLoading = index % 2 == 0
                )
            }
        }
        
        // 验证性能在合理范围内
        assert(time < 100) { "状态更新性能过慢: ${time}ms" }
        assertEquals(100L, state.version)
    }

    @Test
    fun bookDetailReducer_处理性能_基准测试() {
        val reducer = BookDetailReducer()
        val initialState = BookDetailState()
        
        val intents = listOf(
            BookDetailIntent.LoadBookDetail("1"),
            BookDetailIntent.ToggleDescriptionExpanded,
            BookDetailIntent.ClearError,
            BookDetailIntent.RefreshBookDetail("1")
        )
        
        val time = measureTimeMillis {
            var currentState = initialState
            repeat(25) {
                intents.forEach { intent ->
                    val result = reducer.reduce(currentState, intent)
                    currentState = result.newState
                }
            }
        }
        
        // 验证Reducer处理性能
        assert(time < 50) { "Reducer处理性能过慢: ${time}ms" }
    }

    @Test
    fun bookDetailPage_UI渲染性能_基准测试() {
        val time = measureTimeMillis {
            composeTestRule.setContent {
                NovelTheme {
                    BookDetailPage(
                        bookId = "1",
                        onNavigateToReader = { _, _ -> }
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // 验证UI渲染性能
        assert(time < 1000) { "UI渲染性能过慢: ${time}ms" }
    }

    @Test
    fun bookDetailState_版本递增_性能测试() {
        val reducer = BookDetailReducer()
        var currentState = BookDetailState()
        
        val time = measureTimeMillis {
            repeat(1000) {
                val result = reducer.reduce(currentState, BookDetailIntent.ClearError)
                currentState = result.newState
            }
        }
        
        // 验证版本递增性能
        assert(time < 50) { "版本递增性能过慢: ${time}ms" }
        assertEquals(1000L, currentState.version)
    }

    @Test
    fun bookDetailState_计算属性性能_基准测试() {
        val state = BookDetailState(
            bookInfo = BookDetailState.BookInfo(
                id = "1",
                bookName = "测试书籍",
                authorName = "测试作者",
                bookDesc = "测试描述",
                picUrl = "https://example.com/cover.jpg",
                visitCount = 1000L,
                wordCount = 100000,
                categoryName = "测试分类"
            ),
            isLoading = false,
            error = null
        )
        
        val time = measureTimeMillis {
            repeat(1000) {
                // 测试计算属性性能
                val isEmpty = state.isEmpty
                val isSuccess = state.isSuccess
                val hasBookInfo = state.bookInfo != null
                val hasError = state.error != null
            }
        }
        
        // 验证计算属性性能
        assert(time < 50) { "计算属性性能过慢: ${time}ms" }
    }
} 