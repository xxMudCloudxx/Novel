package com.novel.page.read.viewmodel

import androidx.compose.ui.graphics.Color
import com.novel.core.adapter.StateAdapter
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.PageFlipEffect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Reader状态适配器
 *
 * 提供ReaderState的便利方法和计算属性
 * 简化UI层对状态的访问和判断
 */
class ReaderStateAdapter(stateFlow: StateFlow<ReaderState>) : StateAdapter<ReaderState>(stateFlow) {

    // ======================= 基础状态判断 =======================

    /** 是否正在初始化 */
    val isInitializing =
        createConditionFlow { it.isLoading && it.bookId.isNotEmpty() && it.chapterList.isEmpty() }

    /** 是否初始化完成 */
    val isInitialized =
        createConditionFlow { !it.isLoading && it.chapterList.isNotEmpty() && it.currentChapter != null }

    /** 是否正在切换章节 */
    val isSwitchingChapter = mapState { it.isSwitchingChapter }

    /** 错误信息 */
    val errorMessage = mapState { it.error ?: "" }

    // ======================= 阅读状态 =======================

    /** 当前章节名称 */
    val currentChapterName = mapState { it.currentChapter?.chapterName ?: "" }

    /** 当前章节索引（从1开始） */
    val currentChapterNumber = mapState { it.currentChapterIndex + 1 }

    /** 总章节数 */
    val totalChapters = mapState { it.chapterList.size }

    /** 章节进度文本 */
    val chapterProgressText = mapState { "${it.currentChapterIndex + 1}/${it.chapterList.size}" }

    /** 当前页码（从1开始） */
    val currentPageNumber = mapState {
        when {
            it.currentPageIndex == -1 -> 0 // 书籍详情页
            else -> it.currentPageIndex + 1
        }
    }

    /** 当前章节总页数 */
    val currentChapterTotalPages = mapState { it.currentPageData?.pages?.size ?: 0 }

    /** 页面进度文本 */
    val pageProgressText = mapState { state ->
        when {
            state.currentPageIndex == -1 -> "详情页"
            (state.currentPageData?.pages?.size ?: 0) > 0 -> "${state.currentPageIndex + 1}/${state.currentPageData!!.pages.size}"
            else -> "0/0"
        }
    }

    /** 阅读进度百分比（0-100） */
    val readingProgressPercent = mapState { (it.computedReadingProgress * 100).toInt() }

    /** 阅读进度文本 */
    val readingProgressText = mapState { "${(it.computedReadingProgress * 100).toInt()}%" }

    // ======================= 翻页相关 =======================

    /** 是否可以翻到上一页 */
    val canFlipToPreviousPage = createConditionFlow {
        it.virtualPages.isNotEmpty() && it.virtualPageIndex > 0
    }

    /** 是否可以翻到下一页 */
    val canFlipToNextPage = createConditionFlow {
        it.virtualPages.isNotEmpty() && it.virtualPageIndex < it.virtualPages.size - 1
    }

    /** 是否可以翻到上一章 */
    val canFlipToPreviousChapter = createConditionFlow { it.currentChapterIndex > 0 }

    /** 是否可以翻到下一章 */
    val canFlipToNextChapter =
        createConditionFlow { it.currentChapterIndex < it.chapterList.size - 1 }

    /** 是否在第一页 */
    val isFirstPage = createConditionFlow { it.currentPageIndex == 0 || it.currentPageIndex == -1 }

    /** 是否在最后一页 */
    val isLastPage = createConditionFlow {
        val pageData = it.currentPageData
        pageData != null && it.currentPageIndex >= pageData.pages.size - 1
    }

    /** 是否在书籍详情页 */
    val isOnBookDetailPage = createConditionFlow { it.currentPageIndex == -1 }

    // ======================= 翻页效果 =======================

    /** 当前翻页效果 */
    val pageFlipEffect = mapState { it.readerSettings.pageFlipEffect }

    /** 是否为纵向滚动模式 */
    val isVerticalScrollMode =
        createConditionFlow { it.readerSettings.pageFlipEffect == PageFlipEffect.VERTICAL }

    /** 是否为平移翻页模式 */
    val isSlideFlipMode =
        createConditionFlow { it.readerSettings.pageFlipEffect == PageFlipEffect.SLIDE }

    /** 是否为覆盖翻页模式 */
    val isCoverFlipMode =
        createConditionFlow { it.readerSettings.pageFlipEffect == PageFlipEffect.COVER }

    /** 是否为卷曲翻页模式 */
    val isPageCurlMode =
        createConditionFlow { it.readerSettings.pageFlipEffect == PageFlipEffect.PAGECURL }

    /** 是否为无动画翻页模式 */
    val isNoAnimationMode =
        createConditionFlow { it.readerSettings.pageFlipEffect == PageFlipEffect.NONE }

    // ======================= UI状态 =======================

    /** 是否显示菜单 */
    val isMenuVisible = mapState { it.isMenuVisible }

    /** 是否显示章节列表 */
    val isChapterListVisible = mapState { it.isChapterListVisible }

    /** 是否显示设置面板 */
    val isSettingsPanelVisible = mapState { it.isSettingsPanelVisible }

    /** 是否有任何面板显示 */
    val hasAnyPanelVisible =
        createConditionFlow { it.isMenuVisible || it.isChapterListVisible || it.isSettingsPanelVisible }

    // ======================= 设置相关 =======================

    /** 字体大小 */
    val fontSize = mapState { it.readerSettings.fontSize }

    /** 亮度值 */
    val brightness = mapState { it.readerSettings.brightness }

    /** 亮度百分比 */
    val brightnessPercent = mapState { (it.readerSettings.brightness * 100).toInt() }

    /** 背景颜色 */
    val backgroundColor = mapState { it.readerSettings.backgroundColor }

    /** 文字颜色 */
    val textColor = mapState { it.readerSettings.textColor }

    // ======================= 缓存和性能 =======================

    /** 是否有页数缓存 */
    val hasPageCountCache = createConditionFlow { it.pageCountCache != null }

    /** 全书总页数 */
    val totalBookPages = mapState { it.pageCountCache?.totalPages ?: 0 }

    /** 是否正在计算分页 */
    val isCalculatingPagination = mapState { it.paginationState.isCalculating }

    /** 分页计算进度 */
    val paginationProgress = mapState {
        if (it.paginationState.totalChapters > 0) {
            it.paginationState.calculatedChapters.toFloat() / it.paginationState.totalChapters.toFloat()
        } else 0f
    }

    /** 分页计算进度百分比 */
    val paginationProgressPercent = mapState { state ->
        val progress = if (state.paginationState.totalChapters > 0) {
            state.paginationState.calculatedChapters.toFloat() / state.paginationState.totalChapters.toFloat()
        } else 0f
        (progress * 100).toInt()
    }

    // ======================= 预加载状态 =======================

    /** 已加载的章节数量 */
    val loadedChapterCount = mapState { it.loadedChapterData.size }

    /** 是否有预加载的下一章 */
    val hasPreloadedNextChapter = createConditionFlow { it.nextChapterData != null }

    /** 是否有预加载的上一章 */
    val hasPreloadedPreviousChapter = createConditionFlow { it.previousChapterData != null }

    // ======================= 便利方法 =======================

    /**
     * 获取指定索引的章节
     */
    fun getChapterAt(index: Int): Chapter? {
        return getCurrentSnapshot().chapterList.getOrNull(index)
    }

    /**
     * 根据ID查找章节
     */
    fun findChapterById(chapterId: String): Chapter? {
        return getCurrentSnapshot().chapterList.find { it.id == chapterId }
    }

    /**
     * 获取章节在列表中的索引
     */
    fun getChapterIndex(chapterId: String): Int {
        return getCurrentSnapshot().chapterList.indexOfFirst { it.id == chapterId }
    }

    /**
     * 获取上一章
     */
    fun getPreviousChapter(): Chapter? {
        val state = getCurrentSnapshot()
        return if (state.currentChapterIndex > 0) {
            state.chapterList.getOrNull(state.currentChapterIndex - 1)
        } else null
    }

    /**
     * 获取下一章
     */
    fun getNextChapter(): Chapter? {
        val state = getCurrentSnapshot()
        return if (state.currentChapterIndex < state.chapterList.size - 1) {
            state.chapterList.getOrNull(state.currentChapterIndex + 1)
        } else null
    }

    /**
     * 检查是否为指定章节
     */
    fun isCurrentChapter(chapterId: String): Boolean {
        return getCurrentSnapshot().currentChapter?.id == chapterId
    }

    /**
     * 检查是否为指定页面
     */
    fun isCurrentPage(pageIndex: Int): Boolean {
        return getCurrentSnapshot().currentPageIndex == pageIndex
    }

    /**
     * 获取当前虚拟页面
     */
    fun getCurrentVirtualPage(): VirtualPage? {
        val state = getCurrentSnapshot()
        return state.virtualPages.getOrNull(state.virtualPageIndex)
    }

    /**
     * 检查容器是否已初始化
     */
    fun isContainerInitialized(): Boolean {
        val state = getCurrentSnapshot()
        return state.containerSize.width > 0 && state.containerSize.height > 0 && state.density != null
    }

    /**
     * 格式化阅读时长
     */
    fun formatReadingTime(durationMs: Long): String {
        val minutes = durationMs / 60000
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}小时${minutes % 60}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }

    /**
     * 计算预计剩余阅读时间
     */
    fun estimateRemainingReadingTime(avgReadingSpeedWpm: Int = 200): String {
        val state = getCurrentSnapshot()
        val remainingProgress = 1f - state.computedReadingProgress
        if (remainingProgress <= 0f || state.pageCountCache == null) return "未知"

        val totalWords = (state.pageCountCache?.totalPages ?: 0) * 300 // 假设每页300字
        val remainingWords = (totalWords * remainingProgress).toInt()
        val remainingMinutes = remainingWords / avgReadingSpeedWpm

        return formatReadingTime(remainingMinutes * 60000L)
    }

    // ======================= 调试信息 =======================

    /**
     * 获取状态调试信息
     */
    fun getDebugInfo(): String {
        val state = getCurrentSnapshot()
        return buildString {
            appendLine("=== Reader State Debug Info ===")
            appendLine("Version: ${state.version}")
            appendLine("Loading: ${state.isLoading}")
            appendLine("Error: ${state.error}")
            appendLine("BookId: ${state.bookId}")
            appendLine("Current Chapter: ${state.currentChapter?.chapterName} (${state.currentChapterIndex + 1}/${state.chapterList.size})")
            appendLine("Current Page: ${if (state.currentPageIndex == -1) "详情页" else "${state.currentPageIndex + 1}/${state.currentPageData?.pages?.size ?: 0}"}")
            appendLine("Reading Progress: ${(state.computedReadingProgress * 100).toInt()}%")
            appendLine("Flip Effect: ${state.readerSettings.pageFlipEffect}")
            appendLine("Virtual Pages: ${state.virtualPages.size}")
            appendLine("Virtual Page Index: ${state.virtualPageIndex}")
            appendLine("Loaded Chapters: ${state.loadedChapterData.size}")
            appendLine("Container Size: ${state.containerSize}")
            appendLine("Has Cache: ${state.pageCountCache != null}")
            if (state.pageCountCache != null) {
                appendLine("Total Book Pages: ${state.pageCountCache.totalPages}")
            }
            appendLine("UI States: Menu=$isMenuVisible, ChapterList=$isChapterListVisible, Settings=$isSettingsPanelVisible")
        }
    }
}

/**
 * StateAdapter工厂方法
 * 为StateFlow提供便捷的适配器创建
 */
fun StateFlow<ReaderState>.asReaderAdapter(): ReaderStateAdapter {
    return ReaderStateAdapter(this)
}