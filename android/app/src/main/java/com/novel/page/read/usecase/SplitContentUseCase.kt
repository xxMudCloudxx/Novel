package com.novel.page.read.usecase

import android.util.Log
import com.novel.page.read.service.ChapterService
import com.novel.page.read.service.PaginationService
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import javax.inject.Inject

/**
 * 内容分割用例
 * 
 * 负责将章节内容分割成页面，并处理相邻章节数据：
 * 1. 当前章节内容分页
 * 2. 相邻章节数据加载和分页
 * 3. 进度恢复和页面索引计算
 * 4. 书籍信息加载（首章）
 */
class SplitContentUseCase @Inject constructor(
    private val paginationService: PaginationService,
    private val chapterService: ChapterService
) {
    companion object {
        private const val TAG = ReaderLogTags.SPLIT_CONTENT_USE_CASE
    }

    sealed class SplitResult {
        data class Success(
            val pageData: PageData,
            val safePageIndex: Int
        ) : SplitResult()
        
        data class Failure(val error: Throwable) : SplitResult()
    }

    /**
     * 执行内容分割
     * 
     * @param state 当前阅读器状态
     * @param restoreProgress 需要恢复的进度（0.0-1.0），用于重新分页后恢复位置
     * @param includeAdjacentChapters 是否包含相邻章节数据
     */
    suspend fun execute(
        state: ReaderUiState,
        restoreProgress: Float? = null,
        includeAdjacentChapters: Boolean = false
    ): SplitResult {
        return try {
            val currentChapter = state.currentChapter
            val chapterContent = state.bookContent
            val density = state.density
            
            if (currentChapter == null || chapterContent.isEmpty() || density == null) {
                Log.w(TAG, "内容分割失败: 缺少必要参数")
                return SplitResult.Failure(IllegalStateException("缺少必要的分割参数"))
            }

            Log.d(TAG, "开始内容分割: 章节=${currentChapter.chapterName}, 内容长度=${chapterContent.length}")

            // 1. 分割当前章节内容
            val pages = paginationService.splitContent(
                content = chapterContent,
                containerSize = state.containerSize,
                readerSettings = state.readerSettings,
                density = density
            )

            Log.d(TAG, "章节分页完成: 页数=${pages.size}")

            // 2. 创建基础PageData
            var pageData = PageData(
                chapterId = currentChapter.id,
                chapterName = currentChapter.chapterName,
                content = chapterContent,
                pages = pages,
                isFirstChapter = state.isFirstChapter,
                isLastChapter = state.isLastChapter,
                hasBookDetailPage = state.isFirstChapter
            )

            // 3. 加载书籍信息（如果是第一章）
            if (state.isFirstChapter) {
                Log.d(TAG, "加载书籍信息")
                val bookInfo = chapterService.loadBookInfo(state.bookId)
                if (bookInfo != null) {
                    pageData = pageData.copy(bookInfo = bookInfo)
                    Log.d(TAG, "书籍信息加载成功: ${bookInfo.bookName}")
                } else {
                    Log.w(TAG, "书籍信息加载失败")
                }
            }

            // 4. 加载相邻章节数据
            if (includeAdjacentChapters) {
                Log.d(TAG, "开始加载相邻章节数据")
                val (previousChapterData, nextChapterData) = loadAdjacentChapterData(state)
                pageData = pageData.copy(
                    previousChapterData = previousChapterData,
                    nextChapterData = nextChapterData
                )
                Log.d(TAG, "相邻章节数据加载完成: 前章=${previousChapterData != null}, 后章=${nextChapterData != null}")
            }

            // 4. 计算安全的页面索引
            val safePageIndex = calculateSafePageIndex(state, pageData, restoreProgress)
            
            Log.d(TAG, "内容分割成功: 最终页面索引=$safePageIndex")
            
            // 5. 更新章节缓存中的分页数据
            chapterService.setCachedChapterPageData(currentChapter.id, pageData)

            SplitResult.Success(pageData, safePageIndex)
            
        } catch (e: Exception) {
            Log.e(TAG, "内容分割失败", e)
            SplitResult.Failure(e)
        }
    }

    /**
     * 加载相邻章节数据
     * 加载并分页前后章节的内容
     */
    private suspend fun loadAdjacentChapterData(state: ReaderUiState): Pair<PageData?, PageData?> {
        val chapterList = state.chapterList
        val currentChapterIndex = state.currentChapterIndex
        val density = state.density ?: return Pair(null, null)

        var previousChapterData: PageData? = null
        var nextChapterData: PageData? = null

        // 加载前一章节
        val prevIndex = currentChapterIndex - 1
        if (prevIndex >= 0) {
            try {
                val prevChapter = chapterList[prevIndex]
                Log.d(TAG, "加载前序章节: ${prevChapter.chapterName}")
                
                val prevContent = chapterService.getChapterContent(prevChapter.id)
                if (prevContent != null) {
                    val prevPages = paginationService.splitContent(
                        content = prevContent.content,
                        containerSize = state.containerSize,
                        readerSettings = state.readerSettings,
                        density = density
                    )
                    
                    previousChapterData = PageData(
                        chapterId = prevChapter.id,
                        chapterName = prevChapter.chapterName,
                        content = prevContent.content,
                        pages = prevPages,
                        isFirstChapter = prevIndex == 0,
                        isLastChapter = false
                    )
                    
                    // 更新缓存
                    chapterService.setCachedChapterPageData(prevChapter.id, previousChapterData)
                    Log.d(TAG, "前序章节加载成功: 页数=${prevPages.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "前序章节加载失败", e)
            }
        }

        // 加载后一章节
        val nextIndex = currentChapterIndex + 1
        if (nextIndex < chapterList.size) {
            try {
                val nextChapter = chapterList[nextIndex]
                Log.d(TAG, "加载后续章节: ${nextChapter.chapterName}")
                
                val nextContent = chapterService.getChapterContent(nextChapter.id)
                if (nextContent != null) {
                    val nextPages = paginationService.splitContent(
                        content = nextContent.content,
                        containerSize = state.containerSize,
                        readerSettings = state.readerSettings,
                        density = density
                    )
                    
                    nextChapterData = PageData(
                        chapterId = nextChapter.id,
                        chapterName = nextChapter.chapterName,
                        content = nextContent.content,
                        pages = nextPages,
                        isFirstChapter = false,
                        isLastChapter = nextIndex == chapterList.size - 1
                    )
                    
                    // 更新缓存
                    chapterService.setCachedChapterPageData(nextChapter.id, nextChapterData)
                    Log.d(TAG, "后续章节加载成功: 页数=${nextPages.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "后续章节加载失败", e)
            }
        }

        return Pair(previousChapterData, nextChapterData)
    }

    /**
     * 计算安全的页面索引
     * 根据当前状态、分页结果和进度恢复需求计算合适的页面索引
     */
    private fun calculateSafePageIndex(
        state: ReaderUiState,
        pageData: PageData,
        restoreProgress: Float?
    ): Int {
        val pages = pageData.pages
        if (pages.isEmpty()) {
            Log.w(TAG, "页面为空，返回索引0")
            return 0
        }

        return when {
            // 如果有恢复进度，按进度计算
            restoreProgress != null -> {
                val progressIndex = (restoreProgress * pages.size).toInt()
                val safeIndex = progressIndex.coerceIn(0, pages.size - 1)
                Log.d(TAG, "按进度恢复页面索引: 进度=$restoreProgress -> 索引=$safeIndex")
                safeIndex
            }
            
            // 如果当前页面索引有效，保持不变
            state.currentPageIndex in 0 until pages.size -> {
                Log.d(TAG, "保持当前页面索引: ${state.currentPageIndex}")
                state.currentPageIndex
            }
            
            // 如果是书籍详情页（索引-1），保持不变
            state.currentPageIndex == -1 && pageData.hasBookDetailPage -> {
                Log.d(TAG, "保持书籍详情页索引: -1")
                -1
            }
            
            // 其他情况，使用第一页
            else -> {
                Log.d(TAG, "使用默认页面索引: 0")
                0
            }
        }
    }
} 