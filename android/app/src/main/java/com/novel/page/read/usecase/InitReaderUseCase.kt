package com.novel.page.read.usecase

import com.novel.page.read.components.Chapter
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.service.ChapterService
import com.novel.page.read.service.PaginationService
import com.novel.page.read.service.ProgressService
import com.novel.page.read.service.SettingsService
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 初始化阅读器结果数据
 * 包含初始化成功后需要的所有数据
 */
data class InitReaderResult(
    val settings: ReaderSettings,
    val chapterList: List<Chapter>,
    val initialChapter: Chapter,
    val initialChapterIndex: Int,
    val initialPageData: PageData,
    val initialPageIndex: Int,
    val pageCountCache: com.novel.page.read.repository.PageCountCacheData?
)

/**
 * 初始化阅读器用例
 * 
 * 负责阅读器的完整初始化流程：
 * 1. 加载用户设置和章节列表
 * 2. 确定初始章节（从指定章节或恢复进度）
 * 3. 加载并分页初始章节内容
 * 4. 启动预加载和后台分页任务
 * 5. 返回初始化结果
 */
class InitReaderUseCase @Inject constructor(
    private val settingsService: SettingsService,
    private val chapterService: ChapterService,
    private val progressService: ProgressService,
    private val paginateChapterUseCase: PaginateChapterUseCase,
    private val preloadChaptersUseCase: PreloadChaptersUseCase,
    private val splitContentUseCase: SplitContentUseCase,
    private val paginationService: PaginationService
) {
    /**
     * 执行阅读器初始化
     * 
     * @param bookId 书籍ID
     * @param chapterId 指定章节ID（可选，为null时恢复上次阅读进度）
     * @param initialState 初始状态（包含容器尺寸等信息）
     * @param scope 协程作用域
     * @return 初始化结果
     */
    suspend fun execute(
        bookId: String,
        chapterId: String?,
        initialState: ReaderUiState,
        scope: CoroutineScope
    ): Result<InitReaderResult> {
        return try {
            // 1. 加载设置和章节列表
            val settings = settingsService.loadSettings()
            val chapterList = chapterService.getChapterList(bookId)
            if (chapterList.isEmpty()) {
                return Result.failure(Exception("未找到章节列表"))
            }

            // 2. 确定初始章节和页面索引
            val progress = chapterId?.let { null } ?: progressService.getProgress(bookId)
            val initialChapter = chapterList.find { it.id == (chapterId ?: progress?.chapterId) } ?: chapterList.first()
            val initialChapterIndex = chapterList.indexOf(initialChapter)
            val initialPageIndex = progress?.pageIndex ?: 0

            // 3. 获取初始章节内容
            val initialContent = chapterService.getChapterContent(initialChapter.id)
                ?: return Result.failure(Exception("章节内容加载失败"))

            // 4. 创建分页状态
            val stateForSplitting = initialState.copy(
                bookId = bookId,
                chapterList = chapterList,
                currentChapter = initialChapter,
                currentChapterIndex = initialChapterIndex,
                bookContent = initialContent.content,
                readerSettings = settings,
                currentPageIndex = initialPageIndex
            )

            // 5. 使用SplitContentUseCase进行内容分割
            val splitResult = splitContentUseCase.execute(
                state = stateForSplitting,
                restoreProgress = null,
                includeAdjacentChapters = true
            )

            val (initialPageData, safePageIndex) = when (splitResult) {
                is SplitContentUseCase.SplitResult.Success -> {
                    splitResult.pageData to splitResult.safePageIndex
                }
                is SplitContentUseCase.SplitResult.Failure -> {
                    // 降级到基础分页
                    val basicPageData = paginateChapterUseCase.execute(
                        chapter = initialChapter,
                        content = initialContent.content,
                        readerSettings = settings,
                        containerSize = initialState.containerSize,
                        density = initialState.density!!,
                        isFirstChapter = initialChapterIndex == 0,
                        isLastChapter = initialChapterIndex == chapterList.size - 1
                    )
                    basicPageData to initialPageIndex.coerceIn(0, basicPageData.pages.size - 1)
                }
            }
            
            // 6. 启动预加载任务
            preloadChaptersUseCase.execute(scope, chapterList, initialChapter.id)

            // 7. 启动后台全书分页计算（非纵向滚动模式）
            if (initialState.containerSize.width > 0 && initialState.containerSize.height > 0) {
                scope.launch {
                    paginationService.fetchAllBookContentAndPaginateInBackground(
                        bookId = bookId,
                        chapterList = chapterList,
                        readerSettings = settings,
                        containerSize = initialState.containerSize,
                        density = initialState.density!!
                    )
                }
            }

            // 8. 立即获取或创建页数缓存，确保首次打开时页数能正确显示
            val pageCountCache = paginationService.getPageCountCache(
                bookId = bookId,
                fontSize = settings.fontSize,
                containerSize = initialState.containerSize
            )

            Result.success(
                InitReaderResult(
                    settings = settings,
                    chapterList = chapterList,
                    initialChapter = initialChapter,
                    initialChapterIndex = initialChapterIndex,
                    initialPageData = initialPageData,
                    initialPageIndex = safePageIndex,
                    pageCountCache = pageCountCache
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 