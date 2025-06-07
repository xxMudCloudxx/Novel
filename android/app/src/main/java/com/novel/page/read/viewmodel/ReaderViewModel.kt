package com.novel.page.read.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewModelScope
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.utils.PageSplitter
import com.novel.page.component.BaseViewModel
import com.novel.utils.network.api.front.BookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Density
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.repository.BookCacheData
import com.novel.page.read.repository.BookCacheManager
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.repository.ProgressiveCalculationState
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * 翻页数据类
 */
data class PageData(
    val chapterId: String,
    val chapterName: String,
    val content: String,
    val pages: List<String> = emptyList(), // 分页后的内容
    val isLastPage: Boolean = false, // 是否是章节最后一页
    val isFirstPage: Boolean = false, // 是否是章节第一页
    val nextChapterData: PageData? = null, // 下一章数据
    val previousChapterData: PageData? = null, // 上一章数据
    val bookInfo: BookInfo? = null, // 书籍信息，用于第0页显示书籍详情
    val hasBookDetailPage: Boolean = false // 是否包含书籍详情页（第0页）
) {
    /**
     * 书籍信息数据类，用于第0页的书籍详情显示
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
    
    /**
     * 获取实际页面总数（包含书籍详情页）
     */
    val totalPageCount: Int 
        get() = pages.size + if (hasBookDetailPage) 1 else 0
    
    /**
     * 获取实际内容页面数量（不包含书籍详情页）
     */
    val contentPageCount: Int
        get() = pages.size
}

/**
 * 翻页状态
 */
sealed class FlipState {
    data object Idle : FlipState()
    data class Flipping(val progress: Float, val direction: FlipDirection) : FlipState()
    data class PageChanged(val newPageIndex: Int, val direction: FlipDirection) : FlipState()
}

/**
 * 翻页方向
 */
enum class FlipDirection {
    NEXT, PREVIOUS
}


/**
 * 章节缓存数据
 */
data class ChapterCache(
    val chapter: Chapter,
    val content: String,
    val pageData: PageData? = null
)

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
    val readerSettings: ReaderSettings = ReaderSettings(),
    var readingProgress: Float = 0f,
    // 新增分页相关状态
    val currentPageData: PageData? = null,
    val currentPageIndex: Int = 0,
    val containerSize: IntSize = IntSize.Zero,
    val density: Density? = null,

    // New state for global pagination
    val pageCountCache: PageCountCacheData? = null,
    val paginationState: ProgressiveCalculationState = ProgressiveCalculationState()
) {
    val isSuccess: Boolean get() = !isLoading && !hasError && currentChapter != null
    val isEmpty: Boolean get() = !isLoading && !hasError && chapterList.isEmpty()

    val computedReadingProgress: Float get() {
        val cache = pageCountCache ?: return 0f
        if (cache.totalPages <= 0) return 0f

        val chapterRange = cache.chapterPageRanges.find { it.chapterId == currentChapter?.id }

        val globalCurrentPage = if (chapterRange != null) {
            chapterRange.startPage + currentPageIndex
        } else {
            0
        }

        readingProgress = (globalCurrentPage + 1).toFloat() / cache.totalPages.toFloat()
        return (globalCurrentPage + 1).toFloat() / cache.totalPages.toFloat()
    }
}

/**
 * 阅读器Intent(用户操作意图)
 */
sealed class ReaderIntent {
    data class InitReader(val bookId: String, val chapterId: String?) : ReaderIntent()
    data object LoadChapterList : ReaderIntent()
    data class LoadChapterContent(val chapterId: String) : ReaderIntent()
    data object PreviousChapter : ReaderIntent()
    data object NextChapter : ReaderIntent()
    data class SwitchToChapter(val chapterId: String) : ReaderIntent()
    data class SeekToProgress(val progress: Float) : ReaderIntent()
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent()
    data class PageFlip(val direction: FlipDirection) : ReaderIntent()
    data class ChapterFlip(val direction: FlipDirection) : ReaderIntent()
    data class UpdateContainerSize(val size: IntSize, val density: Density) : ReaderIntent()
    data object Retry : ReaderIntent()
}

/**
 * 阅读器ViewModel - MVI架构
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookService: BookService,
    private val userDefaults: com.novel.utils.Store.UserDefaults.NovelUserDefaults,
    private val bookCacheManager: BookCacheManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // 章节缓存，最多缓存8个章节（当前+前后各3章+缓冲）
    private val chapterCache = mutableMapOf<String, ChapterCache>()
    private val maxCacheSize = 8

    // 防重复触发机制
    private var lastFlipTime = 0L
    private val flipCooldownMs = 300L // 翻页冷却时间300ms

    init {
        // 初始化时加载保存的翻页方式
        loadSavedPageFlipEffect()
    }

    /**
     * 加载保存的翻页方式
     */
    private fun loadSavedPageFlipEffect() {
        try {
            val savedEffect = userDefaults.get<String>(com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey.PAGE_FLIP_EFFECT)
            if (savedEffect != null) {
                val pageFlipEffect = PageFlipEffect.entries
                    .find { it.name == savedEffect } ?: com.novel.page.read.components.PageFlipEffect.PAGECURL
                
                val currentSettings = _uiState.value.readerSettings
                _uiState.value = _uiState.value.copy(
                    readerSettings = currentSettings.copy(pageFlipEffect = pageFlipEffect)
                )
            }
        } catch (e: Exception) {
            // 如果加载失败，使用默认值
        }
    }

    /**
     * 保存翻页方式
     */
    private fun savePageFlipEffect(pageFlipEffect: com.novel.page.read.components.PageFlipEffect) {
        try {
            userDefaults.set(pageFlipEffect.name, com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey.PAGE_FLIP_EFFECT)
        } catch (e: Exception) {
            // 保存失败时静默处理
        }
    }

    /**
     * 获取当前翻页方式（供外部调用）
     */
    fun getCurrentPageFlipEffect(): com.novel.page.read.components.PageFlipEffect {
        return _uiState.value.readerSettings.pageFlipEffect
    }

    /**
     * 处理用户意图
     */
    private fun handleIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.InitReader -> initReaderInternal(intent.bookId, intent.chapterId)
            is ReaderIntent.LoadChapterList -> loadChapterListMethod()
            is ReaderIntent.LoadChapterContent -> loadChapterContentMethod(intent.chapterId)
            is ReaderIntent.PreviousChapter -> previousChapterInternal()
            is ReaderIntent.NextChapter -> nextChapterInternal()
            is ReaderIntent.SwitchToChapter -> switchToChapterInternal(intent.chapterId)
            is ReaderIntent.SeekToProgress -> seekToProgressInternal(intent.progress)
            is ReaderIntent.UpdateSettings -> updateSettingsInternal(intent.settings)
            is ReaderIntent.PageFlip -> handlePageFlip(intent.direction)
            is ReaderIntent.ChapterFlip -> handleChapterFlip(intent.direction)
            is ReaderIntent.UpdateContainerSize -> updateContainerSizeInternal(
                intent.size,
                intent.density
            )

            is ReaderIntent.Retry -> retryInternal()
        }
    }

    // 便捷方法，保持与UI层的兼容性
    fun initReader(bookId: String, chapterId: String?) = handleIntent(
        ReaderIntent.InitReader(bookId, chapterId)
    )

    fun previousChapter() = handleIntent(ReaderIntent.PreviousChapter)
    fun nextChapter() = handleIntent(ReaderIntent.NextChapter)
    fun switchToChapter(chapterId: String) = handleIntent(ReaderIntent.SwitchToChapter(chapterId))
    fun seekToProgress(progress: Float) = handleIntent(ReaderIntent.SeekToProgress(progress))
    fun updateSettings(settings: ReaderSettings) =
        handleIntent(ReaderIntent.UpdateSettings(settings))

    fun nextPage() = handleIntent(ReaderIntent.PageFlip(FlipDirection.NEXT))
    fun previousPage() = handleIntent(ReaderIntent.PageFlip(FlipDirection.PREVIOUS))
    fun retry() = handleIntent(ReaderIntent.Retry)

    // 新增的便捷方法
    fun onPageChange(direction: FlipDirection) = handleIntent(ReaderIntent.PageFlip(direction))
    fun onChapterChange(direction: FlipDirection) =
        handleIntent(ReaderIntent.ChapterFlip(direction))

    fun updateContainerSize(size: IntSize, density: Density) =
        handleIntent(ReaderIntent.UpdateContainerSize(size, density))

    fun updateCurrentPageFromScroll(page: Int) {
        if (page != _uiState.value.currentPageIndex) {
            _uiState.value = _uiState.value.copy(currentPageIndex = page)
        }
    }

    /**
     * 从书籍详情页导航到第一页内容
     */
    fun navigateToContent() {
        _uiState.value = _uiState.value.copy(currentPageIndex = 0)
    }

    /**
     * 初始化阅读器 - 支持书籍详情页
     */
    private fun initReaderInternal(bookId: String, chapterId: String?) {
        viewModelScope.launchWithLoading {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    hasError = false,
                    bookId = bookId
                )

                // 加载章节列表
                loadChapterListInternal()

                // 如果指定了章节ID，加载该章节；否则加载第一章
                val targetChapterId = chapterId ?: _uiState.value.chapterList.firstOrNull()?.id
                if (targetChapterId != null) {
                    // 如果是第一章且没有指定章节ID，从书籍详情页开始
                    val isFirstChapter = _uiState.value.chapterList.firstOrNull()?.id == targetChapterId
                    val shouldStartFromBookDetail = isFirstChapter && chapterId == null
                    
                    if (shouldStartFromBookDetail) {
                        // 设置为书籍详情页（索引-1）
                        _uiState.value = _uiState.value.copy(currentPageIndex = -1)
                    }
                    
                    loadChapterContentInternal(targetChapterId)

                    // 在后台获取全本内容并开始计算分页
                    fetchAllBookContentInBackground(bookId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = "未找到可用章节"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "初始化失败"
                )
            }
        }
    }

    /**
     * 加载章节列表
     */
    private fun loadChapterListMethod() {
        viewModelScope.launchWithLoading {
            loadChapterListInternal()
        }
    }

    private suspend fun loadChapterListInternal() {
        val bookId = _uiState.value.bookId
        if (bookId.isEmpty()) return

        try {
            val bookIdLong = bookId.toLong()
            val response = bookService.getBookChaptersBlocking(bookIdLong)

            if (response.code == "00000" && response.data != null) {
                val chapters = response.data.map { chapter ->
                    Chapter(
                        id = chapter.id.toString(),
                        chapterName = chapter.chapterName,
                        chapterNum = chapter.chapterNum.toString(),
                        isVip = if (chapter.isVip == 1) "1" else "0"
                    )
                }

                _uiState.value = _uiState.value.copy(chapterList = chapters)
            } else {
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = response.message ?: "加载章节列表失败"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "加载章节列表失败: ${e.message}"
            )
        }
    }

    /**
     * 加载章节内容方法 - 增加翻页方向参数
     */
    private fun loadChapterContentMethod(chapterId: String, flipDirection: FlipDirection? = null) {
        viewModelScope.launchWithLoading {
            loadChapterContentInternal(chapterId, flipDirection)
        }
    }

    /**
     * 加载章节内容内部方法 - 修复页面索引设置，支持书籍详情页
     */
    private suspend fun loadChapterContentInternal(
        chapterId: String,
        flipDirection: FlipDirection? = null,
        initialPageIndex: Int? = null
    ) {
        // 先检查缓存
        val cachedChapter = chapterCache[chapterId]
        if (cachedChapter != null) {
            updateCurrentChapter(cachedChapter, flipDirection, initialPageIndex)
            preloadAdjacentChapters(chapterId)
            return
        }

        // 从网络加载
        try {
            val chapterIdLong = chapterId.toLong()
            val response = bookService.getBookContentBlocking(chapterIdLong)

            if (response.code == "00000" && response.data != null) {
                val data = response.data
                val chapterInfo = data.chapterInfo
                val bookContent = data.bookContent

                val chapter = Chapter(
                    id = chapterInfo.id.toString(),
                    chapterName = chapterInfo.chapterName,
                    chapterNum = chapterInfo.chapterNum.toString(),
                    isVip = if (chapterInfo.isVip == 1) "1" else "0"
                )

                val chapterCache = ChapterCache(
                    chapter = chapter,
                    content = bookContent
                )

                // 缓存章节
                addToCache(chapterId, chapterCache)

                // 更新状态
                updateCurrentChapter(chapterCache, flipDirection, initialPageIndex)

                // 预加载前后章节
                preloadAdjacentChapters(chapterId)
            } else {
                throw Exception("章节内容加载失败")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasError = true,
                error = e.message ?: "章节加载失败"
            )
        }
    }
    
    /**
     * 加载书籍信息用于第0页显示
     */
    private suspend fun loadBookInfo(bookId: String): PageData.BookInfo? {
        return try {
            val bookResponse = bookService.getBookByIdBlocking(bookId.toLong())
            if (bookResponse.ok == true && bookResponse.data != null) {
                PageData.BookInfo(
                    bookId = bookResponse.data.id.toString(),
                    bookName = bookResponse.data.bookName,
                    authorName = bookResponse.data.authorName,
                    bookDesc = bookResponse.data.bookDesc,
                    picUrl = bookResponse.data.picUrl,
                    visitCount = bookResponse.data.visitCount,
                    wordCount = bookResponse.data.wordCount,
                    categoryName = bookResponse.data.categoryName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 更新当前章节信息 - 修复页面索引设置逻辑，支持书籍详情页
     */
    private fun updateCurrentChapter(
        chapterCache: ChapterCache,
        flipDirection: FlipDirection? = null,
        initialPageIndexOverride: Int? = null
    ) {
        val currentIndex =
            _uiState.value.chapterList.indexOfFirst { it.id == chapterCache.chapter.id }

        // 根据翻页方向正确设置初始页面索引
        val initialPageIndex = if (initialPageIndexOverride != null) {
            initialPageIndexOverride
        } else {
            when (flipDirection) {
                FlipDirection.PREVIOUS -> -1 // 标记为需要设置到最后一页，在分页完成后设置
                FlipDirection.NEXT -> 0     // 下一章从第一页开始
                else -> {
                    // 如果是第一章且当前页面索引为-1，保持在书籍详情页
                    val isFirstChapter = currentIndex == 0
                    if (isFirstChapter && _uiState.value.currentPageIndex == -1) {
                        -1 // 保持在书籍详情页
                    } else {
                        _uiState.value.currentPageIndex.coerceAtLeast(0) // 保持当前页面或默认第一页
                    }
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasError = false,
            currentChapter = chapterCache.chapter,
            currentChapterIndex = if (currentIndex >= 0) currentIndex else 0,
            bookContent = chapterCache.content,
            currentPageIndex = initialPageIndex
        )

        // 如果有容器尺寸信息，进行分页
        val state = _uiState.value
        if (state.containerSize != IntSize.Zero && state.density != null) {
            splitContent()
        }
    }

    /**
     * 分页处理 - 优化章节衔接和相邻章节数据获取，支持书籍详情页
     */
    private fun splitContent(restoreProgress: Float? = null) {
        val state = _uiState.value
        val chapter = state.currentChapter
        val content = state.bookContent
        val chapterList = state.chapterList
        val currentIndex = state.currentChapterIndex

        // 立即设置基本的PageData，避免长时间为null
        if (chapter != null && content.isNotEmpty()) {
            val basicPageData = PageData(
                chapterId = chapter.id,
                chapterName = chapter.chapterName,
                content = content,
                pages = listOf(content), // 暂时将整个内容作为一页
                hasBookDetailPage = currentIndex == 0
            )
            
            _uiState.value = state.copy(
                currentPageData = basicPageData,
                currentPageIndex = 0
            )
        }

        if (chapter != null && content.isNotEmpty() && state.containerSize != IntSize.Zero && state.density != null) {
            // 分页
            val finalPages = PageSplitter.splitContent(
                content = content,
                chapterTitle = chapter.chapterName,
                containerSize = state.containerSize,
                readerSettings = state.readerSettings,
                density = state.density
            )

            // 判断是否是第一章且需要添加书籍详情页
            val isFirstChapter = currentIndex == 0
            val hasBookDetailPage = isFirstChapter

            // 准备相邻章节数据（用于PageCurl和Slide模式）
            val nextChapterData = if (currentIndex + 1 < chapterList.size) {
                val nextChapter = chapterList[currentIndex + 1]
                val nextChapterCache = chapterCache[nextChapter.id]
                nextChapterCache?.let { cache ->
                    // 为下一章进行分页（如果还没有分页数据）
                    val nextPages = if (cache.pageData?.pages?.isNotEmpty() == true) {
                        cache.pageData.pages
                    } else {
                        PageSplitter.splitContent(
                            content = cache.content,
                            chapterTitle = cache.chapter.chapterName,
                            containerSize = state.containerSize,
                            readerSettings = state.readerSettings,
                            density = state.density
                        )
                    }
                    
                    PageData(
                        chapterId = cache.chapter.id,
                        chapterName = cache.chapter.chapterName,
                        content = cache.content,
                        pages = nextPages,
                        isFirstPage = true,
                        isLastPage = false
                    )
                }
            } else null

            val previousChapterData = if (currentIndex > 0) {
                val previousChapter = chapterList[currentIndex - 1]
                val previousChapterCache = chapterCache[previousChapter.id]
                previousChapterCache?.let { cache ->
                    // 为上一章进行分页（如果还没有分页数据）
                    val prevPages = if (cache.pageData?.pages?.isNotEmpty() == true) {
                        cache.pageData.pages
                    } else {
                        PageSplitter.splitContent(
                            content = cache.content,
                            chapterTitle = cache.chapter.chapterName,
                            containerSize = state.containerSize,
                            readerSettings = state.readerSettings,
                            density = state.density
                        )
                    }
                    
                    PageData(
                        chapterId = cache.chapter.id,
                        chapterName = cache.chapter.chapterName,
                        content = cache.content,
                        pages = prevPages,
                        isFirstPage = false,
                        isLastPage = true
                    )
                }
            } else null

            // 确定重新分页后的页面索引
            val pageIndexAfterSplit = if (restoreProgress != null && finalPages.isNotEmpty()) {
                (restoreProgress * finalPages.size).toInt().coerceIn(0, finalPages.size - 1)
            } else {
                state.currentPageIndex
            }

            // 确保当前页面索引在有效范围内，考虑书籍详情页
            val totalPagesInChapter = finalPages.size
            val safeCurrentPageIndex = when {
                state.currentPageIndex == -1 -> {
                    if (hasBookDetailPage) {
                        -1 // 保持在书籍详情页
                    } else {
                        // This case happens when flipping to a previous chapter. We want the last page.
                        (totalPagesInChapter - 1).coerceAtLeast(0)
                    }
                }
                else -> pageIndexAfterSplit.coerceIn(0, totalPagesInChapter - 1)
            }

            // 异步加载书籍信息（仅在第一章且需要时）
            viewModelScope.launch {
                val bookInfo = if (hasBookDetailPage) {
                    loadBookInfo(state.bookId)
                } else null

                val pageData = PageData(
                    chapterId = chapter.id,
                    chapterName = chapter.chapterName,
                    content = content,
                    pages = finalPages,
                    isFirstPage = safeCurrentPageIndex == 0,
                    isLastPage = safeCurrentPageIndex == totalPagesInChapter - 1,
                    nextChapterData = nextChapterData,
                    previousChapterData = previousChapterData,
                    bookInfo = bookInfo,
                    hasBookDetailPage = hasBookDetailPage
                )

                _uiState.value = _uiState.value.copy(
                    currentPageData = pageData,
                    currentPageIndex = safeCurrentPageIndex, // 确保页面索引有效
                    readingProgress = when {
                        safeCurrentPageIndex == -1 -> 0f // 书籍详情页进度为0
                        totalPagesInChapter > 0 && hasBookDetailPage -> (safeCurrentPageIndex + 1).toFloat() / (totalPagesInChapter + 1).toFloat()
                        totalPagesInChapter > 0 -> (safeCurrentPageIndex + 1).toFloat() / totalPagesInChapter.toFloat()
                        else -> 0f
                    }
                )

                // 更新缓存中的分页数据
                val cachedChapter = chapterCache[chapter.id]
                if (cachedChapter != null) {
                    chapterCache[chapter.id] = cachedChapter.copy(pageData = pageData)
                }
            }
        }
    }

    /**
     * 预加载相邻章节 - 扩展预加载范围并优化分页
     */
    private fun preloadAdjacentChapters(currentChapterId: String) {
        viewModelScope.launch {
            val chapterList = _uiState.value.chapterList
            val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }

            if (currentIndex >= 0) {
                // 预加载前2章和后2章
                val preloadRange = listOf(
                    currentIndex - 2,
                    currentIndex - 1,
                    currentIndex + 1,
                    currentIndex + 2
                ).filter { it in chapterList.indices }

                preloadRange.forEach { index ->
                    val chapter = chapterList[index]
                    if (!chapterCache.containsKey(chapter.id)) {
                        try {
                            preloadChapter(chapter.id)
                        } catch (e: Exception) {
                            // 预加载失败不影响主流程
                        }
                    }
                }
                
                // 为已缓存但未分页的章节进行分页
                val state = _uiState.value
                if (state.containerSize != IntSize.Zero && state.density != null) {
                    preloadRange.forEach { index ->
                        val chapter = chapterList[index]
                        val cachedChapter = chapterCache[chapter.id]
                        if (cachedChapter != null && cachedChapter.pageData == null) {
                            // 异步分页处理
                            launch {
                                try {
                                    val pages = PageSplitter.splitContent(
                                        content = cachedChapter.content,
                                        chapterTitle = cachedChapter.chapter.chapterName,
                                        containerSize = state.containerSize,
                                        readerSettings = state.readerSettings,
                                        density = state.density
                                    )
                                    
                                    val pageData = PageData(
                                        chapterId = chapter.id,
                                        chapterName = chapter.chapterName,
                                        content = cachedChapter.content,
                                        pages = pages
                                    )
                                    
                                    // 更新缓存
                                    chapterCache[chapter.id] = cachedChapter.copy(pageData = pageData)
                                } catch (e: Exception) {
                                    // 分页失败，忽略
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理页面翻页 - 修复翻页逻辑，添加防重复触发机制，支持书籍详情页
     */
    private fun handlePageFlip(direction: FlipDirection) {
        // 防重复触发检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < flipCooldownMs) {
            return // 在冷却时间内，忽略翻页请求
        }
        lastFlipTime = currentTime

        val state = _uiState.value
        val pageData = state.currentPageData ?: return
        
        // 计算总页数（包含书籍详情页）
        val totalPages = pageData.totalPageCount
        
        // 确保页面数据有效
        if (totalPages == 0) {
            // 如果没有页面数据，直接切换章节
            handleChapterFlip(direction)
            return
        }

        when (direction) {
            FlipDirection.NEXT -> {
                val currentIndex = state.currentPageIndex
                if (currentIndex == -1) {
                    // 从书籍详情页翻到第一章第一页
                    if (pageData.pages.isNotEmpty()) {
                        val newIndex = 0
                        val newPageData = pageData.copy(
                            isFirstPage = true,
                            isLastPage = newIndex == pageData.pages.size - 1
                        )
                        
                        _uiState.value = state.copy(
                            currentPageIndex = newIndex,
                            currentPageData = newPageData,
                            readingProgress = if (pageData.hasBookDetailPage) {
                                (newIndex + 1).toFloat() / totalPages.toFloat()
                            } else {
                                newIndex.toFloat() / totalPages.toFloat()
                            }
                        )
                    }
                } else if (currentIndex < pageData.pages.size - 1) {
                    // 还有下一页，执行页面翻页
                    val newIndex = currentIndex + 1
                    val newPageData = pageData.copy(
                        isFirstPage = newIndex == 0,
                        isLastPage = newIndex == pageData.pages.size - 1
                    )
                    
                    _uiState.value = state.copy(
                        currentPageIndex = newIndex,
                        currentPageData = newPageData,
                        readingProgress = if (pageData.hasBookDetailPage) {
                            (newIndex + 1).toFloat() / totalPages.toFloat()
                        } else {
                            newIndex.toFloat() / totalPages.toFloat()
                        }
                    )
                } else {
                    // 已经是章节最后一页，切换到下一章
                    handleChapterFlip(FlipDirection.NEXT)
                }
            }

            FlipDirection.PREVIOUS -> {
                val currentIndex = state.currentPageIndex
                if (currentIndex > 0) {
                    // 还有上一页，执行页面翻页
                    val newIndex = currentIndex - 1
                    val newPageData = pageData.copy(
                        isFirstPage = newIndex == 0,
                        isLastPage = newIndex == pageData.pages.size - 1
                    )
                    
                    _uiState.value = state.copy(
                        currentPageIndex = newIndex,
                        currentPageData = newPageData,
                        readingProgress = if (pageData.hasBookDetailPage) {
                            (newIndex + 1).toFloat() / totalPages.toFloat()
                        } else {
                            newIndex.toFloat() / totalPages.toFloat()
                        }
                    )
                } else if (currentIndex == 0 && pageData.hasBookDetailPage) {
                    // 从第一页翻到书籍详情页
                    val newIndex = -1
                    val newPageData = pageData.copy(
                        isFirstPage = false,
                        isLastPage = false
                    )
                    
                    _uiState.value = state.copy(
                        currentPageIndex = newIndex,
                        currentPageData = newPageData,
                        readingProgress = 0f // 书籍详情页进度为0
                    )
                } else {
                    // 已经是章节第一页且没有书籍详情页，切换到上一章
                    handleChapterFlip(FlipDirection.PREVIOUS)
                }
            }
        }
    }

    /**
     * 处理章节切换 - 优化版本，修复页面索引设置问题，添加防重复触发
     */
    private fun handleChapterFlip(direction: FlipDirection) {
        // 防重复触发检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < flipCooldownMs) {
            return // 在冷却时间内，忽略章节切换请求
        }
        lastFlipTime = currentTime

        val state = _uiState.value
        val currentIndex = state.currentChapterIndex
        val chapterList = state.chapterList

        val targetIndex = when (direction) {
            FlipDirection.NEXT -> currentIndex + 1
            FlipDirection.PREVIOUS -> currentIndex - 1
        }

        if (targetIndex in chapterList.indices) {
            val targetChapter = chapterList[targetIndex]
            loadChapterContentMethod(targetChapter.id, direction)
        }
    }

    /**
     * 更新容器尺寸
     */
    private fun updateContainerSizeInternal(size: IntSize, density: Density) {
        val oldSize = _uiState.value.containerSize
        _uiState.value = _uiState.value.copy(
            containerSize = size,
            density = density
        )

        if (size != oldSize && _uiState.value.bookId.isNotEmpty()) {
            viewModelScope.launch {
                val bookCache = bookCacheManager.getBookContentCache(_uiState.value.bookId)
                if (bookCache != null) {
                    startProgressivePagination(bookCache)
                }
            }
        } else if (_uiState.value.currentChapter != null) {
            splitContent()
        }
    }

    /**
     * 上一章
     */
    private fun previousChapterInternal() {
        handleChapterFlip(FlipDirection.PREVIOUS)
    }

    /**
     * 下一章
     */
    private fun nextChapterInternal() {
        handleChapterFlip(FlipDirection.NEXT)
    }

    /**
     * 切换到指定章节
     */
    private fun switchToChapterInternal(chapterId: String) {
        loadChapterContentMethod(chapterId)
    }

    /**
     * 跳转到指定进度
     */
    private fun seekToProgressInternal(progress: Float) {
        val pageCountCache = _uiState.value.pageCountCache ?: return
        if (pageCountCache.totalPages <= 0) return

        val targetGlobalPage = (progress * pageCountCache.totalPages).toInt()
            .coerceIn(0, pageCountCache.totalPages - 1)

        val chapterAndPage = bookCacheManager.findChapterByAbsolutePage(pageCountCache, targetGlobalPage)
        if (chapterAndPage != null) {
            val (targetChapterId, relativePageInChapter) = chapterAndPage

            if (targetChapterId == _uiState.value.currentChapter?.id) {
                _uiState.value = _uiState.value.copy(
                    currentPageIndex = relativePageInChapter
                )
            } else {
                viewModelScope.launch {
                    loadChapterContentInternal(targetChapterId, initialPageIndex = relativePageInChapter)
                }
            }
        }
    }

    /**
     * 更新阅读器设置
     */
    private fun updateSettingsInternal(settings: ReaderSettings) {
        val oldSettings = _uiState.value.readerSettings
        val oldState = _uiState.value // Capture state before change
        _uiState.value = _uiState.value.copy(readerSettings = settings)

        // 如果翻页方式发生改变，保存到本地存储
        if (oldSettings.pageFlipEffect != settings.pageFlipEffect) {
            savePageFlipEffect(settings.pageFlipEffect)
        }

        if (oldSettings.fontSize != settings.fontSize) {
            // Font size changed, need to maintain reading position
            val oldPages = oldState.currentPageData?.pages ?: emptyList()
            val chapterProgress = if (oldPages.isNotEmpty() && oldState.currentPageIndex >= 0) {
                (oldState.currentPageIndex.toFloat() + 1) / oldPages.size.toFloat()
            } else 0f

            // Re-split content, restoring progress
            splitContent(restoreProgress = chapterProgress)

            viewModelScope.launch {
                val bookCache = bookCacheManager.getBookContentCache(_uiState.value.bookId)
                if (bookCache != null) {
                    startProgressivePagination(bookCache)
                }
            }
        } else if (_uiState.value.currentChapter != null) {
            // Settings other than font size changed, just re-split
            splitContent()
        }
    }

    /**
     * 重试
     */
    private fun retryInternal() {
        val currentState = _uiState.value
        if (currentState.bookId.isNotEmpty()) {
            if (currentState.chapterList.isEmpty()) {
                loadChapterListMethod()
            } else if (currentState.currentChapter == null) {
                val firstChapter = currentState.chapterList.firstOrNull()
                if (firstChapter != null) {
                    loadChapterContentMethod(firstChapter.id)
                }
            }
        }
    }

    /**
     * 预加载单个章节
     */
    private suspend fun preloadChapter(chapterId: String) {
        try {
            val chapterIdLong = chapterId.toLong()
            val response = bookService.getBookContentBlocking(chapterIdLong)

            if (response.code == "00000" && response.data != null) {
                val data = response.data
                val chapterInfo = data.chapterInfo
                val bookContent = data.bookContent

                val chapter = Chapter(
                    id = chapterInfo.id.toString(),
                    chapterName = chapterInfo.chapterName,
                    chapterNum = chapterInfo.chapterNum.toString(),
                    isVip = if (chapterInfo.isVip == 1) "1" else "0"
                )

                val chapterCache = ChapterCache(
                    chapter = chapter,
                    content = bookContent
                )

                addToCache(chapterId, chapterCache)

                // FIX: Check if preloaded chapter is adjacent and re-split content to update pageData
                val state = _uiState.value
                val currentChapterId = state.currentChapter?.id
                val chapterList = state.chapterList
                val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
                val preloadedIndex = chapterList.indexOfFirst { it.id == chapterId }

                if (currentIndex != -1 && (preloadedIndex == currentIndex + 1 || preloadedIndex == currentIndex - 1)) {
                    splitContent()
                }
            }
        } catch (e: Exception) {
            // 预加载失败，忽略
        }
    }

    /**
     * 添加到缓存 - 扩展缓存大小
     */
    private fun addToCache(chapterId: String, chapterCache: ChapterCache) {
        // 如果缓存满了，移除最旧的条目
        if (this.chapterCache.size >= maxCacheSize) {
            val oldestKey = this.chapterCache.keys.first()
            this.chapterCache.remove(oldestKey)
        }

        this.chapterCache[chapterId] = chapterCache
    }

    private fun fetchAllBookContentInBackground(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookCache = bookCacheManager.getBookContentCache(bookId)
            val chapterList = _uiState.value.chapterList
            val allChapterIds = chapterList.map { it.id }.toSet()

            // 检查缓存是否完整
            if (bookCache != null && bookCache.chapterIds.toSet() == allChapterIds) {
                startProgressivePagination(bookCache)
                return@launch
            }

            val cachedChapters = bookCache?.chapters?.toMutableList() ?: mutableListOf()
            val cachedChapterIds = cachedChapters.map { it.chapterId }.toSet()
            val chaptersToFetch = chapterList.filterNot { cachedChapterIds.contains(it.id) }

            chaptersToFetch.forEach { chapter ->
                try {
                    val response = bookService.getBookContentBlocking(chapter.id.toLong())
                    if (response.code == "00000" && response.data != null) {
                        cachedChapters.add(
                            BookCacheData.ChapterContentData(
                                chapterId = response.data.chapterInfo.id.toString(),
                                chapterName = response.data.chapterInfo.chapterName,
                                content = response.data.bookContent,
                                chapterNum = response.data.chapterInfo.chapterNum
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Log error but continue
                }
            }

            cachedChapters.sortBy { it.chapterNum }

            val bookInfoData = loadBookInfo(bookId)
            val bookInfoForCache = bookInfoData?.let {
                BookCacheData.BookInfo(
                    bookName = it.bookName,
                    authorName = it.authorName,
                    bookDesc = it.bookDesc,
                    picUrl = it.picUrl,
                    visitCount = it.visitCount,
                    wordCount = it.wordCount,
                    categoryName = it.categoryName
                )
            }

            val newBookCacheData = BookCacheData(
                bookId = bookId,
                chapters = cachedChapters,
                chapterIds = cachedChapters.map { it.chapterId },
                cacheTime = System.currentTimeMillis(),
                bookInfo = bookInfoForCache
            )

            bookCacheManager.saveBookContentCache(newBookCacheData)
            startProgressivePagination(newBookCacheData)
        }
    }

    private fun startProgressivePagination(bookCacheData: BookCacheData) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val containerSize = state.containerSize
            if (containerSize == IntSize.Zero || state.density == null) return@launch

            val existingPageCountCache = bookCacheManager.getPageCountCache(
                bookCacheData.bookId,
                state.readerSettings.fontSize,
                containerSize
            )
            if (existingPageCountCache != null) {
                _uiState.value = _uiState.value.copy(pageCountCache = existingPageCountCache)
                return@launch
            }

            val progressJob = launch {
                bookCacheManager.progressiveCalculationState.collect {
                    _uiState.value = _uiState.value.copy(paginationState = it)
                }
            }

            val newPageCountCache = bookCacheManager.calculateAllPagesProgressively(
                bookCacheData = bookCacheData,
                readerSettings = state.readerSettings,
                containerSize = containerSize,
                density = state.density,
                onProgressUpdate = { _, _ -> }
            )

            progressJob.cancel()
            _uiState.value = _uiState.value.copy(
                pageCountCache = newPageCountCache,
                paginationState = ProgressiveCalculationState(isCalculating = false)
            )
        }
    }
} 