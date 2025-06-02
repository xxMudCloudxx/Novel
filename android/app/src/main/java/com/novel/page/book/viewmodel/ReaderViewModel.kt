package com.novel.page.book.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.page.book.components.Chapter
import com.novel.page.book.components.ReaderSettings
import com.novel.page.component.BaseViewModel
import com.novel.utils.network.ApiService
import com.novel.utils.network.api.front.BookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * 阅读器UI状态
 */
data class ReaderUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String = "",
    val bookId: String = "",
    val chapterList: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val bookContent: String = "",
    val readingProgress: Float = 0f,
    val readerSettings: ReaderSettings = ReaderSettings()
) {
    val isSuccess: Boolean get() = !isLoading && !hasError && currentChapter != null
    val isEmpty: Boolean get() = !isLoading && !hasError && chapterList.isEmpty()
}

/**
 * 阅读器Intent(用户操作意图)
 */
sealed class ReaderIntent {
    data class InitReader(val bookId: String, val chapterId: String?) : ReaderIntent()
    object LoadChapterList : ReaderIntent()
    data class LoadChapterContent(val chapterId: String) : ReaderIntent()
    object PreviousChapter : ReaderIntent()
    object NextChapter : ReaderIntent()
    data class SwitchToChapter(val chapterId: String) : ReaderIntent()
    data class SeekToProgress(val progress: Float) : ReaderIntent()
    data class UpdateSettings(val settings: ReaderSettings) : ReaderIntent()
    object NextPage : ReaderIntent()
    object PreviousPage : ReaderIntent()
    object Retry : ReaderIntent()
}

/**
 * 阅读器ViewModel - MVI架构
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookService: BookService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /**
     * 处理用户意图
     */
    fun handleIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.InitReader -> initReaderInternal(intent.bookId, intent.chapterId)
            is ReaderIntent.LoadChapterList -> loadChapterListMethod()
            is ReaderIntent.LoadChapterContent -> loadChapterContentMethod(intent.chapterId)
            is ReaderIntent.PreviousChapter -> previousChapterInternal()
            is ReaderIntent.NextChapter -> nextChapterInternal()
            is ReaderIntent.SwitchToChapter -> switchToChapterInternal(intent.chapterId)
            is ReaderIntent.SeekToProgress -> seekToProgressInternal(intent.progress)
            is ReaderIntent.UpdateSettings -> updateSettingsInternal(intent.settings)
            is ReaderIntent.NextPage -> nextPageInternal()
            is ReaderIntent.PreviousPage -> previousPageInternal()
            is ReaderIntent.Retry -> retryInternal()
        }
    }

    // 便捷方法，保持与UI层的兼容性
    fun initReader(bookId: String, chapterId: String?) = handleIntent(ReaderIntent.InitReader(bookId, chapterId))
    fun previousChapter() = handleIntent(ReaderIntent.PreviousChapter)
    fun nextChapter() = handleIntent(ReaderIntent.NextChapter)
    fun switchToChapter(chapterId: String) = handleIntent(ReaderIntent.SwitchToChapter(chapterId))
    fun seekToProgress(progress: Float) = handleIntent(ReaderIntent.SeekToProgress(progress))
    fun updateSettings(settings: ReaderSettings) = handleIntent(ReaderIntent.UpdateSettings(settings))
    fun nextPage() = handleIntent(ReaderIntent.NextPage)
    fun previousPage() = handleIntent(ReaderIntent.PreviousPage)
    fun retry() = handleIntent(ReaderIntent.Retry)

    /**
     * 初始化阅读器
     */
    private fun initReaderInternal(bookId: String, chapterId: String?) {
        viewModelScope.launchWithLoading {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    hasError = false,
                    bookId = bookId
                )

                // 加载章节列表
                loadChapterListInternal()

                // 如果指定了章节ID，加载该章节；否则加载第一章
                val targetChapterId = chapterId ?: _uiState.value.chapterList.firstOrNull()?.id
                if (targetChapterId != null) {
                    loadChapterContentInternal(targetChapterId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = "未找到可用章节"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "初始化失败"
                )
            }
        }
    }

    /**
     * 加载章节列表
     */
    private fun loadChapterListMethod() {
        viewModelScope.launchWithLoading {
            loadChapterListInternal()
        }
    }

    private suspend fun loadChapterListInternal() {
        val bookId = _uiState.value.bookId
        if (bookId.isEmpty()) return

        try {
            val bookIdLong = bookId.toLong()
            val response = bookService.getBookChaptersBlocking(bookIdLong)
            
            if (response.code == "00000" && response.data != null) {
                val chapters = response.data.map { chapter ->
                    Chapter(
                        id = chapter.id.toString(),
                        chapterName = chapter.chapterName,
                        chapterNum = chapter.chapterNum.toString(),
                        isVip = if (chapter.isVip == 1) "1" else "0"
                    )
                }
                
                _uiState.value = _uiState.value.copy(chapterList = chapters)
            } else {
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = response.message ?: "加载章节列表失败"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "加载章节列表失败: ${e.message}"
            )
        }
    }

    /**
     * 加载章节内容
     */
    private fun loadChapterContentMethod(chapterId: String) {
        viewModelScope.launchWithLoading {
            loadChapterContentInternal(chapterId)
        }
    }

    private suspend fun loadChapterContentInternal(chapterId: String) {
        try {
            val chapterIdLong = chapterId.toLong()
            val response = bookService.getBookContentBlocking(chapterIdLong)
            
            if (response.code == "00000" && response.data != null) {
                val data = response.data
                val chapterInfo = data.chapterInfo
                val bookContent = data.bookContent
                
                val currentChapter = Chapter(
                    id = chapterInfo.id.toString(),
                    chapterName = chapterInfo.chapterName,
                    chapterNum = chapterInfo.chapterNum.toString(),
                    isVip = if (chapterInfo.isVip == 1) "1" else "0"
                )
                
                val currentIndex = _uiState.value.chapterList.indexOfFirst { it.id == chapterId }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = false,
                    currentChapter = currentChapter,
                    currentChapterIndex = if (currentIndex >= 0) currentIndex else 0,
                    bookContent = bookContent,
                    readingProgress = 0f
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    hasError = true,
                    error = response.message ?: "加载章节内容失败"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "加载章节内容失败: ${e.message}"
            )
        }
    }

    /**
     * 上一章
     */
    private fun previousChapterInternal() {
        val currentIndex = _uiState.value.currentChapterIndex
        val chapterList = _uiState.value.chapterList
        
        if (currentIndex > 0 && chapterList.isNotEmpty()) {
            val previousChapter = chapterList[currentIndex - 1]
            loadChapterContentMethod(previousChapter.id)
        }
    }

    /**
     * 下一章
     */
    private fun nextChapterInternal() {
        val currentIndex = _uiState.value.currentChapterIndex
        val chapterList = _uiState.value.chapterList
        
        if (currentIndex < chapterList.size - 1 && chapterList.isNotEmpty()) {
            val nextChapter = chapterList[currentIndex + 1]
            loadChapterContentMethod(nextChapter.id)
        }
    }

    /**
     * 切换到指定章节
     */
    private fun switchToChapterInternal(chapterId: String) {
        loadChapterContentMethod(chapterId)
    }

    /**
     * 跳转到指定进度
     */
    private fun seekToProgressInternal(progress: Float) {
        _uiState.value = _uiState.value.copy(readingProgress = progress)
        // 这里可以实现具体的页面跳转逻辑
    }

    /**
     * 更新阅读器设置
     */
    private fun updateSettingsInternal(settings: ReaderSettings) {
        _uiState.value = _uiState.value.copy(readerSettings = settings)
        // 这里可以保存设置到本地存储
    }

    /**
     * 下一页 (在当前章节内翻页)
     */
    private fun nextPageInternal() {
        val currentProgress = _uiState.value.readingProgress
        val newProgress = (currentProgress + 0.1f).coerceAtMost(1f)
        _uiState.value = _uiState.value.copy(readingProgress = newProgress)
        
        // 如果到达章节末尾，自动翻到下一章
        if (newProgress >= 1f) {
            nextChapterInternal()
        }
    }

    /**
     * 上一页 (在当前章节内翻页)
     */
    private fun previousPageInternal() {
        val currentProgress = _uiState.value.readingProgress
        val newProgress = (currentProgress - 0.1f).coerceAtLeast(0f)
        _uiState.value = _uiState.value.copy(readingProgress = newProgress)
        
        // 如果到达章节开头，自动翻到上一章
        if (newProgress <= 0f) {
            previousChapterInternal()
        }
    }

    /**
     * 重试
     */
    private fun retryInternal() {
        val currentState = _uiState.value
        if (currentState.bookId.isNotEmpty()) {
            if (currentState.chapterList.isEmpty()) {
                loadChapterListMethod()
            } else if (currentState.currentChapter == null) {
                val firstChapter = currentState.chapterList.firstOrNull()
                if (firstChapter != null) {
                    loadChapterContentMethod(firstChapter.id)
                }
            }
        }
    }
} 