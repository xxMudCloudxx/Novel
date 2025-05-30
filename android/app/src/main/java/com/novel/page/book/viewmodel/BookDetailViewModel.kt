package com.novel.page.book.viewmodel

import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.component.StateHolderImpl
import com.novel.utils.network.api.front.BookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookService: BookService
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(
        StateHolderImpl(
            data = BookDetailUiState(),
            isLoading = false,
            error = null
        )
    )
    val uiState: StateFlow<StateHolderImpl<BookDetailUiState>> = _uiState.asStateFlow()

    fun loadBookDetail(bookId: String) {
        viewModelScope.launchWithLoading {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // 加载书籍信息
                val bookResponse = bookService.getBookByIdBlocking(bookId.toLong())
                if (bookResponse.ok == true && bookResponse.data != null) {
                    val bookInfo = BookDetailUiState.BookInfo(
                        id = bookResponse.data.id.toString(),
                        bookName = bookResponse.data.bookName,
                        authorName = bookResponse.data.authorName,
                        bookDesc = bookResponse.data.bookDesc,
                        picUrl = bookResponse.data.picUrl,
                        visitCount = bookResponse.data.visitCount,
                        wordCount = bookResponse.data.wordCount,
                        categoryName = bookResponse.data.categoryName
                    )
                    
                    // 加载最新章节信息
                    var lastChapter: BookDetailUiState.LastChapter? = null
                    try {
                        val chapterResponse = bookService.getLastChapterAboutBlocking(bookId.toLong())
                        if (chapterResponse.ok == true && chapterResponse.data != null) {
                            lastChapter = BookDetailUiState.LastChapter(
                                chapterName = chapterResponse.data.chapterInfo.chapterName,
                                chapterUpdateTime = chapterResponse.data.chapterInfo.chapterUpdateTime
                            )
                        }
                    } catch (e: Exception) {
                        // 忽略章节加载错误，继续显示书籍信息
                    }
                    
                    // 生成模拟书评数据
                    val reviews = generateMockReviews()
                    
                    _uiState.value = _uiState.value.copy(
                        data = BookDetailUiState(
                            bookInfo = bookInfo,
                            lastChapter = lastChapter,
                            reviews = reviews,
                            isDescriptionExpanded = false
                        ),
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "书籍信息加载失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    fun toggleDescriptionExpanded() {
        val currentData = _uiState.value.data
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(
                isDescriptionExpanded = !currentData.isDescriptionExpanded
            )
        )
    }

    private fun generateMockReviews(): List<BookDetailUiState.BookReview> {
        return listOf(
            BookDetailUiState.BookReview(
                id = "1",
                content = "这个职业(老板)无敌了，全天下的天才为之打工。",
                rating = 5,
                readTime = "阅读54分钟后点评",
                userName = "用户1"
            ),
            BookDetailUiState.BookReview(
                id = "2", 
                content = "很不错的脑洞，题材也很新颖，就是主角有点感太低了，全是手下在发力，主角变考全程躺平...",
                rating = 4,
                readTime = "阅读2小时后点评",
                userName = "用户2"
            ),
            BookDetailUiState.BookReview(
                id = "3",
                content = "我看书是不是有点太快了",
                rating = 5,
                readTime = "阅读不足30分钟后点评",
                userName = "用户3"
            )
        )
    }
}

/**
 * 书籍详情页UI状态
 */
data class BookDetailUiState(
    val bookInfo: BookInfo? = null,
    val lastChapter: LastChapter? = null,
    val reviews: List<BookReview> = emptyList(),
    val isDescriptionExpanded: Boolean = false
) {
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
    
    data class LastChapter(
        val chapterName: String,
        val chapterUpdateTime: String
    )
    
    data class BookReview(
        val id: String,
        val content: String,
        val rating: Int, // 1-5星
        val readTime: String,
        val userName: String
    )
} 