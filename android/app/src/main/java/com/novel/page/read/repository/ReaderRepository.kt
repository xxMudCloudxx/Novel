package com.novel.page.read.repository

import com.novel.page.read.components.Chapter
import com.novel.page.read.components.ReaderSettings
import com.novel.utils.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    
    // 预缓存队列 - 记录需要预加载的章节
    private val preCacheQueue = mutableSetOf<String>()
    
    // 协程作用域用于预缓存
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
     * 获取章节内容，支持预缓存
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
     * 预缓存章节内容 - 异步加载，不影响主要流程
     */
    suspend fun preCacheChapterContent(chapterId: String, bookId: String) {
        if (chapterId in preCacheQueue || chapterContentCache.containsKey(chapterId)) {
            return // 已在队列中或已缓存，跳过
        }
        
        preCacheQueue.add(chapterId)
        
        try {
            val content = getChapterContentFromNetwork(chapterId)
            chapterContentCache[chapterId] = content
            
            // 触发前后章节的递归预缓存
            val chapterList = chapterListCache[bookId]
            if (chapterList != null) {
                val currentIndex = chapterList.indexOfFirst { it.id == chapterId }
                if (currentIndex != -1) {
                    // 预缓存前后各2个章节
                    for (offset in listOf(-2, -1, 1, 2)) {
                        val targetIndex = currentIndex + offset
                        if (targetIndex in chapterList.indices) {
                            val targetChapterId = chapterList[targetIndex].id
                            if (!chapterContentCache.containsKey(targetChapterId) && targetChapterId !in preCacheQueue) {
                                // 递归预缓存（但不触发更深层的预缓存）
                                cacheScope.launch {
                                    try {
                                        preCacheQueue.add(targetChapterId)
                                        val targetContent = getChapterContentFromNetwork(targetChapterId)
                                        chapterContentCache[targetChapterId] = targetContent
                                        preCacheQueue.remove(targetChapterId)
                                    } catch (e: Exception) {
                                        preCacheQueue.remove(targetChapterId)
                                        // 预缓存失败不影响主流程，只记录日志
                                        println("预缓存章节失败: $targetChapterId, ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 预缓存失败不影响主流程
            println("预缓存章节失败: $chapterId, ${e.message}")
        } finally {
            preCacheQueue.remove(chapterId)
        }
    }

    /**
     * 获取带有前后章节数据的章节内容
     */
    fun getChapterContentWithAdjacent(chapterId: String, bookId: String): Flow<ApiResult<ChapterContent>> = flow {
        try {
            // 先获取主章节内容
            val mainContent = chapterContentCache[chapterId] ?: getChapterContentFromNetwork(chapterId)
            chapterContentCache[chapterId] = mainContent
            
            // 获取章节列表以确定前后章节
            val chapterList = chapterListCache[bookId] ?: getChapterListFromNetwork(bookId)
            val currentIndex = chapterList.indexOfFirst { it.id == chapterId }
            
            if (currentIndex != -1) {
                // 异步预缓存前后章节
                cacheScope.launch {
                    // 预缓存下一章
                    if (currentIndex + 1 < chapterList.size) {
                        preCacheChapterContent(chapterList[currentIndex + 1].id, bookId)
                    }
                    // 预缓存上一章
                    if (currentIndex - 1 >= 0) {
                        preCacheChapterContent(chapterList[currentIndex - 1].id, bookId)
                    }
                }
            }
            
            emit(ApiResult.Success(mainContent))
        } catch (e: Exception) {
            emit(ApiResult.Error("获取章节内容失败: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 清理缓存 - 避免内存过度占用
     */
    fun clearOldCache(currentChapterId: String, bookId: String) {
        val chapterList = chapterListCache[bookId] ?: return
        val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
        
        if (currentIndex != -1) {
            // 只保留当前章节前后各5章的缓存
            val keepRange = (currentIndex - 5)..(currentIndex + 5)
            val chaptersToKeep = chapterList.filterIndexed { index, _ -> 
                index in keepRange 
            }.map { it.id }.toSet()
            
            // 移除不需要的缓存
            val keysToRemove = chapterContentCache.keys - chaptersToKeep
            keysToRemove.forEach { key ->
                chapterContentCache.remove(key)
            }
        }
    }

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