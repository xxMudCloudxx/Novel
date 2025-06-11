package com.novel.page.search.service

import android.util.Log
import com.novel.page.search.component.SearchRankingItem
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.SearchService as ApiSearchService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索服务类
 * 封装搜索模块相关的网络请求
 */
@Singleton
class SearchService @Inject constructor(
    private val apiSearchService: ApiSearchService,
    private val bookService: BookService
) {
    
    companion object {
        private const val TAG = "SearchPageService"
    }
    
    /**
     * 获取热门小说榜单
     */
    suspend fun getHotNovelRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取热门小说榜单")
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
                Log.w(TAG, "获取热门小说榜单失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热门小说榜单失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取热门短剧榜单
     */
    suspend fun getHotDramaRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取热门短剧榜单")
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
                Log.w(TAG, "获取热门短剧榜单失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热门短剧榜单失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取新书榜单
     */
    suspend fun getNewBookRanking(): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "获取新书榜单")
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
     * 搜索书籍
     */
    suspend fun searchBooks(
        keyword: String,
        pageNum: Int = 1,
        pageSize: Int = 10
    ): List<SearchRankingItem> {
        return try {
            Log.d(TAG, "搜索书籍: $keyword")
            val response = apiSearchService.searchBooksByKeywordBlocking(keyword, pageNum, pageSize)
            
            if (response.ok == true && response.data?.list != null) {
                response.data.list.mapIndexed { index, book ->
                    SearchRankingItem(
                        id = book.id,
                        title = book.bookName,
                        author = book.authorName,
                        rank = index + 1
                    )
                }
            } else {
                Log.w(TAG, "搜索书籍失败: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜索书籍失败", e)
            emptyList()
        }
    }
} 