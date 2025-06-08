package com.novel.page.read.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 翻页效果枚举
 */
enum class PageFlipEffect {
    NONE, PAGECURL, COVER, SLIDE, VERTICAL
}

/**
 * 翻页方向
 */
enum class FlipDirection {
    NEXT, PREVIOUS
}

/**
 * 阅读器配置 - 整合版本
 */
data class ReaderConfig(
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL,
    val backgroundColor: Color = Color.White,
    val textColor: Color = Color.Black,
    val fontSize: TextUnit = 16.sp,
    val lineSpacing: Float = 1.2f,
    val paragraphSpacing: TextUnit = 8.sp,
    val horizontalPadding: TextUnit = 16.sp,
    val verticalPadding: TextUnit = 16.sp,
    val isNightMode: Boolean = false,
    val brightness: Float = 1.0f,
    val keepScreenOn: Boolean = true,
    val volumeKeyFlip: Boolean = true,
    val autoFlip: Boolean = false,
    val autoFlipInterval: Long = 3000L,
    val tapToFlip: Boolean = true,
    val showPageProgress: Boolean = true,
    val showChapterProgress: Boolean = true,
    val showBattery: Boolean = true,
    val showTime: Boolean = true
)

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
    val contentPageCount: Int get() = pages.size
    val totalPageCount: Int get() = pages.size + if (hasBookDetailPage) 1 else 0

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
 * 章节信息
 */
data class ChapterInfo(
    val id: String,
    val name: String,
    val url: String = "",
    val hasContent: Boolean = true,
    val isVip: Boolean = false
)

/**
 * 翻页请求
 */
data class FlipRequest(
    val pageData: PageData,
    val currentPageIndex: Int,
    val direction: FlipDirection,
    val containerSize: IntSize
)

/**
 * 翻页结果
 */
sealed class FlipResult {
    data class Success(val newPageIndex: Int) : FlipResult()
    data class ChapterChange(val direction: FlipDirection) : FlipResult()
    data class Boundary(val isStart: Boolean) : FlipResult()
    data class Error(val exception: Throwable) : FlipResult()
}

/**
 * 阅读进度事件
 */
data class ReadingProgressEvent(
    val bookId: String,
    val chapterId: String,
    val pageIndex: Int,
    val progress: Float,
    val timestamp: Long = System.currentTimeMillis()
) 