package com.novel.page.book.repository

import com.novel.page.book.components.Chapter
import com.novel.page.book.components.ReaderSettings
import com.novel.utils.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 章节内容数据类
 */
data class ChapterContent(
    val chapter: Chapter,
    val content: String,
    val bookInfo: BookInfo? = null
)

/**
 * 书籍信息数据类
 */
data class BookInfo(
    val id: String,
    val bookName: String,
    val authorName: String,
    val bookDesc: String
)

/**
 * API响应数据类
 */
sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val throwable: Throwable? = null) : ApiResult<T>()
}

/**
 * 阅读器Repository - 数据层
 * 负责数据的获取、缓存和管理
 */
@Singleton
class ReaderRepository @Inject constructor() {

    // 设置缓存
    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings: Flow<ReaderSettings> = _readerSettings.asStateFlow()

    // 章节列表缓存
    private val chapterListCache = mutableMapOf<String, List<Chapter>>()
    
    // 章节内容缓存
    private val chapterContentCache = mutableMapOf<String, ChapterContent>()

    /**
     * 获取章节列表
     */
    fun getChapterList(bookId: String): Flow<ApiResult<List<Chapter>>> = flow {
        try {
            // 先检查缓存
            val cachedChapters = chapterListCache[bookId]
            if (cachedChapters != null) {
                emit(ApiResult.Success(cachedChapters))
                return@flow
            }

            // 从网络获取
            val response = getChapterListFromNetwork(bookId)
            chapterListCache[bookId] = response // 缓存结果
            emit(ApiResult.Success(response))
        } catch (e: Exception) {
            emit(ApiResult.Error("获取章节列表失败: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取章节内容
     */
    fun getChapterContent(chapterId: String): Flow<ApiResult<ChapterContent>> = flow {
        try {
            // 先检查缓存
            val cachedContent = chapterContentCache[chapterId]
            if (cachedContent != null) {
                emit(ApiResult.Success(cachedContent))
                return@flow
            }

            // 从网络获取
            val response = getChapterContentFromNetwork(chapterId)
            chapterContentCache[chapterId] = response // 缓存结果
            emit(ApiResult.Success(response))
        } catch (e: Exception) {
            emit(ApiResult.Error("获取章节内容失败: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 更新阅读器设置
     */
    suspend fun updateReaderSettings(settings: ReaderSettings) {
        _readerSettings.value = settings
        // 这里可以添加本地存储逻辑
    }

    /**
     * 从网络获取章节列表
     */
    private suspend fun getChapterListFromNetwork(bookId: String): List<Chapter> = 
        suspendCancellableCoroutine { continuation ->
            ApiService.get(
                baseUrl = ApiService.BASE_URL_FRONT,
                endpoint = "book/chapter/list",
                params = mapOf("bookId" to bookId)
            ) { response, error ->
                if (error != null) {
                    continuation.resume(throw Exception("网络请求失败: ${error.message}"))
                } else if (response != null) {
                    try {
                        val jsonResponse = JSONObject(response)
                        val code = jsonResponse.getString("code")
                        
                        if (code == "00000") {
                            val dataArray = jsonResponse.getJSONArray("data")
                            val chapters = mutableListOf<Chapter>()
                            
                            for (i in 0 until dataArray.length()) {
                                val chapterJson = dataArray.getJSONObject(i)
                                chapters.add(
                                    Chapter(
                                        id = chapterJson.getString("id"),
                                        chapterName = chapterJson.getString("chapterName"),
                                        chapterNum = chapterJson.optString("chapterNum"),
                                        isVip = chapterJson.optString("isVip", "0")
                                    )
                                )
                            }
                            
                            continuation.resume(chapters)
                        } else {
                            val message = jsonResponse.optString("message", "获取章节列表失败")
                            continuation.resume(throw Exception(message))
                        }
                    } catch (e: Exception) {
                        continuation.resume(throw Exception("解析响应失败: ${e.message}"))
                    }
                } else {
                    continuation.resume(throw Exception("响应为空"))
                }
            }
        }

    /**
     * 从网络获取章节内容
     */
    private suspend fun getChapterContentFromNetwork(chapterId: String): ChapterContent = 
        suspendCancellableCoroutine { continuation ->
            ApiService.get(
                baseUrl = ApiService.BASE_URL_FRONT,
                endpoint = "book/content",
                params = mapOf("chapterId" to chapterId)
            ) { response, error ->
                if (error != null) {
                    continuation.resume(throw Exception("网络请求失败: ${error.message}"))
                } else if (response != null) {
                    try {
                        val jsonResponse = JSONObject(response)
                        val code = jsonResponse.getString("code")
                        
                        if (code == "00000") {
                            val data = jsonResponse.getJSONObject("data")
                            val chapterInfo = data.getJSONObject("chapterInfo")
                            val bookContent = data.getString("bookContent")
                            val bookInfoJson = data.optJSONObject("bookInfo")
                            
                            val chapter = Chapter(
                                id = chapterInfo.getString("id"),
                                chapterName = chapterInfo.getString("chapterName"),
                                chapterNum = chapterInfo.optString("chapterNum"),
                                isVip = chapterInfo.optString("isVip", "0")
                            )
                            
                            val bookInfo = bookInfoJson?.let {
                                BookInfo(
                                    id = it.getString("id"),
                                    bookName = it.getString("bookName"),
                                    authorName = it.getString("authorName"),
                                    bookDesc = it.getString("bookDesc")
                                )
                            }
                            
                            val chapterContent = ChapterContent(
                                chapter = chapter,
                                content = bookContent,
                                bookInfo = bookInfo
                            )
                            
                            continuation.resume(chapterContent)
                        } else {
                            val message = jsonResponse.optString("message", "获取章节内容失败")
                            continuation.resume(throw Exception(message))
                        }
                    } catch (e: Exception) {
                        continuation.resume(throw Exception("解析响应失败: ${e.message}"))
                    }
                } else {
                    continuation.resume(throw Exception("响应为空"))
                }
            }
        }

    /**
     * 清除缓存
     */
    fun clearCache() {
        chapterListCache.clear()
        chapterContentCache.clear()
    }

    /**
     * 清除指定书籍的缓存
     */
    fun clearBookCache(bookId: String) {
        chapterListCache.remove(bookId)
        // 清除该书籍相关的章节内容缓存
        val chaptersToRemove = chapterContentCache.keys.filter { chapterId ->
            chapterListCache[bookId]?.any { it.id == chapterId } == true
        }
        chaptersToRemove.forEach { chapterContentCache.remove(it) }
    }

    /**
     * 预加载下一章内容
     */
    suspend fun preloadNextChapter(currentChapterId: String, chapterList: List<Chapter>) {
        val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
        if (currentIndex >= 0 && currentIndex < chapterList.size - 1) {
            val nextChapter = chapterList[currentIndex + 1]
            if (!chapterContentCache.containsKey(nextChapter.id)) {
                try {
                    val content = getChapterContentFromNetwork(nextChapter.id)
                    chapterContentCache[nextChapter.id] = content
                } catch (e: Exception) {
                    // 预加载失败，静默处理
                }
            }
        }
    }
} 