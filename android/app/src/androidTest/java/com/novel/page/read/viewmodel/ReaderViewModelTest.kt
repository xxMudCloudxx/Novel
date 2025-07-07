//package com.novel.page.read.viewmodel
//
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import app.cash.turbine.test
//import com.google.common.truth.Truth.assertThat
//import com.novel.page.read.components.Chapter
//import com.novel.page.read.service.PaginationService
//import com.novel.page.read.service.SettingsService
//import com.novel.page.read.usecase.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.test.*
//import org.junit.After
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.rules.TestWatcher
//import org.junit.runner.Description
//import org.mockito.kotlin.*
//import org.junit.runner.RunWith
//import androidx.test.ext.junit.runners.AndroidJUnit4
//
//@ExperimentalCoroutinesApi
//@RunWith(AndroidJUnit4::class)
//class ReaderViewModelTest {
//
//    @get:Rule
//    val instantTaskExecutorRule = InstantTaskExecutorRule()
//
//    @get:Rule
//    val mainCoroutineRule = MainCoroutineRule()
//
//    private lateinit var viewModel: ReaderViewModel
//
//    // Mocks
//    private val initReaderUseCase: InitReaderUseCase = mock()
//    private val flipPageUseCase: FlipPageUseCase = mock()
//    private val switchChapterUseCase: SwitchChapterUseCase = mock()
//    private val seekProgressUseCase: SeekProgressUseCase = mock()
//    private val updateSettingsUseCase: UpdateSettingsUseCase = mock()
//    private val saveProgressUseCase: SaveProgressUseCase = mock()
//    private val preloadChaptersUseCase: PreloadChaptersUseCase = mock()
//    private val buildVirtualPagesUseCase: BuildVirtualPagesUseCase = mock()
//    private val splitContentUseCase: SplitContentUseCase = mock()
//    private val paginationService: PaginationService = mock()
//    private val settingsService: SettingsService = mock()
//    private val observePaginationProgressUseCase: ObservePaginationProgressUseCase = mock()
//
//    @Before
//    fun setUp() {
//        whenever(observePaginationProgressUseCase.execute()).thenReturn(kotlinx.coroutines.flow.emptyFlow())
//
//        viewModel = ReaderViewModel(
//            initReaderUseCase,
//            flipPageUseCase,
//            switchChapterUseCase,
//            seekProgressUseCase,
//            updateSettingsUseCase,
//            saveProgressUseCase,
//            preloadChaptersUseCase,
//            buildVirtualPagesUseCase,
//            splitContentUseCase,
//            paginationService,
//            settingsService,
//            observePaginationProgressUseCase
//        )
//    }
//
//    @Test
//    fun `InitReader intent should trigger InitReaderUseCase and update state on success`() = runTest {
//        // Given
//        val bookId = "testBook"
//        val chapterId = "testChapter"
//        val intent = ReaderIntent.InitReader(bookId, chapterId)
//        val mockResult = InitReaderUseCase.InitResult(
//            chapterList = listOf(Chapter(id = chapterId, chapterName = "Chapter 1")),
//            initialChapter = Chapter(id = chapterId, chapterName = "Chapter 1"),
//            initialChapterIndex = 0,
//            initialPageData = PageData(chapterId = chapterId, chapterName = "Chapter 1", content = "content", pages = listOf("page1")),
//            initialPageIndex = 0,
//            settings = com.novel.page.read.components.ReaderSettings.getDefault(),
//            pageCountCache = null
//        )
//
//        whenever(initReaderUseCase.execute(any(), any(), any(), any())).thenReturn(Result.success(mockResult))
//        whenever(buildVirtualPagesUseCase.execute(any(), any())).thenReturn(
//            BuildVirtualPagesUseCase.BuildResult.Success(listOf(VirtualPage.ContentPage(chapterId, 0)), 0, emptyMap())
//        )
//
//        // When
//        viewModel.sendIntent(intent)
//
//        // Then
//        viewModel.state.test {
//            val successState = awaitItem()
//            assertThat(successState.isLoading).isFalse()
//            assertThat(successState.currentChapter?.id).isEqualTo(chapterId)
//            verify(initReaderUseCase).execute(eq(bookId), eq(chapterId), any(), any())
//        }
//    }
//
//    @Test
//    fun `PageFlip intent should trigger FlipPageUseCase`() = runTest {
//        // Given
//        val intent = ReaderIntent.PageFlip(FlipDirection.NEXT)
//        whenever(flipPageUseCase.execute(any(), any(), any())).thenReturn(FlipPageUseCase.FlipResult.NoOp)
//
//        // When
//        viewModel.sendIntent(intent)
//
//        // Then
//        verify(flipPageUseCase).execute(any(), eq(FlipDirection.NEXT), any())
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//}
//
//@ExperimentalCoroutinesApi
//class MainCoroutineRule(
//    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())
//) : TestWatcher() {
//    override fun starting(description: Description) {
//        super.starting(description)
//        Dispatchers.setMain(testDispatcher)
//    }
//
//    override fun finished(description: Description) {
//        super.finished(description)
//        Dispatchers.resetMain()
//    }
//}