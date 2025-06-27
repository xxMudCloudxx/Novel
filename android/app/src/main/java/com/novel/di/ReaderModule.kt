package com.novel.page.read.di

import com.novel.page.read.service.*
import com.novel.page.read.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ReaderModule {



    @Provides
    @ViewModelScoped
    fun provideSplitContentUseCase(
        paginationService: PaginationService,
        chapterService: ChapterService
    ): SplitContentUseCase {
        return SplitContentUseCase(paginationService, chapterService)
    }

    @Provides
    @ViewModelScoped
    fun provideBuildVirtualPagesUseCase(
        chapterService: ChapterService
    ): BuildVirtualPagesUseCase {
        return BuildVirtualPagesUseCase(chapterService)
    }

    @Provides
    @ViewModelScoped
    fun provideInitReaderUseCase(
        settingsService: SettingsService,
        chapterService: ChapterService,
        progressService: ProgressService,
        paginateChapterUseCase: PaginateChapterUseCase,
        preloadChaptersUseCase: PreloadChaptersUseCase,
        splitContentUseCase: SplitContentUseCase,
        paginationService: PaginationService
    ): InitReaderUseCase {
        return InitReaderUseCase(
            settingsService,
            chapterService,
            progressService,
            paginateChapterUseCase,
            preloadChaptersUseCase,
            splitContentUseCase,
            paginationService
        )
    }

    @Provides
    @ViewModelScoped
    fun provideFlipPageUseCase(
        switchChapterUseCase: SwitchChapterUseCase,
        preloadChaptersUseCase: PreloadChaptersUseCase
    ): FlipPageUseCase {
        return FlipPageUseCase(switchChapterUseCase, preloadChaptersUseCase)
    }

    @Provides
    @ViewModelScoped
    fun provideSwitchChapterUseCase(
        chapterService: ChapterService,
        paginateChapterUseCase: PaginateChapterUseCase,
        preloadChaptersUseCase: PreloadChaptersUseCase,
        saveProgressUseCase: SaveProgressUseCase,
        splitContentUseCase: SplitContentUseCase
    ): SwitchChapterUseCase {
        return SwitchChapterUseCase(
            chapterService,
            paginateChapterUseCase,
            preloadChaptersUseCase,
            saveProgressUseCase,
            splitContentUseCase
        )
    }

    @Provides
    @ViewModelScoped
    fun provideSeekProgressUseCase(
        paginationService: PaginationService,
        chapterService: ChapterService,
        paginateChapterUseCase: PaginateChapterUseCase
    ): SeekProgressUseCase {
        return SeekProgressUseCase(paginationService, chapterService, paginateChapterUseCase)
    }

    @Provides
    @ViewModelScoped
    fun provideSaveProgressUseCase(progressService: ProgressService): SaveProgressUseCase {
        return SaveProgressUseCase(progressService)
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateSettingsUseCase(
        settingsService: SettingsService,
        paginateChapterUseCase: PaginateChapterUseCase,
        splitContentUseCase: SplitContentUseCase
    ): UpdateSettingsUseCase {
        return UpdateSettingsUseCase(settingsService, paginateChapterUseCase, splitContentUseCase)
    }

    @Provides
    @ViewModelScoped
    fun providePaginateChapterUseCase(paginationService: PaginationService): PaginateChapterUseCase {
        return PaginateChapterUseCase(paginationService)
    }

    @Provides
    @ViewModelScoped
    fun providePreloadChaptersUseCase(
        chapterService: ChapterService,
        paginationService: PaginationService
    ): PreloadChaptersUseCase {
        return PreloadChaptersUseCase(chapterService, paginationService)
    }

    @Provides
    @ViewModelScoped
    fun provideObservePaginationProgressUseCase(paginationService: PaginationService): ObservePaginationProgressUseCase {
        return ObservePaginationProgressUseCase(paginationService)
    }




}