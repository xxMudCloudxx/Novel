package com.novel.page.book.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.component.StateHolderImpl
import com.novel.utils.network.repository.CachedBookRepository
import com.novel.utils.network.cache.CacheStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 书籍详情页面视图模型
 * 
 * 负责管理书籍详情页面的数据获取和状态管理：
 * - 书籍基本信息的加载和缓存
 * - 最新章节信息的异步获取
 * - 用户评价数据的模拟生成
 * - 简介展开/收起状态管理
 * - 错误处理和重试机制
 * 
 * 采用缓存优先策略，提升用户体验：
 * - 首先从本地缓存加载数据
 * - 后台异步更新网络数据
 * - 支持强制刷新机制
 */
@HiltViewModel
class BookDetailViewModel @Inject constructor(
    /** 带缓存的书籍数据仓库，提供数据访问抽象 */
    private val cachedBookRepository: CachedBookRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "BookDetailViewModel"
    }
    
    /** UI状态流，包装了书籍详情数据和加载状态 */
    private val _uiState = MutableStateFlow(
        StateHolderImpl(
            data = BookDetailUiState(),
            isLoading = false,
            error = null
        )
    )
    val uiState: StateFlow<StateHolderImpl<BookDetailUiState>> = _uiState.asStateFlow()

    /**
     * 加载书籍详情信息
     * 
     * 采用缓存优先策略，先显示缓存内容，再更新网络数据：
     * 1. 设置加载状态，清除旧的错误信息
     * 2. 从缓存或网络获取书籍基本信息
     * 3. 构建UI状态数据
     * 4. 异步加载最新章节信息
     * 5. 生成模拟的用户评价数据
     * 
     * @param bookId 书籍唯一标识符
     * @param useCache 是否使用缓存，默认为true（缓存优先）
     */
    fun loadBookDetail(bookId: String, useCache: Boolean = true) {
        viewModelScope.launchWithLoading {
            try {
                Log.d(TAG, "开始加载书籍详情: bookId=$bookId, useCache=$useCache")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // 根据参数选择缓存策略
                val strategy = if (useCache) CacheStrategy.CACHE_FIRST else CacheStrategy.NETWORK_ONLY
                
                // 使用缓存优先策略加载书籍信息
                val bookInfo = cachedBookRepository.getBookInfo(
                    bookId = bookId.toLong(),
                    strategy = strategy
                )
                
                if (bookInfo != null) {
                    Log.d(TAG, "书籍信息加载成功: ${bookInfo.bookName}")
                    
                    // 转换为UI层数据模型
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
                    
                    // 生成模拟书评数据（后续接入真实API时替换）
                    val reviews = generateMockReviews()
                    
                    _uiState.value = _uiState.value.copy(
                        data = BookDetailUiState(
                            bookInfo = bookDetailInfo,
                            lastChapter = null, // 将在异步加载中更新
                            reviews = reviews,
                            isDescriptionExpanded = false
                        ),
                        isLoading = false,
                        error = null
                    )
                    
                    // 异步加载最新章节信息，不阻塞主流程
                    loadLastChapterInfo(bookId.toLong())
                } else {
                    Log.w(TAG, "书籍信息加载失败: bookId=$bookId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "书籍信息加载失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载书籍详情异常: bookId=$bookId", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }
    
    /**
     * 异步加载最新章节信息
     * 
     * 在书籍基本信息加载完成后，独立加载章节信息：
     * - 获取书籍的章节列表
     * - 提取最新章节信息
     * - 更新UI状态中的最新章节数据
     * - 加载失败不影响书籍信息的正常显示
     * 
     * @param bookId 书籍唯一标识符
     */
    private fun loadLastChapterInfo(bookId: Long) {
        viewModelScope.launchWithLoading {
            try {
                Log.d(TAG, "开始加载最新章节信息: bookId=$bookId")
                
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
                    
                    Log.d(TAG, "最新章节加载成功: ${lastChapter.chapterName}")
                    
                    // 更新UI状态中的最新章节信息
                    val currentData = _uiState.value.data
                    _uiState.value = _uiState.value.copy(
                        data = currentData.copy(lastChapter = lastChapterInfo)
                    )
                } else {
                    Log.w(TAG, "未找到章节信息: bookId=$bookId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "最新章节信息加载失败，不影响主要功能", e)
                // 章节信息加载失败，不影响书籍信息显示
            }
        }
    }

    /**
     * 切换书籍简介的展开/收起状态
     * 
     * 用于处理长简介的展示优化，提升阅读体验
     */
    fun toggleDescriptionExpanded() {
        val currentData = _uiState.value.data
        val newExpanded = !currentData.isDescriptionExpanded
        
        Log.d(TAG, "切换简介展开状态: $newExpanded")
        
        _uiState.value = _uiState.value.copy(
            data = currentData.copy(
                isDescriptionExpanded = newExpanded
            )
        )
    }

    /**
     * 生成模拟用户评价数据
     * 
     * 临时方案，用于UI展示和功能验证
     * 后续将替换为真实的用户评价API
     * 
     * @return 模拟的用户评价列表
     */
    private fun generateMockReviews(): List<BookDetailUiState.BookReview> {
        Log.d(TAG, "生成模拟用户评价数据")
        
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
 * 书籍详情页UI状态数据类
 * 
 * 封装书籍详情页面所需的全部状态信息
 */
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
    data class LastChapter(
        val chapterName: String,
        val chapterUpdateTime: String
    )
    
    /**
     * 用户评价数据类
     */
    data class BookReview(
        val id: String,
        val content: String,
        val rating: Int, // 1-5星评级
        val readTime: String,
        val userName: String
    )
} 