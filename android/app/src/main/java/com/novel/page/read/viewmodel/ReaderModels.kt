package com.novel.page.read.viewmodel

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.repository.ProgressiveCalculationState

/**
 * 翻页方向
 */
enum class FlipDirection {
    PREVIOUS,
    NEXT
}

/**
 * 翻页状态
 */
sealed class FlipState {
    data object Idle : FlipState()
}

/**
 * 虚拟页面，用于统一所有翻页模式
 */
sealed class VirtualPage {
    /**
     * 代表书籍详情页
     */
    data object BookDetailPage : VirtualPage()

    /**
     * 代表一个实际的内容页
     * @param chapterId 所属章节ID
     * @param pageIndex 在该章节内的页码 (从0开始)
     */
    data class ContentPage(val chapterId: String, val pageIndex: Int) : VirtualPage()

    /**
     * 代表一个完整的章节，主要用于纵向滚动模式
     */
    data class ChapterSection(val chapterId: String) : VirtualPage()
}

/**
 * 章节缓存数据
 */
data class ChapterCache(
    val chapter: Chapter,
    val content: String,
    var pageData: PageData? = null
)

/**
 * 单页数据
 */
data class PageData(
    val chapterId: String,
    val chapterName: String,
    val content: String,
    val pages: List<String>,
    val isFirstPage: Boolean = false,
    val isLastPage: Boolean = false,
    val isFirstChapter: Boolean = false,
    val isLastChapter: Boolean = false,
    val nextChapterData: PageData? = null,
    val previousChapterData: PageData? = null,
    val bookInfo: BookInfo? = null, // 书籍信息，用于第0页
    val hasBookDetailPage: Boolean = false // 是否有书籍详情页
) {
    val pageCount: Int get() = pages.size

    data class BookInfo(
        val bookId: String,
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
 * 阅读器UI状态
 */
data class ReaderUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String = "",
    val bookId: String = "",
    val chapterList: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val bookContent: String = "",
    val readerSettings: ReaderSettings = ReaderSettings.getDefault(), // 确保使用正确的默认设置
    var readingProgress: Float = 0f,
    // 新增分页相关状态
    val currentPageData: PageData? = null,
    val currentPageIndex: Int = 0,
    val isSwitchingChapter: Boolean = false,
    val containerSize: IntSize = IntSize.Zero,
    val density: Density? = null,

    // 统一翻页模式所需的新状态
    val virtualPages: List<VirtualPage> = emptyList(),
    val virtualPageIndex: Int = 0,
    val loadedChapterData: Map<String, PageData> = emptyMap(),

    // New state for global pagination
    val pageCountCache: PageCountCacheData? = null,
    val paginationState: ProgressiveCalculationState = ProgressiveCalculationState(),

    // 相邻章节数据
    val previousChapterData: PageData? = null,
    val nextChapterData: PageData? = null
) {
    val isSuccess: Boolean get() = !isLoading && !hasError && currentChapter != null
    val isEmpty: Boolean get() = !isLoading && !hasError && chapterList.isEmpty()

    // 添加扩展属性
    val isFirstChapter: Boolean get() = currentChapterIndex == 0
    val isLastChapter: Boolean get() = currentChapterIndex >= chapterList.size - 1

    val computedReadingProgress: Float
        get() {
            if (readerSettings.pageFlipEffect == PageFlipEffect.VERTICAL) {
                // 纵向滚动模式下，进度按章节计算
                if (chapterList.isEmpty()) return 0f
                return (currentChapterIndex + 1).toFloat() / chapterList.size.toFloat()
            }

            val cache = pageCountCache ?: return 0f
            if (cache.totalPages <= 0) return 0f

            val chapterRange = cache.chapterPageRanges.find { it.chapterId == currentChapter?.id }

            val globalCurrentPage = if (chapterRange != null) {
                chapterRange.startPage + currentPageIndex
            } else {
                0
            }

            return (globalCurrentPage + 1).toFloat() / cache.totalPages.toFloat()
        }
}

/**
 * 阅读器Intent(用户操作意图)
 */
sealed class ReaderIntent {
    data class InitReader(val bookId: String, val chapterId: String?) : ReaderIntent()
    data object PreviousChapter : ReaderIntent()
    data object NextChapter : ReaderIntent()
    data class SwitchToChapter(val chapterId: String) : ReaderIntent()
    data class SeekToProgress(val progress: Float) : ReaderIntent()
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent()
    data class PageFlip(val direction: FlipDirection) : ReaderIntent()
    data class UpdateContainerSize(val size: IntSize, val density: Density) : ReaderIntent()
    data object Retry : ReaderIntent()
}