package com.novel.page.read.viewmodel

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.core.mvi.MviIntent
import com.novel.core.mvi.MviState
import com.novel.core.mvi.MviEffect
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.repository.ProgressiveCalculationState

/**
 * Reader模块MVI契约类
 * 
 * 基于核心MVI接口，统一Reader模块的状态管理
 */

/**
 * Reader Intent - 用户意图和系统事件
 * 
 * 基于核心MviIntent接口，包含所有Reader模块的用户操作
 */
sealed class ReaderIntent : MviIntent {
    // 初始化相关
    data class InitReader(val bookId: String, val chapterId: String?) : ReaderIntent()
    data object Retry : ReaderIntent()
    
    // 翻页相关
    data class PageFlip(val direction: FlipDirection) : ReaderIntent()
    data object PreviousChapter : ReaderIntent()
    data object NextChapter : ReaderIntent()
    
    // 章节切换相关
    data class SwitchToChapter(val chapterId: String) : ReaderIntent()
    data class SeekToProgress(val progress: Float) : ReaderIntent()
    
    // 设置相关
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent()
    data class UpdateContainerSize(val size: IntSize, val density: Density) : ReaderIntent()
    
    // 菜单和UI相关
    data class ToggleMenu(val show: Boolean) : ReaderIntent()
    data class ShowChapterList(val show: Boolean) : ReaderIntent()
    data class ShowSettingsPanel(val show: Boolean) : ReaderIntent()
    
    // 进度保存相关
    data class SaveProgress(val force: Boolean = false) : ReaderIntent()
    
    // 预加载相关
    data class PreloadChapters(val currentChapterId: String) : ReaderIntent()

    // UI交互相关
    data class UpdateScrollPosition(val pageIndex: Int) : ReaderIntent()
    data class UpdateSlideIndex(val index: Int) : ReaderIntent()
    data class ShowProgressRestoredHint(val show: Boolean) : ReaderIntent()
}

/**
 * Reader State - 完整的UI状态
 * 
 * 基于核心MviState接口，包含Reader模块的所有状态信息
 */
data class ReaderState(
    override val version: Long = 0L,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // 基础状态
    val bookId: String = "",
    val chapterList: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val bookContent: String = "",
    val readingProgress: Float = 0f,
    
    // 阅读器设置
    val readerSettings: ReaderSettings = ReaderSettings.getDefault(),
    
    // 分页相关状态
    val currentPageData: PageData? = null,
    val currentPageIndex: Int = 0,
    val isSwitchingChapter: Boolean = false,
    val containerSize: IntSize = IntSize.Zero,
    val density: Density? = null,
    
    // 统一翻页模式所需的状态
    val virtualPages: List<VirtualPage> = emptyList(),
    val virtualPageIndex: Int = 0,
    val loadedChapterData: Map<String, PageData> = emptyMap(),
    
    // 全书分页缓存
    val pageCountCache: PageCountCacheData? = null,
    val paginationState: ProgressiveCalculationState = ProgressiveCalculationState(),
    
    // 相邻章节数据
    val previousChapterData: PageData? = null,
    val nextChapterData: PageData? = null,
    
    // UI状态
    val isMenuVisible: Boolean = false,
    val isChapterListVisible: Boolean = false,
    val isSettingsPanelVisible: Boolean = false,
    val showProgressRestoredHint: Boolean = false
) : MviState {
    
    override val isEmpty: Boolean 
        get() = !isLoading && !hasError && chapterList.isEmpty()
    
    override val isSuccess: Boolean
        get() = !isLoading && !hasError && currentChapter != null
    
    // 扩展属性
    val isFirstChapter: Boolean 
        get() = currentChapterIndex == 0
    
    val isLastChapter: Boolean 
        get() = currentChapterIndex >= chapterList.size - 1
    
    val computedReadingProgress: Float
        get() {
            if (readerSettings.pageFlipEffect == com.novel.page.read.components.PageFlipEffect.VERTICAL) {
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

            return (globalCurrentPage + 1).toFloat() / cache.totalPages.toFloat()
        }
}

/**
 * Reader Effect - 一次性副作用
 * 
 * 基于核心MviEffect接口，包含Reader模块的所有副作用
 */
sealed class ReaderEffect : MviEffect {
    // 导航相关
    data class NavigateBack(val reason: String = "") : ReaderEffect()
    data class NavigateToBookDetail(val bookId: String) : ReaderEffect()
    data class NavigateToChapter(val chapterId: String) : ReaderEffect()
    
    // 提示相关
    data class ShowToast(val message: String) : ReaderEffect()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : ReaderEffect()
    
    // 系统相关
    data class SetBrightness(val brightness: Float) : ReaderEffect()
    data class SetKeepScreenOn(val keepOn: Boolean) : ReaderEffect()
    data class TriggerHapticFeedback(val type: HapticFeedbackType = HapticFeedbackType.LIGHT) : ReaderEffect()
    
    // 分享相关
    data class ShareContent(val content: String, val title: String) : ReaderEffect()
    
    // 错误处理
    data class ShowErrorDialog(val title: String, val message: String, val canRetry: Boolean = true) : ReaderEffect()
    
    // 进度相关
    data class SaveProgressCompleted(val success: Boolean) : ReaderEffect()
    
    // 预加载相关
    data class PreloadCompleted(val chapterId: String, val success: Boolean) : ReaderEffect()
}

/**
 * 触觉反馈类型
 */
enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    HEAVY
} 