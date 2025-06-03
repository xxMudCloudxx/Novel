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
    val previousChapterData: PageData? = null // 上一章数据
)

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
    val readingProgress: Float = 0f,
    val readerSettings: ReaderSettings = ReaderSettings(),
    // 新增分页相关状态
    val currentPageData: PageData? = null,
    val currentPageIndex: Int = 0,
    val containerSize: IntSize = IntSize.Zero,
    val density: Density? = null
) {
    val isSuccess: Boolean get() = !isLoading && !hasError && currentChapter != null
    val isEmpty: Boolean get() = !isLoading && !hasError && chapterList.isEmpty()
}

/**
 * 阅读器Intent(用户操作意图)
 */
sealed class ReaderIntent {
    data class InitReader(val bookId: String, val chapterId: String?) : ReaderIntent()
    object LoadChapterList : ReaderIntent()
    data class LoadChapterContent(val chapterId: String) : ReaderIntent()
    object PreviousChapter : ReaderIntent()
    object NextChapter : ReaderIntent()
    data class SwitchToChapter(val chapterId: String) : ReaderIntent()
    data class SeekToProgress(val progress: Float) : ReaderIntent()
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent()
    data class PageFlip(val direction: FlipDirection) : ReaderIntent()
    data class ChapterFlip(val direction: FlipDirection) : ReaderIntent()
    data class UpdateContainerSize(val size: IntSize, val density: Density) : ReaderIntent()
    object Retry : ReaderIntent()
}

/**
 * 阅读器ViewModel - MVI架构
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookService: BookService,
    private val userDefaults: com.novel.utils.Store.UserDefaults.NovelUserDefaults
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // 章节缓存，最多缓存8个章节（当前+前后各3章+缓冲）
    private val chapterCache = mutableMapOf<String, ChapterCache>()
    private val maxCacheSize = 8

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
                val pageFlipEffect = com.novel.page.read.components.PageFlipEffect.values()
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
    fun handleIntent(intent: ReaderIntent) {
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

    /**
     * 初始化阅读器
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
                    loadChapterContentInternal(targetChapterId)
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
     * 加载章节内容内部方法 - 修复页面索引设置
     */
    private suspend fun loadChapterContentInternal(chapterId: String, flipDirection: FlipDirection? = null) {
        // 先检查缓存
        val cachedChapter = chapterCache[chapterId]
        if (cachedChapter != null) {
            updateCurrentChapter(cachedChapter, flipDirection)
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
                updateCurrentChapter(chapterCache, flipDirection)

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
     * 更新当前章节信息 - 修复页面索引设置逻辑
     */
    private fun updateCurrentChapter(chapterCache: ChapterCache, flipDirection: FlipDirection? = null) {
        val currentIndex =
            _uiState.value.chapterList.indexOfFirst { it.id == chapterCache.chapter.id }

        // 根据翻页方向正确设置初始页面索引
        val initialPageIndex = when (flipDirection) {
            FlipDirection.PREVIOUS -> -1 // 标记为需要设置到最后一页，在分页完成后设置
            FlipDirection.NEXT -> 0     // 下一章从第一页开始
            else -> _uiState.value.currentPageIndex.coerceAtLeast(0) // 保持当前页面或默认第一页
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasError = false,
            currentChapter = chapterCache.chapter,
            currentChapterIndex = if (currentIndex >= 0) currentIndex else 0,
            bookContent = chapterCache.content,
            readingProgress = 0f,
            currentPageIndex = initialPageIndex
        )

        // 如果有容器尺寸信息，进行分页
        val state = _uiState.value
        if (state.containerSize != IntSize.Zero && state.density != null) {
            splitContent()
        }
    }

    /**
     * 分页处理 - 优化章节衔接和相邻章节数据获取
     */
    private fun splitContent() {
        val state = _uiState.value
        val chapter = state.currentChapter
        val content = state.bookContent
        val chapterList = state.chapterList
        val currentIndex = state.currentChapterIndex

        if (chapter != null && content.isNotEmpty() && state.containerSize != IntSize.Zero && state.density != null) {
            // 分页
            val finalPages = PageSplitter.splitContent(
                content = content,
                chapterTitle = chapter.chapterName,
                containerSize = state.containerSize,
                readerSettings = state.readerSettings,
                density = state.density
            )

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

            // 确保当前页面索引在有效范围内
            val safeCurrentPageIndex = when {
                state.currentPageIndex == -1 -> finalPages.size - 1 // 切换到上一章时跳转到最后一页
                else -> state.currentPageIndex.coerceIn(0, finalPages.size - 1)
            }

            val pageData = PageData(
                chapterId = chapter.id,
                chapterName = chapter.chapterName,
                content = content,
                pages = finalPages,
                isFirstPage = safeCurrentPageIndex == 0,
                isLastPage = safeCurrentPageIndex == finalPages.size - 1,
                nextChapterData = nextChapterData,
                previousChapterData = previousChapterData
            )

            _uiState.value = _uiState.value.copy(
                currentPageData = pageData,
                currentPageIndex = safeCurrentPageIndex, // 确保页面索引有效
                readingProgress = if (finalPages.isNotEmpty()) safeCurrentPageIndex.toFloat() / finalPages.size.toFloat() else 0f
            )

            // 更新缓存中的分页数据
            val cachedChapter = chapterCache[chapter.id]
            if (cachedChapter != null) {
                chapterCache[chapter.id] = cachedChapter.copy(pageData = pageData)
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
     * 处理页面翻页 - 修复翻页逻辑
     */
    private fun handlePageFlip(direction: FlipDirection) {
        val state = _uiState.value
        val pageData = state.currentPageData ?: return
        
        // 确保页面数据有效
        if (pageData.pages.isEmpty()) {
            // 如果没有分页数据，直接切换章节
            handleChapterFlip(direction)
            return
        }

        when (direction) {
            FlipDirection.NEXT -> {
                val currentIndex = state.currentPageIndex
                if (currentIndex < pageData.pages.size - 1) {
                    // 还有下一页，执行页面翻页
                    val newIndex = currentIndex + 1
                    val newPageData = pageData.copy(
                        isFirstPage = newIndex == 0,
                        isLastPage = newIndex == pageData.pages.size - 1
                    )
                    
                    _uiState.value = state.copy(
                        currentPageIndex = newIndex,
                        currentPageData = newPageData,
                        readingProgress = newIndex.toFloat() / pageData.pages.size.toFloat()
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
                        readingProgress = newIndex.toFloat() / pageData.pages.size.toFloat()
                    )
                } else {
                    // 已经是章节第一页，切换到上一章
                    handleChapterFlip(FlipDirection.PREVIOUS)
                }
            }
        }
    }

    /**
     * 处理章节切换 - 优化版本，修复页面索引设置问题
     */
    private fun handleChapterFlip(direction: FlipDirection) {
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
        _uiState.value = _uiState.value.copy(
            containerSize = size,
            density = density
        )

        // 重新分页
        if (_uiState.value.currentChapter != null) {
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
        val pageData = _uiState.value.currentPageData
        if (pageData != null && pageData.pages.isNotEmpty()) {
            val targetPageIndex = (progress * pageData.pages.size).toInt()
                .coerceIn(0, pageData.pages.size - 1)

            _uiState.value = _uiState.value.copy(
                readingProgress = progress,
                currentPageIndex = targetPageIndex
            )
        } else {
            _uiState.value = _uiState.value.copy(readingProgress = progress)
        }
    }

    /**
     * 更新阅读器设置
     */
    private fun updateSettingsInternal(settings: ReaderSettings) {
        val oldSettings = _uiState.value.readerSettings
        _uiState.value = _uiState.value.copy(readerSettings = settings)

        // 如果翻页方式发生改变，保存到本地存储
        if (oldSettings.pageFlipEffect != settings.pageFlipEffect) {
            savePageFlipEffect(settings.pageFlipEffect)
        }

        // 设置变更后重新分页
        if (_uiState.value.currentChapter != null) {
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
} 