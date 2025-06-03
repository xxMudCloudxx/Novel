package com.novel.page.read.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.novel.page.component.PaperTexture
import com.novel.page.component.pagecurl.config.rememberPageCurlConfig
import com.novel.page.component.pagecurl.page.ExperimentalPageCurlApi
import com.novel.page.component.pagecurl.page.PageCurl
import com.novel.page.component.pagecurl.page.rememberPageCurlState
import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * PageCurl仿真翻页容器
 * 
 * 使用PageCurl库实现真实的书页卷曲翻页效果，并集成纸张纹理
 * 支持章节边界检测和自动章节切换
 *
 * @param pageData 页面数据
 * @param currentPageIndex 当前页面索引
 * @param readerSettings 阅读器设置
 * @param onPageChange 页面变化回调
 * @param onChapterChange 章节变化回调
 * @param onClick 点击回调
 */
@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PageCurlFlipContainer(
    pageData: PageData,
    currentPageIndex: Int,
    readerSettings: ReaderSettings,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit,
    onClick: () -> Unit
) {
    // 计算扩展的页面总数（包括章节边界）
    val totalPages = remember(pageData) {
        var count = pageData.pages.size
        // 如果有上一章，添加一个虚拟页
        if (pageData.previousChapterData != null) count += 1
        // 如果有下一章，添加一个虚拟页
        if (pageData.nextChapterData != null) count += 1
        count
    }
    
    // 计算当前页在扩展页面中的索引
    val adjustedCurrentIndex = remember(pageData, currentPageIndex) {
        var index = currentPageIndex
        if (pageData.previousChapterData != null) index += 1
        index.coerceIn(0, totalPages - 1)
    }
    
    // PageCurl状态管理
    val pageCurlState = rememberPageCurlState(
        initialCurrent = adjustedCurrentIndex
    )
    
    // PageCurl配置
    val config = rememberPageCurlConfig(
        backPageColor = readerSettings.backgroundColor,
        backPageContentAlpha = 0.1f,
        shadowColor = if (readerSettings.backgroundColor.luminance() > 0.5f) Color.Black else Color.White,
        shadowAlpha = 0.2f,
        shadowRadius = 10.dp,
        dragForwardEnabled = true,
        dragBackwardEnabled = true,
        tapForwardEnabled = true,
        tapBackwardEnabled = true
    )

    // 同步外部页面索引变化
    LaunchedEffect(currentPageIndex, pageData.chapterId) {
        val newAdjustedIndex = currentPageIndex + (if (pageData.previousChapterData != null) 1 else 0)
        if (pageCurlState.current != newAdjustedIndex && newAdjustedIndex in 0 until totalPages) {
            pageCurlState.snapTo(newAdjustedIndex)
        }
    }

    // 监听PageCurl状态变化并处理边界情况
    LaunchedEffect(pageCurlState.current) {
        val currentIndex = pageCurlState.current
        val previousChapterOffset = if (pageData.previousChapterData != null) 1 else 0
        val realPageIndex = currentIndex - previousChapterOffset
        
        when {
            // 翻到上一章的虚拟页
            currentIndex == 0 && pageData.previousChapterData != null -> {
                onChapterChange(FlipDirection.PREVIOUS)
            }
            // 翻到下一章的虚拟页
            currentIndex == totalPages - 1 && pageData.nextChapterData != null && 
            realPageIndex >= pageData.pages.size -> {
                onChapterChange(FlipDirection.NEXT)
            }
            // 正常的页面变化
            realPageIndex in 0 until pageData.pages.size && realPageIndex != currentPageIndex -> {
                val direction = if (realPageIndex > currentPageIndex) {
                    FlipDirection.NEXT
                } else {
                    FlipDirection.PREVIOUS
                }
                onPageChange(direction)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PageCurl(
            count = totalPages,
            state = pageCurlState,
            config = config,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val previousChapterOffset = if (pageData.previousChapterData != null) 1 else 0
            val realPageIndex = pageIndex - previousChapterOffset
            
            // 渲染每一页内容，包含纸张纹理
            PaperTexture(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f, // 轻微的纸张纹理效果
                density = 1.2f,
                seed = pageIndex.toLong() * 42L // 每页使用不同的种子
            ) {
                when {
                    // 上一章的虚拟页
                    pageIndex == 0 && pageData.previousChapterData != null -> {
                        pageData.previousChapterData.let { prevChapter ->
                            val lastPageIndex = prevChapter.pages.size - 1
                            if (lastPageIndex >= 0) {
                                PageContentDisplay(
                                    page = prevChapter.pages[lastPageIndex],
                                    chapterName = prevChapter.chapterName,
                                    isFirstPage = false,
                                    isLastPage = true,
                                    nextChapterData = pageData,
                                    readerSettings = readerSettings,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                    // 下一章的虚拟页
                    realPageIndex >= pageData.pages.size && pageData.nextChapterData != null -> {
                        pageData.nextChapterData.let { nextChapter ->
                            if (nextChapter.pages.isNotEmpty()) {
                                PageContentDisplay(
                                    page = nextChapter.pages[0],
                                    chapterName = nextChapter.chapterName,
                                    isFirstPage = true,
                                    isLastPage = false,
                                    previousChapterData = pageData,
                                    readerSettings = readerSettings,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                    // 当前章节的正常页面
                    realPageIndex in 0 until pageData.pages.size -> {
                        PageContentDisplay(
                            page = pageData.pages[realPageIndex],
                            chapterName = pageData.chapterName,
                            isFirstPage = realPageIndex == 0,
                            isLastPage = realPageIndex == pageData.pages.size - 1,
                            nextChapterData = if (realPageIndex == pageData.pages.size - 1) pageData.nextChapterData else null,
                            previousChapterData = if (realPageIndex == 0) pageData.previousChapterData else null,
                            readerSettings = readerSettings,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}