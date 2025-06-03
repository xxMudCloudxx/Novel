package com.novel.page.read.utils

import com.novel.page.read.viewmodel.FlipDirection
import com.novel.page.read.viewmodel.PageData

/**
 * 处理页面翻页逻辑 - 优化版本，添加章节边界检查
 *
 * @param currentPageIndex 当前页面索引
 * @param pageData 页面数据
 * @param direction 翻页方向
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
            } else if (pageData.nextChapterData != null) {
                // 已经是最后一页且有下一章，切换到下一章
                onChapterChange(FlipDirection.NEXT)
            }
            // 如果既没有下一页也没有下一章，不执行任何操作
        }

        FlipDirection.PREVIOUS -> {
            // 检查是否还有上一页
            if (currentPageIndex > 0) {
                // 还有上一页，执行翻页
                onPageChange(FlipDirection.PREVIOUS)
            } else if (pageData.previousChapterData != null) {
                // 已经是第一页且有上一章，切换到上一章
                onChapterChange(FlipDirection.PREVIOUS)
            }
            // 如果既没有上一页也没有上一章，不执行任何操作
        }
    }
}