package com.novel.page.read.usecase

import android.util.Log
import com.novel.page.read.service.ChapterService
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import com.novel.page.read.viewmodel.VirtualPage
import javax.inject.Inject

/**
 * 构建虚拟页面用例
 * 
 * 负责构建统一的虚拟页面列表，支持所有翻页模式：
 * 1. 书籍详情页（首章首页前）
 * 2. 章节内容页面
 * 3. 相邻章节页面整合
 * 4. 虚拟页面索引计算和保持
 */
class BuildVirtualPagesUseCase @Inject constructor(
    private val chapterService: ChapterService
) {
    companion object {
        private const val TAG = ReaderLogTags.BUILD_VIRTUAL_PAGES_USE_CASE
    }

    sealed class BuildResult {
        data class Success(
            val virtualPages: List<VirtualPage>,
            val newVirtualPageIndex: Int,
            val loadedChapterData: Map<String, PageData>
        ) : BuildResult()
        
        data class Failure(val error: Throwable) : BuildResult()
    }

    /**
     * 执行虚拟页面构建
     * 
     * @param state 当前阅读器状态
     * @param preserveCurrentIndex 是否保持当前虚拟页面索引
     */
    fun execute(
        state: ReaderUiState,
        preserveCurrentIndex: Boolean = true
    ): BuildResult {
        return try {
            Log.d(TAG, "开始构建虚拟页面: preserve=$preserveCurrentIndex")
            
            val currentChapter = state.currentChapter
            val currentPageData = state.currentPageData
            
            if (currentChapter == null || currentPageData == null) {
                Log.w(TAG, "虚拟页面构建失败: 缺少必要数据")
                return BuildResult.Failure(IllegalStateException("缺少当前章节或页面数据"))
            }

            // 1. 构建虚拟页面列表
            val virtualPages = mutableListOf<VirtualPage>()
            val loadedChapterData = mutableMapOf<String, PageData>()

            // 2. 添加书籍详情页（如果是第一章）
            if (currentPageData.hasBookDetailPage) {
                virtualPages.add(VirtualPage.BookDetailPage)
                Log.d(TAG, "添加书籍详情页")
            }

            // 3. 获取相邻章节数据
            val adjacentChapterData = getAdjacentChapterData(state)
            Log.d(TAG, "获取相邻章节数据: 前章=${adjacentChapterData.first != null}, 后章=${adjacentChapterData.second != null}")

            // 4. 按顺序添加章节页面
            val chapterList = state.chapterList
            val currentChapterIndex = state.currentChapterIndex

            // 添加前一章节页面
            val prevIndex = currentChapterIndex - 1
            if (prevIndex >= 0 && adjacentChapterData.first != null) {
                val prevChapter = chapterList[prevIndex]
                val prevPageData = adjacentChapterData.first!!
                
                prevPageData.pages.forEachIndexed { pageIndex, _ ->
                    virtualPages.add(VirtualPage.ContentPage(prevChapter.id, pageIndex))
                }
                loadedChapterData[prevChapter.id] = prevPageData
                Log.d(TAG, "添加前序章节页面: ${prevChapter.chapterName}, 页数=${prevPageData.pages.size}")
            }

            // 添加当前章节页面
            currentPageData.pages.forEachIndexed { pageIndex, _ ->
                virtualPages.add(VirtualPage.ContentPage(currentChapter.id, pageIndex))
            }
            loadedChapterData[currentChapter.id] = currentPageData
            Log.d(TAG, "添加当前章节页面: ${currentChapter.chapterName}, 页数=${currentPageData.pages.size}")

            // 添加后一章节页面
            val nextIndex = currentChapterIndex + 1
            if (nextIndex < chapterList.size && adjacentChapterData.second != null) {
                val nextChapter = chapterList[nextIndex]
                val nextPageData = adjacentChapterData.second!!
                
                nextPageData.pages.forEachIndexed { pageIndex, _ ->
                    virtualPages.add(VirtualPage.ContentPage(nextChapter.id, pageIndex))
                }
                loadedChapterData[nextChapter.id] = nextPageData
                Log.d(TAG, "添加后续章节页面: ${nextChapter.chapterName}, 页数=${nextPageData.pages.size}")
            }

            // 5. 计算新的虚拟页面索引
            val newVirtualPageIndex = calculateVirtualPageIndex(
                state, 
                virtualPages, 
                loadedChapterData, 
                preserveCurrentIndex
            )

            Log.d(TAG, "虚拟页面构建成功: 总页数=${virtualPages.size}, 当前索引=$newVirtualPageIndex")

            BuildResult.Success(virtualPages, newVirtualPageIndex, loadedChapterData)
            
        } catch (e: Exception) {
            Log.e(TAG, "虚拟页面构建失败", e)
            BuildResult.Failure(e)
        }
    }

    /**
     * 获取相邻章节数据
     * 从当前页面数据或缓存中获取前后章节的分页数据
     */
    private fun getAdjacentChapterData(state: ReaderUiState): Pair<PageData?, PageData?> {
        val currentPageData = state.currentPageData ?: return Pair(null, null)
        val chapterList = state.chapterList
        val currentChapterIndex = state.currentChapterIndex

        // 优先从当前页面数据中获取
        var previousChapterData = currentPageData.previousChapterData
        var nextChapterData = currentPageData.nextChapterData

        // 如果没有，尝试从缓存中获取
        if (previousChapterData == null) {
            val prevIndex = currentChapterIndex - 1
            if (prevIndex >= 0) {
                val prevChapter = chapterList[prevIndex]
                val cachedChapter = chapterService.getCachedChapter(prevChapter.id)
                previousChapterData = cachedChapter?.pageData
                Log.d(TAG, "从缓存获取前序章节数据: ${previousChapterData != null}")
            }
        }

        if (nextChapterData == null) {
            val nextIndex = currentChapterIndex + 1
            if (nextIndex < chapterList.size) {
                val nextChapter = chapterList[nextIndex]
                val cachedChapter = chapterService.getCachedChapter(nextChapter.id)
                nextChapterData = cachedChapter?.pageData
                Log.d(TAG, "从缓存获取后续章节数据: ${nextChapterData != null}")
            }
        }

        return Pair(previousChapterData, nextChapterData)
    }

    /**
     * 计算虚拟页面索引
     * 根据当前状态和是否保持索引来计算合适的虚拟页面索引
     */
    private fun calculateVirtualPageIndex(
        state: ReaderUiState,
        virtualPages: List<VirtualPage>,
        loadedChapterData: Map<String, PageData>,
        preserveCurrentIndex: Boolean
    ): Int {
        if (virtualPages.isEmpty()) {
            Log.w(TAG, "虚拟页面为空，返回索引0")
            return 0
        }

        val currentChapter = state.currentChapter!!
        val currentPageIndex = state.currentPageIndex

        // 如果需要保持当前索引且索引有效
        if (preserveCurrentIndex && state.virtualPageIndex in virtualPages.indices) {
            val currentVirtualPage = virtualPages[state.virtualPageIndex]
            
            // 检查当前虚拟页面是否仍然有效
            when (currentVirtualPage) {
                is VirtualPage.ContentPage -> {
                    if (currentVirtualPage.chapterId == currentChapter.id && 
                        currentVirtualPage.pageIndex == currentPageIndex) {
                        Log.d(TAG, "保持当前虚拟页面索引: ${state.virtualPageIndex}")
                        return state.virtualPageIndex
                    }
                }
                is VirtualPage.BookDetailPage -> {
                    if (currentPageIndex == -1) {
                        Log.d(TAG, "保持书籍详情页索引: ${state.virtualPageIndex}")
                        return state.virtualPageIndex
                    }
                }
                is VirtualPage.ChapterSection -> {
                    // 章节区域处理
                }
            }
        }

        // 重新计算索引
        return when {
            // 书籍详情页
            currentPageIndex == -1 -> {
                val bookDetailIndex = virtualPages.indexOfFirst { it is VirtualPage.BookDetailPage }
                if (bookDetailIndex >= 0) {
                    Log.d(TAG, "定位到书籍详情页: $bookDetailIndex")
                    bookDetailIndex
                } else {
                    Log.d(TAG, "未找到书籍详情页，使用索引0")
                    0
                }
            }
            
            // 内容页面
            else -> {
                val targetPage = VirtualPage.ContentPage(currentChapter.id, currentPageIndex)
                val foundIndex = virtualPages.indexOfFirst { virtualPage ->
                    virtualPage is VirtualPage.ContentPage && 
                    virtualPage.chapterId == targetPage.chapterId && 
                    virtualPage.pageIndex == targetPage.pageIndex
                }
                
                if (foundIndex >= 0) {
                    Log.d(TAG, "定位到内容页面: 章节=${currentChapter.chapterName}, 页面=$currentPageIndex, 虚拟索引=$foundIndex")
                    foundIndex
                } else {
                    // 如果找不到精确匹配，找到当前章节的第一页
                    val chapterFirstPageIndex = virtualPages.indexOfFirst { virtualPage ->
                        virtualPage is VirtualPage.ContentPage && 
                        virtualPage.chapterId == currentChapter.id
                    }
                    
                    if (chapterFirstPageIndex >= 0) {
                        Log.d(TAG, "定位到章节首页: $chapterFirstPageIndex")
                        chapterFirstPageIndex
                    } else {
                        Log.w(TAG, "未找到匹配页面，使用索引0")
                        0
                    }
                }
            }
        }
    }
} 