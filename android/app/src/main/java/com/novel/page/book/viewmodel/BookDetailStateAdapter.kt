package com.novel.page.book.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.facebook.react.BuildConfig
import com.novel.utils.TimberLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.novel.page.component.StateHolderImpl
import com.novel.core.adapter.StateAdapter

/**
 * BookDetail状态适配器
 * 
 * 为BookDetail模块提供状态适配功能，方便UI层访问MVI状态的特定部分
 * 继承基础StateAdapter，提供BookDetail模块专用的状态适配功能
 * 
 * 特性：
 * - 继承基础StateAdapter的所有功能
 * - BookDetail模块专用状态访问方法
 * - 细粒度状态订阅，减少不必要的重组
 * - 类型安全的强类型状态访问
 * - UI友好的便利方法
 * - 向后兼容原有UI层格式
 * - 优化的@Composable函数提升skippable比例
 */
@Stable
class BookDetailStateAdapter(
    stateFlow: StateFlow<BookDetailState>
) : StateAdapter<BookDetailState>(stateFlow) {
    
    // region Composable 状态访问方法 (用于提升 skippable 比例)
    
    /** 书籍基本信息 - 优化版本 */
    @Composable
    fun bookInfoState(): State<BookDetailState.BookInfo?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo }
    }
    
    /** 最新章节信息 - 优化版本 */
    @Composable
    fun lastChapterState(): State<BookDetailState.LastChapter?> = remember {
        derivedStateOf { getCurrentSnapshot().lastChapter }
    }
    
    /** 用户评价列表 - 优化版本 */
    @Composable
    fun reviewsState(): State<ImmutableList<BookDetailState.BookReview>> = remember {
        derivedStateOf { getCurrentSnapshot().reviews }
    }
    
    /** 简介是否展开 - 优化版本 */
    @Composable
    fun isDescriptionExpandedState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isDescriptionExpanded }
    }
    
    /** 是否在书架中 - 优化版本 */
    @Composable
    fun isInBookshelfState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isInBookshelf }
    }
    
    /** 是否关注作者 - 优化版本 */
    @Composable
    fun isAuthorFollowedState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isAuthorFollowed }
    }
    
    /** 当前书籍ID - 优化版本 */
    @Composable
    fun currentBookIdState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().currentBookId }
    }
    
    /** 书籍名称 - 优化版本 */
    @Composable
    fun bookNameState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.bookName }
    }
    
    /** 作者名称 - 优化版本 */
    @Composable
    fun authorNameState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.authorName }
    }
    
    /** 书籍描述 - 优化版本 */
    @Composable
    fun bookDescState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.bookDesc }
    }
    
    /** 书籍封面URL - 优化版本 */
    @Composable
    fun picUrlState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.picUrl }
    }
    
    /** 访问次数 - 优化版本 */
    @Composable
    fun visitCountState(): State<Long> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.visitCount ?: 0L }
    }
    
    /** 字数统计 - 优化版本 */
    @Composable
    fun wordCountState(): State<Int> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.wordCount ?: 0 }
    }
    
    /** 分类名称 - 优化版本 */
    @Composable
    fun categoryNameState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo?.categoryName }
    }
    
    /** 是否有书籍信息 - 优化版本 */
    @Composable
    fun hasBookInfoState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().bookInfo != null }
    }
    
    /** 最新章节名称 - 优化版本 */
    @Composable
    fun lastChapterNameState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().lastChapter?.chapterName }
    }
    
    /** 最新章节更新时间 - 优化版本 */
    @Composable
    fun lastChapterUpdateTimeState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().lastChapter?.chapterUpdateTime }
    }
    
    /** 是否有最新章节信息 - 优化版本 */
    @Composable
    fun hasLastChapterState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().lastChapter != null }
    }
    
    /** 评价数量 - 优化版本 */
    @Composable
    fun reviewCountState(): State<Int> = remember {
        derivedStateOf { getCurrentSnapshot().reviews.size }
    }
    
    /** 是否有评价 - 优化版本 */
    @Composable
    fun hasReviewsState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().reviews.isNotEmpty() }
    }
    
    /** 平均评分 - 优化版本 */
    @Composable
    fun averageRatingState(): State<Float> = remember {
        derivedStateOf {
            val reviews = getCurrentSnapshot().reviews
            if (reviews.isEmpty()) {
                0f
            } else {
                reviews.map { it.rating }.average().toFloat()
            }
        }
    }
    
    /** 高评分评价（4星及以上）- 优化版本 */
    @Composable
    fun highRatingReviewsState(): State<ImmutableList<BookDetailState.BookReview>> = remember {
        derivedStateOf {
            getCurrentSnapshot().reviews.filter { it.rating >= 4 }.toImmutableList()
        }
    }
    
    /** 最新评价（前3条）- 优化版本 */
    @Composable
    fun latestReviewsState(): State<ImmutableList<BookDetailState.BookReview>> = remember {
        derivedStateOf {
            getCurrentSnapshot().reviews.take(3).toImmutableList()
        }
    }
    
    // endregion
    
    // region 基础状态适配 (向后兼容，但标记为过时)
    
    /** 书籍基本信息 */
    @Deprecated("使用 bookInfoState() 替代以提升性能", ReplaceWith("bookInfoState()"))
    @Stable
    val bookInfo = mapState { it.bookInfo }
    
    /** 最新章节信息 */
    @Deprecated("使用 lastChapterState() 替代以提升性能", ReplaceWith("lastChapterState()"))
    @Stable
    val lastChapter = mapState { it.lastChapter }
    
    /** 用户评价列表 */
    @Deprecated("使用 reviewsState() 替代以提升性能", ReplaceWith("reviewsState()"))
    @Stable
    val reviews = mapState { it.reviews }
    
    /** 简介是否展开 */
    @Deprecated("使用 isDescriptionExpandedState() 替代以提升性能", ReplaceWith("isDescriptionExpandedState()"))
    @Stable
    val isDescriptionExpanded = mapState { it.isDescriptionExpanded }
    
    /** 是否在书架中 */
    @Deprecated("使用 isInBookshelfState() 替代以提升性能", ReplaceWith("isInBookshelfState()"))
    @Stable
    val isInBookshelf = mapState { it.isInBookshelf }
    
    /** 是否关注作者 */
    @Deprecated("使用 isAuthorFollowedState() 替代以提升性能", ReplaceWith("isAuthorFollowedState()"))
    @Stable
    val isAuthorFollowed = mapState { it.isAuthorFollowed }
    
    /** 当前书籍ID */
    @Deprecated("使用 currentBookIdState() 替代以提升性能", ReplaceWith("currentBookIdState()"))
    @Stable
    val currentBookId = mapState { it.currentBookId }
    
    // endregion
    
    // region 书籍信息相关状态适配 (向后兼容，但标记为过时)
    
    /** 书籍名称 */
    @Deprecated("使用 bookNameState() 替代以提升性能", ReplaceWith("bookNameState()"))
    @Stable
    val bookName = mapState { it.bookInfo?.bookName }
    
    /** 作者名称 */
    @Deprecated("使用 authorNameState() 替代以提升性能", ReplaceWith("authorNameState()"))
    @Stable
    val authorName = mapState { it.bookInfo?.authorName }
    
    /** 书籍描述 */
    @Deprecated("使用 bookDescState() 替代以提升性能", ReplaceWith("bookDescState()"))
    @Stable
    val bookDesc = mapState { it.bookInfo?.bookDesc }
    
    /** 书籍封面URL */
    @Deprecated("使用 picUrlState() 替代以提升性能", ReplaceWith("picUrlState()"))
    @Stable
    val picUrl = mapState { it.bookInfo?.picUrl }
    
    /** 访问次数 */
    @Deprecated("使用 visitCountState() 替代以提升性能", ReplaceWith("visitCountState()"))
    @Stable
    val visitCount = mapState { it.bookInfo?.visitCount ?: 0L }
    
    /** 字数统计 */
    @Deprecated("使用 wordCountState() 替代以提升性能", ReplaceWith("wordCountState()"))
    @Stable
    val wordCount = mapState { it.bookInfo?.wordCount ?: 0 }
    
    /** 分类名称 */
    @Deprecated("使用 categoryNameState() 替代以提升性能", ReplaceWith("categoryNameState()"))
    @Stable
    val categoryName = mapState { it.bookInfo?.categoryName }
    
    /** 是否有书籍信息 */
    @Deprecated("使用 hasBookInfoState() 替代以提升性能", ReplaceWith("hasBookInfoState()"))
    @Stable
    val hasBookInfo = createConditionFlow { it.bookInfo != null }
    
    // endregion
    
    // region 章节相关状态适配 (向后兼容，但标记为过时)
    
    /** 最新章节名称 */
    @Deprecated("使用 lastChapterNameState() 替代以提升性能", ReplaceWith("lastChapterNameState()"))
    @Stable
    val lastChapterName = mapState { it.lastChapter?.chapterName }
    
    /** 最新章节更新时间 */
    @Deprecated("使用 lastChapterUpdateTimeState() 替代以提升性能", ReplaceWith("lastChapterUpdateTimeState()"))
    @Stable
    val lastChapterUpdateTime = mapState { it.lastChapter?.chapterUpdateTime }
    
    /** 是否有最新章节信息 */
    @Deprecated("使用 hasLastChapterState() 替代以提升性能", ReplaceWith("hasLastChapterState()"))
    @Stable
    val hasLastChapter = createConditionFlow { it.lastChapter != null }
    
    // endregion
    
    // region 评价相关状态适配 (向后兼容，但标记为过时)
    
    /** 评价数量 */
    @Deprecated("使用 reviewCountState() 替代以提升性能", ReplaceWith("reviewCountState()"))
    @Stable
    val reviewCount = mapState { it.reviews.size }
    
    /** 是否有评价 */
    @Deprecated("使用 hasReviewsState() 替代以提升性能", ReplaceWith("hasReviewsState()"))
    @Stable
    val hasReviews = createConditionFlow { it.reviews.isNotEmpty() }
    
    /** 平均评分 */
    @Deprecated("使用 averageRatingState() 替代以提升性能", ReplaceWith("averageRatingState()"))
    @Stable
    val averageRating = mapState { state ->
        val reviews = state.reviews
        if (reviews.isEmpty()) {
            0f
        } else {
            reviews.map { it.rating }.average().toFloat()
        }
    }
    
    /** 高评分评价（4星及以上） */
    @Deprecated("使用 highRatingReviewsState() 替代以提升性能", ReplaceWith("highRatingReviewsState()"))
    @Stable
    val highRatingReviews = mapState { state ->
        state.reviews.filter { it.rating >= 4 }.toImmutableList()
    }
    
    /** 最新评价（前3条） */
    @Deprecated("使用 latestReviewsState() 替代以提升性能", ReplaceWith("latestReviewsState()"))
    @Stable
    val latestReviews = mapState { state ->
        state.reviews.take(3).toImmutableList()
    }
    
    // endregion
    
    // region BookDetail模块专用便利方法
    
    /** 检查是否可以开始阅读 */
    fun canStartReading(): Boolean {
        val state = getCurrentSnapshot()
        return state.bookInfo != null && !state.isLoading
    }
    
    /** 检查是否可以添加到书架 */
    fun canAddToBookshelf(): Boolean {
        val state = getCurrentSnapshot()
        return state.bookInfo != null && !state.isInBookshelf && !state.isLoading
    }
    
    /** 检查是否可以从书架移除 */
    fun canRemoveFromBookshelf(): Boolean {
        val state = getCurrentSnapshot()
        return state.bookInfo != null && state.isInBookshelf && !state.isLoading
    }
    
    /** 检查是否可以关注作者 */
    fun canFollowAuthor(): Boolean {
        val state = getCurrentSnapshot()
        return state.bookInfo != null && !state.isAuthorFollowed && !state.isLoading
    }
    
    /** 检查是否可以分享书籍 */
    fun canShareBook(): Boolean {
        val state = getCurrentSnapshot()
        return state.bookInfo != null && !state.isLoading
    }
    
    /** 获取书架操作文本 */
    fun getBookshelfActionText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "处理中..."
            state.isInBookshelf -> "移出书架"
            else -> "加入书架"
        }
    }
    
    /** 获取关注作者操作文本 */
    fun getFollowAuthorActionText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "处理中..."
            state.isAuthorFollowed -> "已关注"
            else -> "关注作者"
        }
    }
    
    /** 获取阅读按钮文本 */
    fun getReadButtonText(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "加载中..."
            state.hasError -> "重新加载"
            state.lastChapter != null -> "开始阅读"
            else -> "暂无章节"
        }
    }
    
    /** 获取简介展开操作文本 */
    fun getDescriptionToggleText(): String {
        val state = getCurrentSnapshot()
        return if (state.isDescriptionExpanded) {
            "收起"
        } else {
            "展开"
        }
    }
    
    /** 获取书籍统计信息文本 */
    fun getBookStatsText(): String {
        val state = getCurrentSnapshot()
        val bookInfo = state.bookInfo ?: return "暂无统计信息"
        return "阅读量：${formatCount(bookInfo.visitCount)} | 字数：${formatWordCount(bookInfo.wordCount)}"
    }
    
    /** 获取评价摘要文本 */
    fun getReviewSummaryText(): String {
        val state = getCurrentSnapshot()
        val reviews = state.reviews
        if (reviews.isEmpty()) {
            return "暂无评价"
        }
        
        val avgRating = if (reviews.isEmpty()) {
            0f
        } else {
            reviews.map { it.rating }.average().toFloat()
        }
        
        return when {
            reviews.size == 1 -> "1条评价"
            else -> "${reviews.size}条评价，平均${String.format("%.1f", avgRating)}星"
        }
    }
    
    /** 获取最新章节信息文本 */
    fun getLastChapterInfoText(): String {
        val state = getCurrentSnapshot()
        val lastChapter = state.lastChapter
        return if (lastChapter != null) {
            "最新：${lastChapter.chapterName}"
        } else {
            "暂无章节"
        }
    }
    
    /** 获取更新时间文本 */
    fun getUpdateTimeText(): String {
        val state = getCurrentSnapshot()
        val lastChapter = state.lastChapter
        return if (lastChapter != null) {
            "更新于 ${lastChapter.chapterUpdateTime}"
        } else {
            "暂无更新"
        }
    }
    
    /** 获取书籍详情状态摘要 */
    fun getBookDetailStatusSummary(): String {
        val state = getCurrentSnapshot()
        return when {
            state.isLoading -> "加载中"
            state.hasError -> "加载失败"
            state.isEmpty -> "暂无数据"
            state.bookInfo != null -> "加载完成"
            else -> "未知状态"
        }
    }
    
    /** 检查是否显示空状态 */
    fun shouldShowEmptyState(): Boolean {
        val state = getCurrentSnapshot()
        return !state.isLoading && !state.hasError && state.bookInfo == null
    }
    
    /** 检查是否显示重试按钮 */
    fun shouldShowRetryButton(): Boolean {
        val state = getCurrentSnapshot()
        return state.hasError && !state.isLoading
    }
    
    /** 格式化数量显示 */
    private fun formatCount(count: Long): String {
        return when {
            count >= 10000 -> "${count / 10000}万"
            count >= 1000 -> "${count / 1000}千"
            else -> count.toString()
        }
    }
    
    /** 格式化字数显示 */
    private fun formatWordCount(wordCount: Int): String {
        return when {
            wordCount >= 10000 -> "${wordCount / 10000}万字"
            wordCount >= 1000 -> "${wordCount / 1000}千字"
            else -> "${wordCount}字"
        }
    }
    
    // endregion
    
    // region 向后兼容方法
    
    /**
     * 将BookDetailState转换为UI层期望的StateHolderImpl<BookDetailUiState>格式
     * 保持与原有UI层的兼容性
     */
    fun toUiState(): StateHolderImpl<BookDetailUiState> {
        val state = getCurrentSnapshot()
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
                }.toImmutableList(),
                isDescriptionExpanded = state.isDescriptionExpanded
            ),
            isLoading = state.isLoading,
            error = state.error
        )
    }
    
    // endregion
}

/**
 * StateAdapter工厂方法
 * 简化BookDetailStateAdapter的创建
 */
fun StateFlow<BookDetailState>.asBookDetailAdapter(): BookDetailStateAdapter {
    return BookDetailStateAdapter(this)
}

/**
 * 状态组合器
 * 将多个状态组合成UI需要的复合状态
 */
@Stable
data class BookDetailScreenState(
    val isLoading: Boolean,
    val error: String?,
    val bookInfo: BookDetailState.BookInfo?,
    val lastChapter: BookDetailState.LastChapter?,
    val reviews: ImmutableList<BookDetailState.BookReview>,
    val isDescriptionExpanded: Boolean,
    val isInBookshelf: Boolean,
    val isAuthorFollowed: Boolean,
    val canStartReading: Boolean,
    val canAddToBookshelf: Boolean,
    val canRemoveFromBookshelf: Boolean,
    val canFollowAuthor: Boolean,
    val canShareBook: Boolean,
    val bookshelfActionText: String,
    val followAuthorActionText: String,
    val readButtonText: String,
    val descriptionToggleText: String,
    val bookStatsText: String,
    val reviewSummaryText: String,
    val lastChapterInfoText: String,
    val updateTimeText: String,
    val bookDetailStatusSummary: String,
    val shouldShowEmptyState: Boolean,
    val shouldShowRetryButton: Boolean
)

/**
 * 将BookDetailState转换为UI友好的组合状态
 */
fun BookDetailStateAdapter.toScreenState(): BookDetailScreenState {
    val snapshot = getCurrentSnapshot()
    return BookDetailScreenState(
        isLoading = snapshot.isLoading,
        error = snapshot.error,
        bookInfo = snapshot.bookInfo,
        lastChapter = snapshot.lastChapter,
        reviews = snapshot.reviews,
        isDescriptionExpanded = snapshot.isDescriptionExpanded,
        isInBookshelf = snapshot.isInBookshelf,
        isAuthorFollowed = snapshot.isAuthorFollowed,
        canStartReading = canStartReading(),
        canAddToBookshelf = canAddToBookshelf(),
        canRemoveFromBookshelf = canRemoveFromBookshelf(),
        canFollowAuthor = canFollowAuthor(),
        canShareBook = canShareBook(),
        bookshelfActionText = getBookshelfActionText(),
        followAuthorActionText = getFollowAuthorActionText(),
        readButtonText = getReadButtonText(),
        descriptionToggleText = getDescriptionToggleText(),
        bookStatsText = getBookStatsText(),
        reviewSummaryText = getReviewSummaryText(),
        lastChapterInfoText = getLastChapterInfoText(),
        updateTimeText = getUpdateTimeText(),
        bookDetailStatusSummary = getBookDetailStatusSummary(),
        shouldShowEmptyState = shouldShowEmptyState(),
        shouldShowRetryButton = shouldShowRetryButton()
    )
}

/**
 * BookDetail模块状态监听器
 * 提供BookDetail模块特定的状态变更监听
 */
class BookDetailStateListener(
    private val adapter: BookDetailStateAdapter
) {
    
    /** 监听书籍信息变更 */
    fun onBookInfoChanged(action: (BookDetailState.BookInfo?) -> Unit) = adapter.bookInfo.map { bookInfo ->
        action(bookInfo)
        bookInfo
    }
    
    /** 监听书架状态变更 */
    fun onBookshelfStatusChanged(action: (Boolean) -> Unit) = adapter.isInBookshelf.map { inBookshelf ->
        action(inBookshelf)
        inBookshelf
    }
    
    /** 监听关注状态变更 */
    fun onFollowStatusChanged(action: (Boolean) -> Unit) = adapter.isAuthorFollowed.map { followed ->
        action(followed)
        followed
    }
    
    /** 监听简介展开状态变更 */
    fun onDescriptionExpandedChanged(action: (Boolean) -> Unit) = adapter.isDescriptionExpanded.map { expanded ->
        action(expanded)
        expanded
    }
    
    /** 监听评价数量变更 */
    fun onReviewCountChanged(action: (Int) -> Unit) = adapter.reviewCount.map { count ->
        action(count)
        count
    }
}

/**
 * 为BookDetailStateAdapter创建专用监听器
 */
fun BookDetailStateAdapter.createBookDetailListener(): BookDetailStateListener {
    return BookDetailStateListener(this)
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
    val reviews: ImmutableList<BookReview> = persistentListOf(),
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