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
 * 翻页状态
 */
sealed class FlipState {
    data object Idle : FlipState()
    data class Flipping(val progress: Float, val direction: FlipDirection) : FlipState()
    data class PageChanged(val newPageIndex: Int, val direction: FlipDirection) : FlipState()
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

    val computedReadingProgress: Float get() {
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

    // 章节缓存，扩大到可以缓存前后各3章（当前+前后各3章+缓冲）
    private val chapterCache = mutableMapOf<String, ChapterCache>()
    private val maxCacheSize = 12 // 增大缓存尺寸

    // 防重复触发机制
    private var lastFlipTime = 0L
    private val flipCooldownMs = 200L // 减少冷却时间，提升响应性

    // 预加载状态跟踪
    private val preloadingChapters = mutableSetOf<String>()
    
    // 动态预加载范围管理
    private var currentPreloadStartIndex = -1 // 当前预加载的起始章节索引
    private var currentPreloadEndIndex = -1   // 当前预加载的结束章节索引
    private val minPreloadRange = 2           // 最小预加载范围：前后各2章
    private val maxPreloadRange = 4           // 最大预加载范围：前后各4章
    
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
                val pageFlipEffect = PageFlipEffect.valueOf(savedEffect)
                
                val currentSettings = _uiState.value.readerSettings
                _uiState.value = _uiState.value.copy(
                    readerSettings = currentSettings.copy(pageFlipEffect = pageFlipEffect)
                )
            }
        } catch (e: Exception) {
            // 如果加载失败，使用默认值
            _uiState.value = _uiState.value.copy(
                readerSettings = _uiState.value.readerSettings.copy(pageFlipEffect = PageFlipEffect.PAGECURL)
            )
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
            // 同时更新阅读进度，用于纵向滚动模式的进度计算
            updateReadingProgressFromPageIndex(page)
        }
    }

    /**
     * 根据页面索引更新阅读进度 - 用于纵向滚动模式
     */
    private fun updateReadingProgressFromPageIndex(pageIndex: Int) {
        val state = _uiState.value
        val pageCountCache = state.pageCountCache
        
        if (pageCountCache != null && pageCountCache.totalPages > 0) {
            val currentChapter = state.currentChapter
            if (currentChapter != null) {
                val chapterRange = pageCountCache.chapterPageRanges.find { it.chapterId == currentChapter.id }
                if (chapterRange != null) {
                    val globalPage = chapterRange.startPage + pageIndex
                    val newProgress = globalPage.toFloat() / pageCountCache.totalPages.toFloat()
                    _uiState.value = _uiState.value.copy(readingProgress = newProgress.coerceIn(0f, 1f))
                }
            }
        }
    }

    /**
     * 从书籍详情页导航到第一页内容
     */
    fun navigateToContent() {
        if (_uiState.value.currentPageIndex == -1) {
            onPageChange(FlipDirection.NEXT)
        }
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
                        _uiState.value = _uiState.value.copy(currentPageIndex = -1, virtualPageIndex = 0)
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
     * 加载章节内容内部方法 - 修复页面索引设置，支持书籍详情页，增强预加载
     */
    private suspend fun loadChapterContentInternal(
        chapterId: String,
        flipDirection: FlipDirection? = null,
        initialPageIndex: Int? = null,
        preserveVirtualIndex: Boolean = false
    ) {
        // 先检查缓存
        val cachedChapter = chapterCache[chapterId]
        if (cachedChapter != null) {
            updateCurrentChapter(cachedChapter, flipDirection, initialPageIndex, preserveVirtualIndex)
            // 立即开始动态预加载（前后各2章）
            performDynamicPreload(chapterId, triggerExpansion = false)
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
                updateCurrentChapter(chapterCache, flipDirection, initialPageIndex, preserveVirtualIndex)

                // 启动动态预加载（前后各2章）
                performDynamicPreload(chapterId, triggerExpansion = false)
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
        initialPageIndexOverride: Int? = null,
        preserveVirtualIndex: Boolean = false
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
            currentPageIndex = initialPageIndex,
            isSwitchingChapter = true // 标记正在切换，以触发分页和虚拟页面重建
        )

        // 如果有容器尺寸信息，进行分页
        val state = _uiState.value
        if (state.containerSize != IntSize.Zero && state.density != null) {
            splitContent(restoreProgress = null, preserveVirtualIndex = preserveVirtualIndex)
        }
    }

    /**
     * 分页处理 - 优化章节衔接和相邻章节数据获取，支持书籍详情页
     */
    private fun splitContent(restoreProgress: Float? = null, preserveVirtualIndex: Boolean = false) {
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
                isFirstChapter = currentIndex == 0,
                isLastChapter = currentIndex == chapterList.size - 1,
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
            val isLastChapter = currentIndex == chapterList.size - 1
            val hasBookDetailPage = isFirstChapter // 只有第一章前有详情页

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
                        isLastPage = false,
                        isFirstChapter = false,
                        isLastChapter = false
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
                        isLastPage = true,
                        isFirstChapter = false,
                        isLastChapter = true
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
                else -> pageIndexAfterSplit.coerceIn(0, totalPagesInChapter.coerceAtLeast(1) - 1)
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
                    isFirstChapter = isFirstChapter,
                    isLastChapter = isLastChapter,
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
                    },
                    isSwitchingChapter = false
                )

                // 更新缓存中的分页数据
                val cachedChapter = chapterCache[chapter.id]
                if (cachedChapter != null) {
                    chapterCache[chapter.id] = cachedChapter.copy(pageData = pageData)
                }

                // 分页完成后，立即构建虚拟页面列表
                buildVirtualPages(preserveCurrentIndex = preserveVirtualIndex)
            }
        }
    }

    /**
     * 构建或更新虚拟页面列表 - 包含所有已预加载的章节
     */
    private fun buildVirtualPages(preserveCurrentIndex: Boolean) {
        val state = _uiState.value
        val currentChapter = state.currentChapter ?: return
        val chapterList = state.chapterList
        val currentIndex = state.currentChapterIndex
        val currentPageData = state.currentPageData ?: return

        // 获取所有已加载且已分页的章节数据，按章节顺序排列
        val loadedChaptersData = mutableMapOf<String, PageData>()
        val orderedLoadedChapters = mutableListOf<Pair<Int, PageData>>()
        
        chapterList.forEachIndexed { index, chapter ->
            val cachedChapter = chapterCache[chapter.id]
            if (cachedChapter?.pageData != null) {
                loadedChaptersData[chapter.id] = cachedChapter.pageData
                orderedLoadedChapters.add(index to cachedChapter.pageData)
            }
        }

        // 按章节索引排序
        orderedLoadedChapters.sortBy { it.first }

        var currentChapterStartIndex = 0
        
        val newVirtualPages = buildList<VirtualPage> {
            orderedLoadedChapters.forEach { (chapterIndex, pageData) ->
                if (chapterIndex < currentIndex) {
                    // 在当前章节之前的章节
                    pageData.pages.forEachIndexed { pageIndex, _ ->
                        add(VirtualPage.ContentPage(pageData.chapterId, pageIndex))
                    }
                } else if (chapterIndex == currentIndex) {
                    // 当前章节
                    currentChapterStartIndex = size
                    
                    // 添加书籍详情页（仅对第一章）
                    if (pageData.hasBookDetailPage) {
                        add(VirtualPage.BookDetailPage)
                    }
                    
                    // 添加当前章节页面
                    pageData.pages.forEachIndexed { pageIndex, _ ->
                        add(VirtualPage.ContentPage(pageData.chapterId, pageIndex))
                    }
                } else {
                    // 在当前章节之后的章节
                    pageData.pages.forEachIndexed { pageIndex, _ ->
                        add(VirtualPage.ContentPage(pageData.chapterId, pageIndex))
                    }
                }
            }
        }

        val newVirtualPageIndex = if (preserveCurrentIndex) {
            // 保持当前虚拟页面索引，但需要确保在有效范围内
            // 首先检查当前虚拟页面是否仍然有效
            val currentVirtualPage = state.virtualPages.getOrNull(state.virtualPageIndex)
            if (currentVirtualPage != null) {
                // 尝试在新的虚拟页面列表中找到相同的页面
                val newIndex = newVirtualPages.indexOfFirst { newPage ->
                    when {
                        currentVirtualPage is VirtualPage.BookDetailPage && newPage is VirtualPage.BookDetailPage -> true
                        currentVirtualPage is VirtualPage.ContentPage && newPage is VirtualPage.ContentPage ->
                            currentVirtualPage.chapterId == newPage.chapterId && 
                            currentVirtualPage.pageIndex == newPage.pageIndex
                        else -> false
                    }
                }
                if (newIndex >= 0) {
                    newIndex // 找到了对应的页面，使用新索引
                } else {
                    state.virtualPageIndex.coerceIn(0, newVirtualPages.size.coerceAtLeast(1) - 1)
                }
            } else {
                state.virtualPageIndex.coerceIn(0, newVirtualPages.size.coerceAtLeast(1) - 1)
            }
        } else {
            // 重新计算虚拟页面索引
            val bookDetailPageCount = if (currentPageData.hasBookDetailPage) 1 else 0
            
            when (state.currentPageIndex) {
                -1 -> currentChapterStartIndex // 书籍详情页的索引
                else -> currentChapterStartIndex + bookDetailPageCount + state.currentPageIndex
            }.coerceIn(0, newVirtualPages.size.coerceAtLeast(1) - 1)
        }

        // 获取相邻章节数据用于兼容性
        val (previousChapterData, nextChapterData) = getAdjacentChapterData(currentChapter.id)

        _uiState.value = state.copy(
            virtualPages = newVirtualPages,
            virtualPageIndex = newVirtualPageIndex,
            loadedChapterData = loadedChaptersData,
            previousChapterData = previousChapterData,
            nextChapterData = nextChapterData,
            isSwitchingChapter = false
        )
    }

    /**
     * 动态双向预加载 - 根据当前阅读位置动态调整预加载范围
     * 确保始终前后各有至少2章已加载，最多扩展到前后各4章
     */
    private fun performDynamicPreload(currentChapterId: String, triggerExpansion: Boolean = false) {
        viewModelScope.launch {
            val chapterList = _uiState.value.chapterList
            val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }

            if (currentIndex < 0) return@launch

            // 计算新的预加载范围
            val newPreloadRange = calculatePreloadRange(currentIndex, chapterList.size, triggerExpansion)
            val (newStartIndex, newEndIndex) = newPreloadRange

            // 更新预加载范围
            currentPreloadStartIndex = newStartIndex
            currentPreloadEndIndex = newEndIndex

            // 执行预加载
            val preloadIndices = (newStartIndex..newEndIndex).filter { it != currentIndex }
            
            // 并行预加载所有需要的章节
            preloadIndices.map { index ->
                launch {
                    val chapter = chapterList[index]
                    if (!chapterCache.containsKey(chapter.id) && !preloadingChapters.contains(chapter.id)) {
                        try {
                            preloadingChapters.add(chapter.id)
                            preloadChapter(chapter.id)
                        } catch (e: Exception) {
                            // 预加载失败不影响主流程
                        } finally {
                            preloadingChapters.remove(chapter.id)
                        }
                    }
                }
            }.forEach { it.join() }
            
            // 为已缓存但未分页的章节进行分页
            val state = _uiState.value
            if (state.containerSize != IntSize.Zero && state.density != null) {
                val paginationJobs = preloadIndices.mapNotNull { index ->
                    val chapter = chapterList[index]
                    val cachedChapter = chapterCache[chapter.id]
                    if (cachedChapter != null && cachedChapter.pageData == null) {
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
                                    pages = pages,
                                    isFirstChapter = index == 0,
                                    isLastChapter = index == chapterList.size - 1
                                )
                                
                                // 更新缓存
                                chapterCache[chapter.id] = cachedChapter.copy(pageData = pageData)
                            } catch (e: Exception) {
                                // 分页失败,忽略
                            }
                        }
                    } else null
                }
                
                // 等待所有分页任务完成
                paginationJobs.forEach { it.join() }
            }

            // 清理超出预加载范围的缓存
            cleanupOutOfRangeCache(newStartIndex, newEndIndex, currentIndex)
            
            // 只有在新加载了相邻章节时才重建虚拟页面
            val hasNewAdjacentChapters = checkIfNewAdjacentChaptersLoaded(currentIndex)
            if (hasNewAdjacentChapters) {
                buildVirtualPages(preserveCurrentIndex = true)
            }
        }
    }

    /**
     * 计算预加载范围
     */
    private fun calculatePreloadRange(currentIndex: Int, totalChapters: Int, triggerExpansion: Boolean): Pair<Int, Int> {
        val range = if (triggerExpansion && canExpandRange()) {
            // 扩展到最大范围
            maxPreloadRange
        } else {
            // 使用最小范围
            minPreloadRange
        }

        val startIndex = (currentIndex - range).coerceAtLeast(0)
        val endIndex = (currentIndex + range).coerceAtMost(totalChapters - 1)
        
        return Pair(startIndex, endIndex)
    }

    /**
     * 判断是否可以扩展预加载范围
     */
    private fun canExpandRange(): Boolean {
        // 如果缓存使用率较低，可以扩展
        return chapterCache.size < maxCacheSize * 0.8
    }

    /**
     * 清理超出预加载范围的缓存
     */
    private fun cleanupOutOfRangeCache(startIndex: Int, endIndex: Int, currentIndex: Int) {
        val chapterList = _uiState.value.chapterList
        val keysToRemove = mutableListOf<String>()
        
        chapterCache.keys.forEach { chapterId ->
            val chapterIndex = chapterList.indexOfFirst { it.id == chapterId }
            if (chapterIndex >= 0 && (chapterIndex < startIndex || chapterIndex > endIndex)) {
                keysToRemove.add(chapterId)
            }
        }
        
        // 保留当前章节
        val currentChapterId = chapterList.getOrNull(currentIndex)?.id
        keysToRemove.removeAll { it == currentChapterId }
        
        // 清理
        keysToRemove.forEach { chapterCache.remove(it) }
    }

    /**
     * 检查是否有新的相邻章节被加载
     */
    private fun checkIfNewAdjacentChaptersLoaded(currentIndex: Int): Boolean {
        val chapterList = _uiState.value.chapterList
        val currentVirtualPages = _uiState.value.virtualPages
        
        // 检查前一章节
        val prevIndex = currentIndex - 1
        if (prevIndex >= 0) {
            val prevChapter = chapterList[prevIndex]
            val prevChapterInVirtual = currentVirtualPages.any { 
                it is VirtualPage.ContentPage && it.chapterId == prevChapter.id 
            }
            val prevChapterLoaded = chapterCache[prevChapter.id]?.pageData != null
            
            if (prevChapterLoaded && !prevChapterInVirtual) {
                return true // 有新的前一章节已加载但不在虚拟页面中
            }
        }
        
        // 检查后一章节
        val nextIndex = currentIndex + 1
        if (nextIndex < chapterList.size) {
            val nextChapter = chapterList[nextIndex]
            val nextChapterInVirtual = currentVirtualPages.any { 
                it is VirtualPage.ContentPage && it.chapterId == nextChapter.id 
            }
            val nextChapterLoaded = chapterCache[nextChapter.id]?.pageData != null
            
            if (nextChapterLoaded && !nextChapterInVirtual) {
                return true // 有新的后一章节已加载但不在虚拟页面中
            }
        }
        
        return false
    }

    /**
     * 预加载相邻章节 - 兼容旧接口，调用新的动态预加载
     */
    private fun preloadAdjacentChaptersExtended(currentChapterId: String) {
        performDynamicPreload(currentChapterId, triggerExpansion = false)
    }

    /**
     * 处理页面翻页 - 优化版本，更好的边界处理和状态同步，支持平移模式防回跳
     */
    private fun handlePageFlip(direction: FlipDirection) {
        // 防重复触发检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < flipCooldownMs) {
            return
        }
        lastFlipTime = currentTime

        val state = _uiState.value
        val virtualPages = state.virtualPages
        if (virtualPages.isEmpty()) return

        val currentVirtualIndex = state.virtualPageIndex
        val newVirtualIndex = when (direction) {
            FlipDirection.NEXT -> currentVirtualIndex + 1
            FlipDirection.PREVIOUS -> currentVirtualIndex - 1
        }

        if (newVirtualIndex in virtualPages.indices) {
            // 在虚拟页面列表内移动
            val newVirtualPage = virtualPages[newVirtualIndex]
            
            // 检查是否为平移模式，如果是则需要特殊处理以避免循环更新
            val isSlideMode = state.readerSettings.pageFlipEffect == PageFlipEffect.SLIDE
            
            if (isSlideMode) {
                // 平移模式：只更新相关状态，不更新 virtualPageIndex
                // virtualPageIndex 将由 SlideFlipContainer 的用户手势直接管理
                updateSlideFlipState(newVirtualPage, newVirtualIndex)
            } else {
                // 其他翻页模式：立即更新virtualPageIndex
                _uiState.value = state.copy(virtualPageIndex = newVirtualIndex)
                updatePageFlipState(newVirtualPage, newVirtualIndex)
            }
            
            // 检查是否需要预加载更多
            if (newVirtualPage is VirtualPage.ContentPage) {
                val pageData = _uiState.value.loadedChapterData[newVirtualPage.chapterId]
                if (pageData != null) {
                    val isAtChapterBoundary = newVirtualPage.pageIndex == 0 || 
                                             newVirtualPage.pageIndex == pageData.pages.size - 1
                    // 只在章节边界时检查预加载
                    if (isAtChapterBoundary) {
                        checkAndPreload(newVirtualIndex, virtualPages.size)
                    }
                }
            }

        } else {
            // 到达虚拟列表边界，需要加载新章节
            handleChapterFlip(direction)
        }
    }

    /**
     * 处理平移模式的状态更新（避免循环更新）
     */
    private fun updateSlideFlipState(newVirtualPage: VirtualPage, newVirtualIndex: Int) {
        when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                val state = _uiState.value
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    // 切换章节
                    val newChapterCache = chapterCache[newVirtualPage.chapterId]
                    if (newChapterCache != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        _uiState.value = _uiState.value.copy(
                            currentChapter = newChapterCache.chapter,
                            currentChapterIndex = newChapterIndex,
                            currentPageIndex = newVirtualPage.pageIndex,
                            currentPageData = newChapterCache.pageData,
                            virtualPageIndex = newVirtualIndex // 只在这里更新 virtualPageIndex
                        )
                        
                        // 触发动态预加载
                        performDynamicPreload(newVirtualPage.chapterId, triggerExpansion = false)
                    }
                } else {
                    // 同章节内翻页
                    _uiState.value = _uiState.value.copy(
                        currentPageIndex = newVirtualPage.pageIndex,
                        virtualPageIndex = newVirtualIndex
                    )
                }
            }
            is VirtualPage.BookDetailPage -> {
                _uiState.value = _uiState.value.copy(
                    currentPageIndex = -1,
                    virtualPageIndex = newVirtualIndex
                )
            }
            is VirtualPage.ChapterSection -> {
                _uiState.value = _uiState.value.copy(virtualPageIndex = newVirtualIndex)
            }
        }
    }

    /**
     * 处理其他翻页模式的状态更新
     */
    private fun updatePageFlipState(newVirtualPage: VirtualPage, newVirtualIndex: Int) {
        when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                val state = _uiState.value
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    // 切换章节
                    val newChapterCache = chapterCache[newVirtualPage.chapterId]
                    if (newChapterCache != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        _uiState.value = _uiState.value.copy(
                            currentChapter = newChapterCache.chapter,
                            currentChapterIndex = newChapterIndex,
                            currentPageIndex = newVirtualPage.pageIndex,
                            currentPageData = newChapterCache.pageData
                        )
                        
                        // 触发动态预加载
                        performDynamicPreload(newVirtualPage.chapterId, triggerExpansion = false)
                    }
                } else {
                    // 同章节内翻页
                    _uiState.value = _uiState.value.copy(currentPageIndex = newVirtualPage.pageIndex)
                }
            }
            is VirtualPage.BookDetailPage -> {
                _uiState.value = _uiState.value.copy(currentPageIndex = -1)
            }
            is VirtualPage.ChapterSection -> {
                // 章节模式暂不支持
            }
        }
    }

    /**
     * 检查是否需要动态扩展预加载范围
     * 当用户进入预加载边缘章节时，自动扩展预加载范围
     */
    private fun checkAndPreload(currentVirtualIndex: Int, totalVirtualPages: Int) {
        val state = _uiState.value
        val currentContentPage = state.virtualPages.getOrNull(currentVirtualIndex) as? VirtualPage.ContentPage ?: return
        
        val chapterList = state.chapterList
        val currentChapterIndex = chapterList.indexOfFirst { it.id == currentContentPage.chapterId }
        
        if (currentChapterIndex < 0) return

        // 检查是否需要扩展预加载范围
        val shouldExpandPreload = shouldExpandPreloadRange(currentChapterIndex, currentContentPage)
        
        if (shouldExpandPreload) {
            // 触发动态预加载扩展
            performDynamicPreload(currentContentPage.chapterId, triggerExpansion = true)
        } else {
            // 常规预加载检查
            checkRegularPreload(currentChapterIndex, currentContentPage)
        }
    }

    /**
     * 判断是否需要扩展预加载范围
     * 当用户进入预加载边缘的章节时返回true
     */
    private fun shouldExpandPreloadRange(currentChapterIndex: Int, currentContentPage: VirtualPage.ContentPage): Boolean {
        // 如果还没有初始化预加载范围，不扩展
        if (currentPreloadStartIndex == -1 || currentPreloadEndIndex == -1) {
            return false
        }

        val chapterData = _uiState.value.loadedChapterData[currentContentPage.chapterId] ?: return false
        val isAtChapterStart = currentContentPage.pageIndex == 0
        val isAtChapterEnd = currentContentPage.pageIndex == chapterData.pages.size - 1

        // 检查是否接近预加载范围边缘
        val isNearStartEdge = currentChapterIndex <= currentPreloadStartIndex + 1
        val isNearEndEdge = currentChapterIndex >= currentPreloadEndIndex - 1

        // 在章节边界且接近预加载范围边缘时扩展
        return (isAtChapterStart && isNearStartEdge) || (isAtChapterEnd && isNearEndEdge)
    }

    /**
     * 常规预加载检查
     * 确保基本的预加载范围始终维持
     */
    private fun checkRegularPreload(currentChapterIndex: Int, currentContentPage: VirtualPage.ContentPage) {
        val chapterData = _uiState.value.loadedChapterData[currentContentPage.chapterId] ?: return
        val isAtChapterStart = currentContentPage.pageIndex == 0
        val isAtChapterEnd = currentContentPage.pageIndex == chapterData.pages.size - 1

        // 基本预加载检查：在章节边界时确保相邻章节已加载
        if (isAtChapterStart || isAtChapterEnd) {
            // 检查前后相邻章节是否已加载
            val needsPreload = checkAdjacentChaptersLoaded(currentChapterIndex)
            if (needsPreload) {
                performDynamicPreload(currentContentPage.chapterId, triggerExpansion = false)
            }
        }
    }

    /**
     * 检查相邻章节是否已加载
     */
    private fun checkAdjacentChaptersLoaded(currentChapterIndex: Int): Boolean {
        val chapterList = _uiState.value.chapterList
        
        // 检查前两章
        for (i in 1..minPreloadRange) {
            val prevIndex = currentChapterIndex - i
            if (prevIndex >= 0) {
                val prevChapter = chapterList[prevIndex]
                if (!chapterCache.containsKey(prevChapter.id)) {
                    return true // 需要预加载
                }
            }
        }

        // 检查后两章
        for (i in 1..minPreloadRange) {
            val nextIndex = currentChapterIndex + i
            if (nextIndex < chapterList.size) {
                val nextChapter = chapterList[nextIndex]
                if (!chapterCache.containsKey(nextChapter.id)) {
                    return true // 需要预加载
                }
            }
        }

        return false // 都已加载，无需预加载
    }

    /**
     * 更新页面索引 - 新增辅助方法，统一页面索引更新逻辑
     */
    private fun updatePageIndex(newIndex: Int, pageData: PageData, totalPages: Int) {
        val newPageData = pageData.copy(
            isFirstPage = newIndex == 0,
            isLastPage = newIndex == pageData.pages.size - 1
        )
        
        _uiState.value = _uiState.value.copy(
            currentPageIndex = newIndex,
            currentPageData = newPageData,
            readingProgress = when {
                newIndex == -1 -> 0f // 书籍详情页进度为0
                pageData.hasBookDetailPage -> (newIndex + 1).toFloat() / totalPages.toFloat()
                else -> (newIndex + 1).toFloat() / totalPages.toFloat()
            }
        )
        buildVirtualPages(preserveCurrentIndex = false)
    }

    /**
     * 处理章节切换 - 优化版本，更好的状态管理和预加载
     */
    private fun handleChapterFlip(direction: FlipDirection) {
        // 防重复触发检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < flipCooldownMs) {
            return
        }
        lastFlipTime = currentTime

        _uiState.value = _uiState.value.copy(isSwitchingChapter = true)

        val state = _uiState.value
        val currentIndex = state.currentChapterIndex
        val chapterList = state.chapterList

        val targetIndex = when (direction) {
            FlipDirection.NEXT -> currentIndex + 1
            FlipDirection.PREVIOUS -> currentIndex - 1
        }

        if (targetIndex in chapterList.indices) {
            val targetChapter = chapterList[targetIndex]
            
            // 切换时，我们不保留旧的虚拟页面索引，因为整个页面列表会重建
            viewModelScope.launch {
                loadChapterContentInternal(targetChapter.id, direction, preserveVirtualIndex = false)
            }
        } else {
            // 到达书籍开头或结尾
            _uiState.value = _uiState.value.copy(isSwitchingChapter = false)
        }
    }

    /**
     * 更新容器尺寸
     */
    private fun updateContainerSizeInternal(size: IntSize, density: Density) {
        val oldSize = _uiState.value.containerSize
        val needsRecalculation = size != oldSize

        _uiState.value = _uiState.value.copy(
            containerSize = size,
            density = density
        )

        if (needsRecalculation) {
             if (_uiState.value.bookId.isNotEmpty() && _uiState.value.readerSettings.pageFlipEffect != PageFlipEffect.VERTICAL) {
                viewModelScope.launch {
                    val bookCache = bookCacheManager.getBookContentCache(_uiState.value.bookId)
                    if (bookCache != null) {
                        startProgressivePagination(bookCache)
                    }
                }
            }

            // 对当前章节和已加载的相邻章节重新分页
            if (_uiState.value.currentChapter != null) {
                splitContent()
            }
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
        val state = _uiState.value
        if (chapterId == state.currentChapter?.id) return

        _uiState.value = state.copy(isSwitchingChapter = true)
        viewModelScope.launch {
            loadChapterContentInternal(chapterId, preserveVirtualIndex = false)
        }
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
                buildVirtualPages(preserveCurrentIndex = false)
            } else {
                viewModelScope.launch {
                    loadChapterContentInternal(targetChapterId, initialPageIndex = relativePageInChapter, preserveVirtualIndex = false)
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

        // 如果字体大小、背景色或翻页模式发生变化，需要重新计算分页
        val needsRecalculation = oldSettings.fontSize != settings.fontSize ||
                oldSettings.backgroundColor != settings.backgroundColor ||
                (oldSettings.pageFlipEffect == PageFlipEffect.VERTICAL && settings.pageFlipEffect != PageFlipEffect.VERTICAL) ||
                (oldSettings.pageFlipEffect != PageFlipEffect.VERTICAL && settings.pageFlipEffect == PageFlipEffect.VERTICAL)

        if (needsRecalculation) {
            // Font size changed, need to maintain reading position
            val oldPages = oldState.currentPageData?.pages ?: emptyList()
            val chapterProgress = if (oldPages.isNotEmpty() && oldState.currentPageIndex >= 0) {
                (oldState.currentPageIndex.toFloat() + 1) / oldPages.size.toFloat()
            } else 0f

            // Re-split content, restoring progress
            splitContent(restoreProgress = chapterProgress)

            if (settings.pageFlipEffect != PageFlipEffect.VERTICAL) {
                viewModelScope.launch {
                    val bookCache = bookCacheManager.getBookContentCache(_uiState.value.bookId)
                    if (bookCache != null) {
                        startProgressivePagination(bookCache)
                    }
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
        if (currentState.hasError) {
             _uiState.value = currentState.copy(isLoading = true, hasError = false, error = "")
             initReaderInternal(currentState.bookId, currentState.currentChapter?.id)
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

                // 检查预加载的章节是否是相邻章节，如果是则更新虚拟页面
                val state = _uiState.value
                val currentChapterId = state.currentChapter?.id
                val chapterList = state.chapterList
                val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
                val preloadedIndex = chapterList.indexOfFirst { it.id == chapterId }

                if (currentIndex != -1 && (preloadedIndex == currentIndex + 1 || preloadedIndex == currentIndex - 1)) {
                    // 预加载完成后，如果影响到了当前显示的相邻章节，则需要重建虚拟页面
                     buildVirtualPages(preserveCurrentIndex = true)
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

    /**
     * 获取邻近章节数据 - 新增方法，用于翻页容器获取相邻章节
     */
    fun getAdjacentChapterData(currentChapterId: String): Pair<PageData?, PageData?> {
        val chapterList = _uiState.value.chapterList
        val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
        
        val previousChapterData = if (currentIndex > 0) {
            val previousChapter = chapterList[currentIndex - 1]
            val previousChapterCache = chapterCache[previousChapter.id]
            previousChapterCache?.pageData
        } else null

        val nextChapterData = if (currentIndex < chapterList.size - 1) {
            val nextChapter = chapterList[currentIndex + 1]
            val nextChapterCache = chapterCache[nextChapter.id]
            nextChapterCache?.pageData
        } else null

        return Pair(previousChapterData, nextChapterData)
    }

    /**
     * 检查章节是否可以切换 - 新增方法，用于翻页容器边界检测
     */
    fun canSwitchChapter(direction: FlipDirection): Boolean {
        val chapterList = _uiState.value.chapterList
        val currentIndex = _uiState.value.currentChapterIndex
        
        return when (direction) {
            FlipDirection.NEXT -> currentIndex < chapterList.size - 1
            FlipDirection.PREVIOUS -> currentIndex > 0
        }
    }

    /**
     * 立即切换章节 - 新增方法，用于翻页容器直接切换章节
     */
    fun switchChapterImmediately(direction: FlipDirection) {
        handleChapterFlip(direction)
    }

    /**
     * 统一的翻页状态更新方法 - 确保所有翻页模式的状态一致性
     */
    fun updateFlipState(
        newVirtualPageIndex: Int,
        newChapterId: String? = null,
        newPageIndexInChapter: Int? = null,
        triggerPreload: Boolean = true
    ) {
        val state = _uiState.value
        val virtualPages = state.virtualPages
        
        if (newVirtualPageIndex !in virtualPages.indices) return

        val newVirtualPage = virtualPages[newVirtualPageIndex]
        var updatedState = state.copy(virtualPageIndex = newVirtualPageIndex)

        // 更新章节和页面信息
        when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                // 更新当前页面索引
                updatedState = updatedState.copy(currentPageIndex = newVirtualPage.pageIndex)
                
                // 如果切换到不同章节
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    val newChapterCache = chapterCache[newVirtualPage.chapterId]
                    if (newChapterCache != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        updatedState = updatedState.copy(
                            currentChapter = newChapterCache.chapter,
                            currentChapterIndex = newChapterIndex,
                            currentPageData = newChapterCache.pageData
                        )
                        
                        // 触发预加载
                        if (triggerPreload) {
                            viewModelScope.launch {
                                performDynamicPreload(newVirtualPage.chapterId, triggerExpansion = false)
                            }
                        }
                    }
                }
            }
            is VirtualPage.BookDetailPage -> {
                updatedState = updatedState.copy(currentPageIndex = -1)
            }
            is VirtualPage.ChapterSection -> {
                // 章节模式暂不支持
            }
        }

        // 更新阅读进度
        updatedState = updateReadingProgressForState(updatedState)
        
        _uiState.value = updatedState
    }

    /**
     * 更新阅读进度 - 基于当前状态计算
     */
    private fun updateReadingProgressForState(state: ReaderUiState): ReaderUiState {
        val pageCountCache = state.pageCountCache
        val currentChapter = state.currentChapter
        val currentPageIndex = state.currentPageIndex
        
        if (pageCountCache != null && currentChapter != null && pageCountCache.totalPages > 0) {
            val chapterRange = pageCountCache.chapterPageRanges.find { it.chapterId == currentChapter.id }
            if (chapterRange != null) {
                val globalPage = when {
                    currentPageIndex == -1 -> chapterRange.startPage // 书籍详情页
                    else -> chapterRange.startPage + currentPageIndex
                }
                val newProgress = globalPage.toFloat() / pageCountCache.totalPages.toFloat()
                return state.copy(readingProgress = newProgress.coerceIn(0f, 1f))
            }
        }
        
        return state
    }

    /**
     * 重置翻页状态 - 用于翻页模式切换时清理状态
     */
    fun resetFlipState() {
        val state = _uiState.value
        if (state.currentChapter != null && state.containerSize != IntSize.Zero && state.density != null) {
            // 重新分页并重建虚拟页面
            splitContent(preserveVirtualIndex = false)
        }
    }

    /**
     * 强制更新虚拟页面索引 - 用于某些翻页模式需要强制同步状态的情况
     */
    fun forceUpdateVirtualPageIndex(newIndex: Int) {
        val state = _uiState.value
        if (newIndex in state.virtualPages.indices && newIndex != state.virtualPageIndex) {
            updateFlipState(
                newVirtualPageIndex = newIndex,
                triggerPreload = false // 强制更新时不触发预加载，避免性能问题
            )
        }
    }

    /**
     * 平移模式专用：直接更新虚拟页面索引和相关状态
     * 用于平移容器中的用户手势完成后同步状态，避免循环更新
     */
    fun updateSlideFlipIndex(newIndex: Int) {
        val state = _uiState.value
        val virtualPages = state.virtualPages
        
        if (newIndex !in virtualPages.indices || newIndex == state.virtualPageIndex) {
            return // 索引无效或没有变化
        }

        val newVirtualPage = virtualPages[newIndex]
        
        // 只更新必要的状态，不触发重新构建虚拟页面
        when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    // 切换到不同章节
                    val newChapterCache = chapterCache[newVirtualPage.chapterId]
                    if (newChapterCache != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        _uiState.value = state.copy(
                            virtualPageIndex = newIndex,
                            currentChapter = newChapterCache.chapter,
                            currentChapterIndex = newChapterIndex,
                            currentPageIndex = newVirtualPage.pageIndex,
                            currentPageData = newChapterCache.pageData
                        )
                        
                        // 章节切换时触发预加载
                        performDynamicPreload(newVirtualPage.chapterId, triggerExpansion = false)
                    } else {
                        // 章节数据未加载，只更新索引
                        _uiState.value = state.copy(virtualPageIndex = newIndex)
                    }
                } else {
                    // 同章节内翻页
                    _uiState.value = state.copy(
                        virtualPageIndex = newIndex,
                        currentPageIndex = newVirtualPage.pageIndex
                    )
                }
            }
            is VirtualPage.BookDetailPage -> {
                _uiState.value = state.copy(
                    virtualPageIndex = newIndex,
                    currentPageIndex = -1
                )
            }
            is VirtualPage.ChapterSection -> {
                _uiState.value = state.copy(virtualPageIndex = newIndex)
            }
        }
        
        // 检查预加载（轻量级检查）
        if (newVirtualPage is VirtualPage.ContentPage) {
            val pageData = _uiState.value.loadedChapterData[newVirtualPage.chapterId]
            if (pageData != null) {
                val isAtBoundary = newVirtualPage.pageIndex == 0 || 
                                  newVirtualPage.pageIndex == pageData.pages.size - 1
                if (isAtBoundary) {
                    viewModelScope.launch {
                        checkRegularPreload(
                            _uiState.value.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId },
                            newVirtualPage
                        )
                    }
                }
            }
        }
    }
} 