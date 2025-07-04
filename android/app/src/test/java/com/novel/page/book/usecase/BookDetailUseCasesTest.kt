package com.novel.page.book.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * BookDetail UseCase单元测试
 * 
 * 验证UseCase的业务逻辑正确性
 */
class BookDetailUseCasesTest {
    
    private lateinit var addToBookshelfUseCase: AddToBookshelfUseCase
    private lateinit var removeFromBookshelfUseCase: RemoveFromBookshelfUseCase
    private lateinit var followAuthorUseCase: FollowAuthorUseCase
    private lateinit var checkBookInShelfUseCase: CheckBookInShelfUseCase
    
    @Before
    fun setUp() {
        addToBookshelfUseCase = AddToBookshelfUseCase()
        removeFromBookshelfUseCase = RemoveFromBookshelfUseCase()
        followAuthorUseCase = FollowAuthorUseCase()
        checkBookInShelfUseCase = CheckBookInShelfUseCase()
    }
    
    @Test
    fun `测试AddToBookshelfUseCase - 成功添加到书架`() = runTest {
        // Given
        val bookId = "123"
        val params = AddToBookshelfUseCase.Params(bookId)
        
        // When
        val result = addToBookshelfUseCase(params)
        
        // Then
        assertTrue(result.success)
        assertEquals("已添加到书架", result.message)
    }
    
    @Test
    fun `测试RemoveFromBookshelfUseCase - 成功从书架移除`() = runTest {
        // Given
        val bookId = "123"
        val params = RemoveFromBookshelfUseCase.Params(bookId)
        
        // When
        val result = removeFromBookshelfUseCase(params)
        
        // Then
        assertTrue(result.success)
        assertEquals("已从书架移除", result.message)
    }
    
    @Test
    fun `测试FollowAuthorUseCase - 成功关注作者`() = runTest {
        // Given
        val authorName = "测试作者"
        val params = FollowAuthorUseCase.Params(authorName)
        
        // When
        val result = followAuthorUseCase(params)
        
        // Then
        assertTrue(result.success)
        assertEquals("已关注作者：测试作者", result.message)
    }
    
    @Test
    fun `测试CheckBookInShelfUseCase - 检查书籍是否在书架`() = runTest {
        // Given
        val bookId = "123"
        val params = CheckBookInShelfUseCase.Params(bookId)
        
        // When
        val result = checkBookInShelfUseCase(params)
        
        // Then
        // 目前返回false，因为是模拟实现
        assertFalse(result.isInShelf)
    }
    
    @Test
    fun `测试UseCase执行时间 - 验证模拟延迟`() = runTest {
        // Given
        val bookId = "123"
        val startTime = System.currentTimeMillis()
        
        // When
        addToBookshelfUseCase(AddToBookshelfUseCase.Params(bookId))
        
        // Then
        val duration = System.currentTimeMillis() - startTime
        // 验证至少有模拟延迟时间（允许一些误差）
        assertTrue("执行时间应该至少有模拟延迟", duration >= 400)
    }
} 