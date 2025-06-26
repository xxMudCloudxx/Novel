package com.novel.page.read.usecase

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.service.PaginationService
import com.novel.page.read.viewmodel.PageData
import javax.inject.Inject

class PaginateChapterUseCase @Inject constructor(
    private val paginationService: PaginationService
) {
    fun execute(
        chapter: Chapter,
        content: String,
        readerSettings: ReaderSettings,
        containerSize: IntSize,
        density: Density,
        isFirstChapter: Boolean,
        isLastChapter: Boolean
    ): PageData {
        val pages = paginationService.splitContent(content, containerSize, readerSettings, density)
        return PageData(
            chapterId = chapter.id,
            chapterName = chapter.chapterName,
            content = content,
            pages = pages,
            isFirstChapter = isFirstChapter,
            isLastChapter = isLastChapter,
            hasBookDetailPage = isFirstChapter
        )
    }
} 