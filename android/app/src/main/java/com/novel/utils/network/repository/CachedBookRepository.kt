package com.novel.utils.network.repository

import androidx.compose.runtime.Stable
import com.novel.utils.TimberLogger
import com.novel.utils.asStable
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.SearchService
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
import com.novel.utils.network.cache.searchBooksCached
import com.novel.utils.network.cache.onSuccess
import com.novel.utils.network.cache.onError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存书籍数据仓库
 * 
 * 功能：
 * - Cache-First数据获取策略
 * - 自动重试和兜底机制
 * - StateFlow状态管理
 * - 统一错误处理
 * 
 * 特点：
 * - 支持多级缓存策略
 * - 业务数据有效性验证
 * - 最大2次重试机制
 * - 响应式数据更新
 */
@Stable
@Singleton
class CachedBookRepository @Inject constructor(
    @Stable
    private val bookService: BookService,
    @Stable
    private val searchService: SearchService,
    @Stable
    private val cacheManager: NetworkCacheManager
) {
    companion object {
        private const val TAG = "CachedBookRepository"
        private const val MAX_RETRY_COUNT = 2 // 最大重试次数
        private const val RETRY_DELAY_MS = 1500L // 重试延迟
    }
    
    // 数据状态流
    private val _bookInfo = MutableStateFlow<BookService.BookInfo?>(null)
    val bookInfo: StateFlow<BookService.BookInfo?> = _bookInfo.asStateFlow().asStable()
    
    private val _bookChapters = MutableStateFlow<ImmutableList<BookService.BookChapter>>(persistentListOf())
    val bookChapters: StateFlow<ImmutableList<BookService.BookChapter>> = _bookChapters.asStateFlow().asStable()
    
    private val _visitRankBooks = MutableStateFlow<ImmutableList<BookService.BookRank>>(persistentListOf())
    val visitRankBooks: StateFlow<ImmutableList<BookService.BookRank>> = _visitRankBooks.asStateFlow().asStable()
    
    private val _updateRankBooks = MutableStateFlow<ImmutableList<BookService.BookRank>>(persistentListOf())
    val updateRankBooks: StateFlow<ImmutableList<BookService.BookRank>> = _updateRankBooks.asStateFlow().asStable()
    
    private val _newestRankBooks = MutableStateFlow<ImmutableList<BookService.BookRank>>(persistentListOf())
    val newestRankBooks: StateFlow<ImmutableList<BookService.BookRank>> = _newestRankBooks.asStateFlow().asStable()
    
    private val _bookCategories = MutableStateFlow<ImmutableList<BookService.BookCategory>>(persistentListOf())
    val bookCategories: StateFlow<ImmutableList<BookService.BookCategory>> = _bookCategories.asStateFlow().asStable()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow().asStable()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow().asStable()
    
    /**
     * 增强的数据获取方法，支持自动重试和兜底机制
     */
    private suspend fun <T> getDataWithFallback(
        operation: suspend () -> CacheResult<T>,
        extractData: (T) -> Any?,
        updateState: (Any?) -> Unit,
        operationName: String
    ): T? {
        var lastError: Throwable? = null
        
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                when (val result = operation()) {
                    is CacheResult.Success -> {
                        val data = extractData(result.data)
                        if (data != null && isValidBusinessData(data)) {
                            updateState(data)
                            TimberLogger.d(TAG, "$operationName succeeded on attempt ${attempt + 1}")
                            return result.data
                        } else {
                            TimberLogger.w(TAG, "$operationName returned invalid data on attempt ${attempt + 1}")
                            if (attempt < MAX_RETRY_COUNT - 1) {
                                kotlinx.coroutines.delay(RETRY_DELAY_MS)
                            }
                        }
                    }
                    is CacheResult.Error -> {
                        lastError = result.error
                        TimberLogger.w(TAG, "$operationName failed on attempt ${attempt + 1}: ${result.error.message}")
                        
                        // 如果有缓存数据，尝试使用缓存数据
                        result.cachedData?.let { cachedData ->
                            val data = extractData(cachedData)
                            if (data != null) {
                                updateState(data)
                                TimberLogger.d(TAG, "$operationName using cached data as fallback")
                                return cachedData
                            }
                        }
                        
                        if (attempt < MAX_RETRY_COUNT - 1) {
                            kotlinx.coroutines.delay(RETRY_DELAY_MS)
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
                TimberLogger.e(TAG, "$operationName exception on attempt ${attempt + 1}", e)
                if (attempt < MAX_RETRY_COUNT - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }
        
        _error.value = lastError?.message ?: "Data retrieval failed after $MAX_RETRY_COUNT attempts"
        TimberLogger.e(TAG, "$operationName failed after $MAX_RETRY_COUNT attempts")
        return null
    }
    
    /**
     * 检查业务数据是否有效
     */
    private fun isValidBusinessData(data: Any?): Boolean {
        return when (data) {
            null -> false
            is String -> data.isNotBlank()
            is Collection<*> -> data.isNotEmpty()
            is BookService.BookInfo -> data.bookName.isNotBlank() && data.id > 0
            is BookService.BookContentAbout -> data.bookContent.isNotBlank()
            else -> true
        }
    }
    
    /**
     * 获取书籍信息（增强兜底机制）
     */
    suspend fun getBookInfo(
        bookId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): BookService.BookInfo? {
        return executeWithLoading {
            getDataWithFallback(
                operation = {
                    bookService.getBookByIdCached(
                        bookId = bookId,
                        cacheManager = cacheManager,
                        strategy = strategy,
                        onCacheUpdate = { response ->
                            response.data?.let { _bookInfo.value = it }
                            TimberLogger.d(TAG, "Book info cache updated for bookId: $bookId")
                        }
                    )
                },
                extractData = { response -> response.data },
                updateState = { data -> _bookInfo.value = data as? BookService.BookInfo },
                operationName = "getBookInfo(bookId=$bookId)"
            )?.data
        }
    }
    
    /**
     * 获取书籍章节列表（增强兜底机制）
     */
    suspend fun getBookChapters(
        bookId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): ImmutableList<BookService.BookChapter> {
        return executeWithLoading {
            getDataWithFallback(
                operation = {
                    bookService.getBookChaptersCached(
                        bookId = bookId,
                        cacheManager = cacheManager,
                        strategy = strategy,
                        onCacheUpdate = { response ->
                            response.data?.let { _bookChapters.value = it.toImmutableList() }
                            TimberLogger.d(TAG, "Book chapters cache updated for bookId: $bookId")
                        }
                    )
                },
                extractData = { response -> response.data },
                updateState = { data -> _bookChapters.value = (data as? List<BookService.BookChapter>)?.toImmutableList()
                    ?: persistentListOf() },
                operationName = "getBookChapters(bookId=$bookId)"
            )?.data?.toImmutableList() ?: persistentListOf()
        } ?: persistentListOf()
    }
    
    /**
     * 获取书籍内容（增强兜底机制）
     */
    suspend fun getBookContent(
        chapterId: Long,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): BookService.BookContentAbout? {
        return executeWithLoading {
            getDataWithFallback(
                operation = {
                    bookService.getBookContentCached(
                        chapterId = chapterId,
                        cacheManager = cacheManager,
                        strategy = strategy,
                        onCacheUpdate = {
                            TimberLogger.d(TAG, "Book content cache updated for chapterId: $chapterId")
                        }
                    )
                },
                extractData = { response -> response.data },
                updateState = { _ -> /* 书籍内容不需要更新状态流 */ },
                operationName = "getBookContent(chapterId=$chapterId)"
            )?.data
        }
    }
    
    /**
     * 获取点击排行榜
     */
    suspend fun getVisitRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): ImmutableList<BookService.BookRank> {
        return executeWithLoading {
            try {
                bookService.getVisitRankBooksCached(
                    cacheManager = cacheManager,
                    strategy = strategy,
                    onCacheUpdate = { response ->
                        response.data?.let { _visitRankBooks.value = it.toImmutableList() }
                        TimberLogger.d(TAG, "Visit rank books cache updated")
                    }
                ).onSuccess { response, fromCache ->
                    response.data?.let { _visitRankBooks.value = it.toImmutableList() }
                    TimberLogger.d(TAG, "Visit rank books loaded from ${if (fromCache) "cache" else "network"}")
                }.onError { error, cachedData ->
                    TimberLogger.e(TAG, "Failed to load visit rank books", error)
                    cachedData?.data?.let { _visitRankBooks.value = it.toImmutableList() }
                    _error.value = error.message
                }.let { result ->
                    when (result) {
                        is CacheResult.Success -> result.data.data?.toImmutableList() ?: persistentListOf()
                        is CacheResult.Error -> result.cachedData?.data?.toImmutableList() ?: persistentListOf()
                    }
                }
            } catch (e: ClassCastException) {
                TimberLogger.e(TAG, "ClassCastException in getVisitRankBooks, clearing cache and retrying", e)
                // 清理相关缓存
                cacheManager.clearCache("visit_rank_books")
                _visitRankBooks.value = persistentListOf()
                try {
                    // 直接从网络获取数据
                    val response = bookService.getVisitRankBooksBlocking()
                    response.data?.let { books ->
                        _visitRankBooks.value = books.toImmutableList()
                        books.toImmutableList()
                    } ?: persistentListOf()
                } catch (networkError: Exception) {
                    TimberLogger.e(TAG, "Network fallback also failed for visit rank books", networkError)
                    _error.value = networkError.message
                    persistentListOf()
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Unexpected error in getVisitRankBooks", e)
                _error.value = e.message
                persistentListOf()
            }
        } ?: persistentListOf()
    }
    
    /**
     * 获取更新排行榜
     */
    suspend fun getUpdateRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): ImmutableList<BookService.BookRank> {
        return executeWithLoading {
            try {
                bookService.getUpdateRankBooksCached(
                    cacheManager = cacheManager,
                    strategy = strategy,
                    onCacheUpdate = { response ->
                        response.data?.let { _updateRankBooks.value = it.toImmutableList() }
                        TimberLogger.d(TAG, "Update rank books cache updated")
                    }
                ).onSuccess { response, fromCache ->
                    response.data?.let { _updateRankBooks.value = it.toImmutableList() }
                    TimberLogger.d(TAG, "Update rank books loaded from ${if (fromCache) "cache" else "network"}")
                }.onError { error, cachedData ->
                    TimberLogger.e(TAG, "Failed to load update rank books", error)
                    cachedData?.data?.let { _updateRankBooks.value = it.toImmutableList() }
                    _error.value = error.message
                }.let { result ->
                    when (result) {
                        is CacheResult.Success -> result.data.data?.toImmutableList() ?: persistentListOf()
                        is CacheResult.Error -> result.cachedData?.data?.toImmutableList() ?: persistentListOf()
                    }
                }
            } catch (e: ClassCastException) {
                TimberLogger.e(TAG, "ClassCastException in getUpdateRankBooks, clearing cache and retrying", e)
                // 清理相关缓存
                cacheManager.clearCache("update_rank_books")
                _updateRankBooks.value = persistentListOf()
                try {
                    // 直接从网络获取数据
                    val response = bookService.getUpdateRankBooksBlocking()
                    response.data?.let { books ->
                        _updateRankBooks.value = books.toImmutableList()
                        books.toImmutableList()
                    } ?: persistentListOf()
                } catch (networkError: Exception) {
                    TimberLogger.e(TAG, "Network fallback also failed for update rank books", networkError)
                    _error.value = networkError.message
                    persistentListOf()
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Unexpected error in getUpdateRankBooks", e)
                _error.value = e.message
                persistentListOf()
            }
        } ?: persistentListOf()
    }
    
    /**
     * 获取新书排行榜
     */
    suspend fun getNewestRankBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): ImmutableList<BookService.BookRank> {
        return executeWithLoading {
            try {
                bookService.getNewestRankBooksCached(
                    cacheManager = cacheManager,
                    strategy = strategy,
                    onCacheUpdate = { response ->
                        response.data?.let { _newestRankBooks.value = it.toImmutableList() }
                        TimberLogger.d(TAG, "Newest rank books cache updated")
                    }
                ).onSuccess { response, fromCache ->
                    response.data?.let { _newestRankBooks.value = it.toImmutableList() }
                    TimberLogger.d(TAG, "Newest rank books loaded from ${if (fromCache) "cache" else "network"}")
                }.onError { error, cachedData ->
                    TimberLogger.e(TAG, "Failed to load newest rank books", error)
                    cachedData?.data?.let { _newestRankBooks.value = it.toImmutableList() }
                    _error.value = error.message
                }.let { result ->
                    when (result) {
                        is CacheResult.Success -> result.data.data?.toImmutableList() ?: persistentListOf()
                        is CacheResult.Error -> result.cachedData?.data?.toImmutableList() ?: persistentListOf()
                    }
                }
            } catch (e: ClassCastException) {
                TimberLogger.e(TAG, "ClassCastException in getNewestRankBooks, clearing cache and retrying", e)
                // 清理相关缓存
                clearNewestRankCache()
                try {
                    // 直接从网络获取数据
                    val response = bookService.getNewestRankBooksBlocking()
                    response.data?.let { books ->
                        _newestRankBooks.value = books.toImmutableList()
                        books.toImmutableList()
                    } ?: persistentListOf()
                } catch (networkError: Exception) {
                    TimberLogger.e(TAG, "Network fallback also failed for newest rank books", networkError)
                    _error.value = networkError.message
                    persistentListOf()
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Unexpected error in getNewestRankBooks", e)
                _error.value = e.message
                persistentListOf()
            }
        } ?: persistentListOf()
    }
    
    /**
     * 获取书籍分类
     */
    suspend fun getBookCategories(
        workDirection: Int,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): ImmutableList<BookService.BookCategory> {
        return executeWithLoading {
            try {
                bookService.getBookCategoriesCached(
                    workDirection = workDirection,
                    cacheManager = cacheManager,
                    strategy = strategy,
                    onCacheUpdate = { response ->
                        response.data?.let { _bookCategories.value = it.toImmutableList() }
                        TimberLogger.d(TAG, "Book categories cache updated for workDirection: $workDirection")
                    }
                ).onSuccess { response, fromCache ->
                    response.data?.let { _bookCategories.value = it.toImmutableList() }
                    TimberLogger.d(TAG, "Book categories loaded from ${if (fromCache) "cache" else "network"} for workDirection: $workDirection")
                }.onError { error, cachedData ->
                    TimberLogger.e(TAG, "Failed to load book categories for workDirection: $workDirection", error)
                    cachedData?.data?.let { _bookCategories.value = it.toImmutableList() }
                    _error.value = error.message
                }.let { result ->
                    when (result) {
                        is CacheResult.Success -> result.data.data?.toImmutableList() ?: persistentListOf()
                        is CacheResult.Error -> result.cachedData?.data?.toImmutableList() ?: persistentListOf()
                    }
                }
            } catch (e: ClassCastException) {
                TimberLogger.e(TAG, "ClassCastException in getBookCategories, clearing cache and retrying", e)
                // 清理相关缓存
                cacheManager.clearCache("book_categories_$workDirection")
                _bookCategories.value = persistentListOf()
                try {
                    // 直接从网络获取数据
                    val response = bookService.getBookCategoriesBlocking(workDirection)
                    response.data?.let { categories ->
                        _bookCategories.value = categories.toImmutableList()
                        categories.toImmutableList()
                    } ?: persistentListOf()
                } catch (networkError: Exception) {
                    TimberLogger.e(TAG, "Network fallback also failed for book categories", networkError)
                    _error.value = networkError.message
                    persistentListOf()
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Unexpected error in getBookCategories", e)
                _error.value = e.message
                persistentListOf()
            }
        } ?: persistentListOf()
    }
    
    /**
     * 清理指定书籍的缓存
     */
    suspend fun clearBookCache(bookId: Long) {
        cacheManager.clearCache("book_info_$bookId")
        cacheManager.clearCache("book_chapters_$bookId")
        TimberLogger.d(TAG, "Book cache cleared for bookId: $bookId")
    }
    
    /**
     * 清理所有排行榜缓存
     */
    suspend fun clearRankCache() {
        cacheManager.clearCache("visit_rank_books")
        cacheManager.clearCache("update_rank_books")
        cacheManager.clearCache("newest_rank_books")
        TimberLogger.d(TAG, "Rank cache cleared")
    }
    
    /**
     * 清理新书榜缓存 - 用于解决缓存反序列化问题
     */
    suspend fun clearNewestRankCache() {
        cacheManager.clearCache("newest_rank_books")
        _newestRankBooks.value = persistentListOf()
        TimberLogger.d(TAG, "Newest rank cache cleared")
    }
    
    /**
     * 清理错误状态
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 搜索书籍 - 使用缓存优先策略
     */
    suspend fun searchBooks(
        keyword: String? = null,
        workDirection: Int? = null,
        categoryId: Int? = null,
        isVip: Int? = null,
        bookStatus: Int? = null,
        wordCountMin: Int? = null,
        wordCountMax: Int? = null,
        updateTimeMin: String? = null,
        sort: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 20,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): SearchService.PageResponse<SearchService.BookInfo> {
        return executeWithLogging {
            try {
                searchService.searchBooksCached(
                    keyword = keyword,
                    workDirection = workDirection,
                    categoryId = categoryId,
                    isVip = isVip,
                    bookStatus = bookStatus,
                    wordCountMin = wordCountMin,
                    wordCountMax = wordCountMax,
                    updateTimeMin = updateTimeMin,
                    sort = sort,
                    pageNum = pageNum,
                    pageSize = pageSize,
                    cacheManager = cacheManager,
                    strategy = strategy,
                    onCacheUpdate = {
                        TimberLogger.d(TAG, "Search books cache updated for keyword: $keyword")
                    }
                ).onSuccess { response, fromCache ->
                    TimberLogger.d(TAG, "Search books loaded from ${if (fromCache) "cache" else "network"} for keyword: $keyword")
                }.onError { error, cachedData ->
                    TimberLogger.e(TAG, "Failed to search books for keyword: $keyword", error)
                    _error.value = error.message
                }.let { result ->
                    when (result) {
                        is CacheResult.Success -> result.data.data ?: SearchService.PageResponse(0, 0, 0, emptyList(), 0)
                        is CacheResult.Error -> result.cachedData?.data ?: SearchService.PageResponse(0, 0, 0, emptyList(), 0)
                    }
                }
            } catch (e: ClassCastException) {
                TimberLogger.e(TAG, "ClassCastException in searchBooks, clearing search cache and retrying", e)
                // 清理搜索缓存
                clearSearchCache(keyword, workDirection, categoryId, isVip, bookStatus, wordCountMin, wordCountMax, updateTimeMin, sort, pageNum, pageSize)
                try {
                    // 直接从网络搜索
                    val response = searchService.searchBooksBlocking(
                        keyword = keyword,
                        workDirection = workDirection,
                        categoryId = categoryId,
                        isVip = isVip,
                        bookStatus = bookStatus,
                        wordCountMin = wordCountMin,
                        wordCountMax = wordCountMax,
                        updateTimeMin = updateTimeMin,
                        sort = sort,
                        pageNum = pageNum,
                        pageSize = pageSize
                    )
                    response.data ?: SearchService.PageResponse(0, 0, 0, emptyList(), 0)
                } catch (networkError: Exception) {
                    TimberLogger.e(TAG, "Network fallback also failed for search", networkError)
                    _error.value = networkError.message
                    SearchService.PageResponse(0, 0, 0, emptyList(), 0)
                }
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Unexpected error in searchBooks", e)
                _error.value = e.message
                SearchService.PageResponse(0, 0, 0, emptyList(), 0)
            }
        } ?: SearchService.PageResponse(0, 0, 0, emptyList(), 0)
    }
    
    /**
     * 清理搜索缓存 - 支持具体参数的缓存清理
     */
    private suspend fun clearSearchCache(
        keyword: String? = null,
        workDirection: Int? = null,
        categoryId: Int? = null,
        isVip: Int? = null,
        bookStatus: Int? = null,
        wordCountMin: Int? = null,
        wordCountMax: Int? = null,
        updateTimeMin: String? = null,
        sort: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ) {
        val paramsHash = listOf(
            keyword, workDirection, categoryId, isVip, bookStatus,
            wordCountMin, wordCountMax, updateTimeMin, sort, pageNum, pageSize
        ).joinToString("_").hashCode().toString()
        
        val cacheKey = "search_books_$paramsHash"
        cacheManager.clearCache(cacheKey)
        TimberLogger.d(TAG, "Search cache cleared for key: $cacheKey")
    }
    
    /**
     * 执行带日志的操作
     */
    private suspend fun <T> executeWithLogging(block: suspend () -> T): T? {
        return try {
            _isLoading.value = true
            _error.value = null
            block()
        } catch (e: Exception) {
            TimberLogger.e(TAG, "Operation failed", e)
            _error.value = e.message
            null
        } finally {
            _isLoading.value = false
        }
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
            TimberLogger.e(TAG, "Operation failed", e)
            _error.value = e.message
            null
        } finally {
            _isLoading.value = false
        }
    }
} 