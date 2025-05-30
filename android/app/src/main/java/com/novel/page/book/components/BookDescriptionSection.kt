package com.novel.page.book.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.ui.theme.PingFangFamily
import com.novel.utils.AdaptiveScreen
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp

@Composable
fun BookDescriptionSection(
    description: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val cleaned = HtmlTextUtil.cleanHtml(description)

    // 1. 动态获取容器可用宽度（px）
    var containerWidthPx by remember { mutableIntStateOf(0) }

    // 2. TextMeasurer 用于离线测量
    val textMeasurer = rememberTextMeasurer()


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width - 30 },  // 获取宽度
    ) {
        NovelText(
            text = "简介",
            fontSize = 16.ssp,
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
                color = NovelColors.NovelText.copy(alpha = 0.7f),
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
                        color = NovelColors.NovelText.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Ellipsis,
                        modifier = Modifier.padding(end = 60.wdp).weight(5f)
                    )
                }

                if (showExpand) {
                    NovelText(
                        text = "更多",
                        fontSize = 14.ssp,
                        lineHeight = 14.ssp,
                        color = NovelColors.NovelMain,
                        modifier = Modifier
                            .clickable { onToggleExpand() }
                    )
                }
            }
        }
    }
}
