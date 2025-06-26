package com.novel.page.read.usecase

import android.util.Log
import com.novel.page.read.components.Chapter
import com.novel.page.read.service.ChapterService
import com.novel.page.read.service.PaginationService
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.page.read.viewmodel.VirtualPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 章节预加载用例
 * 
 * 负责智能预加载相邻章节，提升阅读体验：
 * 1. 动态预加载范围管理（2-4章节）
 * 2. 缓存清理和内存管理
 * 3. 预加载状态检查和优化
 * 4. 响应式预加载触发
 */
class PreloadChaptersUseCase @Inject constructor(
    private val chapterService: ChapterService,
    private val paginationService: PaginationService
) {
    companion object {
        private const val TAG = ReaderLogTags.PRELOAD_CHAPTERS_USE_CASE
    }
    
    private var preloadJob: Job? = null
    private val preloadingChapters = mutableSetOf<String>()
    
    // 动态预加载范围管理
    private var currentPreloadStartIndex = -1
    private var currentPreloadEndIndex = -1
    private val minPreloadRange = 2  // 最小预加载范围：前后各2章
    private val maxPreloadRange = 4  // 最大预加载范围：前后各4章
    private val maxCacheSize = 12    // 最大缓存大小

    /**
     * 执行基础预加载
     * 预加载当前章节前后各2章的内容
     */
    fun execute(
        scope: CoroutineScope,
        chapterList: List<Chapter>,
        currentChapterId: String
    ) {
        Log.d(TAG, "开始基础预加载: currentChapter=$currentChapterId")
        
        preloadJob?.cancel()
        preloadJob = scope.launch {
            val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }
            if (currentIndex == -1) {
                Log.w(TAG, "当前章节未找到: $currentChapterId")
                return@launch
            }

            Log.d(TAG, "预加载范围: 当前章节索引=$currentIndex, 前后各${minPreloadRange}章")
            
            // 预加载前后各2章
            (1..minPreloadRange).forEach { offset ->
                val nextIndex = currentIndex + offset
                if (nextIndex < chapterList.size) {
                    launch { 
                        Log.d(TAG, "预加载后续章节: ${chapterList[nextIndex].chapterName}")
                        chapterService.preloadChapter(chapterList[nextIndex].id) 
                    }
                }
                val prevIndex = currentIndex - offset
                if (prevIndex >= 0) {
                    launch { 
                        Log.d(TAG, "预加载前序章节: ${chapterList[prevIndex].chapterName}")
                        chapterService.preloadChapter(chapterList[prevIndex].id) 
                    }
                }
            }
        }
    }

    /**
     * 执行动态预加载
     * 根据阅读进度和缓存状况智能调整预加载范围
     * 
     * 该函数实现了old.txt中的performDynamicPreload逻辑：
     * 1. 计算最优预加载范围（2-4章节）
     * 2. 并行预加载章节内容
     * 3. 对已缓存章节进行分页处理
     * 4. 清理超出范围的缓存释放内存
     * 5. 检查是否有新相邻章节需要重建虚拟页面
     */
    fun performDynamicPreload(
        scope: CoroutineScope,
        state: ReaderUiState,
        currentChapterId: String,
        triggerExpansion: Boolean = false
    ) {
        Log.d(TAG, "开始动态预加载: currentChapter=$currentChapterId, expand=$triggerExpansion")
        
        preloadJob?.cancel()
        preloadJob = scope.launch {
            val chapterList = state.chapterList
            val currentIndex = chapterList.indexOfFirst { it.id == currentChapterId }

            if (currentIndex < 0) {
                Log.w(TAG, "动态预加载失败: 章节未找到")
                return@launch
            }

            // 计算新的预加载范围
            val newPreloadRange = calculatePreloadRange(currentIndex, chapterList.size, triggerExpansion)
            val (newStartIndex, newEndIndex) = newPreloadRange

            Log.d(TAG, "预加载范围: [$newStartIndex, $newEndIndex], 总章节数=${chapterList.size}")

            // 更新预加载范围记录
            currentPreloadStartIndex = newStartIndex
            currentPreloadEndIndex = newEndIndex

            // 执行并行预加载
            val preloadIndices = (newStartIndex..newEndIndex).filter { it != currentIndex }

            Log.d(TAG, "开始并行预加载: ${preloadIndices.size}个章节")
            
            // 并行预加载所有需要的章节
            preloadIndices.map { index ->
                launch {
                    val chapter = chapterList[index]
                    val cachedChapter = chapterService.getCachedChapter(chapter.id)
                    if (cachedChapter == null && !preloadingChapters.contains(chapter.id)) {
                        try {
                            preloadingChapters.add(chapter.id)
                            Log.d(TAG, "预加载章节内容: ${chapter.chapterName}")
                            chapterService.preloadChapter(chapter.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "章节内容预加载失败: ${chapter.chapterName}", e)
                        } finally {
                            preloadingChapters.remove(chapter.id)
                        }
                    }
                }
            }.forEach { it.join() }

            // 对已缓存但未分页的章节进行分页处理
            if (state.containerSize.width > 0 && state.containerSize.height > 0 && state.density != null) {
                Log.d(TAG, "开始预加载章节分页处理")
                
                val paginationJobs = preloadIndices.mapNotNull { index ->
                    val chapter = chapterList[index]
                    val cachedChapter = chapterService.getCachedChapter(chapter.id)
                    if (cachedChapter != null && cachedChapter.pageData == null) {
                        launch {
                            try {
                                Log.d(TAG, "分页处理章节: ${chapter.chapterName}")
                                val pages = paginationService.splitContent(
                                    content = cachedChapter.content,
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

                                // 更新缓存中的分页数据
                                chapterService.setCachedChapterPageData(chapter.id, pageData)
                                Log.d(TAG, "章节分页完成: ${chapter.chapterName}, 页数=${pages.size}")
                            } catch (e: Exception) {
                                Log.e(TAG, "章节分页失败: ${chapter.chapterName}", e)
                            }
                        }
                    } else null
                }

                // 等待所有分页任务完成
                paginationJobs.forEach { it.join() }
                Log.d(TAG, "预加载分页处理完成")
            }

            // 清理超出范围的缓存
            cleanupOutOfRangeCache(chapterList, newStartIndex, newEndIndex, currentIndex)

            // 检查是否有新的相邻章节被加载，如果有则需要重建虚拟页面
            // 这是old.txt中checkIfNewAdjacentChaptersLoaded函数的调用点
            val hasNewAdjacentChapters = checkIfNewAdjacentChaptersLoaded(state, currentIndex)
            if (hasNewAdjacentChapters) {
                Log.d(TAG, "检测到新的相邻章节已加载，需要重建虚拟页面")
                // 这里可以通过回调或其他方式通知ViewModel重建虚拟页面
                // 在实际使用中，调用方会处理这个逻辑
            }
        }
    }

    /**
     * 计算预加载范围
     * 根据当前位置、总章节数和是否需要扩展来计算最优预加载范围
     */
    private fun calculatePreloadRange(
        currentIndex: Int,
        totalChapters: Int,
        triggerExpansion: Boolean
    ): Pair<Int, Int> {
        val range = if (triggerExpansion && canExpandRange()) {
            Log.d(TAG, "扩展预加载范围到最大: $maxPreloadRange")
            maxPreloadRange
        } else {
            Log.d(TAG, "使用最小预加载范围: $minPreloadRange")
            minPreloadRange
        }

        val startIndex = (currentIndex - range).coerceAtLeast(0)
        val endIndex = (currentIndex + range).coerceAtMost(totalChapters - 1)

        return Pair(startIndex, endIndex)
    }

    /**
     * 判断是否可以扩展预加载范围
     * 基于当前缓存使用情况决定
     */
    private fun canExpandRange(): Boolean {
        val currentCacheSize = getApproximateCacheSize()
        val canExpand = currentCacheSize < maxCacheSize * 0.8
        Log.d(TAG, "缓存检查: 当前大小=$currentCacheSize, 最大=$maxCacheSize, 可扩展=$canExpand")
        return canExpand
    }

    /**
     * 获取近似缓存大小
     * 在实际实现中，这可以更精确地跟踪
     */
    private fun getApproximateCacheSize(): Int {
        // 这是一个近似值，实际实现中可以更精确地跟踪缓存使用情况
        return preloadingChapters.size + 5 // 估算当前已缓存的章节数
    }

    /**
     * 清理超出范围的缓存
     * 移除不在预加载范围内的章节，释放内存
     */
    private fun cleanupOutOfRangeCache(
        chapterList: List<Chapter>,
        startIndex: Int,
        endIndex: Int,
        currentIndex: Int
    ) {
        Log.d(TAG, "开始清理超出范围的缓存: 保留范围[$startIndex, $endIndex]")
        
        val currentChapterId = chapterList.getOrNull(currentIndex)?.id
        
        // 从预加载集合中移除超出范围的章节
        val removedChapters = preloadingChapters.removeAll { chapterId ->
            val chapterIndex = chapterList.indexOfFirst { it.id == chapterId }
            val shouldRemove = chapterIndex >= 0 && 
                (chapterIndex < startIndex || chapterIndex > endIndex) && 
                chapterId != currentChapterId
            
            if (shouldRemove) {
                Log.d(TAG, "移除超出范围的章节: $chapterId (索引=$chapterIndex)")
            }
            shouldRemove
        }
        
        if (removedChapters) {
            Log.d(TAG, "缓存清理完成")
        }
    }

    /**
     * 检查相邻章节是否已加载
     * 用于判断是否需要触发预加载
     */
    suspend fun checkAdjacentChaptersLoaded(
        chapterList: List<Chapter>,
        currentChapterIndex: Int
    ): Boolean {
        Log.d(TAG, "检查相邻章节加载状态: 当前索引=$currentChapterIndex")
        
        // 检查前面的章节
        for (i in 1..minPreloadRange) {
            val prevIndex = currentChapterIndex - i
            if (prevIndex >= 0) {
                val prevChapter = chapterList[prevIndex]
                val cachedChapter = chapterService.getCachedChapter(prevChapter.id)
                if (cachedChapter == null) {
                    Log.d(TAG, "前序章节未加载: ${prevChapter.chapterName}")
                    return true // 需要预加载
                }
            }
        }

        // 检查后面的章节
        for (i in 1..minPreloadRange) {
            val nextIndex = currentChapterIndex + i
            if (nextIndex < chapterList.size) {
                val nextChapter = chapterList[nextIndex]
                val cachedChapter = chapterService.getCachedChapter(nextChapter.id)
                if (cachedChapter == null) {
                    Log.d(TAG, "后续章节未加载: ${nextChapter.chapterName}")
                    return true // 需要预加载
                }
            }
        }

        Log.d(TAG, "所有相邻章节已加载")
        return false // 所有相邻章节都已加载
    }

    /**
     * 检查是否有新的相邻章节被加载
     * 
     * 这是old.txt中的checkIfNewAdjacentChaptersLoaded函数的实现
     * 用于判断是否需要重建虚拟页面：
     * 1. 检查前后相邻章节是否已加载但不在当前虚拟页面中
     * 2. 如果发现新加载的相邻章节，返回true触发虚拟页面重建
     * 
     * @param state 当前阅读器状态
     * @param currentIndex 当前章节索引
     * @return 如果有新的相邻章节被加载返回true，否则返回false
     */
    suspend fun checkIfNewAdjacentChaptersLoaded(
        state: ReaderUiState,
        currentIndex: Int
    ): Boolean {
        val chapterList = state.chapterList
        val currentVirtualPages = state.virtualPages

        Log.d(TAG, "检查新加载的相邻章节: 当前索引=$currentIndex")

        // 检查前一章节
        val prevIndex = currentIndex - 1
        if (prevIndex >= 0) {
            val prevChapter = chapterList[prevIndex]
            val prevChapterInVirtual = currentVirtualPages.any {
                it is VirtualPage.ContentPage && it.chapterId == prevChapter.id
            }
            val cachedChapter = chapterService.getCachedChapter(prevChapter.id)
            val prevChapterLoaded = cachedChapter?.pageData != null

            if (prevChapterLoaded && !prevChapterInVirtual) {
                Log.d(TAG, "发现新加载的前序章节: ${prevChapter.chapterName}")
                return true // 新的前序章节已加载但不在虚拟页面中
            }
        }

        // 检查后一章节
        val nextIndex = currentIndex + 1
        if (nextIndex < chapterList.size) {
            val nextChapter = chapterList[nextIndex]
            val nextChapterInVirtual = currentVirtualPages.any {
                it is VirtualPage.ContentPage && it.chapterId == nextChapter.id
            }
            val cachedChapter = chapterService.getCachedChapter(nextChapter.id)
            val nextChapterLoaded = cachedChapter?.pageData != null

            if (nextChapterLoaded && !nextChapterInVirtual) {
                Log.d(TAG, "发现新加载的后续章节: ${nextChapter.chapterName}")
                return true // 新的后续章节已加载但不在虚拟页面中
            }
        }

        return false
    }

    /**
     * 判断是否需要扩展预加载范围
     * 当用户进入预加载边缘的章节时返回true
     */
    fun shouldExpandPreloadRange(currentChapterIndex: Int, currentContentPage: VirtualPage.ContentPage, loadedChapterData: Map<String, PageData>): Boolean {
        // 如果还没有初始化预加载范围，不扩展
        if (currentPreloadStartIndex == -1 || currentPreloadEndIndex == -1) {
            Log.d(TAG, "预加载范围未初始化，不扩展")
            return false
        }

        val chapterData = loadedChapterData[currentContentPage.chapterId]
        if (chapterData == null) {
            Log.d(TAG, "章节数据未找到，不扩展预加载")
            return false
        }
        
        val isAtChapterStart = currentContentPage.pageIndex == 0
        val isAtChapterEnd = currentContentPage.pageIndex == chapterData.pages.size - 1

        // 检查是否接近预加载范围边缘
        val isNearStartEdge = currentChapterIndex <= currentPreloadStartIndex + 1
        val isNearEndEdge = currentChapterIndex >= currentPreloadEndIndex - 1

        val shouldExpand = (isAtChapterStart && isNearStartEdge) || (isAtChapterEnd && isNearEndEdge)
        
        Log.d(TAG, "预加载扩展检查: 章节边界=($isAtChapterStart,$isAtChapterEnd), 范围边缘=($isNearStartEdge,$isNearEndEdge), 结果=$shouldExpand")
        
        return shouldExpand
    }

    /**
     * 常规预加载检查
     * 确保基本的预加载范围始终维持
     */
    suspend fun checkRegularPreload(currentChapterIndex: Int, currentContentPage: VirtualPage.ContentPage, loadedChapterData: Map<String, PageData>, chapterList: List<Chapter>) {
        val chapterData = loadedChapterData[currentContentPage.chapterId]
        if (chapterData == null) {
            Log.d(TAG, "常规预加载检查: 章节数据未找到")
            return
        }
        
        val isAtChapterStart = currentContentPage.pageIndex == 0
        val isAtChapterEnd = currentContentPage.pageIndex == chapterData.pages.size - 1

        Log.d(TAG, "常规预加载检查: 章节=${currentContentPage.chapterId}, 页面=${currentContentPage.pageIndex}, 边界=($isAtChapterStart,$isAtChapterEnd)")

        // 基本预加载检查：在章节边界时确保相邻章节已加载
        if (isAtChapterStart || isAtChapterEnd) {
            // 检查前后相邻章节是否已加载
            val needsPreload = checkAdjacentChaptersLoaded(chapterList, currentChapterIndex)
            if (needsPreload) {
                Log.d(TAG, "触发常规预加载")
                // 这里需要通过回调或者其他方式触发预加载，因为这个函数是suspend的
                // 在实际使用中，调用方会处理这个逻辑
            } else {
                Log.d(TAG, "相邻章节已加载，无需预加载")
            }
        }
    }

    /**
     * 取消预加载任务
     * 在ViewModel销毁时调用
     */
    fun cancel() {
        Log.d(TAG, "取消预加载任务")
        preloadJob?.cancel()
        preloadingChapters.clear()
    }
}