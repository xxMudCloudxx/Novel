package com.novel.page.read.utils

import android.text.TextPaint
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.novel.page.read.components.ReaderSettings
import com.novel.page.read.utils.ReaderLogTags
import com.novel.utils.HtmlTextUtil
import com.novel.utils.wdp

/**
 * 文本分页工具类
 * 
 * 核心功能：
 * - 根据屏幕尺寸和字体设置计算分页
 * - 支持HTML内容清理和格式化
 * - 智能换行和段落处理
 * - 性能优化的固定字符数分行算法
 * 
 * 算法特点：
 * - 按固定字符数分行，避免复杂的断点计算
 * - 支持首页标题预留空间
 * - 保持段落格式和空行结构
 * - 响应式布局适配不同屏幕
 */
class PageSplitter {

    companion object {
        private const val TAG = ReaderLogTags.PAGE_SPLITTER

        /**
         * 将章节内容分页
         * 
         * 分页策略：
         * 1. 清理HTML内容并计算可用空间
         * 2. 测量字符宽度和行高
         * 3. 按固定字符数分行
         * 4. 按最大行数组装分页
         * 
         * @param content 章节内容（原始文本，可能包含HTML标签）
         * @param containerSize 容器尺寸（像素），屏幕宽高减去状态栏等
         * @param readerSettings 阅读器设置，包括字体大小、文字颜色等
         * @param density 密度信息，用于将dp/sp转px
         * @return 分页后的文本列表，每页是完整的文本块
         */
        fun splitContent(
            content: String,
            containerSize: IntSize,
            readerSettings: ReaderSettings,
            density: Density
        ): List<String> {
            val startTime = System.currentTimeMillis()
            
            // 1. 边界检查
            if (content.isEmpty() || containerSize.width <= 0 || containerSize.height <= 0) {
                Log.w(TAG, "分页参数无效: content.length=${content.length}, size=$containerSize")
                return listOf(content)
            }

            Log.d(TAG, "开始分页: 内容长度=${content.length}, 容器尺寸=$containerSize")

            // 2. 清理内容格式，将HTML转纯文本
            val cleanContent = HtmlTextUtil.cleanHtml(content).trim()
            Log.d(TAG, "内容清理完成: ${content.length} -> ${cleanContent.length}")

            // 3. 计算可用宽度与高度（剔除左右/上下padding）
            val horizontalPadding = with(density) { 32.wdp.toPx() }   // 左右各16dp
            val verticalPadding   = with(density) { 60.wdp.toPx() }   // 上下各30dp
            val availableWidth = containerSize.width - horizontalPadding
            val availableHeight = containerSize.height - verticalPadding

            Log.d(TAG, "可用空间: 宽度=${availableWidth}px, 高度=${availableHeight}px")

            // 4. 创建TextPaint，用于测量字符宽度
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = with(density) { readerSettings.fontSize.sp.toPx() }  // sp转px
                color = readerSettings.textColor.toArgb()
            }

            // 5. 测量平均字符宽度
            val avgCharWidth = textPaint.measureText("测")   // 使用中文字符测量
            Log.d(TAG, "字符宽度: ${avgCharWidth}px, 字体大小: ${readerSettings.fontSize}sp")

            // 6. 计算每行最大字符数
            val maxCharsPerLine = (availableWidth / avgCharWidth).toInt().coerceAtLeast(1)

            // 7. 计算行高
            val lineHeight = with(density) { (readerSettings.fontSize * 1.5f).sp.toPx() }

            // 8. 计算每页最大行数
            val baseLineCount = (availableHeight / lineHeight).toInt().coerceAtLeast(1)
            val firstPageTitleReserve = 2  // 首页预留2行给章节标题
            val firstPageLineCount = (baseLineCount - firstPageTitleReserve).coerceAtLeast(1)

            Log.d(TAG, "分页参数: 每行${maxCharsPerLine}字符, 行高${lineHeight}px, 每页${baseLineCount}行")

            if (baseLineCount <= 0) {
                Log.w(TAG, "每页行数无效，返回原内容")
                return listOf(cleanContent)
            }

            // 9. 分段拆分 - 按"\n\n"拆段，保留空行
            val paragraphs = cleanContent.split("\n\n").filter { it.isNotBlank() || it == "" }

            // 10. 逐段拆分成行
            val allLines = mutableListOf<String>()
            for (para in paragraphs) {
                if (para.isEmpty()) {
                    // 空段落视为一行空字符串
                    allLines.add("")
                    continue
                }

                // 段落内按硬回车"\n"拆分
                val subParas = para.split("\n")
                for (subPara in subParas) {
                    if (subPara.isEmpty()) {
                        // 保留段落内部的空行
                        allLines.add("")
                        continue
                    }

                    // 按固定字符数分行
                    var idx = 0
                    while (idx < subPara.length) {
                        val end = (idx + maxCharsPerLine).coerceAtMost(subPara.length)
                        allLines.add(subPara.substring(idx, end))
                        idx = end
                    }
                }
            }

            Log.d(TAG, "文本拆分完成: 总行数=${allLines.size}")

            // 11. 按每页最大行数组装分页
            val pages = mutableListOf<String>()
            val currentPageLines = mutableListOf<String>()
            var currentLineCount = 0
            var isFirstPage = true
            var lineIndex = 0

            while (lineIndex < allLines.size) {
                // 决定本页还可放多少行
                val maxLinesThisPage = if (isFirstPage) firstPageLineCount else baseLineCount

                // 如果本页已满，则收尾并新开一页
                if (currentLineCount >= maxLinesThisPage) {
                    pages.add(currentPageLines.joinToString("\n"))
                    currentPageLines.clear()
                    currentLineCount = 0
                    isFirstPage = false
                    continue
                }

                // 将当前行加到本页
                currentPageLines.add(allLines[lineIndex])
                currentLineCount++
                lineIndex++
            }

            // 最后一页如果还有行，补入结果
            if (currentPageLines.isNotEmpty()) {
                pages.add(currentPageLines.joinToString("\n"))
            }

            val endTime = System.currentTimeMillis()
            val resultPages = pages.ifEmpty { listOf(cleanContent) }
            
            Log.d(TAG, "分页完成: ${resultPages.size}页, 耗时${endTime - startTime}ms")
            
            return resultPages
        }
    }
}