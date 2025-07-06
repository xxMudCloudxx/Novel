package com.novel.page.read.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.novel.page.read.components.Chapter
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.service.PaginationService
import com.novel.page.read.service.SettingsService
import com.novel.page.read.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
class ReaderViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: ReaderViewModel

    private val initReaderUseCase: InitReaderUseCase = mock()
    private val flipPageUseCase: FlipPageUseCase = mock()
    private val switchChapterUseCase: SwitchChapterUseCase = mock()
    private val seekProgressUseCase: SeekProgressUseCase = mock()
    private val updateSettingsUseCase: UpdateSettingsUseCase = mock()
    private val saveProgressUseCase: SaveProgressUseCase = mock()
    private val preloadChaptersUseCase: PreloadChaptersUseCase = mock()
    private val buildVirtualPagesUseCase: BuildVirtualPagesUseCase = mock()
    private val splitContentUseCase: SplitContentUseCase = mock()
    private val paginationService: PaginationService = mock()
    private val settingsService: SettingsService = mock()
    private val observePaginationProgressUseCase: ObservePaginationProgressUseCase = mock()

    @Before
    fun setUp() {
        whenever(observePaginationProgressUseCase.execute()).thenReturn(emptyFlow())

        viewModel = ReaderViewModel(
            initReaderUseCase,
            flipPageUseCase,
            switchChapterUseCase,
            seekProgressUseCase,
            updateSettingsUseCase,
            saveProgressUseCase,
            preloadChaptersUseCase,
            buildVirtualPagesUseCase,
            splitContentUseCase,
            paginationService,
            settingsService,
            observePaginationProgressUseCase
        )
    }
    
    @After
    fun tearDown() {
        verify(observePaginationProgressUseCase).execute()
        validateMockitoUsage()
    }

    @Test
    fun `intent InitReader success - should update state from loading to success`() = runTest {
        // Given
        val bookId = "book1"
        val chapterId = "chapter1"
        val intent = ReaderIntent.InitReader(bookId, chapterId)
        val mockChapter = Chapter(id = chapterId, chapterName = "Chapter 1")
        val mockPageData = PageData(chapterId, "Chapter 1", "content", listOf("page1"), isFirstChapter = true, isLastChapter = false)
        val mockResult = InitReaderResult(
            settings = ReaderSettings.getDefault(),
            chapterList = listOf(mockChapter),
            initialChapter = mockChapter,
            initialChapterIndex = 0,
            initialPageData = mockPageData,
            initialPageIndex = 0,
            pageCountCache = null
        )
        whenever(initReaderUseCase.execute(any(), any(), any(), any())).thenReturn(Result.success(mockResult))
        whenever(buildVirtualPagesUseCase.execute(any(), any())).thenReturn(BuildVirtualPagesUseCase.BuildResult.Success(
            virtualPages = listOf(VirtualPage.ContentPage(chapterId, 0)),
            newVirtualPageIndex = 0,
            loadedChapterData = mapOf(chapterId to mockPageData)
        ))


        // Then
        viewModel.state.test {
            // Initial State
            assertThat(awaitItem().isLoading).isFalse()

            // Send Intent
            viewModel.sendIntent(intent)

            // Loading State
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()
            assertThat(loadingState.bookId).isEqualTo(bookId)

            // Success State
            val successState = awaitItem()
            assertThat(successState.isSuccess).isTrue()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.currentChapter).isEqualTo(mockChapter)
            assertThat(successState.currentPageIndex).isEqualTo(0)
            
            // Virtual page build state
            val virtualPageState = awaitItem()
            assertThat(virtualPageState.virtualPages).isNotEmpty()

            cancelAndIgnoreRemainingEvents()
        }

        // Verify
        verify(initReaderUseCase).execute(eq(bookId), eq(chapterId), any(), any())
    }
    
    @Test
    fun `intent InitReader failure - should update state with error`() = runTest {
         // Given
        val bookId = "book1"
        val chapterId = "chapter1"
        val intent = ReaderIntent.InitReader(bookId, chapterId)
        val exception = RuntimeException("Failed to load")
        whenever(initReaderUseCase.execute(any(), any(), any(), any())).thenReturn(Result.failure(exception))

        // Then
        viewModel.state.test {
            // Initial State
            awaitItem()

            // Send Intent
            viewModel.sendIntent(intent)

            // Loading State
            assertThat(awaitItem().isLoading).isTrue()

            // Error State
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.hasError).isTrue()
            assertThat(errorState.error).isEqualTo("Failed to load")

            cancelAndIgnoreRemainingEvents()
        }
        
        viewModel.effect.test {
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(ReaderEffect.ShowErrorDialog::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify
        verify(initReaderUseCase).execute(eq(bookId), eq(chapterId), any(), any())
    }
} 