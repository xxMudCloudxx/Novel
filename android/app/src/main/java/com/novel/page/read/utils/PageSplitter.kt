package com.novel.page.read.utils

import android.graphics.Paint
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.novel.page.read.components.ReaderSettings
import com.novel.utils.HtmlTextUtil
import com.novel.utils.wdp
import kotlin.math.ceil

/**
 * 文本分页工具类
 * 根据屏幕尺寸、字体大小等参数将文本内容分页
 * — 改为“按固定字符数分行”，不做精细断点
 */
class PageSplitter {

    companion object {

        /**
         * 将章节内容分页
         * @param content 章节内容（原始文本，可能包含 HTML 标签）
         * @param chapterTitle 章节标题（仅在首页显示，可根据需要自行拼接）
         * @param containerSize 容器尺寸（像素），如屏幕宽高减去状态栏、底部栏等
         * @param readerSettings 阅读器设置，包括字体大小、文字颜色等
         * @param density 密度信息，用于将 dp/sp 转 px
         * @return 分页后的“每页字符串列表”。每个页面是一块完整的文本，行内用 '\n' 分隔。
         */
        fun splitContent(
            content: String,
            chapterTitle: String,
            containerSize: IntSize,
            readerSettings: ReaderSettings,
            density: Density
        ): List<String> {
            // 1. 边界检查
            if (content.isEmpty() || containerSize.width <= 0 || containerSize.height <= 0) {
                return listOf(content)
            }

            // 2. 清理内容格式，将 HTML 转纯文本
            val cleanContent = HtmlTextUtil.cleanHtml(content).trim()
            //    — HtmlTextUtil 会移除所有标签，保留纯文字和 <br>、<p> 等换行符；若没有此 util，可以自行按 <br>、<p> 转 \n
            //    :contentReference[oaicite:8]{index=8}

            // 3. 计算可用宽度与高度（剔除左右/上下 padding）
            val horizontalPadding = with(density) { 32.wdp.toPx() }   // 左右各16dp
            val verticalPadding   = with(density) { 20.wdp.toPx() }   // 上下各20dp
            val availableWidth = containerSize.width - horizontalPadding
            val availableHeight = containerSize.height - verticalPadding

            // 4. 创建 TextPaint，用于测量字符宽度和后续绘制
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = with(density) { readerSettings.fontSize.sp.toPx() }  // sp 转 px
                color = readerSettings.textColor.toArgb()
                // 如需更精确，可使用等宽字体：typeface = Typeface.MONOSPACE
            }

            // 5. 测量“平均字符宽度” —— 此处简单测一个中文 “测” 字
            val avgCharWidth = textPaint.measureText("测")   // 单字符宽度（像素） :contentReference[oaicite:9]{index=9}
            //    若要更平滑，可测量多字符后除以长度，如测量 "测测测测" 再除4

            // 6. 计算“每行最大字符数”，确保至少显示 1 个字符
            val maxCharsPerLine = (availableWidth / avgCharWidth).toInt().coerceAtLeast(1)
            //    向下取整，保证“不会超出宽度” :contentReference[oaicite:10]{index=10}

            // 7. 计算行高（像素） — 字体行高可简单按 1.5 倍字号
            val lineHeight = with(density) { (readerSettings.fontSize * 1.5f).sp.toPx() }
            //    1.5f 系数可根据设计稿微调（1.2~1.6 均有可能） :contentReference[oaicite:11]{index=11}

            // 8. 计算每页最大行数（首页需预留标题行数）
            val baseLineCount = (availableHeight / lineHeight).toInt().coerceAtLeast(1)
            //    确保至少 1 行可展示 :contentReference[oaicite:12]{index=12}
            val firstPageTitleReserve = 2  // 首页预留 2 行给章节标题
            val firstPageLineCount = (baseLineCount - firstPageTitleReserve).coerceAtLeast(1)
            val otherPageLineCount = baseLineCount

            if (baseLineCount <= 0) {
                return listOf(cleanContent)
            }

            // 9. 分段拆分 —— 先按“\n\n”拆段，保留空行
            val paragraphs = cleanContent.split("\n\n").filter { it.isNotBlank() || it == "" }
            //    如果一个段落本身就是空字符串 (“”)，则保留一个空行。否则对非空段落进行后续拆分。 :contentReference[oaicite:13]{index=13}

            // 10. 逐段拆分成“行”，所有行最终放到 allLines
            val allLines = mutableListOf<String>()
            for (para in paragraphs) {
                if (para.isEmpty()) {
                    // 空段落（即“\n\n”拆出来的空字符）直接视为一行空字符串
                    allLines.add("")
                    continue
                }

                // 段落内若包含硬回车“\n”，先对其拆分
                val subParas = para.split("\n")
                for (subPara in subParas) {
                    if (subPara.isEmpty()) {
                        // 保留 subPara 内部硬回车生成的空行
                        allLines.add("")
                        continue
                    }

                    // 按“固定字符数”拆行
                    var idx = 0
                    while (idx < subPara.length) {
                        val end = (idx + maxCharsPerLine).coerceAtMost(subPara.length)
                        allLines.add(subPara.substring(idx, end))
                        idx = end
                    }
                }
            }

            // 11. 按“每页最大行数”组装分页
            val pages = mutableListOf<String>()
            var currentPageLines = mutableListOf<String>()
            var currentLineCount = 0
            var isFirstPage = true
            var lineIndex = 0

            while (lineIndex < allLines.size) {
                // 决定本页还可放多少行
                val maxLinesThisPage = if (isFirstPage) firstPageLineCount else otherPageLineCount

                // 如果本页已满，则先收尾并新开一页
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

            return pages.ifEmpty { listOf(cleanContent) }
        }

        /**
         * 估算文本分页数量（快速估算，用于预加载等场景）
         * 逻辑与 splitContent 保持一致：按固定字符数估算行数与页数
         */
        fun estimatePageCount(
            content: String,
            containerSize: IntSize,
            readerSettings: ReaderSettings,
            density: Density
        ): Int {
            if (content.isEmpty() || containerSize.width <= 0 || containerSize.height <= 0) {
                return 1
            }

            // 同样清理内容
            val cleanContent = HtmlTextUtil.cleanHtml(content).trim()

            // 可用宽高
            val horizontalPadding = with(density) { 32.wdp.toPx() }
            val verticalPadding   = with(density) { 40.wdp.toPx() }
            val availableWidth = containerSize.width - horizontalPadding
            val availableHeight = containerSize.height - verticalPadding

            // 行高估算：1.5 倍字号
            val lineHeight = with(density) { (readerSettings.fontSize * 1.5f).sp.toPx() }
            val linesPerPage = (availableHeight / lineHeight).toInt().coerceAtLeast(1)

            // 估算“平均字符宽度”：取 0.7 倍字号近似（保守估算），与 splitContent 中方法保持一致
            val avgCharWidth = with(density) { readerSettings.fontSize.sp.toPx() * 0.7f }
            val charsPerLine = (availableWidth / avgCharWidth).toInt().coerceAtLeast(1)

            // 估算总行数 = ceil(总字符数 / charsPerLine)
            val totalChars = cleanContent.length
            val estimatedLines = ceil(totalChars.toFloat() / charsPerLine).toInt()
            // 估算总页数 = ceil(总行数 / linesPerPage)
            val estimatedPages = ceil(estimatedLines.toFloat() / linesPerPage).toInt()

            return maxOf(1, estimatedPages)
        }
    }
}