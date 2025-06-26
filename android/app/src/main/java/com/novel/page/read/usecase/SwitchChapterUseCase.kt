package com.novel.page.read.usecase

import com.novel.page.read.service.ChapterService
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class SwitchChapterUseCase @Inject constructor(
    private val chapterService: ChapterService,
    private val paginateChapterUseCase: PaginateChapterUseCase,
    private val preloadChaptersUseCase: PreloadChaptersUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val splitContentUseCase: SplitContentUseCase
) {

    sealed class SwitchResult {
        data class Success(
            val newChapterIndex: Int,
            val pageData: PageData,
            val initialPageIndex: Int = 0
        ) : SwitchResult()
        data class Failure(val error: Throwable) : SwitchResult()
        data object NoOp : SwitchResult()
    }

    suspend fun execute(
        state: ReaderUiState,
        newChapterId: String,
        scope: CoroutineScope,
        flipDirection: FlipDirection? = null
    ): SwitchResult {
        val currentChapter = state.currentChapter ?: return SwitchResult.NoOp
        if (currentChapter.id == newChapterId) {
            return SwitchResult.NoOp
        }

        val newChapterIndex = state.chapterList.indexOfFirst { it.id == newChapterId }
        if (newChapterIndex == -1) {
            return SwitchResult.Failure(IllegalArgumentException("Chapter not found in list"))
        }
        val newChapter = state.chapterList[newChapterIndex]

        // 1. Save progress for the current chapter
        saveProgressUseCase.execute(state)

        return try {
            // 2. Fetch new chapter content
            val chapterContent = chapterService.getChapterContent(newChapterId)
                ?: return SwitchResult.Failure(IllegalStateException("Chapter content not available"))

            // 3. Create state for content splitting
            val stateForSplitting = state.copy(
                currentChapter = newChapter,
                currentChapterIndex = newChapterIndex,
                bookContent = chapterContent.content,
                currentPageIndex = when (flipDirection) {
                    FlipDirection.PREVIOUS -> -1 // Will be set to last page after splitting
                    FlipDirection.NEXT -> 0
                    else -> 0
                }
            )

            // 4. Split content using SplitContentUseCase
            val splitResult = splitContentUseCase.execute(
                state = stateForSplitting,
                restoreProgress = null,
                includeAdjacentChapters = true
            )

            val (pageData, initialPageIndex) = when (splitResult) {
                is SplitContentUseCase.SplitResult.Success -> {
                    val adjustedPageIndex = when (flipDirection) {
                        FlipDirection.PREVIOUS -> (splitResult.pageData.pages.size - 1).coerceAtLeast(0)
                        else -> splitResult.safePageIndex
                    }
                    splitResult.pageData to adjustedPageIndex
                }
                is SplitContentUseCase.SplitResult.Failure -> {
                    // Fallback to basic pagination
                    val basicPageData = paginateChapterUseCase.execute(
                        chapter = newChapter,
                        content = chapterContent.content,
                        readerSettings = state.readerSettings,
                        containerSize = state.containerSize,
                        density = state.density!!,
                        isFirstChapter = newChapterIndex == 0,
                        isLastChapter = newChapterIndex == state.chapterList.size - 1
                    )
                    val adjustedPageIndex = when (flipDirection) {
                        FlipDirection.PREVIOUS -> (basicPageData.pages.size - 1).coerceAtLeast(0)
                        else -> 0
                    }
                    basicPageData to adjustedPageIndex
                }
            }

            // 5. Trigger preloading for surrounding chapters
            preloadChaptersUseCase.execute(scope, state.chapterList, newChapterId)

            SwitchResult.Success(newChapterIndex, pageData, initialPageIndex)
        } catch (e: Exception) {
            SwitchResult.Failure(e)
        }
    }
} 