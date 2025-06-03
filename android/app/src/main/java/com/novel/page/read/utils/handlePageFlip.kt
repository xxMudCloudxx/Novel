package com.novel.page.read.utils

import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * 处理页面翻页逻辑
 *
 * @param direction 翻页方向
 * @param currentPageIndex 当前页面索引
 * @param onPageChange 页面变化回调
 * @param onChapterChange 章节变化回调
 */
fun handlePageFlip(
    currentPageIndex: Int,
    pageData: PageData,
    direction: FlipDirection,
    onPageChange: (FlipDirection) -> Unit,
    onChapterChange: (FlipDirection) -> Unit
) {
    when (direction) {
        FlipDirection.NEXT -> {
            // 检查是否还有下一页
            if (currentPageIndex < pageData.pages.size - 1) {
                // 还有下一页，执行翻页
                onPageChange(FlipDirection.NEXT)
            } else {
                // 已经是最后一页，切换到下一章
                onChapterChange(FlipDirection.NEXT)
            }
        }

        FlipDirection.PREVIOUS -> {
            // 检查是否还有上一页
            if (currentPageIndex > 0) {
                // 还有上一页，执行翻页
                onPageChange(FlipDirection.PREVIOUS)
            } else {
                // 已经是第一页，切换到上一章
                onChapterChange(FlipDirection.PREVIOUS)
            }
        }
    }
}