package com.novel.page.book.viewmodel

import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.component.StateHolderImpl
 import com.novel.repository.CachedBookRepository
import com.novel.utils.network.cache.CacheStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val cachedBookRepository: CachedBookRepository
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(
        StateHolderImpl(
            data = BookDetailUiState(),
            isLoading = false,
            error = null
        )
    )
    val uiState: StateFlow<StateHolderImpl<BookDetailUiState>> = _uiState.asStateFlow()

    fun loadBookDetail(bookId: String, useCache: Boolean = true) {
        viewModelScope.launchWithLoading {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val strategy = if (useCache) CacheStrategy.CACHE_FIRST else CacheStrategy.NETWORK_ONLY
                
                // 使用缓存优先策略加载书籍信息
                val bookInfo = cachedBookRepository.getBookInfo(
                    bookId = bookId.toLong(),
                    strategy = strategy
                )
                
                if (bookInfo != null) {
                    val bookDetailInfo = BookDetailUiState.BookInfo(
                        id = bookInfo.id.toString(),
                        bookName = bookInfo.bookName,
                        authorName = bookInfo.authorName,
                        bookDesc = bookInfo.bookDesc,
                        picUrl = bookInfo.picUrl,
                        visitCount = bookInfo.visitCount,
                        wordCount = bookInfo.wordCount,
                        categoryName = bookInfo.categoryName
                    )
                    
                    // 生成模拟书评数据（这部分还没有API，暂时保留模拟数据）
                    val reviews = generateMockReviews()
                    
                    _uiState.value = _uiState.value.copy(
                        data = BookDetailUiState(
                            bookInfo = bookDetailInfo,
                            lastChapter = null, // 需要从章节列表中获取
                            reviews = reviews,
                            isDescriptionExpanded = false
                        ),
                        isLoading = false,
                        error = null
                    )
                    
                    // 异步加载最新章节信息
                    loadLastChapterInfo(bookId.toLong())
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
    
    /**
     * 异步加载最新章节信息
     */
    private fun loadLastChapterInfo(bookId: Long) {
        viewModelScope.launchWithLoading {
            try {
                val chapters = cachedBookRepository.getBookChapters(
                    bookId = bookId,
                    strategy = CacheStrategy.CACHE_FIRST
                )
                
                if (chapters.isNotEmpty()) {
                    val lastChapter = chapters.last()
                    val lastChapterInfo = BookDetailUiState.LastChapter(
                        chapterName = lastChapter.chapterName,
                        chapterUpdateTime = lastChapter.chapterUpdateTime
                    )
                    
                    val currentData = _uiState.value.data
                    _uiState.value = _uiState.value.copy(
                        data = currentData.copy(lastChapter = lastChapterInfo)
                    )
                }
            } catch (e: Exception) {
                // 章节信息加载失败，不影响书籍信息显示
            }
        }
    }
    
    /**
     * 强制刷新书籍信息（绕过缓存）
     */
    fun refreshBookDetail(bookId: String) {
        loadBookDetail(bookId, useCache = false)
    }
    
    /**
     * 清理书籍缓存
     */
    fun clearBookCache(bookId: String) {
        viewModelScope.launchWithLoading {
            cachedBookRepository.clearBookCache(bookId.toLong())
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