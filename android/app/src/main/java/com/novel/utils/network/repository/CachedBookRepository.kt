package com.novel.repository

import android.util.Log
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.cache.NetworkCacheManager
import com.novel.utils.network.cache.CacheStrategy
import com.novel.utils.network.cache.CacheResult
import com.novel.utils.network.cache.getBookByIdCached
import com.novel.utils.network.cache.getBookChaptersCached
import com.novel.utils.network.cache.getBookContentCached
import com.novel.utils.network.cache.getVisitRankBooksCached
import com.novel.utils.network.cache.getUpdateRankBooksCached
import com.novel.utils.network.cache.getNewestRankBooksCached
import com.novel.utils.network.cache.getBookCategoriesCached
import com.novel.utils.network.cache.onSuccess
import com.novel.utils.network.cache.onError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 带缓存功能的书籍Repository
 * 
 * 功能：
 * 1. 提供cache-first策略的数据获取
 * 2. 自动处理缓存更新回调
 * 3. 统一的错误处理
 * 4. 数据状态管理
 */
@Singleton
class CachedBookRepository @Inject constructor(
    private val bookService: BookService,
    private val cacheManager: NetworkCacheManager
) {
    companion object {
        private const val TAG = "CachedBookRepository"
    }
    
    // 数据状态流
    private val _bookInfo = MutableStateFlow<BookService.BookInfo?>(null)
    val bookInfo: StateFlow<BookService.BookInfo?> = _bookInfo.asStateFlow()
    
    private val _bookChapters = MutableStateFlow<List<BookService.BookChapter>>(emptyList())
    val bookChapters: StateFlow<List<BookService.BookChapter>> = _bookChapters.asStateFlow()
    
    private val _visitRankBooks = MutableStateFlow<List<BookService.BookRank>>(emptyList())
    val visitRankBooks: StateFlow<List<BookService.BookRank>> = _visitRankBooks.asStateFlow()
    
    private val _updateRankBooks = MutableStateFlow<List<BookService.BookRank>>(emptyList())
    val updateRankBooks: StateFlow<List<BookService.BookRank>> = _updateRankBooks.asStateFlow()
    
    private val _newestRankBooks = MutableStateFlow<List<BookService.BookRank>>(emptyList())
    val newestRankBooks: StateFlow<List<BookService.BookRank>> = _newestRankBooks.asStateFlow()
    
    private val _bookCategories = MutableStateFlow<List<BookService.BookCategory>>(emptyList())
    val bookCategories: StateFlow<List<BookService.BookCategory>> = _bookCategories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 获取书籍信息
     */
    suspend fun getBookInfo(
        bookId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): BookService.BookInfo? {
        return executeWithLoading {
            bookService.getBookByIdCached(
                bookId = bookId,
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _bookInfo.value = it }
                    Log.d(TAG, "Book info cache updated for bookId: $bookId")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _bookInfo.value = it }
                Log.d(TAG, "Book info loaded from ${if (fromCache) "cache" else "network"} for bookId: $bookId")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load book info for bookId: $bookId", error)
                cachedData?.data?.let { _bookInfo.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data
                    is CacheResult.Error -> result.cachedData?.data
                }
            }
        }
    }
    
    /**
     * 获取书籍章节列表
     */
    suspend fun getBookChapters(
        bookId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<BookService.BookChapter> {
        return executeWithLoading {
            bookService.getBookChaptersCached(
                bookId = bookId,
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _bookChapters.value = it }
                    Log.d(TAG, "Book chapters cache updated for bookId: $bookId")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _bookChapters.value = it }
                Log.d(TAG, "Book chapters loaded from ${if (fromCache) "cache" else "network"} for bookId: $bookId")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load book chapters for bookId: $bookId", error)
                cachedData?.data?.let { _bookChapters.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data ?: emptyList()
                    is CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } ?: emptyList()
    }
    
    /**
     * 获取书籍内容
     */
    suspend fun getBookContent(
        chapterId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): BookService.BookContentAbout? {
        return executeWithLoading {
            bookService.getBookContentCached(
                chapterId = chapterId,
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    Log.d(TAG, "Book content cache updated for chapterId: $chapterId")
                }
            ).onSuccess { response, fromCache ->
                Log.d(TAG, "Book content loaded from ${if (fromCache) "cache" else "network"} for chapterId: $chapterId")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load book content for chapterId: $chapterId", error)
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data
                    is CacheResult.Error -> result.cachedData?.data
                }
            }
        }
    }
    
    /**
     * 获取点击排行榜
     */
    suspend fun getVisitRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<BookService.BookRank> {
        return executeWithLoading {
            bookService.getVisitRankBooksCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _visitRankBooks.value = it }
                    Log.d(TAG, "Visit rank books cache updated")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _visitRankBooks.value = it }
                Log.d(TAG, "Visit rank books loaded from ${if (fromCache) "cache" else "network"}")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load visit rank books", error)
                cachedData?.data?.let { _visitRankBooks.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data ?: emptyList()
                    is CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } ?: emptyList()
    }
    
    /**
     * 获取更新排行榜
     */
    suspend fun getUpdateRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<BookService.BookRank> {
        return executeWithLoading {
            bookService.getUpdateRankBooksCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _updateRankBooks.value = it }
                    Log.d(TAG, "Update rank books cache updated")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _updateRankBooks.value = it }
                Log.d(TAG, "Update rank books loaded from ${if (fromCache) "cache" else "network"}")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load update rank books", error)
                cachedData?.data?.let { _updateRankBooks.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data ?: emptyList()
                    is CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } ?: emptyList()
    }
    
    /**
     * 获取新书排行榜
     */
    suspend fun getNewestRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<BookService.BookRank> {
        return executeWithLoading {
            bookService.getNewestRankBooksCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _newestRankBooks.value = it }
                    Log.d(TAG, "Newest rank books cache updated")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _newestRankBooks.value = it }
                Log.d(TAG, "Newest rank books loaded from ${if (fromCache) "cache" else "network"}")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load newest rank books", error)
                cachedData?.data?.let { _newestRankBooks.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data ?: emptyList()
                    is CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } ?: emptyList()
    }
    
    /**
     * 获取书籍分类
     */
    suspend fun getBookCategories(
        workDirection: Int,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<BookService.BookCategory> {
        return executeWithLoading {
            bookService.getBookCategoriesCached(
                workDirection = workDirection,
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    response.data?.let { _bookCategories.value = it }
                    Log.d(TAG, "Book categories cache updated for workDirection: $workDirection")
                }
            ).onSuccess { response, fromCache ->
                response.data?.let { _bookCategories.value = it }
                Log.d(TAG, "Book categories loaded from ${if (fromCache) "cache" else "network"} for workDirection: $workDirection")
            }.onError { error, cachedData ->
                Log.e(TAG, "Failed to load book categories for workDirection: $workDirection", error)
                cachedData?.data?.let { _bookCategories.value = it }
                _error.value = error.message
            }.let { result ->
                when (result) {
                    is CacheResult.Success -> result.data.data ?: emptyList()
                    is CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } ?: emptyList()
    }
    
    /**
     * 清理指定书籍的缓存
     */
    suspend fun clearBookCache(bookId: Long) {
        cacheManager.clearCache("book_info_$bookId")
        cacheManager.clearCache("book_chapters_$bookId")
        Log.d(TAG, "Book cache cleared for bookId: $bookId")
    }
    
    /**
     * 清理所有排行榜缓存
     */
    suspend fun clearRankCache() {
        cacheManager.clearCache("visit_rank_books")
        cacheManager.clearCache("update_rank_books")
        cacheManager.clearCache("newest_rank_books")
        Log.d(TAG, "Rank cache cleared")
    }
    
    /**
     * 清理错误状态
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 执行带加载状态的操作
     */
    private suspend fun <T> executeWithLoading(block: suspend () -> T): T? {
        return try {
            _isLoading.value = true
            _error.value = null
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Operation failed", e)
            _error.value = e.message
            null
        } finally {
            _isLoading.value = false
        }
    }
} 