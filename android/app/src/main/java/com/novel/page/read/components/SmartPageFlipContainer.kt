package com.novel.page.read.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.read.repository.PageCountCacheData
import com.novel.page.read.viewmodel.*
import com.novel.page.read.utils.*

/**
 * 智能翻页容器
 * 根据设备性能和内容复杂度自动选择最优的翻页实现
 */
@Composable
fun SmartPageFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    flipEffect: PageFlipEffect,
    readerSettings: ReaderSettings,
    pageCountCache: PageCountCacheData?,
    containerSize: IntSize,
    onPageChange: (direction: FlipDirection) -> Unit,
    onChapterChange: (direction: FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentChapterIndex: Int = 0,
    totalChapters: Int = 1
) {
    // 转换为新的数据格式
    val readerConfig = remember(readerSettings) {
        readerSettings.toReaderConfig()
    }
    
    // 使用策略工厂
    val strategyFactory = remember { DefaultPageFlipStrategyFactory() }
    val strategy = remember(flipEffect) {
        strategyFactory.createStrategy(flipEffect.toNewPageFlipEffect())
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        strategy.RenderFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            readerConfig = readerConfig,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )
    }
}

/**
 * 集成翻页容器
 * 提供新旧架构之间的无缝切换
 */
@Composable
fun IntegratedPageFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    flipEffect: PageFlipEffect,
    readerSettings: ReaderSettings,
    pageCountCache: PageCountCacheData?,
    containerSize: IntSize,
    onPageChange: (direction: FlipDirection) -> Unit,
    onChapterChange: (direction: FlipDirection) -> Unit,
    onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
    onSwipeBack: (() -> Unit)? = null,
    onVerticalScrollPageChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentChapterIndex: Int = 0,
    totalChapters: Int = 1,
    useNewImplementation: Boolean = true // 开关控制 - 启用新的策略模式优化
) {
    if (useNewImplementation) {
        // 使用新的策略模式实现
        SmartPageFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            flipEffect = flipEffect,
            readerSettings = readerSettings,
            pageCountCache = pageCountCache,
            containerSize = containerSize,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onVerticalScrollPageChange = onVerticalScrollPageChange,
            onClick = onClick,
            modifier = modifier,
            currentChapterIndex = currentChapterIndex,
            totalChapters = totalChapters
        )
    } else {
        // 使用原有实现
        PageFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            flipEffect = flipEffect,
            readerSettings = readerSettings,
            pageCountCache = pageCountCache,
            containerSize = containerSize,
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onVerticalScrollPageChange = onVerticalScrollPageChange,
            onClick = onClick,
            modifier = modifier,
            currentChapterIndex = currentChapterIndex,
            totalChapters = totalChapters
        )
    }
}

/**
 * 转换扩展函数
 */
private fun ReaderSettings.toReaderConfig(): ReaderConfig {
    return ReaderConfig(
        pageFlipEffect = this.pageFlipEffect.toNewPageFlipEffect(),
        backgroundColor = this.backgroundColor,
        textColor = this.textColor,
        fontSize = this.fontSize.sp,
        // 使用默认值，因为ReaderSettings中没有这些字段
        lineSpacing = 1.2f,
        paragraphSpacing = 8.sp,
        horizontalPadding = 16.sp,
        verticalPadding = 16.sp,
        isNightMode = false,
        brightness = this.brightness,
        keepScreenOn = false,
        volumeKeyFlip = false,
        autoFlip = false,
        autoFlipInterval = 3000L,
        tapToFlip = true,
        showPageProgress = true,
        showChapterProgress = true,
        showBattery = true,
        showTime = true
    )
}

private fun com.novel.page.read.components.PageFlipEffect.toNewPageFlipEffect(): com.novel.page.read.viewmodel.PageFlipEffect {
    return when (this) {
        com.novel.page.read.components.PageFlipEffect.NONE -> com.novel.page.read.viewmodel.PageFlipEffect.NONE
        com.novel.page.read.components.PageFlipEffect.PAGECURL -> com.novel.page.read.viewmodel.PageFlipEffect.PAGECURL
        com.novel.page.read.components.PageFlipEffect.COVER -> com.novel.page.read.viewmodel.PageFlipEffect.COVER
        com.novel.page.read.components.PageFlipEffect.SLIDE -> com.novel.page.read.viewmodel.PageFlipEffect.SLIDE
        com.novel.page.read.components.PageFlipEffect.VERTICAL -> com.novel.page.read.viewmodel.PageFlipEffect.VERTICAL
    }
} 