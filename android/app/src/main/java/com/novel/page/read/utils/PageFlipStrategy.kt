package com.novel.page.read.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.novel.page.read.viewmodel.*
import com.novel.page.read.components.*

/**
 * 翻页策略接口
 */
interface PageFlipStrategy {
    /**
     * 执行翻页操作
     */
    suspend fun flip(request: FlipRequest): FlipResult
    
    /**
     * 检查是否可以翻页
     */
    fun canFlip(request: FlipRequest): Boolean
    
    /**
     * 渲染翻页组件
     */
    @Composable
    fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)? = null,
        onSwipeBack: (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    )
}

/**
 * 翻页策略工厂
 */
interface PageFlipStrategyFactory {
    fun createStrategy(effect: com.novel.page.read.viewmodel.PageFlipEffect): PageFlipStrategy
}

/**
 * 默认翻页策略工厂实现
 */
class DefaultPageFlipStrategyFactory : PageFlipStrategyFactory {
    override fun createStrategy(effect: com.novel.page.read.viewmodel.PageFlipEffect): PageFlipStrategy {
        return when (effect) {
            com.novel.page.read.viewmodel.PageFlipEffect.NONE -> NoAnimationFlipStrategy()
            com.novel.page.read.viewmodel.PageFlipEffect.PAGECURL -> PageCurlFlipStrategy()
            com.novel.page.read.viewmodel.PageFlipEffect.COVER -> CoverFlipStrategy()
            com.novel.page.read.viewmodel.PageFlipEffect.SLIDE -> SlideFlipStrategy()
            com.novel.page.read.viewmodel.PageFlipEffect.VERTICAL -> VerticalFlipStrategy()
        }
    }
}

/**
 * 抽象翻页策略基类
 */
abstract class BasePageFlipStrategy : PageFlipStrategy {
    
    override suspend fun flip(request: FlipRequest): FlipResult {
        return try {
            if (!canFlip(request)) {
                return FlipResult.Boundary(request.direction == FlipDirection.PREVIOUS)
            }
            
            when (request.direction) {
                FlipDirection.NEXT -> handleNextFlip(request)
                FlipDirection.PREVIOUS -> handlePreviousFlip(request)
            }
        } catch (e: Exception) {
            FlipResult.Error(e)
        }
    }
    
    override fun canFlip(request: FlipRequest): Boolean {
        val pageData = request.pageData
        val currentIndex = request.currentPageIndex
        
        return when (request.direction) {
            FlipDirection.NEXT -> {
                currentIndex < pageData.totalPageCount - 1 || pageData.nextChapterData != null
            }
            FlipDirection.PREVIOUS -> {
                currentIndex > 0 || pageData.previousChapterData != null
            }
        }
    }
    
    abstract suspend fun handleNextFlip(request: FlipRequest): FlipResult
    abstract suspend fun handlePreviousFlip(request: FlipRequest): FlipResult
}

/**
 * 无动画翻页策略
 */
class NoAnimationFlipStrategy : BasePageFlipStrategy() {
    
    override suspend fun handleNextFlip(request: FlipRequest): FlipResult {
        val pageData = request.pageData
        val currentIndex = request.currentPageIndex
        
        return when {
            currentIndex == -1 && pageData.hasBookDetailPage -> {
                FlipResult.Success(0) // 从详情页到第一页
            }
            currentIndex < pageData.pages.size - 1 -> {
                FlipResult.Success(currentIndex + 1)
            }
            else -> {
                FlipResult.ChapterChange(FlipDirection.NEXT)
            }
        }
    }
    
    override suspend fun handlePreviousFlip(request: FlipRequest): FlipResult {
        val pageData = request.pageData
        val currentIndex = request.currentPageIndex
        
        return when {
            currentIndex > 0 -> {
                FlipResult.Success(currentIndex - 1)
            }
            currentIndex == 0 && pageData.hasBookDetailPage -> {
                FlipResult.Success(-1) // 到详情页
            }
            else -> {
                FlipResult.ChapterChange(FlipDirection.PREVIOUS)
            }
        }
    }
    
    @Composable
    override fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)?,
        onSwipeBack: (() -> Unit)?,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        NoAnimationContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            readerSettings = readerConfig.toReaderSettings(),
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )
    }
}

/**
 * PageCurl翻页策略
 */
class PageCurlFlipStrategy : BasePageFlipStrategy() {
    
    override suspend fun handleNextFlip(request: FlipRequest): FlipResult {
        return NoAnimationFlipStrategy().handleNextFlip(request)
    }
    
    override suspend fun handlePreviousFlip(request: FlipRequest): FlipResult {
        return NoAnimationFlipStrategy().handlePreviousFlip(request)
    }
    
    @Composable
    override fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)?,
        onSwipeBack: (() -> Unit)?,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        PageCurlFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            readerSettings = readerConfig.toReaderSettings(),
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )
    }
}

/**
 * Cover翻页策略
 */
class CoverFlipStrategy : BasePageFlipStrategy() {
    override suspend fun handleNextFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handleNextFlip(request)
    
    override suspend fun handlePreviousFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handlePreviousFlip(request)
    
    @Composable
    override fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)?,
        onSwipeBack: (() -> Unit)?,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        CoverFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            readerSettings = readerConfig.toReaderSettings(),
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )
    }
}

/**
 * Slide翻页策略
 */
class SlideFlipStrategy : BasePageFlipStrategy() {
    override suspend fun handleNextFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handleNextFlip(request)
    
    override suspend fun handlePreviousFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handlePreviousFlip(request)
    
    @Composable
    override fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)?,
        onSwipeBack: (() -> Unit)?,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        SlideFlipContainer(
            pageData = pageData,
            currentPageIndex = currentPageIndex,
            readerSettings = readerConfig.toReaderSettings(),
            onPageChange = onPageChange,
            onChapterChange = onChapterChange,
            onSwipeBack = onSwipeBack,
            onClick = onClick
        )
    }
}

/**
 * Vertical翻页策略
 */
class VerticalFlipStrategy : BasePageFlipStrategy() {
    override suspend fun handleNextFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handleNextFlip(request)
    
    override suspend fun handlePreviousFlip(request: FlipRequest): FlipResult = 
        NoAnimationFlipStrategy().handlePreviousFlip(request)
    
    @Composable
    override fun RenderFlipContainer(
        pageData: PageData,
        currentPageIndex: Int,
        readerConfig: ReaderConfig,
        onPageChange: (FlipDirection) -> Unit,
        onChapterChange: (FlipDirection) -> Unit,
        onNavigateToReader: ((bookId: String, chapterId: String?) -> Unit)?,
        onSwipeBack: (() -> Unit)?,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        VerticalScrollContainer(
            pageData = pageData,
            readerSettings = readerConfig.toReaderSettings(),
            pageCountCache = null, // TODO: 需要传递
            containerSize = androidx.compose.ui.unit.IntSize.Zero, // TODO: 需要传递
            onChapterChange = onChapterChange,
            onNavigateToReader = onNavigateToReader,
            onSwipeBack = onSwipeBack,
            onVerticalScrollPageChange = { }, // TODO: 需要实现
            onClick = onClick,
            currentChapterIndex = 0, // TODO: 需要传递
            totalChapters = 1 // TODO: 需要传递
        )
    }
}

/**
 * 翻页处理器
 */
class FlipHandler(
    private val strategyFactory: PageFlipStrategyFactory
) {
    
    suspend fun handleFlip(
        effect: com.novel.page.read.viewmodel.PageFlipEffect,
        request: FlipRequest
    ): FlipResult {
        return try {
            val strategy = strategyFactory.createStrategy(effect)
            strategy.flip(request)
        } catch (e: Exception) {
            FlipResult.Error(e)
        }
    }
    
    fun canFlip(
        effect: com.novel.page.read.viewmodel.PageFlipEffect,
        request: FlipRequest
    ): Boolean {
        return try {
            val strategy = strategyFactory.createStrategy(effect)
            strategy.canFlip(request)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * ReaderConfig转换为ReaderSettings的扩展函数
 */
private fun ReaderConfig.toReaderSettings(): ReaderSettings {
    return ReaderSettings(
        pageFlipEffect = when (this.pageFlipEffect) {
            com.novel.page.read.viewmodel.PageFlipEffect.NONE -> com.novel.page.read.components.PageFlipEffect.NONE
            com.novel.page.read.viewmodel.PageFlipEffect.PAGECURL -> com.novel.page.read.components.PageFlipEffect.PAGECURL
            com.novel.page.read.viewmodel.PageFlipEffect.COVER -> com.novel.page.read.components.PageFlipEffect.COVER
            com.novel.page.read.viewmodel.PageFlipEffect.SLIDE -> com.novel.page.read.components.PageFlipEffect.SLIDE
            com.novel.page.read.viewmodel.PageFlipEffect.VERTICAL -> com.novel.page.read.components.PageFlipEffect.VERTICAL
        },
        backgroundColor = this.backgroundColor,
        textColor = this.textColor,
        fontSize = this.fontSize.value.toInt(),
        brightness = this.brightness
    )
} 