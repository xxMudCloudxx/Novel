package com.novel.page.read.viewmodel

import com.novel.utils.TimberLogger
import androidx.lifecycle.viewModelScope
import com.novel.page.component.BaseViewModel
import com.novel.page.read.usecase.*
import com.novel.page.read.utils.ReaderLogTags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.toArgb
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.components.ReaderSettings
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

/**
 * 阅读器ViewModel
 *
 * 负责管理阅读器的UI状态和业务逻辑：
 * 1. 阅读器初始化和章节加载
 * 2. 翻页逻辑和虚拟页面管理
 * 3. 设置更新和进度保存
 * 4. 预加载和缓存管理
 * 5. 响应式状态更新
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val initReaderUseCase: InitReaderUseCase,
    private val flipPageUseCase: FlipPageUseCase,
    private val switchChapterUseCase: SwitchChapterUseCase,
    private val seekProgressUseCase: SeekProgressUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val preloadChaptersUseCase: PreloadChaptersUseCase,
    private val buildVirtualPagesUseCase: BuildVirtualPagesUseCase,
    private val splitContentUseCase: SplitContentUseCase,
    private val paginationService: com.novel.page.read.service.PaginationService,
    private val settingsService: com.novel.page.read.service.SettingsService,
    observePaginationProgressUseCase: ObservePaginationProgressUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = ReaderLogTags.READER_VIEW_MODEL
        private const val FLIP_COOLDOWN_MS = 300L // 翻页防抖时间
    }

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // 翻页防抖控制
    private var lastFlipTime = 0L

    init {
        TimberLogger.d(TAG, "ReaderViewModel初始化")
        observePaginationProgressUseCase.execute()
            .onEach { state ->
                TimberLogger.d(TAG, "分页进度更新: 章节 ${state.calculatedChapters}/${state.totalChapters}, 估算总页=${state.estimatedTotalPages}")
                _uiState.value = _uiState.value.copy(paginationState = state)

                // 当渐进计算完成且尚未写入页数缓存时，主动查询并更新
                if (!state.isCalculating && state.estimatedTotalPages > 0) {
                    val current = _uiState.value
                    if (current.pageCountCache == null &&
                        current.containerSize != IntSize.Zero) {
                        viewModelScope.launch {
                            var cacheFound = false
                            for (attempt in 0 until 3) {
                                val cache = paginationService.getPageCountCache(
                                    current.bookId,
                                    current.readerSettings.fontSize,
                                    current.containerSize
                                )
                                if (cache != null) {
                                    _uiState.value = _uiState.value.copy(pageCountCache = cache)
                                    TimberLogger.d(TAG, "分页完成后写入页数缓存: 总页数=${cache.totalPages}")
                                    cacheFound = true
                                    break
                                } else {
                                    TimberLogger.d(TAG, "页数缓存暂未生成，等待重试(${attempt + 1}/3)")
                                    delay(500)
                                }
                            }
                            if (!cacheFound) {
                                TimberLogger.w(TAG, "多次尝试仍未获取到页数缓存，放弃更新")
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 加载初始设置
     * 在页面初始化时调用，确保背景色等设置能立即应用
     */
    fun loadInitialSettings() {
        TimberLogger.d(TAG, "开始加载初始设置")
        viewModelScope.launch {
            try {
                TimberLogger.d(TAG, "调用SettingsService加载设置")
                val loadedSettings = settingsService.loadSettings()

                TimberLogger.d(TAG, "设置加载成功，更新UI状态")
                TimberLogger.d(TAG, "加载的设置详情:")
                TimberLogger.d(TAG, "  - 字体大小: ${loadedSettings.fontSize}sp")
                TimberLogger.d(TAG, "  - 亮度: ${(loadedSettings.brightness * 100).toInt()}%")
                TimberLogger.d(TAG, "  - 背景颜色: ${colorToHex(loadedSettings.backgroundColor)}")
                TimberLogger.d(TAG, "  - 文字颜色: ${colorToHex(loadedSettings.textColor)}")
                TimberLogger.d(TAG, "  - 翻页效果: ${loadedSettings.pageFlipEffect}")

                val oldSettings = _uiState.value.readerSettings
                TimberLogger.d(TAG, "更新前的设置:")
                TimberLogger.d(TAG, "  - 字体大小: ${oldSettings.fontSize}sp")
                TimberLogger.d(TAG, "  - 背景颜色: ${colorToHex(oldSettings.backgroundColor)}")
                TimberLogger.d(TAG, "  - 文字颜色: ${colorToHex(oldSettings.textColor)}")
                TimberLogger.d(TAG, "  - 翻页效果: ${oldSettings.pageFlipEffect}")

                _uiState.value = _uiState.value.copy(readerSettings = loadedSettings)

                TimberLogger.d(TAG, "UI状态更新完成，当前背景颜色: ${colorToHex(_uiState.value.readerSettings.backgroundColor)}")

            } catch (e: Exception) {
                TimberLogger.e(TAG, "初始设置加载失败", e)
                val defaultSettings = com.novel.page.read.components.ReaderSettings.getDefault()
                TimberLogger.d(TAG, "使用默认设置: 背景颜色=${colorToHex(defaultSettings.backgroundColor)}")
                _uiState.value = _uiState.value.copy(readerSettings = defaultSettings)
            }
        }
    }

    /**
     * 将Color对象转换为十六进制字符串，用于日志显示
     */
    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        return try {
            String.format("#%08X", color.toArgb())
        } catch (e: Exception) {
            "INVALID_COLOR"
        }
    }

    /**
     * 处理用户意图
     * 统一的用户操作入口点
     */
    fun onIntent(intent: ReaderIntent) {
        TimberLogger.d(TAG, "处理用户意图: ${intent::class.simpleName}")

        viewModelScope.launch {
            // 除了初始化外，其他操作都先保存进度
            if (intent !is ReaderIntent.InitReader) {
                saveProgressUseCase.execute(uiState.value)
            }

            when (intent) {
                is ReaderIntent.InitReader -> handleInitReader(intent)
                is ReaderIntent.PageFlip -> handleFlipPage(intent)
                is ReaderIntent.SwitchToChapter -> handleSwitchToChapter(intent)
                is ReaderIntent.UpdateSettings -> handleUpdateSettings(intent)
                is ReaderIntent.SeekToProgress -> handleSeekToProgress(intent)
                is ReaderIntent.UpdateContainerSize -> handleUpdateContainerSize(intent)
                is ReaderIntent.Retry -> handleRetry()
                is ReaderIntent.PreviousChapter, is ReaderIntent.NextChapter -> {
                    val direction = if (intent is ReaderIntent.NextChapter) FlipDirection.NEXT else FlipDirection.PREVIOUS
                    onIntent(ReaderIntent.PageFlip(direction))
                }
            }
        }
    }

    // ======================= 兼容性方法 =======================

    /**
     * 章节切换 - 兼容UI调用
     */
    fun onChapterChange(direction: FlipDirection) {
        TimberLogger.d(TAG, "章节切换: $direction")
        onIntent(ReaderIntent.PageFlip(direction))
    }

    /**
     * 导航到内容页 - 从书籍详情页跳转到第一页内容
     */
    fun navigateToContent() {
        if (_uiState.value.currentPageIndex == -1) {
            TimberLogger.d(TAG, "从书籍详情页导航到内容页")
            onIntent(ReaderIntent.PageFlip(FlipDirection.NEXT))
        }
    }

    /**
     * 从滚动更新当前页面 - 用于纵向滚动模式
     */
    fun updateCurrentPageFromScroll(page: Int) {
        if (page != _uiState.value.currentPageIndex) {
            TimberLogger.d(TAG, "滚动更新页面索引: ${_uiState.value.currentPageIndex} -> $page")
            _uiState.value = _uiState.value.copy(currentPageIndex = page)

            // 页面变化后更新进度并保存
            viewModelScope.launch {
            updateReadingProgressFromPageIndex(page)
                saveProgressUseCase.execute(_uiState.value)
            }
        }
    }

    /**
     * 保存当前阅读进度 - 兼容UI调用
     */
    fun saveCurrentReadingProgress() {
        TimberLogger.d(TAG, "手动保存阅读进度")
        viewModelScope.launch {
            saveProgressUseCase.execute(_uiState.value)
        }
    }

    /**
     * 更新滑动翻页索引 - 平移模式专用
     * 用于平移容器中的用户手势完成后同步状态
     */
    fun updateSlideFlipIndex(newIndex: Int) {
        val state = _uiState.value
        val virtualPages = state.virtualPages

        if (newIndex !in virtualPages.indices || newIndex == state.virtualPageIndex) {
            TimberLogger.d(TAG, "滑动翻页索引无效或未变化: $newIndex")
            return
        }

        TimberLogger.d(TAG, "更新滑动翻页索引: ${state.virtualPageIndex} -> $newIndex")

        val newVirtualPage = virtualPages[newIndex]
        var updatedState = state.copy(virtualPageIndex = newIndex)

        when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                updatedState = updatedState.copy(currentPageIndex = newVirtualPage.pageIndex)

                // 如果切换到不同章节
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                    if (newChapterIndex != -1) {
                        TimberLogger.d(TAG, "滑动翻页切换章节: ${state.currentChapter?.chapterName} -> ${state.chapterList[newChapterIndex].chapterName}")
                        updatedState = updatedState.copy(
                            currentChapterIndex = newChapterIndex,
                            currentChapter = state.chapterList[newChapterIndex]
                        )
                    }
                }
            }
            is VirtualPage.BookDetailPage -> {
                TimberLogger.d(TAG, "滑动翻页到书籍详情页")
                updatedState = updatedState.copy(currentPageIndex = -1)
            }
            is VirtualPage.ChapterSection -> {
                TimberLogger.d(TAG, "滑动翻页到章节区域")
                // 处理章节区域逻辑
            }
        }

        _uiState.value = updatedState

        // 页面切换后保存进度
        viewModelScope.launch {
            saveProgressUseCase.execute(_uiState.value)

            // 检查预加载
            if (newVirtualPage is VirtualPage.ContentPage) {
                checkAndPreload(newIndex)
            }
        }
    }

    // ======================= 私有方法 =======================

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
                    TimberLogger.d(TAG, "更新阅读进度: 全局页面=$globalPage/${pageCountCache.totalPages}, 进度=${(newProgress * 100).toInt()}%")
                    _uiState.value = _uiState.value.copy(readingProgress = newProgress.coerceIn(0f, 1f))
                }
            }
        }
    }

    /**
     * 分割内容并构建虚拟页面
     */
    private suspend fun splitContentAndBuildVirtualPages(
        restoreProgress: Float? = null,
        preserveVirtualIndex: Boolean = false
    ) {
        TimberLogger.d(TAG, "开始分割内容并构建虚拟页面: restoreProgress=$restoreProgress, preserve=$preserveVirtualIndex")

        val state = _uiState.value

        // 首先分割内容
        val splitResult = splitContentUseCase.execute(
            state = state,
            restoreProgress = restoreProgress,
            includeAdjacentChapters = true
        )

        when (splitResult) {
            is SplitContentUseCase.SplitResult.Success -> {
                TimberLogger.d(TAG, "内容分割成功: 页数=${splitResult.pageData.pages.size}, 安全页面索引=${splitResult.safePageIndex}")
                _uiState.value = _uiState.value.copy(
                    currentPageData = splitResult.pageData,
                    currentPageIndex = splitResult.safePageIndex,
                    isSwitchingChapter = false
                )

                // 构建虚拟页面
                buildVirtualPages(preserveVirtualIndex)

                // 若仍未开始全书分页，启动后台任务
                val st = _uiState.value
                if (st.pageCountCache == null && st.containerSize != IntSize.Zero && st.density != null) {
                    TimberLogger.d(TAG, "未检测到页数缓存，启动后台全书分页计算")
                    paginationService.fetchAllBookContentAndPaginateInBackground(
                        bookId = st.bookId,
                        chapterList = st.chapterList,
                        readerSettings = st.readerSettings,
                        containerSize = st.containerSize,
                        density = st.density!!
                    )

                    // 轮询获取页数缓存，避免第一次读取为空
                    var cacheFound = false
                    for (attempt in 0 until 6) {
                        val cache = paginationService.getPageCountCache(
                            st.bookId,
                            st.readerSettings.fontSize,
                            st.containerSize
                        )
                        if (cache != null) {
                            _uiState.value = _uiState.value.copy(pageCountCache = cache)
                            TimberLogger.d(TAG, "后台分页完成写入页数缓存: 总页数=${cache.totalPages}")
                            cacheFound = true
                            break
                        }
                        delay(500)
                    }
                    if (!cacheFound) {
                        TimberLogger.w(TAG, "后台分页完成后仍未获取到页数缓存，放弃轮询")
                    }
                }
            }
            is SplitContentUseCase.SplitResult.Failure -> {
                TimberLogger.e(TAG, "内容分割失败", splitResult.error)
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = splitResult.error.message ?: "内容分割失败",
                    isSwitchingChapter = false
                )
            }
        }
    }

    /**
     * 使用BuildVirtualPagesUseCase构建虚拟页面
     */
    private suspend fun buildVirtualPages(preserveCurrentIndex: Boolean = true) {
        TimberLogger.d(TAG, "构建虚拟页面: preserve=$preserveCurrentIndex")

        val buildResult = buildVirtualPagesUseCase.execute(
            state = _uiState.value,
            preserveCurrentIndex = preserveCurrentIndex
        )

        when (buildResult) {
            is BuildVirtualPagesUseCase.BuildResult.Success -> {
                TimberLogger.d(TAG, "虚拟页面构建成功: 总页数=${buildResult.virtualPages.size}, 当前索引=${buildResult.newVirtualPageIndex}")
                    _uiState.value = _uiState.value.copy(
                    virtualPages = buildResult.virtualPages,
                    virtualPageIndex = buildResult.newVirtualPageIndex,
                    loadedChapterData = buildResult.loadedChapterData
                )
            }
            is BuildVirtualPagesUseCase.BuildResult.Failure -> {
                TimberLogger.e(TAG, "虚拟页面构建失败", buildResult.error)
                // 不中断整个操作，只记录错误
            }
        }
    }

    /**
     * 检查并执行预加载
     * 根据当前阅读位置智能预加载相邻章节
     */
    private suspend fun checkAndPreload(currentVirtualIndex: Int) {
                val state = _uiState.value
        val currentContentPage = state.virtualPages.getOrNull(currentVirtualIndex) as? VirtualPage.ContentPage

        if (currentContentPage == null) {
            TimberLogger.d(TAG, "预加载检查: 非内容页面，跳过")
            return
        }

        val chapterList = state.chapterList
        val currentChapterIndex = chapterList.indexOfFirst { it.id == currentContentPage.chapterId }

        if (currentChapterIndex < 0) {
            TimberLogger.w(TAG, "预加载检查: 章节索引未找到")
            return
        }

        TimberLogger.d(TAG, "预加载检查: 章节=${currentContentPage.chapterId}, 页面=${currentContentPage.pageIndex}")

        // 执行预加载检查
        TimberLogger.d(TAG, "触发预加载检查")
        preloadChaptersUseCase.execute(
            viewModelScope,
            chapterList,
            currentContentPage.chapterId
        )

        // 预加载完成后，检查是否有新的相邻章节需要重建虚拟页面
        // 延迟执行，给预加载一些时间
        kotlinx.coroutines.delay(500)
        checkAndRebuildVirtualPagesIfNeeded(currentChapterIndex)
    }

    /**
     * 检查并重建虚拟页面（如果需要）
     * 当有新的章节被预加载后，主动检查是否需要重建虚拟页面
     */
    private suspend fun checkAndRebuildVirtualPagesIfNeeded(currentChapterIndex: Int) {
        // 简化版本：直接重建虚拟页面
        TimberLogger.d(TAG, "检查虚拟页面重建需求")
        buildVirtualPages(preserveCurrentIndex = true)
    }

    /**
     * 处理章节翻页
     * 优化版本，更好的状态管理和预加载
     */
    private suspend fun handleChapterFlip(direction: FlipDirection) {
        // 防重复触发检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < FLIP_COOLDOWN_MS) {
            TimberLogger.d(TAG, "章节翻页防抖: 忽略重复操作")
            return
        }
        lastFlipTime = currentTime

        TimberLogger.d(TAG, "处理章节翻页: $direction")

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
            TimberLogger.d(TAG, "切换到章节: ${targetChapter.chapterName} (索引=$targetIndex)")

            // 使用SwitchChapterUseCase进行章节切换
            val result = switchChapterUseCase.execute(state, targetChapter.id, viewModelScope, direction)
            when (result) {
                is SwitchChapterUseCase.SwitchResult.Success -> {
                    TimberLogger.d(TAG, "章节切换成功")
                    _uiState.value = _uiState.value.copy(
                        currentChapter = chapterList[result.newChapterIndex],
                        currentChapterIndex = result.newChapterIndex,
                        currentPageData = result.pageData,
                        currentPageIndex = result.initialPageIndex,
                        bookContent = result.pageData.content,
                        isSwitchingChapter = false
                    ).let { updated ->
                        updateReadingProgressForState(updated)
                    }
                    // 重建虚拟页面
                    buildVirtualPages(preserveCurrentIndex = false)
                }
                is SwitchChapterUseCase.SwitchResult.Failure -> {
                    TimberLogger.e(TAG, "章节切换失败", result.error)
                    _uiState.value = _uiState.value.copy(
                        hasError = true,
                        error = result.error.message ?: "章节切换失败",
                        isSwitchingChapter = false
                    )
                }
                is SwitchChapterUseCase.SwitchResult.NoOp -> {
                    TimberLogger.d(TAG, "章节切换无操作")
                    _uiState.value = _uiState.value.copy(isSwitchingChapter = false)
                }
            }
        } else {
            TimberLogger.d(TAG, "到达书籍边界: 无法继续翻页")
            _uiState.value = _uiState.value.copy(isSwitchingChapter = false)
        }
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
                TimberLogger.d(TAG, "更新状态阅读进度: 全局页面=$globalPage/${pageCountCache.totalPages}, 进度=${(newProgress * 100).toInt()}%")
                return state.copy(readingProgress = newProgress.coerceIn(0f, 1f))
            }
        }

        return state
    }

    // ======================= Intent处理方法 =======================

    private suspend fun handleInitReader(intent: ReaderIntent.InitReader) {
        TimberLogger.d(TAG, "初始化阅读器: bookId=${intent.bookId}, chapterId=${intent.chapterId}")

        _uiState.value = _uiState.value.copy(isLoading = true, bookId = intent.bookId)

        initReaderUseCase.execute(
            bookId = intent.bookId,
            chapterId = intent.chapterId,
            initialState = _uiState.value,
            scope = viewModelScope
        ).onSuccess { result ->
            TimberLogger.d(TAG, "阅读器初始化成功: 章节=${result.initialChapter.chapterName}, 页面=${result.initialPageIndex}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasError = false,
                readerSettings = result.settings,
                chapterList = result.chapterList,
                currentChapter = result.initialChapter,
                currentChapterIndex = result.initialChapterIndex,
                currentPageData = result.initialPageData,
                currentPageIndex = result.initialPageIndex,
                bookContent = result.initialPageData.content,
                pageCountCache = result.pageCountCache
            )

            // 初始化后构建虚拟页面
            buildVirtualPages(preserveCurrentIndex = false)

            // 如果由于容器信息缺失导致初始分页退化为单页，且此时容器尺寸 & density 已就绪，则立即重新分页
            val currentData = _uiState.value.currentPageData
            if (currentData != null && currentData.pages.size == 1 &&
                _uiState.value.containerSize != androidx.compose.ui.unit.IntSize.Zero &&
                _uiState.value.density != null) {
                TimberLogger.d(TAG, "检测到初始分页为单页，容器信息已就绪，触发重新分页")
                // 使用完整流程重新分割内容并重建虚拟页面
                splitContentAndBuildVirtualPages(preserveVirtualIndex = false)
            }
        }.onFailure { error ->
            TimberLogger.e(TAG, "阅读器初始化失败", error)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasError = true,
                error = error.message ?: "初始化失败"
            )
        }
    }

    /**
     * 处理翻页操作
     * 根据old.txt中的handlePageFlip逻辑增强，支持：
     * 1. 防抖处理（在FlipPageUseCase中实现）
     * 2. 虚拟页面索引管理
     * 3. 平移模式特殊处理
     * 4. 章节边界预加载检查
     * 5. BookDetailPage和ContentPage切换
     * 6. 虚拟页面重建触发
     */
    private suspend fun handleFlipPage(intent: ReaderIntent.PageFlip) {
        TimberLogger.d(TAG, "处理翻页操作: ${intent.direction}")

        val result = flipPageUseCase.execute(_uiState.value, intent.direction, viewModelScope)
        when (result) {
            is FlipPageUseCase.FlipResult.VirtualPageChanged -> {
                TimberLogger.d(TAG, "虚拟页面翻页: 索引=${result.newVirtualPageIndex}, 页面类型=${result.newVirtualPage::class.simpleName}")

                // 检查是否为平移模式，如果是则需要特殊处理以避免循环更新
                val isSlideMode = _uiState.value.readerSettings.pageFlipEffect == com.novel.page.read.components.PageFlipEffect.SLIDE

                if (isSlideMode) {
                    // 平移模式：只更新相关状态，不更新 virtualPageIndex
                    // virtualPageIndex 将由 SlideFlipContainer 的用户手势直接管理
                    updateSlideFlipState(result.newVirtualPage, result.newVirtualPageIndex)
                } else {
                    // 其他翻页模式：立即更新virtualPageIndex
                    _uiState.value = _uiState.value.copy(virtualPageIndex = result.newVirtualPageIndex)
                    updatePageFlipState(result.newVirtualPage)
                }

                // 如果需要预加载检查，执行预加载检查
                if (result.needsPreloadCheck && result.newVirtualPage is com.novel.page.read.viewmodel.VirtualPage.ContentPage) {
                    flipPageUseCase.executePreloadCheck(_uiState.value, result.newVirtualPage, viewModelScope)
                }
            }

            is FlipPageUseCase.FlipResult.ChapterChanged -> {
                TimberLogger.d(TAG, "翻页触发章节切换")
                when (val switchResult = result.switchResult) {
                    is SwitchChapterUseCase.SwitchResult.Success -> {
                        TimberLogger.d(TAG, "翻页触发章节切换成功")
                        _uiState.value = _uiState.value.copy(
                            currentChapter = _uiState.value.chapterList[switchResult.newChapterIndex],
                            currentChapterIndex = switchResult.newChapterIndex,
                            currentPageData = switchResult.pageData,
                            currentPageIndex = switchResult.initialPageIndex,
                            bookContent = switchResult.pageData.content
                        ).let { updated ->
                            updateReadingProgressForState(updated)
                        }
                        // 重建虚拟页面
                        buildVirtualPages(preserveCurrentIndex = false)
                    }
                    is SwitchChapterUseCase.SwitchResult.Failure -> {
                        TimberLogger.e(TAG, "翻页触发章节切换失败", switchResult.error)
                        _uiState.value = _uiState.value.copy(
                            hasError = true,
                            error = switchResult.error.message ?: "翻页失败"
                        )
                    }
                    is SwitchChapterUseCase.SwitchResult.NoOp -> {
                        TimberLogger.d(TAG, "翻页章节切换无操作")
                    }
                }
            }

            is FlipPageUseCase.FlipResult.NeedsVirtualPageRebuild -> {
                TimberLogger.d(TAG, "检测到新的相邻章节已加载，处理翻页并重建虚拟页面")

                // 首先，像处理普通翻页一样更新状态
                val isSlideMode = _uiState.value.readerSettings.pageFlipEffect == com.novel.page.read.components.PageFlipEffect.SLIDE
                if (isSlideMode) {
                    updateSlideFlipState(result.newVirtualPage, result.newVirtualPageIndex)
                } else {
                    _uiState.value = _uiState.value.copy(virtualPageIndex = result.newVirtualPageIndex)
                    updatePageFlipState(result.newVirtualPage)
                }

                // 其次，在状态更新后重建虚拟页面
                buildVirtualPages(preserveCurrentIndex = true)

                // 最后，如果需要，执行预加载检查
                if (result.needsPreloadCheck && result.newVirtualPage is com.novel.page.read.viewmodel.VirtualPage.ContentPage) {
                    flipPageUseCase.executePreloadCheck(_uiState.value, result.newVirtualPage, viewModelScope)
                }
            }

            is FlipPageUseCase.FlipResult.Failure -> {
                TimberLogger.e(TAG, "翻页失败", result.error)
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = result.error.message ?: "翻页失败"
                )
            }

            is FlipPageUseCase.FlipResult.NoOp -> {
                TimberLogger.d(TAG, "翻页无操作")
            }
        }
    }

    /**
     * 处理平移模式的状态更新（避免循环更新）
     * 根据old.txt中的updateSlideFlipState逻辑实现
     */
    private fun updateSlideFlipState(newVirtualPage: com.novel.page.read.viewmodel.VirtualPage, newVirtualIndex: Int) {
        TimberLogger.d(TAG, "更新平移模式状态: 索引=$newVirtualIndex, 页面=${newVirtualPage::class.simpleName}")

        when (newVirtualPage) {
            is com.novel.page.read.viewmodel.VirtualPage.ContentPage -> {
                val state = _uiState.value
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    // 切换章节
                    val newChapterData = state.loadedChapterData[newVirtualPage.chapterId]
                    if (newChapterData != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        val newChapter = state.chapterList.getOrNull(newChapterIndex)

                        if (newChapter != null) {
                            TimberLogger.d(TAG, "平移模式章节切换: ${state.currentChapter?.chapterName} -> ${newChapter.chapterName}")
                            var updatedState = _uiState.value.copy(
                                currentChapter = newChapter,
                                currentChapterIndex = newChapterIndex,
                                currentPageIndex = newVirtualPage.pageIndex,
                                currentPageData = newChapterData,
                                virtualPageIndex = newVirtualIndex, // 只在这里更新 virtualPageIndex
                                bookContent = newChapterData.content
                            )

                            // 更新阅读进度
                            updatedState = updateReadingProgressForState(updatedState)
                            _uiState.value = updatedState

                            // 触发动态预加载
                            viewModelScope.launch {
                                preloadChaptersUseCase.performDynamicPreload(viewModelScope, _uiState.value, newVirtualPage.chapterId, triggerExpansion = false)
                            }
                        }
                    } else {
                        // 未预加载到章节数据，主动触发章节加载
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        val newChapter = state.chapterList.getOrNull(newChapterIndex)
                        if (newChapter == null) {
                            TimberLogger.e(TAG, "目标章节未在章节列表中: ${newVirtualPage.chapterId}")
                        } else {
                            TimberLogger.d(TAG, "章节数据未预加载，主动加载章节: ${newChapter.chapterName}")
                            viewModelScope.launch {
                                val switchResult = switchChapterUseCase.execute(_uiState.value, newVirtualPage.chapterId, this, null)
                                when (switchResult) {
                                    is SwitchChapterUseCase.SwitchResult.Success -> {
                                        var loadedState = _uiState.value.copy(
                                            currentChapter = newChapter,
                                            currentChapterIndex = newChapterIndex,
                                            currentPageData = switchResult.pageData,
                                            currentPageIndex = switchResult.initialPageIndex,
                                            virtualPageIndex = newVirtualIndex,
                                            bookContent = switchResult.pageData.content
                                        )
                                        loadedState = updateReadingProgressForState(loadedState)
                                        _uiState.value = loadedState
                                        // 重新构建虚拟页面，确保后续滑动正确
                                        buildVirtualPages(preserveCurrentIndex = true)
                                    }
                                    is SwitchChapterUseCase.SwitchResult.Failure -> TimberLogger.e(TAG, "主动切换章节失败", switchResult.error)
                                    else -> {}
                                }
                            }
                        }
                    }
                } else {
                    // 同章节内翻页
                    TimberLogger.d(TAG, "平移模式同章节翻页: 页面${state.currentPageIndex} -> ${newVirtualPage.pageIndex}")
                    var updatedState = _uiState.value.copy(
                        currentPageIndex = newVirtualPage.pageIndex,
                        virtualPageIndex = newVirtualIndex
                    )

                    // 更新阅读进度
                    updatedState = updateReadingProgressForState(updatedState)
                    _uiState.value = updatedState
                }
            }
            is com.novel.page.read.viewmodel.VirtualPage.BookDetailPage -> {
                TimberLogger.d(TAG, "平移模式切换到书籍详情页")
                var updatedState = _uiState.value.copy(
                    currentPageIndex = -1,
                    virtualPageIndex = newVirtualIndex
                )

                // 更新阅读进度
                updatedState = updateReadingProgressForState(updatedState)
                _uiState.value = updatedState
            }
            is com.novel.page.read.viewmodel.VirtualPage.ChapterSection -> {
                TimberLogger.d(TAG, "平移模式切换到章节区域")
                _uiState.value = _uiState.value.copy(virtualPageIndex = newVirtualIndex)
            }
        }
    }

    /**
     * 处理其他翻页模式的状态更新
     * 根据old.txt中的updatePageFlipState逻辑实现
     */
    private fun updatePageFlipState(newVirtualPage: com.novel.page.read.viewmodel.VirtualPage) {
        TimberLogger.d(TAG, "更新翻页状态: 页面=${newVirtualPage::class.simpleName}")

        when (newVirtualPage) {
            is com.novel.page.read.viewmodel.VirtualPage.ContentPage -> {
                val state = _uiState.value
                if (newVirtualPage.chapterId != state.currentChapter?.id) {
                    // 切换章节
                    val newChapterData = state.loadedChapterData[newVirtualPage.chapterId]
                    if (newChapterData != null) {
                        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
                        val newChapter = state.chapterList.getOrNull(newChapterIndex)

                        if (newChapter != null) {
                            TimberLogger.d(TAG, "翻页模式章节切换: ${state.currentChapter?.chapterName} -> ${newChapter.chapterName}")
                            var updatedState = _uiState.value.copy(
                                currentChapter = newChapter,
                                currentChapterIndex = newChapterIndex,
                                currentPageIndex = newVirtualPage.pageIndex,
                                currentPageData = newChapterData,
                                bookContent = newChapterData.content
                            )

                            // 更新阅读进度
                            updatedState = updateReadingProgressForState(updatedState)
                            _uiState.value = updatedState

                            // 保存进度并触发动态预加载
                            viewModelScope.launch {
                                saveProgressUseCase.execute(_uiState.value)
                                preloadChaptersUseCase.performDynamicPreload(viewModelScope, _uiState.value, newVirtualPage.chapterId, triggerExpansion = false)
                            }
                        }
                    }
                } else {
                    // 同章节内翻页
                    TimberLogger.d(TAG, "翻页模式同章节翻页: 页面${state.currentPageIndex} -> ${newVirtualPage.pageIndex}")
                    var updatedState = _uiState.value.copy(currentPageIndex = newVirtualPage.pageIndex)

                    // 更新阅读进度
                    updatedState = updateReadingProgressForState(updatedState)
                    _uiState.value = updatedState

                    // 保存进度
                    viewModelScope.launch {
                        saveProgressUseCase.execute(_uiState.value)
                    }
                }

            }
            is com.novel.page.read.viewmodel.VirtualPage.BookDetailPage -> {
                TimberLogger.d(TAG, "翻页模式切换到书籍详情页")
                var updatedState = _uiState.value.copy(currentPageIndex = -1)

                // 更新阅读进度
                updatedState = updateReadingProgressForState(updatedState)
                _uiState.value = updatedState

                // 保存进度
                viewModelScope.launch {
                    saveProgressUseCase.execute(_uiState.value)
                }
            }
            is com.novel.page.read.viewmodel.VirtualPage.ChapterSection -> {
                TimberLogger.d(TAG, "翻页模式章节区域（暂不支持）")
                // 章节模式暂不支持
            }
        }
    }

    private suspend fun handleSwitchToChapter(intent: ReaderIntent.SwitchToChapter) {
        TimberLogger.d(TAG, "切换到指定章节: ${intent.chapterId}")

        _uiState.value = _uiState.value.copy(isSwitchingChapter = true)
        val result = switchChapterUseCase.execute(_uiState.value, intent.chapterId, viewModelScope)
        when (result) {
            is SwitchChapterUseCase.SwitchResult.Success -> {
                TimberLogger.d(TAG, "章节切换成功")
                _uiState.value = _uiState.value.copy(
                    isSwitchingChapter = false,
                    currentChapterIndex = result.newChapterIndex,
                    currentPageData = result.pageData,
                    currentPageIndex = result.initialPageIndex,
                    currentChapter = _uiState.value.chapterList[result.newChapterIndex],
                    bookContent = result.pageData.content
                ).let { updated ->
                    updateReadingProgressForState(updated)
                }
                // 重建虚拟页面
                buildVirtualPages(preserveCurrentIndex = false)
            }
            is SwitchChapterUseCase.SwitchResult.Failure -> {
                TimberLogger.e(TAG, "章节切换失败", result.error)
                _uiState.value = _uiState.value.copy(
                    isSwitchingChapter = false,
                    hasError = true,
                    error = result.error.message ?: "章节切换失败"
                )
            }
            is SwitchChapterUseCase.SwitchResult.NoOp -> {
                TimberLogger.d(TAG, "章节切换无操作")
                _uiState.value = _uiState.value.copy(isSwitchingChapter = false)
            }
        }
    }

    private suspend fun handleUpdateSettings(intent: ReaderIntent.UpdateSettings) {
        TimberLogger.d(TAG, "开始更新阅读器设置")
        TimberLogger.d(TAG, "新设置详情:")
        TimberLogger.d(TAG, "  - 字体大小: ${intent.settings.fontSize}sp")
        TimberLogger.d(TAG, "  - 亮度: ${(intent.settings.brightness * 100).toInt()}%")
        TimberLogger.d(TAG, "  - 背景颜色: ${colorToHex(intent.settings.backgroundColor)}")
        TimberLogger.d(TAG, "  - 文字颜色: ${colorToHex(intent.settings.textColor)}")
        TimberLogger.d(TAG, "  - 翻页效果: ${intent.settings.pageFlipEffect}")

        val oldSettings = _uiState.value.readerSettings
        TimberLogger.d(TAG, "当前设置:")
        TimberLogger.d(TAG, "  - 字体大小: ${oldSettings.fontSize}sp")
        TimberLogger.d(TAG, "  - 背景颜色: ${colorToHex(oldSettings.backgroundColor)}")
        TimberLogger.d(TAG, "  - 翻页效果: ${oldSettings.pageFlipEffect}")

        val result = updateSettingsUseCase.execute(intent.settings, _uiState.value)
        when (result) {
            is UpdateSettingsUseCase.UpdateResult.Success -> {
                TimberLogger.d(TAG, "设置更新成功")
                TimberLogger.d(TAG, "是否需要重新分页: ${result.newPageData != null}")

                val updatedState = result.newPageData?.let {
                    TimberLogger.d(TAG, "应用新的分页数据: 页数=${it.pages.size}, 新页面索引=${result.newPageIndex}")
                    _uiState.value.copy(
                        readerSettings = intent.settings,
                        currentPageData = it,
                        currentPageIndex = result.newPageIndex
                    )
                } ?: run {
                    TimberLogger.d(TAG, "只更新设置，无需重新分页")
                    _uiState.value.copy(readerSettings = intent.settings)
                }

                _uiState.value = updatedState
                TimberLogger.d(TAG, "UI状态更新完成，当前背景颜色: ${colorToHex(_uiState.value.readerSettings.backgroundColor)}")

                // 如果内容重新分页，重建虚拟页面
                if (result.newPageData != null) {
                    TimberLogger.d(TAG, "重新分页后重建虚拟页面")
                    buildVirtualPages(preserveCurrentIndex = true)
                } else {
                    TimberLogger.d(TAG, "设置更新完成，无需重建虚拟页面")
                }
            }
        }
    }

    private suspend fun handleSeekToProgress(intent: ReaderIntent.SeekToProgress) {
        TimberLogger.d(TAG, "跳转到进度: ${(intent.progress * 100).toInt()}%")

        val result = seekProgressUseCase.execute(intent.progress, _uiState.value)
        when (result) {
            is SeekProgressUseCase.SeekResult.Success -> {
                TimberLogger.d(TAG, "进度跳转成功")
                _uiState.value = _uiState.value.copy(
                    currentChapterIndex = result.newChapterIndex,
                    currentPageData = result.newPageData,
                    currentPageIndex = result.newPageIndex,
                    currentChapter = _uiState.value.chapterList[result.newChapterIndex],
                    bookContent = result.newPageData.content
                )
                // 重建虚拟页面
                buildVirtualPages(preserveCurrentIndex = false)
            }
            is SeekProgressUseCase.SeekResult.Failure -> {
                TimberLogger.e(TAG, "进度跳转失败", result.error)
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = result.error.message ?: "进度跳转失败"
                )
            }
            is SeekProgressUseCase.SeekResult.NoOp -> {
                TimberLogger.d(TAG, "进度跳转无操作")
            }
        }
    }

    private suspend fun handleUpdateContainerSize(intent: ReaderIntent.UpdateContainerSize) {
        val oldState = _uiState.value
        if (intent.size.width == 0 || intent.size.height == 0 ||
            (oldState.containerSize == intent.size && oldState.density == intent.density)) {
            TimberLogger.d(TAG, "容器尺寸无变化，跳过更新")
            return
        }

        TimberLogger.d(TAG, "更新容器尺寸: ${oldState.containerSize} -> ${intent.size}")
        _uiState.value = _uiState.value.copy(containerSize = intent.size, density = intent.density)

        if (oldState.isSuccess) {
            // 重新分割内容
            splitContentAndBuildVirtualPages(preserveVirtualIndex = true)

            // 启动后台全书分页计算（old.txt中的fetchAllBookContentAndPaginateInBackground逻辑）
            val state = _uiState.value
            if (state.readerSettings.pageFlipEffect != com.novel.page.read.components.PageFlipEffect.VERTICAL) {
                // 非纵向滚动模式才需要全书分页
                viewModelScope.launch {
                    paginationService.fetchAllBookContentAndPaginateInBackground(
                        bookId = state.bookId,
                        chapterList = state.chapterList,
                        readerSettings = state.readerSettings,
                        containerSize = intent.size,
                        density = intent.density
                    )

                    // 获取页数缓存并更新状态
                    val pageCountCache = paginationService.getPageCountCache(
                        state.bookId,
                        state.readerSettings.fontSize,
                        intent.size
                    )
                    if (pageCountCache != null) {
                        _uiState.value = _uiState.value.copy(pageCountCache = pageCountCache)
                        TimberLogger.d(TAG, "页数缓存更新: 总页数=${pageCountCache.totalPages}")
                    }
                }
            }
        }
    }

    private fun handleRetry() {
        TimberLogger.d(TAG, "重试初始化")
        val bookId = _uiState.value.bookId
        val chapterId = _uiState.value.currentChapter?.id
        _uiState.value = ReaderUiState() // 重置状态
        onIntent(ReaderIntent.InitReader(bookId, chapterId))
    }

    override fun onCleared() {
        super.onCleared()
        TimberLogger.d(TAG, "ReaderViewModel销毁，保存进度并清理资源")
        viewModelScope.launch {
            saveProgressUseCase.execute(_uiState.value)
        }
        preloadChaptersUseCase.cancelPreload()
    }
} 