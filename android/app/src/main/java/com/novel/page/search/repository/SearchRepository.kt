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

@Singleton
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchService: SearchService,
    private val bookService: BookService,
    private val novelUserDefaults: NovelUserDefaults,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "SearchRepository"
        private const val MAX_HISTORY_SIZE = 10
        
        // Storage keys
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val HISTORY_EXPANDED_KEY = "history_expanded"
    }
    
    // region 搜索历史管理
    
    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(): List<String> {
        return try {
            val historyJson = novelUserDefaults.getString(SEARCH_HISTORY_KEY) ?: "[]"
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(historyJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取搜索历史失败", e)
            emptyList()
        }
    }
    
    /**
     * 添加搜索历史
     */
    suspend fun addSearchHistory(query: String) {
        try {
            val currentHistory = getSearchHistory().toMutableList()
            
            // 移除已存在的相同项
            currentHistory.remove(query)
            
            // 添加到首位
            currentHistory.add(0, query)
            
            // 限制最大数量
            if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            
            // 保存到NovelUserDefaults
            val historyJson = gson.toJson(currentHistory)
            novelUserDefaults.setString(SEARCH_HISTORY_KEY, historyJson)
            
            Log.d(TAG, "添加搜索历史成功: $query")
        } catch (e: Exception) {
            Log.e(TAG, "添加搜索历史失败", e)
        }
    }
    
    /**
     * 清空搜索历史
     */
    suspend fun clearSearchHistory() {
        try {
            novelUserDefaults.setString(SEARCH_HISTORY_KEY, "[]")
            Log.d(TAG, "清空搜索历史成功")
        } catch (e: Exception) {
            Log.e(TAG, "清空搜索历史失败", e)
        }
    }
    
    /**
     * 获取历史记录展开状态
     */
    suspend fun getHistoryExpandedState(): Boolean {
        return try {
            novelUserDefaults.getString(HISTORY_EXPANDED_KEY)?.toBoolean() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "获取历史记录展开状态失败", e)
            false
        }
    }
    
    /**
     * 保存历史记录展开状态
     */
    suspend fun saveHistoryExpandedState(isExpanded: Boolean) {
        try {
            novelUserDefaults.setString(HISTORY_EXPANDED_KEY, isExpanded.toString())
            Log.d(TAG, "保存历史记录展开状态成功: $isExpanded")
        } catch (e: Exception) {
            Log.e(TAG, "保存历史记录展开状态失败", e)
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
                response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
            } else {
                Log.w(TAG, "获取推荐榜单失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取推荐榜单失败", e)
            emptyList()
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
                response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
            } else {
                Log.w(TAG, "获取热搜短剧榜失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热搜短剧榜失败", e)
            emptyList()
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
                response.data.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
            } else {
                Log.w(TAG, "获取新书榜单失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取新书榜单失败", e)
            emptyList()
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
     * 搜索小说
     */
    suspend fun searchBooks(
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
    ): RestRespPageRespDtoBookInfoRespDto {
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
        
        // 将API响应转换为我们的数据模型
        return RestRespPageRespDtoBookInfoRespDto(
            code = response.code?.toIntOrNull(),
            message = response.message,
            data = response.data?.let { apiData ->
                PageRespDtoBookInfoRespDto(
                    pageNum = apiData.pageNum,
                    pageSize = apiData.pageSize,
                    total = apiData.total,
                    pages = apiData.pages,
                    list = apiData.list.map { apiBook ->
                        BookInfoRespDto(
                            id = apiBook.id,
                            categoryId = apiBook.categoryId,
                            categoryName = apiBook.categoryName,
                            picUrl = apiBook.picUrl,
                            bookName = apiBook.bookName,
                            authorId = apiBook.authorId,
                            authorName = apiBook.authorName,
                            bookDesc = apiBook.bookDesc,
                            bookStatus = apiBook.bookStatus,
                            visitCount = apiBook.visitCount,
                            wordCount = apiBook.wordCount,
                            commentCount = apiBook.commentCount,
                            firstChapterId = apiBook.firstChapterId,
                            lastChapterId = apiBook.lastChapterId,
                            lastChapterName = apiBook.lastChapterName,
                            updateTime = apiBook.updateTime
                        )
                    }
                )
            },
            ok = response.ok
        )
    }
    
    // endregion
} 