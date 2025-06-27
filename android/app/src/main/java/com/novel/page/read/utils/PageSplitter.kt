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
 * 高性能文本分页工具
 *
 * ▶️ 核心改进
 * 1. 单趟扫描完成「拆行 + 组页」，不再构造 allLines 列表
 * 2. 使用 StringBuilder 聚合行 / 页，降低内存与 GC
 * 3. 缓存 TextPaint 与测量结果，避免重复测宽
 * 4. 全程 O(n) 复杂度，低常数
 */
class PageSplitter {

    companion object {
        private const val TAG = ReaderLogTags.PAGE_SPLITTER
        private const val LINE_SPACING_MULTIPLIER = 1.5f

        /**
         * 将章节内容分页
         *
         * @param content 章节内容（可能含 HTML）
         * @param containerSize 可用区域（像素），已扣除系统栏
         * @param readerSettings 字体、颜色等阅读配置
         * @param density Compose Density，用于 dp/sp→px
         * @return 每页完整文本，list.size 即页数
         */
        fun splitContent(
            content: String,
            containerSize: IntSize,
            readerSettings: ReaderSettings,
            density: Density
        ): List<String> {
            val t0 = System.nanoTime()

            // -------- 1. 参数校验 --------
            if (content.isBlank() ||
                containerSize.width <= 0 || containerSize.height <= 0
            ) {
                Log.w(TAG, "参数无效，返回原始内容")
                return listOf(content)
            }

            // -------- 2. HTML → 纯文本 --------
            val plain = HtmlTextUtil.cleanHtml(content).trim()
            Log.d(TAG, "内容清理完成：${content.length} → ${plain.length}")

            // -------- 3. 计算可用宽高 --------
            val hPaddingPx = with(density) { 32.wdp.toPx() }   // 16 dp ×2
            val vPaddingPx = with(density) { 60.wdp.toPx() }   // 30 dp ×2
            val availW = containerSize.width - hPaddingPx
            val availH = containerSize.height - vPaddingPx

            // -------- 4. 准备测量工具 --------
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = with(density) { readerSettings.fontSize.sp.toPx() }
                color = readerSettings.textColor.toArgb()
            }
            val avgCharW = textPaint.measureText("测") // 任意中/英混排字符
            val maxCharsPerLine = (availW / avgCharW).toInt().coerceAtLeast(1)

            val lineHeight = textPaint.textSize * LINE_SPACING_MULTIPLIER
            val baseLinesPerPage = (availH / lineHeight).toInt().coerceAtLeast(1)
            val firstPageReserve = 2 // 首页标题预留
            val firstPageMaxLines = (baseLinesPerPage - firstPageReserve).coerceAtLeast(1)

            Log.d(TAG, "分页参数：每行${maxCharsPerLine}字符，行高${lineHeight}px，每页${baseLinesPerPage}行")

            // -------- 5. 单趟遍历分页 --------
            val pages = mutableListOf<String>()
            val pageBuilder = StringBuilder()    // 当前页
            val lineBuilder = StringBuilder()    // 当前行

            var currentLineCount = 0
            var isFirstPage = true
            var currentPageLimit = firstPageMaxLines

            fun flushLine() {
                // 把一行写入当前页并计数
                pageBuilder.append(lineBuilder).append('\n')
                lineBuilder.setLength(0)
                currentLineCount++

                // 如果超过本页行数限制 → 保存页
                if (currentLineCount >= currentPageLimit) {
                    pages.add(pageBuilder.trimEnd().toString())
                    pageBuilder.setLength(0)
                    currentLineCount = 0
                    isFirstPage = false
                    currentPageLimit = baseLinesPerPage
                }
            }

            // 用 Seq 避免一次性 split 占用内存
            val rawLines = plain.splitToSequence('\n')
            for (raw in rawLines) {
                if (raw.isBlank()) {
                    // 空行直接 flush 空行
                    lineBuilder.clear()
                    flushLine()
                    continue
                }

                var idx = 0
                val len = raw.length
                while (idx < len) {
                    val end = (idx + maxCharsPerLine).coerceAtMost(len)
                    lineBuilder.append(raw, idx, end)
                    flushLine()
                    idx = end
                }
            }

            // 末尾残余
            if (lineBuilder.isNotEmpty() || pageBuilder.isNotEmpty()) {
                if (lineBuilder.isNotEmpty()) flushLine()
                if (pageBuilder.isNotEmpty()) {
                    pages.add(pageBuilder.trimEnd().toString())
                }
            }

            val t1 = System.nanoTime()
            Log.d(TAG, "分页完成：${pages.size}页，耗时${(t1 - t0) / 1_000_000}毫秒")

            return if (pages.isEmpty()) listOf(plain) else pages
        }
    }
}
