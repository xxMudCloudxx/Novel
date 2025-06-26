package com.novel.page.read.usecase

import com.novel.page.read.service.ProgressService
import com.novel.page.read.viewmodel.ReaderUiState
import javax.inject.Inject

class SaveProgressUseCase @Inject constructor(
    private val progressService: ProgressService
) {
    suspend fun execute(state: ReaderUiState) {
        val currentChapter = state.currentChapter ?: return
        progressService.saveProgress(
            bookId = state.bookId,
            chapterId = currentChapter.id,
            currentPageIndex = state.currentPageIndex,
            pageCountCache = state.pageCountCache,
            pageFlipEffect = state.readerSettings.pageFlipEffect,
            computedReadingProgress = state.computedReadingProgress,
            currentPageData = state.currentPageData
        )
    }
} 