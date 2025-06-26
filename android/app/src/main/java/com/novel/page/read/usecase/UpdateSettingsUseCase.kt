package com.novel.page.read.usecase

import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.novel.page.read.components.PageFlipEffect
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.service.SettingsService
import com.novel.page.read.utils.ReaderLogTags
import com.novel.page.read.viewmodel.PageData
import com.novel.page.read.viewmodel.ReaderUiState
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsService: SettingsService,
    private val paginateChapterUseCase: PaginateChapterUseCase,
    private val splitContentUseCase: SplitContentUseCase
) {
    companion object {
        private const val TAG = ReaderLogTags.UPDATE_SETTINGS_USE_CASE
    }

    sealed class UpdateResult {
        data class Success(val newPageData: PageData?, val newPageIndex: Int = 0) : UpdateResult()
        // No failure case for now, as saveSettings is fire-and-forget
    }

    suspend fun execute(
        newSettings: ReaderSettings,
        state: ReaderUiState
    ): UpdateResult {
        Log.d(TAG, "开始更新设置")
        Log.d(TAG, "新设置详情:")
        Log.d(TAG, "  - 字体大小: ${newSettings.fontSize}sp")
        Log.d(TAG, "  - 亮度: ${(newSettings.brightness * 100).toInt()}%")
        Log.d(TAG, "  - 背景颜色: ${colorToHex(newSettings.backgroundColor)}")
        Log.d(TAG, "  - 文字颜色: ${colorToHex(newSettings.textColor)}")
        Log.d(TAG, "  - 翻页效果: ${newSettings.pageFlipEffect}")
        
        Log.d(TAG, "保存设置到本地存储")
        settingsService.saveSettings(newSettings)

        val needsRepagination = needsRepagination(state.readerSettings, newSettings, state)
        Log.d(TAG, "是否需要重新分页: $needsRepagination")

        if (!needsRepagination) {
            Log.d(TAG, "无需重新分页，直接返回成功")
            return UpdateResult.Success(null)
        }

        val currentChapter = state.currentChapter ?: run {
            Log.w(TAG, "当前章节为空，无法重新分页")
            return UpdateResult.Success(null)
        }
        val chapterContent = state.bookContent
        val density = state.density ?: run {
            Log.w(TAG, "屏幕密度为空，无法重新分页")
            return UpdateResult.Success(null)
        }

        // Calculate current progress to restore after re-pagination
        val currentProgress = if (state.currentPageData?.pages?.isNotEmpty() == true && state.currentPageIndex >= 0) {
            (state.currentPageIndex.toFloat() + 1) / state.currentPageData.pages.size.toFloat()
        } else 0f
        
        Log.d(TAG, "重新分页前当前进度: ${(currentProgress * 100).toInt()}% (页面${state.currentPageIndex + 1}/${state.currentPageData?.pages?.size ?: 0})")

        // Create state for re-pagination
        val stateForSplitting = state.copy(
            readerSettings = newSettings,
            currentPageIndex = state.currentPageIndex
        )

        return try {
            Log.d(TAG, "开始使用SplitContentUseCase重新分页")
            // Use SplitContentUseCase for re-pagination
            val splitResult = splitContentUseCase.execute(
                state = stateForSplitting,
                restoreProgress = currentProgress,
                includeAdjacentChapters = true
            )

            when (splitResult) {
                is SplitContentUseCase.SplitResult.Success -> {
                    Log.d(TAG, "重新分页成功: 新页数=${splitResult.pageData.pages.size}, 恢复到页面=${splitResult.safePageIndex + 1}")
                    UpdateResult.Success(splitResult.pageData, splitResult.safePageIndex)
                }
                is SplitContentUseCase.SplitResult.Failure -> {
                    Log.w(TAG, "SplitContentUseCase失败，回退到基础分页")
                    // Fallback to basic pagination
                    val newPageData = paginateChapterUseCase.execute(
                        chapter = currentChapter,
                        content = chapterContent,
                        readerSettings = newSettings,
                        containerSize = state.containerSize,
                        density = density,
                        isFirstChapter = state.isFirstChapter,
                        isLastChapter = state.isLastChapter
                    )
                    
                    val restoredPageIndex = if (newPageData.pages.isNotEmpty()) {
                        (currentProgress * newPageData.pages.size).toInt()
                            .coerceIn(0, newPageData.pages.size - 1)
                    } else 0
                    
                    Log.d(TAG, "基础分页完成: 新页数=${newPageData.pages.size}, 恢复到页面=${restoredPageIndex + 1}")
                    UpdateResult.Success(newPageData, restoredPageIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "重新分页失败", e)
            // If splitting fails, return success with no new page data
            UpdateResult.Success(null)
        }
    }

    private fun needsRepagination(oldSettings: ReaderSettings, newSettings: ReaderSettings, state: ReaderUiState): Boolean {
        if (state.containerSize.width == 0 || state.containerSize.height == 0) {
            Log.d(TAG, "容器尺寸无效，跳过重新分页")
            return false
        }
        
        val fontSizeChanged = oldSettings.fontSize != newSettings.fontSize
        val backgroundColorChanged = oldSettings.backgroundColor != newSettings.backgroundColor
        val pageFlipEffectChanged = oldSettings.pageFlipEffect != newSettings.pageFlipEffect &&
                (oldSettings.pageFlipEffect.isVertical() || newSettings.pageFlipEffect.isVertical())
        
        Log.d(TAG, "分页影响因素检查:")
        Log.d(TAG, "  - 字体大小变化: $fontSizeChanged (${oldSettings.fontSize}sp -> ${newSettings.fontSize}sp)")
        Log.d(TAG, "  - 背景颜色变化: $backgroundColorChanged (${colorToHex(oldSettings.backgroundColor)} -> ${colorToHex(newSettings.backgroundColor)})")
        Log.d(TAG, "  - 翻页效果变化影响布局: $pageFlipEffectChanged")
        
        return fontSizeChanged || backgroundColorChanged || pageFlipEffectChanged
    }
    
    /**
     * 将Color对象转换为十六进制字符串，用于日志显示
     */
    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        return try {
            String.format("#%08X", color.toArgb())
        } catch (e: Exception) {
            "INVALID_COLOR"
        }
    }
}

private fun PageFlipEffect.isVertical() = this == PageFlipEffect.VERTICAL