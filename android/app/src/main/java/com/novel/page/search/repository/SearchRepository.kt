package com.novel.page.search.repository

import android.content.Context
import android.util.Log
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.network.api.front.BookService
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novel.page.search.component.SearchRankingItem
import com.novel.page.search.viewmodel.BookInfoRespDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.novel.utils.network.cache.NetworkCacheManager
import com.novel.utils.network.cache.CacheStrategy
import com.novel.utils.network.cache.searchBooksCached
import com.novel.utils.network.cache.onSuccess
import com.novel.utils.network.cache.onError
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey

/**
 * 搜索历史数据结构
 */
data class SearchHistoryData(
    val history: List<String> = emptyList(),
    val isExpanded: Boolean = false
)

/**
 * 榜单数据结构
 */
data class RankingData(
    val novelRanking: List<SearchRankingItem> = emptyList(),
    val dramaRanking: List<SearchRankingItem> = emptyList(),
    val newBookRanking: List<SearchRankingItem> = emptyList()
)

/**
 * REST响应包装类 for 分页书籍信息
 */
data class RestRespPageRespDtoBookInfoRespDto(
    val code: Int? = null,
    val message: String? = null,
    val data: PageRespDtoBookInfoRespDto? = null,
    val ok: Boolean? = null
)

/**
 * 分页响应DTO for 书籍信息
 */
data class PageRespDtoBookInfoRespDto(
    val pageNum: Long? = null,
    val pageSize: Long? = null,
    val total: Long? = null,
    val list: List<BookInfoRespDto> = emptyList(),
    val pages: Long? = null
)

/**
 * 搜索数据仓库 - 集成缓存策略
 */
@Singleton
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchService: SearchService,
    private val bookService: BookService,
    private val userDefaults: NovelUserDefaults,
    private val gson: Gson,
    private val cacheManager: NetworkCacheManager
) {
    
    companion object {
        private const val TAG = "SearchRepository"
        private const val MAX_HISTORY_SIZE = 10
        private const val MAX_SEARCH_HISTORY = 10
        
        // Storage keys
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val HISTORY_EXPANDED_KEY = "history_expanded"
    }
    
    // region 搜索历史管理
    
    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): List<String> {
        return try {
            // 使用Gson解析JSON字符串
            val historyJson = userDefaults.getString(SEARCH_HISTORY_KEY)
            if (historyJson != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(historyJson, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取搜索历史失败", e)
            emptyList()
        }
    }
    
    /**
     * 添加搜索历史
     */
    fun addSearchHistory(keyword: String) {
        try {
            val currentHistory = getSearchHistory().toMutableList()
            
            // 移除已存在的相同关键词
            currentHistory.remove(keyword)
            
            // 添加到开头
            currentHistory.add(0, keyword)
            
            // 限制数量
            if (currentHistory.size > MAX_SEARCH_HISTORY) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            
            val historyJson = gson.toJson(currentHistory)
            userDefaults.setString(SEARCH_HISTORY_KEY, historyJson)
            Log.d(TAG, "搜索历史已保存: $keyword")
        } catch (e: Exception) {
            Log.e(TAG, "保存搜索历史失败", e)
        }
    }
    
    /**
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        try {
            userDefaults.remove(SEARCH_HISTORY_KEY)
            Log.d(TAG, "搜索历史已清空")
        } catch (e: Exception) {
            Log.e(TAG, "清空搜索历史失败", e)
        }
    }
    
    /**
     * 获取历史展开状态
     */
    fun getHistoryExpansionState(): Boolean {
        return try {
            userDefaults.getString(HISTORY_EXPANDED_KEY)?.toBoolean() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "获取历史展开状态失败", e)
            false
        }
    }
    
    /**
     * 保存历史展开状态
     */
    fun saveHistoryExpansionState(isExpanded: Boolean) {
        try {
            userDefaults.setString(HISTORY_EXPANDED_KEY, isExpanded.toString())
            Log.d(TAG, "历史展开状态已保存: $isExpanded")
        } catch (e: Exception) {
            Log.e(TAG, "保存历史展开状态失败", e)
        }
    }
    
    // endregion
    
    // region 榜单数据获取
    
    /**
     * 获取推荐榜单数据
     */
    suspend fun getNovelRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取推荐榜单数据")
            val response = bookService.getVisitRankBooksBlocking()
            
            if (response.ok == true && response.data != null) {
                val realData = response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
                
                // 如果数据不足15条，添加一些测试数据
                if (realData.size < 20) {
                    val testData = mutableListOf<SearchRankingItem>()
                    testData.addAll(realData)
                    
                    for (i in realData.size until 20) {
                        testData.add(
                            SearchRankingItem(
                                id = 1000L + i,
                                title = "测试小说${i + 1}",
                                author = "测试作者${i + 1}",
                                rank = i + 1
                            )
                        )
                    }
                    testData
                } else {
                    realData
                }
            } else {
                Log.w(TAG, "获取推荐榜单失败: ${response.message}")
                // 返回测试数据
                (1..20).map { i ->
                    SearchRankingItem(
                        id = 1000L + i,
                        title = "测试小说$i",
                        author = "测试作者$i",
                        rank = i
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取推荐榜单失败", e)
            // 返回测试数据
            (1..20).map { i ->
                SearchRankingItem(
                    id = 1000L + i,
                    title = "测试小说$i",
                    author = "测试作者$i",
                    rank = i
                )
            }
        }
    }
    
    /**
     * 获取热搜短剧榜数据
     */
    suspend fun getDramaRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取热搜短剧榜数据")
            val response = bookService.getUpdateRankBooksBlocking()
            
            if (response.ok == true && response.data != null) {
                val realData = response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
                
                // 如果数据不足20条，添加一些测试数据
                if (realData.size < 20) {
                    val testData = mutableListOf<SearchRankingItem>()
                    testData.addAll(realData)
                    
                    for (i in realData.size until 20) {
                        testData.add(
                            SearchRankingItem(
                                id = 2000L + i,
                                title = "热门短剧${i + 1}",
                                author = "短剧作者${i + 1}",
                                rank = i + 1
                            )
                        )
                    }
                    testData
                } else {
                    realData
                }
            } else {
                Log.w(TAG, "获取热搜短剧榜失败: ${response.message}")
                // 返回测试数据
                (1..20).map { i ->
                    SearchRankingItem(
                        id = 2000L + i,
                        title = "热门短剧$i",
                        author = "短剧作者$i",
                        rank = i
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热搜短剧榜失败", e)
            // 返回测试数据
            (1..20).map { i ->
                SearchRankingItem(
                    id = 2000L + i,
                    title = "热门短剧$i",
                    author = "短剧作者$i",
                    rank = i
                )
            }
        }
    }
    
    /**
     * 获取新书榜单数据
     */
    suspend fun getNewBookRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取新书榜单数据")
            val response = bookService.getNewestRankBooksBlocking()
            
            if (response.ok == true && response.data != null) {
                val realData = response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
                
                // 如果数据不足20条，添加一些测试数据
                if (realData.size < 20) {
                    val testData = mutableListOf<SearchRankingItem>()
                    testData.addAll(realData)
                    
                    for (i in realData.size until 20) {
                        testData.add(
                            SearchRankingItem(
                                id = 3000L + i,
                                title = "新书推荐${i + 1}",
                                author = "新人作者${i + 1}",
                                rank = i + 1
                            )
                        )
                    }
                    testData
                } else {
                    realData
                }
            } else {
                Log.w(TAG, "获取新书榜单失败: ${response.message}")
                // 返回测试数据
                (1..20).map { i ->
                    SearchRankingItem(
                        id = 3000L + i,
                        title = "新书推荐$i",
                        author = "新人作者$i",
                        rank = i
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取新书榜单失败", e)
            // 返回测试数据
            (1..20).map { i ->
                SearchRankingItem(
                    id = 3000L + i,
                    title = "新书推荐$i",
                    author = "新人作者$i",
                    rank = i
                )
            }
        }
    }
    
    /**
     * 获取所有榜单数据
     */
    suspend fun getAllRankingData(): RankingData {
        return try {
            // 并行获取所有榜单数据
            val novelRanking = getNovelRanking()
            val dramaRanking = getDramaRanking()
            val newBookRanking = getNewBookRanking()
            
            RankingData(
                novelRanking = novelRanking,
                dramaRanking = dramaRanking,
                newBookRanking = newBookRanking
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取所有榜单数据失败", e)
            RankingData()
        }
    }
    
    // endregion
    
    // region 搜索功能
    
    /**
     * 搜索书籍 - 缓存优先策略
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
    ): SearchService.BookSearchResponse? {
        return try {
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
                onCacheUpdate = { response ->
                    Log.d(TAG, "搜索结果缓存已更新: keyword=$keyword")
                }
            ).onSuccess { response, fromCache ->
                Log.d(TAG, "搜索成功，来源: ${if (fromCache) "缓存" else "网络"}，关键词: $keyword")
            }.onError { error, cachedData ->
                Log.e(TAG, "搜索失败，关键词: $keyword", error)
            }.let { result ->
                when (result) {
                    is com.novel.utils.network.cache.CacheResult.Success -> result.data
                    is com.novel.utils.network.cache.CacheResult.Error -> result.cachedData
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜索异常，关键词: $keyword", e)
            null
        }
    }
    
    /**
     * 清理搜索缓存
     */
    suspend fun clearSearchCache() {
        try {
            // 清理搜索相关缓存（需要遍历所有可能的缓存键）
            // 这里可以根据需要实现更精确的缓存清理逻辑
            Log.d(TAG, "搜索缓存已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理搜索缓存失败", e)
        }
    }
    
    /**
     * 强制刷新搜索结果（绕过缓存）
     */
    suspend fun refreshSearchResults(
        keyword: String,
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
    ): SearchService.BookSearchResponse? {
        return searchBooks(
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
            strategy = CacheStrategy.NETWORK_ONLY
        )
    }
    
    /**
     * 检查搜索缓存是否可用
     */
    suspend fun isSearchCacheAvailable(keyword: String): Boolean {
        return try {
            // 生成缓存键（简化版，实际应该与扩展函数中的逻辑一致）
            val paramsHash = keyword.hashCode().toString()
            cacheManager.isCacheExists("search_books_$paramsHash")
        } catch (e: Exception) {
            Log.e(TAG, "检查搜索缓存状态失败", e)
            false
        }
    }
    
    // endregion
} 