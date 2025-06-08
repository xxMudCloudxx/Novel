package com.novel.page.read.repository

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.page.read.viewmodel.*
import com.novel.page.read.utils.*
import com.novel.utils.network.api.front.BookService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 整合后的Repository实现
 * 使用ReaderRepository中定义的数据源接口，避免重复定义
 */
@Singleton
class ReaderRepositoryImpl @Inject constructor(
    private val readerRepository: ReaderRepository
) {
    
    suspend fun getChapterList(bookId: String): Result<List<ChapterInfo>> {
        return readerRepository.getChapterListNew(bookId)
    }
    
    suspend fun getChapterContent(chapterId: String): Result<PageData> = safeCall {
        val result = readerRepository.getChapterContentNew(chapterId)
        result.getOrThrow()
    }
    
    suspend fun preloadChapter(chapterId: String) {
        try {
            // 通过主Repository预加载
            readerRepository.getChapterContentNew(chapterId)
        } catch (e: Exception) {
            android.util.Log.w("ReaderRepository", "Preload failed for chapter: $chapterId", e)
        }
    }
    
    suspend fun paginateContent(
        content: String,
        chapterTitle: String,
        containerSize: IntSize,
        readerConfig: ReaderConfig,
        density: Density
    ): List<String> {
        return try {
            // 使用现有的PageSplitter进行分页
            PageSplitter.splitContent(
                content = content,
                chapterTitle = chapterTitle,
                containerSize = containerSize,
                readerSettings = readerConfig.toOldReaderSettings(),
                density = density
            )
        } catch (e: Exception) {
            listOf(content)
        }
    }
    
    suspend fun saveReadingProgress(
        bookId: String,
        chapterId: String,
        pageIndex: Int,
        progress: Float
    ) {
        // 通过主Repository保存进度
    }
    
    suspend fun getReadingProgress(bookId: String): Result<ReadingProgressEvent?> = safeCall {
        null // 暂时返回null
    }
    
    suspend fun getBookInfo(bookId: String): Result<PageData.BookInfo> = safeCall {
        // 暂时返回空的BookInfo，因为这个方法在新架构中不需要
        PageData.BookInfo(
            bookId = bookId,
            bookName = "",
            authorName = "",
            bookDesc = "",
            picUrl = "",
            visitCount = 0L,
            wordCount = 0,
            categoryName = ""
        )
    }
    
    suspend fun clearCache(bookId: String?) {
        // 通过主Repository清理缓存
        if (bookId != null) {
            readerRepository.clearBookCache(bookId)
        } else {
            readerRepository.clearCache()
        }
    }
    
    // 扩展函数：新配置转换为旧设置（兼容性）
    private fun ReaderConfig.toOldReaderSettings(): com.novel.page.read.components.ReaderSettings {
        return com.novel.page.read.components.ReaderSettings(
            fontSize = this.fontSize.value.toInt(),
            backgroundColor = this.backgroundColor,
            textColor = this.textColor,
            pageFlipEffect = when (this.pageFlipEffect) {
                PageFlipEffect.NONE -> com.novel.page.read.components.PageFlipEffect.NONE
                PageFlipEffect.PAGECURL -> com.novel.page.read.components.PageFlipEffect.PAGECURL
                PageFlipEffect.COVER -> com.novel.page.read.components.PageFlipEffect.COVER
                PageFlipEffect.SLIDE -> com.novel.page.read.components.PageFlipEffect.SLIDE
                PageFlipEffect.VERTICAL -> com.novel.page.read.components.PageFlipEffect.VERTICAL
            }
        )
    }
} 