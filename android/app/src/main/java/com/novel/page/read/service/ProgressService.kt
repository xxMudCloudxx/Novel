package com.novel.page.read.service

import android.util.Log
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.repository.ReadingProgressData
import com.novel.page.read.repository.ReadingProgressRepository
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.ReaderUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进度服务类
 * 
 * 负责阅读进度的计算和保存：
 * 1. 章节内进度计算（基于当前页面在章节中的位置）
 * 2. 全书进度计算（基于页数缓存的全局页码）
 * 3. 阅读进度持久化保存
 * 4. 多种翻页模式的进度适配
 * 
 * 核心特性：
 * - 支持页面级精确进度定位
 * - 兼容纵向滚动和分页模式
 * - 自动处理章节边界的进度连续性
 * - 基于全书页数缓存的统一进度体系
 */
@Singleton
class ProgressService @Inject constructor(
    private val readingProgressRepository: ReadingProgressRepository
) {
    companion object {
        private const val TAG = ReaderLogTags.PROGRESS_SERVICE
    }

    /**
     * 保存阅读进度
     * 
     * 计算并保存当前的阅读位置，包括：
     * - 书籍ID和章节ID
     * - 章节内页面索引
     * - 基于页数缓存的全局页面索引
     * - 章节内进度百分比
     * - 全书进度百分比
     * - 当前翻页效果设置
     * 
     * @param bookId 书籍唯一标识
     * @param chapterId 章节唯一标识
     * @param currentPageIndex 章节内当前页面索引（从0开始，-1表示书籍详情页）
     * @param pageCountCache 全书页数缓存数据，用于计算全局进度
     * @param pageFlipEffect 当前翻页效果设置
     * @param computedReadingProgress 预计算的阅读进度（0.0-1.0）
     * @param currentPageData 当前页面数据，用于计算章节内进度
     */
    suspend fun saveProgress(
        bookId: String,
        chapterId: String,
        currentPageIndex: Int,
        pageCountCache: com.novel.page.read.repository.PageCountCacheData?,
        pageFlipEffect: PageFlipEffect,
        computedReadingProgress: Float,
        currentPageData: com.novel.page.read.viewmodel.PageData?
    ) {
        Log.d(TAG, "保存阅读进度: 书籍=$bookId, 章节=$chapterId, 页面=$currentPageIndex")
        
        val progressData = ReadingProgressData(
            bookId = bookId,
            chapterId = chapterId,
            pageIndex = currentPageIndex,
            globalPageIndex = calculateGlobalPageIndex(pageCountCache, chapterId, currentPageIndex),
            chapterProgress = calculateChapterProgress(currentPageData, currentPageIndex),
            globalProgress = computedReadingProgress,
            pageFlipEffect = pageFlipEffect
        )
        
        Log.d(TAG, "进度数据: 章节进度=${(progressData.chapterProgress * 100).toInt()}%, 全书进度=${(progressData.globalProgress * 100).toInt()}%")
        readingProgressRepository.saveReadingProgress(progressData)
    }

    /**
     * 获取阅读进度
     * 
     * @param bookId 书籍ID
     * @return 阅读进度数据，未找到时返回null
     */
    suspend fun getProgress(bookId: String): ReadingProgressData? {
        Log.d(TAG, "获取阅读进度: bookId=$bookId")
        val progress = readingProgressRepository.getReadingProgress(bookId)
        if (progress != null) {
            Log.d(TAG, "进度恢复: 章节=${progress.chapterId}, 页面=${progress.pageIndex}")
        } else {
            Log.d(TAG, "未找到保存的阅读进度")
        }
        return progress
    }

    /**
     * 计算全局页面索引
     * 
     * 基于页数缓存中的章节页面范围信息，计算当前页面在全书中的绝对位置
     * 
     * @param pageCountCache 页数缓存数据
     * @param chapterId 当前章节ID
     * @param currentPageIndex 章节内页面索引
     * @return 全局页面索引
     */
    private fun calculateGlobalPageIndex(
        pageCountCache: com.novel.page.read.repository.PageCountCacheData?,
        chapterId: String,
        currentPageIndex: Int
    ): Int {
        if (pageCountCache != null) {
            val chapterRange = pageCountCache.chapterPageRanges.find { it.chapterId == chapterId }
            if (chapterRange != null) {
                val globalIndex = chapterRange.startPage + currentPageIndex.coerceAtLeast(0)
                Log.d(TAG, "全局页面索引计算: 章节起始页=${chapterRange.startPage} + 当前页=$currentPageIndex = $globalIndex")
                return globalIndex
            }
        }
        
        Log.d(TAG, "页数缓存未找到，使用章节内索引: $currentPageIndex")
        return currentPageIndex.coerceAtLeast(0)
    }

    /**
     * 计算章节内进度
     * 
     * 基于当前页面在章节中的位置计算进度百分比
     * 
     * @param currentPageData 当前页面数据
     * @param pageIndex 页面索引
     * @return 章节内进度（0.0-1.0）
     */
    private fun calculateChapterProgress(
        currentPageData: com.novel.page.read.viewmodel.PageData?,
        pageIndex: Int
    ): Float {
        if (currentPageData == null) {
            Log.d(TAG, "页面数据为空，章节进度为0")
            return 0f
        }
        
        return when {
            pageIndex == -1 -> {
                Log.d(TAG, "书籍详情页，章节进度为0")
                0f
            }
            currentPageData.pages.isEmpty() -> {
                Log.d(TAG, "章节页面为空，章节进度为0")
                0f
            }
            else -> {
                val progress = (pageIndex + 1).toFloat() / currentPageData.pages.size.toFloat()
                Log.d(TAG, "章节进度计算: (${pageIndex + 1})/${currentPageData.pages.size} = ${(progress * 100).toInt()}%")
                progress
            }
        }
    }
} 