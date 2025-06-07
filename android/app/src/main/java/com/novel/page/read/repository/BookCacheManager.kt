package com.novel.page.read.repository

import android.content.Context
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.utils.PageSplitter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全书缓存数据
 */
data class BookCacheData(
    val bookId: String,
    val chapters: List<ChapterContentData>,
    val chapterIds: List<String>, // 章节ID列表，用于增量更新
    val cacheTime: Long, // 缓存时间
    val bookInfo: BookInfo? = null
) {
    data class ChapterContentData(
        val chapterId: String,
        val chapterName: String,
        val content: String,
        val chapterNum: Int
    )
    
    data class BookInfo(
        val bookName: String,
        val authorName: String,
        val bookDesc: String,
        val picUrl: String,
        val visitCount: Long,
        val wordCount: Int,
        val categoryName: String
    )
}

/**
 * 页数缓存数据
 */
data class PageCountCacheData(
    val bookId: String,
    val fontSize: Int,
    val containerSize: IntSize,
    val totalPages: Int,
    val chapterPageRanges: List<ChapterPageRange>, // 每章节的页数范围
    val cacheTime: Long
) {
    data class ChapterPageRange(
        val chapterId: String,
        val startPage: Int,
        val endPage: Int,
        val pageCount: Int
    )
}

/**
 * 渐进计算状态
 */
data class ProgressiveCalculationState(
    val isCalculating: Boolean = false,
    val currentCalculatedPages: Int = 0,
    val totalChapters: Int = 0,
    val calculatedChapters: Int = 0,
    val estimatedTotalPages: Int = 0
)

/**
 * 单章节页数缓存数据
 */
data class ChapterPageCountData(
    val chapterId: String,
    val fontSize: Int,
    val containerSize: IntSize,
    val pageCount: Int,
    val cacheTime: Long
)

/**
 * 书籍缓存管理器
 * 
 * 功能：
 * 1. 缓存全书内容，支持增量更新
 * 2. 缓存不同字号的页数信息
 * 3. 两周过期自动清理
 * 4. 渐进式页数计算
 */
@Singleton
class BookCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "book_cache")
    private val contentCacheDir = File(cacheDir, "content")
    private val pageCountCacheDir = File(cacheDir, "page_count")
    
    // 缓存过期时间：两周
    private val cacheExpiryTime = TimeUnit.DAYS.toMillis(14)
    
    // 渐进计算状态
    private val _progressiveCalculationState = MutableStateFlow(ProgressiveCalculationState())
    val progressiveCalculationState: StateFlow<ProgressiveCalculationState> = _progressiveCalculationState.asStateFlow()
    
    init {
        // 创建缓存目录
        contentCacheDir.mkdirs()
        pageCountCacheDir.mkdirs()
        
        // 清理过期缓存
        cleanExpiredCaches()
    }
    
    /**
     * 获取书籍内容缓存
     */
    suspend fun getBookContentCache(bookId: String): BookCacheData? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(contentCacheDir, "${bookId}.json")
            if (!cacheFile.exists()) return@withContext null
            
            val cacheData = gson.fromJson<BookCacheData>(
                cacheFile.readText(),
                object : TypeToken<BookCacheData>() {}.type
            )
            
            // 检查是否过期
            if (System.currentTimeMillis() - cacheData.cacheTime > cacheExpiryTime) {
                cacheFile.delete()
                return@withContext null
            }
            
            cacheData
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存书籍内容缓存
     */
    suspend fun saveBookContentCache(bookCacheData: BookCacheData) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(contentCacheDir, "${bookCacheData.bookId}.json")
            cacheFile.writeText(gson.toJson(bookCacheData))
        } catch (e: Exception) {
            // 保存失败，静默处理
        }
    }
    
    /**
     * 获取页数缓存
     */
    suspend fun getPageCountCache(
        bookId: String, 
        fontSize: Int, 
        containerSize: IntSize
    ): PageCountCacheData? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generatePageCountCacheKey(bookId, fontSize, containerSize)
            val cacheFile = File(pageCountCacheDir, "${cacheKey}.json")
            if (!cacheFile.exists()) return@withContext null
            
            val cacheData = gson.fromJson<PageCountCacheData>(
                cacheFile.readText(),
                object : TypeToken<PageCountCacheData>() {}.type
            )
            
            // 检查是否过期
            if (System.currentTimeMillis() - cacheData.cacheTime > cacheExpiryTime) {
                cacheFile.delete()
                return@withContext null
            }
            
            cacheData
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存页数缓存
     */
    suspend fun savePageCountCache(pageCountCacheData: PageCountCacheData) = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generatePageCountCacheKey(
                pageCountCacheData.bookId,
                pageCountCacheData.fontSize,
                pageCountCacheData.containerSize
            )
            val cacheFile = File(pageCountCacheDir, "${cacheKey}.json")
            cacheFile.writeText(gson.toJson(pageCountCacheData))
        } catch (e: Exception) {
            // 保存失败，静默处理
        }
    }
    
    /**
     * 渐进式计算全书页数
     */
    suspend fun calculateAllPagesProgressively(
        bookCacheData: BookCacheData,
        readerSettings: ReaderSettings,
        containerSize: IntSize,
        density: Density,
        onProgressUpdate: (currentPages: Int, estimatedTotal: Int) -> Unit
    ): PageCountCacheData = withContext(Dispatchers.Default) {
        
        val totalChapters = bookCacheData.chapters.size
        var currentCalculatedPages = 0
        val chapterPageRanges = mutableListOf<PageCountCacheData.ChapterPageRange>()
        
        // 开始渐进计算
        _progressiveCalculationState.value = ProgressiveCalculationState(
            isCalculating = true,
            totalChapters = totalChapters,
            calculatedChapters = 0,
            currentCalculatedPages = 0
        )
        
        // 启动一个协程定时更新UI（每秒一次）
        val updateJob = launch {
            while (true) {
                delay(1000) // 每秒更新一次
                val currentState = _progressiveCalculationState.value
                if (!currentState.isCalculating) break
                
                onProgressUpdate(
                    currentState.currentCalculatedPages,
                    currentState.estimatedTotalPages
                )
            }
        }
        
        try {
            bookCacheData.chapters.forEachIndexed { index, chapter ->
                try {
                    // 分页计算
                    val pages = PageSplitter.splitContent(
                        content = chapter.content,
                        chapterTitle = chapter.chapterName,
                        containerSize = containerSize,
                        readerSettings = readerSettings,
                        density = density
                    )
                    
                    val startPage = currentCalculatedPages
                    val pageCount = pages.size
                    val endPage = startPage + pageCount - 1
                    
                    chapterPageRanges.add(
                        PageCountCacheData.ChapterPageRange(
                            chapterId = chapter.chapterId,
                            startPage = startPage,
                            endPage = endPage,
                            pageCount = pageCount
                        )
                    )
                    
                    currentCalculatedPages += pageCount
                    
                    // 估算剩余页数
                    val calculatedChapters = index + 1
                    val averagePagesPerChapter = if (calculatedChapters > 0) {
                        currentCalculatedPages.toFloat() / calculatedChapters
                    } else 1f
                    val estimatedTotalPages = (averagePagesPerChapter * totalChapters).toInt()
                    
                    // 更新状态
                    _progressiveCalculationState.value = ProgressiveCalculationState(
                        isCalculating = true,
                        currentCalculatedPages = currentCalculatedPages,
                        totalChapters = totalChapters,
                        calculatedChapters = calculatedChapters,
                        estimatedTotalPages = estimatedTotalPages
                    )
                    
                    // 给计算一些时间，让定时更新能够触发
                    delay(50)
                    
                } catch (e: Exception) {
                    // 计算失败，使用默认页数
                    val startPage = currentCalculatedPages
                    val defaultPageCount = 5 // 默认页数
                    val endPage = startPage + defaultPageCount - 1
                    
                    chapterPageRanges.add(
                        PageCountCacheData.ChapterPageRange(
                            chapterId = chapter.chapterId,
                            startPage = startPage,
                            endPage = endPage,
                            pageCount = defaultPageCount
                        )
                    )
                    currentCalculatedPages += defaultPageCount
                    
                    // 更新状态
                    val calculatedChapters = index + 1
                    val averagePagesPerChapter = if (calculatedChapters > 0) {
                        currentCalculatedPages.toFloat() / calculatedChapters
                    } else 1f
                    val estimatedTotalPages = (averagePagesPerChapter * totalChapters).toInt()
                    
                    _progressiveCalculationState.value = ProgressiveCalculationState(
                        isCalculating = true,
                        currentCalculatedPages = currentCalculatedPages,
                        totalChapters = totalChapters,
                        calculatedChapters = calculatedChapters,
                        estimatedTotalPages = estimatedTotalPages
                    )
                }
            }
        } finally {
            updateJob.cancel() // 停止定时更新
        }
        
        // 计算完成
        _progressiveCalculationState.value = ProgressiveCalculationState(
            isCalculating = false,
            currentCalculatedPages = currentCalculatedPages,
            totalChapters = totalChapters,
            calculatedChapters = totalChapters,
            estimatedTotalPages = currentCalculatedPages
        )
        
        // 最后一次更新UI
        onProgressUpdate(currentCalculatedPages, currentCalculatedPages)
        
        val pageCountCacheData = PageCountCacheData(
            bookId = bookCacheData.bookId,
            fontSize = readerSettings.fontSize,
            containerSize = containerSize,
            totalPages = currentCalculatedPages,
            chapterPageRanges = chapterPageRanges,
            cacheTime = System.currentTimeMillis()
        )
        
        // 保存页数缓存
        savePageCountCache(pageCountCacheData)
        
        pageCountCacheData
    }
    
    /**
     * 查找指定章节的页数范围
     */
    fun findChapterPageRange(
        pageCountCache: PageCountCacheData,
        chapterId: String
    ): PageCountCacheData.ChapterPageRange? {
        return pageCountCache.chapterPageRanges.find { it.chapterId == chapterId }
    }
    
    /**
     * 根据绝对页数查找对应的章节和相对页数
     */
    fun findChapterByAbsolutePage(
        pageCountCache: PageCountCacheData,
        absolutePage: Int
    ): Pair<String, Int>? { // 返回(chapterId, relativePageInChapter)
        val range = pageCountCache.chapterPageRanges.find { 
            absolutePage in it.startPage..it.endPage 
        }
        return range?.let { 
            it.chapterId to (absolutePage - it.startPage)
        }
    }
    
    /**
     * 生成页数缓存的键
     */
    private fun generatePageCountCacheKey(
        bookId: String,
        fontSize: Int,
        containerSize: IntSize
    ): String {
        return "${bookId}_${fontSize}_${containerSize.width}x${containerSize.height}"
    }
    
    /**
     * 清理过期缓存
     */
    private fun cleanExpiredCaches() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 清理内容缓存
            contentCacheDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > cacheExpiryTime) {
                    file.delete()
                }
            }
            
            // 清理页数缓存
            pageCountCacheDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > cacheExpiryTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 清理失败，静默处理
        }
    }
    
    /**
     * 清理指定书籍的所有缓存
     */
    suspend fun clearBookCache(bookId: String) = withContext(Dispatchers.IO) {
        try {
            // 清理内容缓存
            File(contentCacheDir, "${bookId}.json").delete()
            
            // 清理相关的页数缓存
            pageCountCacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("${bookId}_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 清理失败，静默处理
        }
    }
    
    /**
     * 获取单章节页数缓存
     */
    suspend fun getChapterPageCountCache(
        chapterId: String,
        fontSize: Int,
        containerSize: IntSize
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateChapterPageCountCacheKey(chapterId, fontSize, containerSize)
            val cacheFile = File(pageCountCacheDir, "chapter_${cacheKey}.json")
            if (!cacheFile.exists()) return@withContext null
            
            val cacheData = gson.fromJson<ChapterPageCountData>(
                cacheFile.readText(),
                object : TypeToken<ChapterPageCountData>() {}.type
            )
            
            // 检查是否过期
            if (System.currentTimeMillis() - cacheData.cacheTime > cacheExpiryTime) {
                cacheFile.delete()
                return@withContext null
            }
            
            cacheData.pageCount
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存单章节页数缓存
     */
    suspend fun saveChapterPageCountCache(
        chapterId: String,
        fontSize: Int,
        containerSize: IntSize,
        pageCount: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateChapterPageCountCacheKey(chapterId, fontSize, containerSize)
            val cacheData = ChapterPageCountData(
                chapterId = chapterId,
                fontSize = fontSize,
                containerSize = containerSize,
                pageCount = pageCount,
                cacheTime = System.currentTimeMillis()
            )
            val cacheFile = File(pageCountCacheDir, "chapter_${cacheKey}.json")
            cacheFile.writeText(gson.toJson(cacheData))
        } catch (e: Exception) {
            // 保存失败，静默处理
        }
    }
    
    /**
     * 生成章节页数缓存的键
     */
    private fun generateChapterPageCountCacheKey(
        chapterId: String,
        fontSize: Int,
        containerSize: IntSize
    ): String {
        return "${chapterId}_${fontSize}_${containerSize.width}x${containerSize.height}"
    }
} 