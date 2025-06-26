package com.novel.page.read.usecase

import com.novel.page.read.service.ChapterService
import com.novel.page.read.service.PaginationService
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import javax.inject.Inject

class SeekProgressUseCase @Inject constructor(
    private val paginationService: PaginationService,
    private val chapterService: ChapterService,
    private val paginateChapterUseCase: PaginateChapterUseCase
) {

    sealed class SeekResult {
        data class Success(
            val newChapterIndex: Int,
            val newPageData: PageData,
            val newPageIndex: Int
        ) : SeekResult()
        data class Failure(val error: Throwable) : SeekResult()
        data object NoOp : SeekResult()
    }

    suspend fun execute(
        progress: Float,
        state: ReaderUiState
    ): SeekResult {
        val pageCountCache = state.pageCountCache ?: return SeekResult.NoOp
        if (pageCountCache.totalPages <= 0) return SeekResult.NoOp
        
        val targetGlobalPage = (progress * pageCountCache.totalPages).toInt().coerceIn(0, pageCountCache.totalPages - 1)
        val targetChapterInfo = paginationService.findChapterByAbsolutePage(pageCountCache, targetGlobalPage)
            ?: return SeekResult.Failure(IllegalStateException("Could not find chapter for progress"))

        val (targetChapterId, targetPageIndex) = targetChapterInfo
        
        if (targetChapterId == state.currentChapter?.id) {
            // It's in the same chapter, we just need to update the index.
            // But the current design doesn't allow returning just an index.
            // So we'll fall through and treat it as a chapter switch to itself.
        }

        val targetChapterIndex = state.chapterList.indexOfFirst { it.id == targetChapterId }
        if (targetChapterIndex == -1) {
            return SeekResult.Failure(IllegalArgumentException("Chapter not found in list"))
        }
        val targetChapter = state.chapterList[targetChapterIndex]

        return try {
            val chapterContent = chapterService.getChapterContent(targetChapterId)
                ?: return SeekResult.Failure(IllegalStateException("Could not load chapter content"))

            val newPageData = paginateChapterUseCase.execute(
                chapter = targetChapter,
                content = chapterContent.content,
                readerSettings = state.readerSettings,
                containerSize = state.containerSize,
                density = state.density!!,
                isFirstChapter = targetChapterIndex == 0,
                isLastChapter = targetChapterIndex == state.chapterList.size - 1
            )
            
            SeekResult.Success(targetChapterIndex, newPageData, targetPageIndex)
        } catch(e: Exception) {
            SeekResult.Failure(e)
        }
    }
} 