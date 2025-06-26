package com.novel.page.read.usecase

import android.util.Log
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.page.read.viewmodel.VirtualPage
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * 翻页用例
 * 
 * 负责处理页面翻页逻辑：
 * 1. 防抖处理避免快速重复触发
 * 2. 虚拟页面索引管理和边界检查
 * 3. 平移模式特殊处理避免循环更新
 * 4. 章节边界预加载检查
 * 5. BookDetailPage和ContentPage的切换
 * 6. 到达虚拟列表边界时的章节切换
 */
class FlipPageUseCase @Inject constructor(
    private val switchChapterUseCase: SwitchChapterUseCase,
    private val preloadChaptersUseCase: PreloadChaptersUseCase
) {
    companion object {
        private const val TAG = ReaderLogTags.FLIP_PAGE_USE_CASE
        private const val FLIP_COOLDOWN_MS = 300L // 翻页防抖时间
    }

    // 翻页防抖控制
    private var lastFlipTime = 0L

    sealed class FlipResult {
        /** 虚拟页面内翻页成功 */
        data class VirtualPageChanged(
            val newVirtualPageIndex: Int,
            val newVirtualPage: VirtualPage,
            val needsPreloadCheck: Boolean = false
        ) : FlipResult()
        
        /** 章节切换成功 */
        data class ChapterChanged(val switchResult: SwitchChapterUseCase.SwitchResult) : FlipResult()
        
        /** 操作失败 */
        data class Failure(val error: Throwable) : FlipResult()
        
        /** 无操作（已到边界或其他原因） */
        data object NoOp : FlipResult()
        
        /** 需要重建虚拟页面 */
        data object NeedsVirtualPageRebuild : FlipResult()
    }

    /**
     * 执行翻页操作
     * 
     * @param state 当前阅读器状态
     * @param direction 翻页方向
     * @param scope 协程作用域
     * @return 翻页结果
     */
    suspend fun execute(
        state: ReaderUiState,
        direction: FlipDirection,
        scope: CoroutineScope
    ): FlipResult {
        Log.d(TAG, "开始翻页操作: direction=$direction, 当前虚拟页面索引=${state.virtualPageIndex}")
        
        // 1. 防抖检查
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < FLIP_COOLDOWN_MS) {
            Log.d(TAG, "翻页被防抖拦截: 距离上次翻页${currentTime - lastFlipTime}ms")
            return FlipResult.NoOp
        }
        lastFlipTime = currentTime

        // 2. 检查虚拟页面是否为空
        val virtualPages = state.virtualPages
        if (virtualPages.isEmpty()) {
            Log.w(TAG, "虚拟页面列表为空，无法翻页")
            return FlipResult.NoOp
        }

        // 3. 计算新的虚拟页面索引
        val currentVirtualIndex = state.virtualPageIndex
        val newVirtualIndex = when (direction) {
            FlipDirection.NEXT -> currentVirtualIndex + 1
            FlipDirection.PREVIOUS -> currentVirtualIndex - 1
        }

        Log.d(TAG, "虚拟页面索引变化: $currentVirtualIndex -> $newVirtualIndex (总页数=${virtualPages.size})")

        // 4. 检查是否在虚拟页面列表范围内
        if (newVirtualIndex in virtualPages.indices) {
            // 在虚拟页面列表内移动
            return handleVirtualPageFlip(state, newVirtualIndex, virtualPages[newVirtualIndex], scope)
        } else {
            // 到达虚拟列表边界，需要切换章节
            Log.d(TAG, "到达虚拟列表边界，尝试切换章节")
            return handleChapterFlip(state, direction, scope)
        }
    }

    /**
     * 处理虚拟页面内的翻页
     */
    private suspend fun handleVirtualPageFlip(
        state: ReaderUiState,
        newVirtualIndex: Int,
        newVirtualPage: VirtualPage,
        scope: CoroutineScope
    ): FlipResult {
        Log.d(TAG, "虚拟页面内翻页: 索引=$newVirtualIndex, 页面类型=${newVirtualPage::class.simpleName}")

        // 检查是否为平移模式，平移模式需要特殊处理避免循环更新
        val isSlideMode = state.readerSettings.pageFlipEffect == PageFlipEffect.SLIDE
        Log.d(TAG, "翻页模式: ${if (isSlideMode) "平移模式" else "其他模式"}")

        // 检查是否需要预加载
        val needsPreloadCheck = when (newVirtualPage) {
            is VirtualPage.ContentPage -> {
                val pageData = state.loadedChapterData[newVirtualPage.chapterId]
                if (pageData != null) {
                    val isAtChapterBoundary = newVirtualPage.pageIndex == 0 ||
                                            newVirtualPage.pageIndex == pageData.pages.size - 1
                    Log.d(TAG, "内容页面边界检查: 页面索引=${newVirtualPage.pageIndex}, 总页数=${pageData.pages.size}, 是否边界=$isAtChapterBoundary")
                    isAtChapterBoundary
                } else {
                    Log.w(TAG, "未找到章节数据: ${newVirtualPage.chapterId}")
                    false
                }
            }
            is VirtualPage.BookDetailPage -> {
                Log.d(TAG, "书籍详情页，无需预加载检查")
                false
            }
            is VirtualPage.ChapterSection -> {
                Log.d(TAG, "章节区域页面，无需预加载检查")
                false
            }
        }

        // 检查是否有新的相邻章节被加载，如果有则需要重建虚拟页面
        if (newVirtualPage is VirtualPage.ContentPage) {
            val currentChapterIndex = state.chapterList.indexOfFirst { it.id == newVirtualPage.chapterId }
            if (currentChapterIndex >= 0) {
                val hasNewAdjacentChapters = preloadChaptersUseCase.checkIfNewAdjacentChaptersLoaded(state, currentChapterIndex)
                if (hasNewAdjacentChapters) {
                    Log.d(TAG, "检测到新的相邻章节已加载，需要重建虚拟页面")
                    return FlipResult.NeedsVirtualPageRebuild
                }
            }
        }

        return FlipResult.VirtualPageChanged(
            newVirtualPageIndex = newVirtualIndex,
            newVirtualPage = newVirtualPage,
            needsPreloadCheck = needsPreloadCheck
        )
    }

    /**
     * 处理章节边界的翻页（切换章节）
     */
    private suspend fun handleChapterFlip(
        state: ReaderUiState,
        direction: FlipDirection,
        scope: CoroutineScope
    ): FlipResult {
        Log.d(TAG, "处理章节切换: direction=$direction")
        
        val chapterList = state.chapterList
        val currentChapterIndex = state.currentChapterIndex

        val targetChapterIndex = when (direction) {
            FlipDirection.NEXT -> currentChapterIndex + 1
            FlipDirection.PREVIOUS -> currentChapterIndex - 1
        }

        Log.d(TAG, "目标章节索引: $currentChapterIndex -> $targetChapterIndex (总章节数=${chapterList.size})")

        // 检查目标章节是否存在
        if (targetChapterIndex !in chapterList.indices) {
            Log.d(TAG, "已到达书籍边界，无法继续翻页")
            return FlipResult.NoOp
        }

        val targetChapter = chapterList[targetChapterIndex]
        Log.d(TAG, "切换到章节: ${targetChapter.chapterName}")

        // 执行章节切换
        val switchResult = switchChapterUseCase.execute(state, targetChapter.id, scope, direction)
        return FlipResult.ChapterChanged(switchResult)
    }

    /**
     * 执行预加载检查
     * 当翻页到章节边界时调用
     */
    suspend fun executePreloadCheck(
        state: ReaderUiState,
        currentVirtualPage: VirtualPage.ContentPage,
        scope: CoroutineScope
    ) {
        Log.d(TAG, "执行预加载检查: 章节=${currentVirtualPage.chapterId}, 页面=${currentVirtualPage.pageIndex}")

        val chapterList = state.chapterList
        val currentChapterIndex = chapterList.indexOfFirst { it.id == currentVirtualPage.chapterId }

        if (currentChapterIndex < 0) {
            Log.w(TAG, "预加载检查失败: 未找到当前章节索引")
            return
        }

        // 检查是否需要扩展预加载范围
        val shouldExpandPreload = preloadChaptersUseCase.shouldExpandPreloadRange(
            currentChapterIndex, 
            currentVirtualPage, 
            state.loadedChapterData
        )

        if (shouldExpandPreload) {
            Log.d(TAG, "触发动态预加载扩展")
            preloadChaptersUseCase.performDynamicPreload(scope, state, currentVirtualPage.chapterId, triggerExpansion = true)
        } else {
            Log.d(TAG, "执行常规预加载检查")
            preloadChaptersUseCase.checkRegularPreload(currentChapterIndex, currentVirtualPage, state.loadedChapterData, chapterList)
        }
    }
} 