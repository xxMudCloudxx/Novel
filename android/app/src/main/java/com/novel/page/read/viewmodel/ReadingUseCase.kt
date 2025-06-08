package com.novel.page.read.viewmodel

import com.novel.page.read.repository.ReaderRepositoryImpl
import com.novel.page.read.utils.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读用例
 * 整合阅读相关的业务逻辑
 */
@Singleton
class ReadingUseCase @Inject constructor(
    private val repository: ReaderRepositoryImpl
) {
    
    private val _progressFlow = MutableSharedFlow<ReadingProgressEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val progressFlow: Flow<ReadingProgressEvent> = _progressFlow
    
    /**
     * 初始化阅读器
     */
    suspend fun initReader(bookId: String, chapterId: String? = null): Result<PageData> = safeCall {
        require(bookId.isNotBlank()) { "Book ID cannot be blank" }
        
        val chapters = repository.getChapterList(bookId).getOrThrow()
        if (chapters.isEmpty()) {
            throw Exception("No chapters found for book $bookId")
        }
        
        val targetChapterId = chapterId ?: chapters.first().id
        val pageData = repository.getChapterContent(targetChapterId).getOrThrow()
        
        // 预加载相邻章节
        preloadAdjacentChapters(bookId, targetChapterId, chapters)
        
        pageData
    }
    
    /**
     * 获取章节内容
     */
    suspend fun getChapterContent(chapterId: String): Result<PageData> = safeCall {
        require(chapterId.isNotBlank()) { "Chapter ID cannot be blank" }
        repository.getChapterContent(chapterId).getOrThrow()
    }
    
    /**
     * 切换章节
     */
    suspend fun switchChapter(
        currentBookId: String, 
        targetChapterId: String, 
        direction: FlipDirection
    ): Result<PageData> = safeCall {
        val pageData = repository.getChapterContent(targetChapterId).getOrThrow()
        
        // 发送进度事件
        val pageIndex = when (direction) {
            FlipDirection.NEXT -> 0
            FlipDirection.PREVIOUS -> pageData.pages.size - 1
        }
        
        val progressEvent = ReadingProgressEvent(
            bookId = currentBookId,
            chapterId = targetChapterId,
            pageIndex = pageIndex,
            progress = calculateProgress(pageIndex, pageData.pages.size)
        )
        
        _progressFlow.tryEmit(progressEvent)
        
        pageData
    }
    
    /**
     * 更新阅读进度
     */
    suspend fun updateProgress(
        bookId: String,
        chapterId: String,
        pageIndex: Int,
        totalPages: Int
    ): Result<Unit> = safeCall {
        val progress = calculateProgress(pageIndex, totalPages)
        
        val progressEvent = ReadingProgressEvent(
            bookId = bookId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            progress = progress
        )
        
        _progressFlow.tryEmit(progressEvent)
        
        repository.saveReadingProgress(bookId, chapterId, pageIndex, progress)
    }
    
    /**
     * 分页内容
     */
    suspend fun paginateContent(
        content: String,
        chapterTitle: String,
        containerSize: androidx.compose.ui.unit.IntSize,
        readerConfig: ReaderConfig,
        density: androidx.compose.ui.unit.Density
    ): Result<List<String>> = safeCall {
        require(content.isNotBlank()) { "Content cannot be blank" }
        require(containerSize.width > 0 && containerSize.height > 0) { 
            "Container size must be positive: $containerSize" 
        }
        
        repository.paginateContent(content, chapterTitle, containerSize, readerConfig, density)
    }
    
    /**
     * 清理和格式化内容
     */
    fun formatContent(rawContent: String): String {
        return rawContent
            .replace("<br/><br/>", "\n\n")
            .replace("<br/>", "\n")
            .replace(Regex("<[^>]*>"), "") // 移除HTML标签
            .trim()
    }
    
    private suspend fun preloadAdjacentChapters(
        bookId: String,
        currentChapterId: String,
        chapters: List<ChapterInfo>
    ) {
        val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIndex == -1) return
        
        // 预加载前后各2章
        val preloadRange = listOf(
            currentIndex - 2,
            currentIndex - 1,
            currentIndex + 1,
            currentIndex + 2
        ).filter { it in chapters.indices }
        
        preloadRange.forEach { index ->
            repository.preloadChapter(chapters[index].id)
        }
    }
    
    private fun calculateProgress(pageIndex: Int, totalPages: Int): Float {
        return if (totalPages > 0) {
            (pageIndex + 1).toFloat() / totalPages.toFloat()
        } else 0f
    }
}

/**
 * 翻页用例
 */
@Singleton
class PageFlipUseCase @Inject constructor() {
    
    private val _progressFlow = MutableSharedFlow<ReadingProgressEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val progressFlow: Flow<ReadingProgressEvent> = _progressFlow
    
    /**
     * 执行翻页操作
     */
    suspend fun flip(
        strategy: PageFlipStrategy,
        request: FlipRequest,
        bookId: String
    ): Result<FlipResult> = safeCall {
        val result = strategy.flip(request)
        
        // 如果翻页成功，更新进度
        if (result is FlipResult.Success) {
            val progressEvent = ReadingProgressEvent(
                bookId = bookId,
                chapterId = request.pageData.chapterId,
                pageIndex = result.newPageIndex,
                progress = calculateProgress(result.newPageIndex, request.pageData.totalPageCount)
            )
            
            _progressFlow.tryEmit(progressEvent)
        }
        
        result
    }
    
    private fun calculateProgress(pageIndex: Int, totalPages: Int): Float {
        return if (totalPages > 0) {
            (pageIndex + 1).toFloat() / totalPages.toFloat()
        } else 0f
    }
}

/**
 * 进度事件总线
 */
@Singleton
class ProgressEventBus @Inject constructor() {
    
    private val _progressEvents = MutableSharedFlow<ReadingProgressEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val progressEvents: Flow<ReadingProgressEvent> = _progressEvents
    
    fun publishProgressUpdate(
        bookId: String,
        chapterId: String,
        pageIndex: Int,
        totalPages: Int,
        readingProgress: Float
    ) {
        val event = ReadingProgressEvent(
            bookId = bookId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            progress = readingProgress
        )
        _progressEvents.tryEmit(event)
    }
} 