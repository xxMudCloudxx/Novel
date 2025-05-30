package com.novel.page.book.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
 * 书籍简介组件
 * 
 * 功能特性：
 * - 自动检测简介内容长度，超过2行时显示"更多"按钮
 * - 点击"更多"按钮弹出半屏弹窗展示完整简介
 * - 弹窗具有上方10dp圆角，可上下滑动查看内容
 * - 支持HTML标签清理和文本格式化
 * 
 * @param description 书籍简介原始内容（可能包含HTML标签）
 * @param onToggleExpand 展开/收起回调（当前未使用，为向后兼容保留）
 */
@Composable
fun BookDescriptionSection(
    description: String,
    onToggleExpand: () -> Unit
) {
    val cleaned = HtmlTextUtil.cleanHtml(description)
    
    // 弹窗状态管理
    var showBottomSheet by remember { mutableStateOf(false) }

    // 1. 动态获取容器可用宽度（px）
    var containerWidthPx by remember { mutableIntStateOf(0) }

    // 2. TextMeasurer 用于离线测量
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = (it.width * 0.95).toInt() },  // 获取宽度
    ) {
        NovelText(
            text = "简介",
            fontSize = 18.ssp,
            fontWeight = FontWeight.Bold,
            color = NovelColors.NovelText,
            modifier = Modifier.padding(bottom = 8.wdp)
        )

        if (containerWidthPx > 0) {
            // ---- 先测量整段文字 ----
            val fullLayout = textMeasurer.measure(
                AnnotatedString(cleaned),
                style = TextStyle(fontSize = 14.ssp, fontFamily = PingFangFamily),
                constraints = Constraints(maxWidth = containerWidthPx)
            )
            val totalLines = fullLayout.lineCount
            val showExpand = totalLines > 2

            // 第一行分割点
            val firstEnd = fullLayout.getLineEnd(0)
            val firstLine = cleaned.substring(0, firstEnd)
            val restAll = cleaned.substring(firstEnd)

            // 渲染第一行
            NovelText(
                text = firstLine,
                fontSize = 14.ssp,
                lineHeight = 14.ssp,
                color = NovelColors.NovelText.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (restAll.isNotBlank()) {
                    NovelText(
                        text = restAll,
                        fontSize = 14.ssp,
                        lineHeight = 14.ssp,
                        color = NovelColors.NovelText.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Ellipsis,
                        modifier = Modifier
                            .padding(end = 60.wdp)
                            .weight(5f)
                    )
                }

                if (showExpand) {
                    NovelText(
                        text = "更多",
                        fontSize = 14.ssp,
                        lineHeight = 14.ssp,
                        color = NovelColors.NovelMain,
                        modifier = Modifier.debounceClickable(onClick = {
                            // 点击"更多"按钮时显示半屏弹窗
                            showBottomSheet = true
                        })
                    )
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