package com.novel.page.book.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable
import com.novel.page.component.StateHolderImpl

/**
 * BookDetail状态适配器
 * 
 * 将新的MVI BookDetailState适配为原有UI层期望的格式
 * 确保UI层无需修改即可正常工作
 */
object BookDetailStateAdapter {
    
    /**
     * 将BookDetailState转换为UI层期望的StateHolderImpl<BookDetailUiState>格式
     */
    fun toUiState(state: BookDetailState): StateHolderImpl<BookDetailUiState> {
        return StateHolderImpl(
            data = BookDetailUiState(
                bookInfo = state.bookInfo?.let { bookInfo ->
                    BookDetailUiState.BookInfo(
                        id = bookInfo.id,
                        bookName = bookInfo.bookName,
                        authorName = bookInfo.authorName,
                        bookDesc = bookInfo.bookDesc,
                        picUrl = bookInfo.picUrl,
                        visitCount = bookInfo.visitCount,
                        wordCount = bookInfo.wordCount,
                        categoryName = bookInfo.categoryName
                    )
                },
                lastChapter = state.lastChapter?.let { lastChapter ->
                    BookDetailUiState.LastChapter(
                        chapterName = lastChapter.chapterName,
                        chapterUpdateTime = lastChapter.chapterUpdateTime
                    )
                },
                reviews = state.reviews.map { review ->
                    BookDetailUiState.BookReview(
                        id = review.id,
                        content = review.content,
                        rating = review.rating,
                        readTime = review.readTime,
                        userName = review.userName
                    )
                },
                isDescriptionExpanded = state.isDescriptionExpanded
            ),
            isLoading = state.isLoading,
            error = state.error
        )
    }
}

/**
 * 书籍详情页UI状态数据类（兼容性保留）
 * 
 * 保持与原有UI组件的兼容性
 */
@Stable
data class BookDetailUiState(
    /** 书籍基本信息 */
    val bookInfo: BookInfo? = null,
    /** 最新章节信息 */
    val lastChapter: LastChapter? = null,
    /** 用户评价列表 */
    val reviews: List<BookReview> = emptyList(),
    /** 简介是否展开 */
    val isDescriptionExpanded: Boolean = false
) {
    /**
     * 书籍基本信息数据类
     */
    @Immutable
    data class BookInfo(
        val id: String,
        val bookName: String,
        val authorName: String,
        val bookDesc: String,
        val picUrl: String,
        val visitCount: Long,
        val wordCount: Int,
        val categoryName: String
    )
    
    /**
     * 最新章节信息数据类
     */
    @Immutable
    data class LastChapter(
        val chapterName: String,
        val chapterUpdateTime: String
    )
    
    /**
     * 用户评价数据类
     */
    @Immutable
    data class BookReview(
        val id: String,
        val content: String,
        val rating: Int, // 1-5星评级
        val readTime: String,
        val userName: String
    )
} 