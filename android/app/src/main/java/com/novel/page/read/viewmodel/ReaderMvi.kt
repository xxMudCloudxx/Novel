package com.novel.page.read.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.core.mvi.MviIntent
import com.novel.core.mvi.MviState
import com.novel.core.mvi.MviEffect
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.repository.ProgressiveCalculationState
import com.novel.utils.TimberLogger

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
@Stable
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

/**
 * 章节信息数据类
 *
 * @property id 章节唯一标识
 * @property chapterName 章节名称
 * @property chapterNum 章节序号（可选）
 * @property isVip VIP标识（"0"为免费，"1"为VIP）
 */
@Immutable
data class Chapter(
    val id: String,
    val chapterName: String,
    val chapterNum: String? = null,
    val isVip: String = "0"
)

/**
 * 翻页方向
 */
enum class FlipDirection {
    PREVIOUS,
    NEXT
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
@Stable
data class ChapterCache(
    val chapter: Chapter,
    val content: String,
    var pageData: PageData? = null
)

/**
 * 单页数据
 */
@Stable
data class PageData(
    val chapterId: String,
    val chapterName: String,
    val content: String,
    val pages: List<String>,
    val isFirstPage: Boolean = false,
    val isLastPage: Boolean = false,
    val isFirstChapter: Boolean = false,
    val isLastChapter: Boolean = false,
    val nextChapterData: PageData? = null,
    val previousChapterData: PageData? = null,
    val bookInfo: BookInfo? = null, // 书籍信息，用于第0页
    val hasBookDetailPage: Boolean = false // 是否有书籍详情页
) {
    val pageCount: Int get() = pages.size

    @Immutable
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
}

/**
 * 阅读器设置数据类
 *
 * 封装阅读器的所有可配置设置项，支持个性化阅读体验
 * 所有设置都会自动持久化到本地存储
 *
 * @param brightness 屏幕亮度值，范围0.0-1.0，0.0最暗，1.0最亮
 * @param fontSize 阅读字体大小，范围12-44，单位sp
 * @param backgroundColor 阅读背景颜色，支持多种预设主题
 * @param textColor 文字颜色，会根据背景色自动适配对比度
 * @param pageFlipEffect 翻页动画效果，支持多种翻页模式
 */
@Stable
data class ReaderSettings(
    /** 屏幕亮度值 - 范围0.0-1.0，影响整个屏幕亮度 */
    val brightness: Float = 0.5f,

    /** 阅读字体大小 - 范围12-44sp，影响文字显示大小 */
    val fontSize: Int = 16,

    /** 阅读背景颜色 - 默认温暖黄色，护眼舒适 */
    val backgroundColor: Color = Color(0xFFF5F5DC),

    /** 文字颜色 - 默认深灰色，与背景形成良好对比 */
    val textColor: Color = Color(0xFF2E2E2E),

    /** 翻页动画效果 - 默认仿真书本翻页效果 */
    val pageFlipEffect: PageFlipEffect = PageFlipEffect.PAGECURL
) {
    companion object {
        /**
         * 获取默认阅读器设置
         *
         * 提供经过优化的默认配置，确保最佳的阅读体验：
         * - 适中的亮度避免眼部疲劳
         * - 标准字体大小适合大多数用户
         * - 温暖背景色减少蓝光刺激
         * - 高对比度文字色保证清晰度
         * - 仿真翻页效果提升沉浸感
         *
         * @return 默认的ReaderSettings实例
         */
        fun getDefault(): ReaderSettings {
            val defaultSettings = ReaderSettings(
                brightness = 0.5f,                    // 中等亮度，平衡护眼与可读性
                fontSize = 16,                        // 标准字体大小，适合大多数设备
                backgroundColor = Color(0xFFF5F5DC),  // 温暖米黄色，护眼舒适
                textColor = Color(0xFF2E2E2E),        // 深灰色文字，清晰易读
                pageFlipEffect = PageFlipEffect.PAGECURL  // 仿真翻页，增强沉浸感
            )

            TimberLogger.d("ReaderSettings", "创建默认设置:")
            TimberLogger.d("ReaderSettings", "  - 字体大小: ${defaultSettings.fontSize}sp")
            TimberLogger.d("ReaderSettings", "  - 亮度: ${(defaultSettings.brightness * 100).toInt()}%")
            TimberLogger.d("ReaderSettings", "  - 背景颜色: ${String.format("#%08X", defaultSettings.backgroundColor.toArgb())}")
            TimberLogger.d("ReaderSettings", "  - 文字颜色: ${String.format("#%08X", defaultSettings.textColor.toArgb())}")
            TimberLogger.d("ReaderSettings", "  - 翻页效果: ${defaultSettings.pageFlipEffect}")

            return defaultSettings
        }
    }
}

/**
 * 翻页动画效果枚举类
 *
 * 定义阅读器支持的各种翻页动画效果，每种效果都有不同的视觉体验和性能特征
 *
 * @param displayName 在设置界面显示的中文名称
 */
enum class PageFlipEffect(val displayName: String) {
    /** 仿真书本翻页 - 模拟真实书本的卷曲翻页效果，最具沉浸感 */
    PAGECURL("书卷"),

    /** 覆盖式翻页 - 新页面从上方覆盖当前页面，简洁流畅 */
    COVER("覆盖"),

    /** 平移式翻页 - 页面左右滑动切换，类似于现代应用的标准交互 */
    SLIDE("平移"),

    /** 垂直滚动 - 连续的上下滚动阅读，适合长篇内容 */
    VERTICAL("上下"),

    /** 无动画翻页 - 直接切换页面，性能最优，适合低端设备 */
    NONE("无动画")
}

/**
 * 背景主题配置类
 *
 * 预定义的阅读背景主题，每个主题都包含优化搭配的背景色和文字色
 * 确保在不同光线环境下都有良好的可读性和舒适度
 *
 * @param name 主题名称，显示在设置界面
 * @param backgroundColor 背景颜色，影响整个阅读区域
 * @param textColor 文字颜色，与背景色形成适当对比度
 */
@Immutable
data class BackgroundTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * 状态信息，通过 CompositionLocal 提供给子组件
 */
@Stable
data class ReaderInfo(
    val paginationState: ProgressiveCalculationState,
    val pageCountCache: PageCountCacheData?,
    val currentChapter: Chapter?,
    val perChapterPageIndex: Int
)