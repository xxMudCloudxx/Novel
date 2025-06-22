package com.novel.page.read.viewmodel

/**
 * 翻页方向
 */
enum class FlipDirection {
    NEXT, PREVIOUS
}

/**
 * 页面数据 - 保持现有结构，增强功能
 */
data class PageData(
    val chapterId: String,
    val chapterName: String,
    val content: String,
    val pages: List<String> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = pages.size,
    val isLastPage: Boolean = false,
    val isFirstPage: Boolean = false,
    val isLastChapter: Boolean = false,
    val isFirstChapter: Boolean = false,
    val nextChapterData: PageData? = null,
    val previousChapterData: PageData? = null,
    val bookInfo: BookInfo? = null,
    val hasBookDetailPage: Boolean = false
) {
    /**
     * 书籍信息
     */
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
 * 翻页结果
 */
sealed class FlipResult {
    data class Success(val newPageIndex: Int) : FlipResult()
    data class Boundary(val isStart: Boolean) : FlipResult()
    data class Error(val exception: Throwable) : FlipResult()
}