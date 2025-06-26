package com.novel.page.read.service

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.repository.BookCacheData
import com.novel.page.read.repository.BookCacheManager
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.utils.PageSplitter
import com.novel.utils.network.repository.CachedBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分页服务类
 * 
 * 负责章节内容的分页计算和全书页数缓存：
 * 1. 单章节内容分页计算
 * 2. 全书页数缓存管理
 * 3. 渐进式分页计算（后台任务）
 * 4. 页数查询和索引转换
 * 
 * 核心特性：
 * - 支持不同字体大小和容器尺寸的分页缓存
 * - 渐进式计算大型书籍避免阻塞UI
 * - 提供全局页码定位功能
 * - 后台全书内容预取和分页计算
 * - 页数缓存管理和查询
 */
@Singleton
class PaginationService @Inject constructor(
    private val bookCacheManager: BookCacheManager,
    private val cachedBookRepository: CachedBookRepository
) {
    // 分页后台任务管理
    private var paginationJob: Job? = null

    /**
     * 分页状态流
     * 实时反馈渐进式分页的计算进度
     */
    val paginationState: StateFlow<com.novel.page.read.repository.ProgressiveCalculationState>
        get() = bookCacheManager.progressiveCalculationState

    /**
     * 分割章节内容为页面
     * 
     * 使用PageSplitter工具根据容器尺寸和阅读设置进行分页
     * 
     * @param content 章节内容文本
     * @param containerSize 容器尺寸（宽高）
     * @param readerSettings 阅读器设置（字体大小等）
     * @param density 屏幕密度信息
     * @return 分页后的文本列表
     */
    fun splitContent(
        content: String,
        containerSize: IntSize,
        readerSettings: ReaderSettings,
        density: Density
    ): List<String> {
        return PageSplitter.splitContent(content, containerSize, readerSettings, density)
    }

    /**
     * 获取页数缓存
     * 
     * 这是old.txt中的getPageCountCache函数的实现
     * 查询指定书籍在给定字体大小和容器尺寸下的分页缓存
     * 
     * @param bookId 书籍ID
     * @param fontSize 字体大小
     * @param containerSize 容器尺寸
     * @return 页数缓存数据，未找到返回null
     */
    suspend fun getPageCountCache(
        bookId: String,
        fontSize: Int,
        containerSize: IntSize
    ): PageCountCacheData? {
        return bookCacheManager.getPageCountCache(bookId, fontSize, containerSize)
    }

    /**
     * 获取全书内容并在后台进行分页计算
     * 
     * 这是old.txt中的fetchAllBookContentAndPaginateInBackground函数的实现
     * 该函数负责：
     * 1. 检查或构建完整的书籍内容缓存
     * 2. 启动后台渐进式分页计算  
     * 3. 实时更新分页进度状态
     * 4. 处理全书页数缓存的生成和更新
     * 
     * @param bookId 书籍ID
     * @param chapterList 章节列表
     * @param readerSettings 阅读器设置
     * @param containerSize 容器尺寸
     * @param density 屏幕密度
     */
    fun fetchAllBookContentAndPaginateInBackground(
        bookId: String,
        chapterList: List<com.novel.page.read.components.Chapter>,
        readerSettings: ReaderSettings,
        containerSize: IntSize,
        density: Density
    ) {
        // 取消之前的分页任务，确保只有一个任务在运行
        paginationJob?.cancel()
        paginationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 获取或构建书籍内容缓存
            val bookCache = bookCacheManager.getBookContentCache(bookId)
            val allChapterIds = chapterList.map { it.id }.toSet()

            val completeBookCache = if (bookCache != null && bookCache.chapterIds.toSet() == allChapterIds) {
                    // 缓存完整，直接使用
                bookCache
            } else {
                    // 缓存不完整，重新构建
                fetchAndBuildBookCache(bookId, chapterList, bookCache)
            }

                // 2. 启动渐进式分页计算
            startProgressivePagination(completeBookCache, readerSettings, containerSize, density)
            } catch (e: Exception) {
                // 后台任务失败，记录错误但不影响主流程
                android.util.Log.e("PaginationService", "后台全书分页计算失败", e)
            }
        }
    }

    /**
     * 构建或更新书籍内容缓存
     * 
     * 获取缺失的章节内容并更新缓存
     * 
     * @param bookId 书籍ID
     * @param chapterList 完整章节列表
     * @param existingBookCache 现有缓存（可能不完整）
     * @return 完整的书籍缓存数据
     */
    private suspend fun fetchAndBuildBookCache(
        bookId: String,
        chapterList: List<com.novel.page.read.components.Chapter>,
        existingBookCache: BookCacheData?
    ): BookCacheData {
        val cachedChapters = existingBookCache?.chapters?.toMutableList() ?: mutableListOf()
        val cachedChapterIds = cachedChapters.map { it.chapterId }.toSet()
        val chaptersToFetch = chapterList.filterNot { cachedChapterIds.contains(it.id) }

        // 并行获取缺失的章节内容
        chaptersToFetch.forEach { chapter ->
            try {
                val contentData = cachedBookRepository.getBookContent(
                    chapterId = chapter.id.toLong(),
                    strategy = com.novel.utils.network.cache.CacheStrategy.CACHE_FIRST
                )
                if (contentData != null) {
                    cachedChapters.add(
                        BookCacheData.ChapterContentData(
                            chapterId = contentData.chapterInfo.id.toString(),
                            chapterName = contentData.chapterInfo.chapterName,
                            content = contentData.bookContent,
                            chapterNum = contentData.chapterInfo.chapterNum
                        )
                    )
                }
            } catch (e: Exception) {
                // 章节获取失败，记录但继续处理其他章节
                android.util.Log.w("PaginationService", "获取章节内容失败: ${chapter.chapterName}", e)
            }
        }

        // 按章节序号排序
        cachedChapters.sortBy { it.chapterNum }

        // 获取书籍信息
        val bookInfoData = try {
            cachedBookRepository.getBookInfo(bookId.toLong())
        } catch (e: Exception) {
            null
        }
        
        val bookInfoForCache = bookInfoData?.let {
            BookCacheData.BookInfo(
                bookName = it.bookName,
                authorName = it.authorName,
                bookDesc = it.bookDesc,
                picUrl = it.picUrl,
                visitCount = it.visitCount,
                wordCount = it.wordCount,
                categoryName = it.categoryName
            )
        }

        val newBookCacheData = BookCacheData(
            bookId = bookId,
            chapters = cachedChapters,
            chapterIds = cachedChapters.map { it.chapterId },
            cacheTime = System.currentTimeMillis(),
            bookInfo = bookInfoForCache
        )

        // 保存更新后的缓存
        bookCacheManager.saveBookContentCache(newBookCacheData)
        return newBookCacheData
    }

    /**
     * 启动渐进式分页计算
     * 
     * 检查是否已有页数缓存，如果没有则启动后台分页计算
     * 
     * @param bookCacheData 完整的书籍内容缓存
     * @param readerSettings 阅读器设置
     * @param containerSize 容器尺寸
     * @param density 屏幕密度
     */
    private suspend fun startProgressivePagination(
        bookCacheData: BookCacheData,
        readerSettings: ReaderSettings,
        containerSize: IntSize,
        density: Density
    ) {
        if (containerSize == IntSize.Zero) return

        // 检查是否已有页数缓存
        val existingPageCountCache = bookCacheManager.getPageCountCache(
            bookCacheData.bookId,
            readerSettings.fontSize,
            containerSize
        )
        if (existingPageCountCache != null) {
            return // 已有缓存，无需重新计算
        }

        // 启动渐进式分页计算
        try {
        bookCacheManager.calculateAllPagesProgressively(
            bookCacheData = bookCacheData,
            readerSettings = readerSettings,
            containerSize = containerSize,
            density = density,
                onProgressUpdate = { calculatedChapters, totalChapters ->
                    // 进度更新回调，可用于日志记录
                    android.util.Log.d("PaginationService", "分页进度: $calculatedChapters/$totalChapters")
                }
        )
        } catch (e: Exception) {
            android.util.Log.e("PaginationService", "渐进式分页计算失败", e)
        }
    }

    /**
     * 根据全局页码查找对应的章节和页面
     * 
     * @param pageCountCache 页数缓存数据
     * @param globalPage 全局页码（从0开始）
     * @return 章节ID和章节内相对页码的配对，未找到返回null
     */
    fun findChapterByAbsolutePage(pageCountCache: PageCountCacheData, globalPage: Int): Pair<String, Int>? {
        return bookCacheManager.findChapterByAbsolutePage(pageCountCache, globalPage)
    }

    /**
     * 取消后台分页任务
     * 在需要时停止正在进行的分页计算
     */
    fun cancelPaginationJob() {
        paginationJob?.cancel()
        paginationJob = null
    }

} 