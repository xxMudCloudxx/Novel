package com.novel.page.book.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.PingFangFamily
import com.novel.utils.HtmlTextUtil
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 书籍简介组件 - 性能优化版本
 * 
 * 功能特性：
 * - 自动检测简介内容长度，超过2行时显示"更多"按钮
 * - 点击"更多"按钮弹出底部弹窗展示完整简介
 * - 弹窗具有动画效果，可拖拽关闭
 * - 支持HTML标签清理和文本格式化
 * - 性能优化：缓存计算结果，减少重组
 * 
 * @param description 书籍简介原始内容（可能包含HTML标签）
 */
@Composable
fun BookDescriptionSection(
    description: String
) {
    // 性能优化：使用remember缓存HTML清理结果
    val cleaned = remember(description) { 
        if (description.isBlank()) "" else HtmlTextUtil.cleanHtml(description)
    }
    
    // 弹窗状态管理
    var showBottomSheet by remember { mutableStateOf(false) }

    // 容器宽度状态
    var containerWidthPx by remember { mutableIntStateOf(0) }

    // 性能优化：使用remember缓存TextMeasurer
    val textMeasurer = rememberTextMeasurer()
    
    // 性能优化：使用remember缓存文本样式
    val textStyle = remember {
        TextStyle(fontSize = 14.ssp, fontFamily = PingFangFamily)
    }
    
    // 性能优化：使用derivedStateOf计算布局信息
    val layoutInfo = remember(cleaned, containerWidthPx, textStyle) {
        derivedStateOf {
            if (containerWidthPx <= 0 || cleaned.isBlank()) {
                LayoutInfo(false, "", "")
            } else {
                val fullLayout = textMeasurer.measure(
                    AnnotatedString(cleaned),
                    style = textStyle,
                    constraints = Constraints(maxWidth = containerWidthPx)
                )
                val totalLines = fullLayout.lineCount
                val showExpand = totalLines > 2
                
                if (showExpand) {
                    val firstEnd = fullLayout.getLineEnd(0).coerceAtMost(cleaned.length)
                    val firstLine = cleaned.substring(0, firstEnd)
                    val restAll = cleaned.substring(firstEnd)
                    LayoutInfo(true, firstLine, restAll)
                } else {
                    LayoutInfo(false, cleaned, "")
                }
            }
        }
    }.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                val newWidth = (size.width * 0.95).toInt()
                if (newWidth != containerWidthPx) {
                    containerWidthPx = newWidth
                }
            }
    ) {
        NovelText(
            text = "简介",
            fontSize = 18.ssp,
            fontWeight = FontWeight.Bold,
            color = NovelColors.NovelText,
            modifier = Modifier.padding(bottom = 8.wdp)
        )

        if (cleaned.isNotBlank()) {
            // 渲染第一行
            NovelText(
                text = layoutInfo.firstLine,
                fontSize = 14.ssp,
                lineHeight = 14.ssp,
                color = NovelColors.NovelText.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )

            // 第二行和更多按钮
            if (layoutInfo.restAll.isNotBlank() || layoutInfo.showExpand) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (layoutInfo.restAll.isNotBlank()) {
                        NovelText(
                            text = layoutInfo.restAll,
                            fontSize = 14.ssp,
                            lineHeight = 14.ssp,
                            color = NovelColors.NovelText.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(end = 60.wdp)
                                .weight(5f)
                        )
                    }

                    if (layoutInfo.showExpand) {
                        NovelText(
                            text = "更多",
                            fontSize = 14.ssp,
                            lineHeight = 14.ssp,
                            color = NovelColors.NovelMain,
                            modifier = Modifier.debounceClickable(onClick = {
                                showBottomSheet = true
                            })
                        )
                    }
                }
            }
        }
    }

    // 半屏弹窗 - 展示完整简介内容
    if (showBottomSheet) {
        BookDescriptionBottomSheet(
            description = description,
            onDismiss = { showBottomSheet = false }
        )
    }
}

/**
 * 布局信息数据类 - 缓存计算结果
 */
private data class LayoutInfo(
    val showExpand: Boolean,
    val firstLine: String,
    val restAll: String
)