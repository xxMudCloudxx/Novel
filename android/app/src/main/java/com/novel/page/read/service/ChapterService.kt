package com.novel.page.read.service

import android.util.Log
import com.novel.page.read.components.Chapter
import com.novel.page.read.repository.BookCacheManager
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.ChapterCache
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.network.repository.CachedBookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 章节服务类
 * 
 * 负责管理章节内容的获取、缓存和预加载：
 * 1. 章节列表的获取和缓存
 * 2. 章节内容的加载和预加载
 * 3. 会话级内存缓存管理（生命周期与阅读会话同步）
 * 4. 章节分页数据的管理
 * 5. 多级缓存策略（内存缓存 + 磁盘缓存 + 网络获取）
 * 
 * 核心特性：
 * - 支持智能预加载相邻章节提升阅读体验
 * - 采用LRU策略管理内存缓存避免OOM
 * - 与BookCacheManager协同工作实现持久化缓存
 * - 提供同步和异步章节获取接口
 * - 支持章节内容分页数据的管理
 */
@Singleton
class ChapterService @Inject constructor(
    private val cachedBookRepository: CachedBookRepository,
    private val bookCacheManager: BookCacheManager
) {
    companion object {
        private const val TAG = ReaderLogTags.CHAPTER_SERVICE
        private const val MAX_CACHE_SIZE = 12 // 最大缓存大小：当前章节+前后各5章+缓冲
    }

    // 会话级章节缓存，生命周期与阅读会话同步
    // 该缓存在用户关闭阅读器时清空，避免内存占用
    private val sessionChapterCache = mutableMapOf<String, ChapterCache>()

    /**
     * 获取章节列表
     * 
     * 从CachedBookRepository获取指定书籍的章节列表
     * 
     * @param bookId 书籍ID
     * @return 章节列表
     */
    suspend fun getChapterList(bookId: String): List<Chapter> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取章节列表: bookId=$bookId")
                
                val chapters = cachedBookRepository.getBookChapters(
                    bookId = bookId.toLong(),
                    strategy = com.novel.utils.network.cache.CacheStrategy.NETWORK_FIRST
                )
                
                Log.d(TAG, "章节列表获取成功: ${chapters.size}章")
                chapters.map { chapter ->
                    Chapter(
                        id = chapter.id.toString(),
                        chapterName = chapter.chapterName,
                        chapterNum = chapter.chapterNum.toString(),
                        isVip = if (chapter.isVip == 1) "1" else "0"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取章节列表失败: bookId=$bookId", e)
                emptyList()
            }
        }
    }

    /**
     * 获取章节内容
     * 
     * 优先从会话缓存获取，缓存未命中时从Repository获取
     * 
     * @param chapterId 章节ID
     * @return 章节内容数据，失败返回null
     */
    suspend fun getChapterContent(chapterId: String): ChapterContentData? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取章节内容: chapterId=$chapterId")
                
                // 1. 优先从会话缓存获取
                sessionChapterCache[chapterId]?.let { cachedChapter ->
                    Log.d(TAG, "从会话缓存获取章节内容: ${cachedChapter.chapter.chapterName}")
                    return@withContext ChapterContentData(
                        chapterId = cachedChapter.chapter.id,
                        chapterName = cachedChapter.chapter.chapterName,
                        content = cachedChapter.content
                    )
                }
                
                // 2. 从Repository获取
                val contentData = cachedBookRepository.getBookContent(
                    chapterId = chapterId.toLong(),
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                
                if (contentData != null) {
                    Log.d(TAG, "从Repository获取章节内容成功: ${contentData.chapterInfo.chapterName}")
                    
                    // 3. 创建章节对象并添加到会话缓存
                    val chapter = Chapter(
                        id = contentData.chapterInfo.id.toString(),
                        chapterName = contentData.chapterInfo.chapterName,
                        chapterNum = contentData.chapterInfo.chapterNum.toString(),
                        isVip = if (contentData.chapterInfo.isVip == 1) "1" else "0"
                    )
                    
                    val chapterCache = ChapterCache(
                        chapter = chapter,
                        content = contentData.bookContent
                    )
                    
                    addToSessionCache(chapterId, chapterCache)
                    
                    return@withContext ChapterContentData(
                        chapterId = chapter.id,
                        chapterName = chapter.chapterName,
                        content = contentData.bookContent
                    )
                } else {
                    Log.w(TAG, "章节内容获取失败: chapterId=$chapterId")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取章节内容异常: chapterId=$chapterId", e)
                null
            }
        }
    }

    /**
     * 预加载章节内容
     * 
     * 异步预加载指定章节，用于提升阅读体验
     * 预加载的内容会存储在会话缓存中
     * 
     * @param chapterId 章节ID
     */
    suspend fun preloadChapter(chapterId: String) {
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已在会话缓存中
                if (sessionChapterCache.containsKey(chapterId)) {
                    Log.d(TAG, "章节已在会话缓存中，跳过预加载: chapterId=$chapterId")
                    return@withContext
                }
                
                Log.d(TAG, "开始预加载章节: chapterId=$chapterId")
                
                // 从Repository获取内容
                val contentData = cachedBookRepository.getBookContent(
                    chapterId = chapterId.toLong(),
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                
                if (contentData != null) {
                    val chapter = Chapter(
                        id = contentData.chapterInfo.id.toString(),
                        chapterName = contentData.chapterInfo.chapterName,
                        chapterNum = contentData.chapterInfo.chapterNum.toString(),
                        isVip = if (contentData.chapterInfo.isVip == 1) "1" else "0"
                    )
                    
                    val chapterCache = ChapterCache(
                        chapter = chapter,
                        content = contentData.bookContent
                    )
                    
                    addToSessionCache(chapterId, chapterCache)
                    Log.d(TAG, "章节预加载完成: ${chapter.chapterName}")
                } else {
                    Log.w(TAG, "章节预加载失败: chapterId=$chapterId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "章节预加载异常: chapterId=$chapterId", e)
            }
        }
    }

    /**
     * 获取会话缓存中的章节
     * 
     * @param chapterId 章节ID
     * @return 缓存的章节数据，未找到返回null
     */
    fun getCachedChapter(chapterId: String): ChapterCache? {
        return sessionChapterCache[chapterId]
    }

    /**
     * 设置缓存章节的分页数据
     * 
     * @param chapterId 章节ID
     * @param pageData 分页数据
     */
    fun setCachedChapterPageData(chapterId: String, pageData: PageData) {
        sessionChapterCache[chapterId]?.let { cachedChapter ->
            sessionChapterCache[chapterId] = cachedChapter.copy(pageData = pageData)
            Log.d(TAG, "更新章节分页数据: ${cachedChapter.chapter.chapterName}, 页数=${pageData.pages.size}")
        }
    }

    /**
     * 添加到会话缓存
     * 
     * 使用LRU策略管理缓存大小，避免内存溢出
     * 
     * @param chapterId 章节ID
     * @param chapterCache 章节缓存数据
     */
    private fun addToSessionCache(chapterId: String, chapterCache: ChapterCache) {
        // 如果缓存已满，移除最旧的条目（LRU策略）
        if (sessionChapterCache.size >= MAX_CACHE_SIZE) {
            val oldestKey = sessionChapterCache.keys.first()
            val removedChapter = sessionChapterCache.remove(oldestKey)
            Log.d(TAG, "移除最旧的缓存章节: ${removedChapter?.chapter?.chapterName}")
        }
        
        sessionChapterCache[chapterId] = chapterCache
        Log.d(TAG, "添加到会话缓存: ${chapterCache.chapter.chapterName} (缓存大小=${sessionChapterCache.size})")
    }

    /**
     * 加载书籍信息
     *
     * 用于在阅读器首页显示书籍详情
     *
     * @param bookId 书籍ID
     * @return 书籍信息，失败时返回null
     */
    suspend fun loadBookInfo(bookId: String): PageData.BookInfo? {
        val bookIdLong = bookId.toLongOrNull() ?: return null

        return try {
            val bookInfoData = cachedBookRepository.getBookInfo(bookIdLong)
            bookInfoData?.let {
                PageData.BookInfo(
                    bookId = bookId,
                    bookName = it.bookName,
                    authorName = it.authorName,
                    bookDesc = it.bookDesc,
                    picUrl = it.picUrl,
                    visitCount = it.visitCount,
                    wordCount = it.wordCount,
                    categoryName = it.categoryName
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清空会话缓存
     * 
     * 在阅读会话结束时调用，释放内存
     */
    fun clearSessionCache() {
        Log.d(TAG, "清空会话缓存: ${sessionChapterCache.size}个章节")
        sessionChapterCache.clear()
    }

    /**
     * 章节内容数据
     */
    data class ChapterContentData(
        val chapterId: String,
        val chapterName: String,
        val content: String
    )
} 