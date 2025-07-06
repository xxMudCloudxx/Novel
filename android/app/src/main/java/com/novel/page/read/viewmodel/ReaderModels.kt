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