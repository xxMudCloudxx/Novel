package com.novel.page.read.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.novel.page.component.NovelText
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 通用页面内容显示组件（供PageCurl等新组件使用）
 */
@Composable
fun PageContentDisplay(
    page: String,
    chapterName: String,
    isFirstPage: Boolean = false,
    isLastPage: Boolean = false,
    nextChapterData: PageData? = null,
    previousChapterData: PageData? = null,
    readerSettings: ReaderSettings = ReaderSettings(),
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerSettings.backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.wdp, vertical = 10.wdp),
    ) {
        Column {
            // 只在第一页显示章节标题
            if (isFirstPage) {
                NovelText(
                    text = chapterName,
                    fontSize = (readerSettings.fontSize + 4).ssp,
                    fontWeight = FontWeight.Bold,
                    color = readerSettings.textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.wdp)
                )
            }

            // 页面内容
            NovelText(
                text = HtmlTextUtil.cleanHtml(page),
                fontSize = readerSettings.fontSize.ssp,
                color = readerSettings.textColor,
                lineHeight = (readerSettings.fontSize * 1.5).ssp
            )

            // 如果是最后一页且有下一章，显示下一章提示
            if (isLastPage && nextChapterData != null) {
                Spacer(modifier = Modifier.height(24.wdp))
                NovelText(
                    text = "下一章: ${nextChapterData.chapterName}",
                    fontSize = (readerSettings.fontSize - 2).ssp,
                    color = readerSettings.textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 如果是第一页且有上一章，显示上一章提示
            if (isFirstPage && previousChapterData != null) {
                Spacer(modifier = Modifier.height(24.wdp))
                NovelText(
                    text = "上一章: ${previousChapterData.chapterName}",
                    fontSize = (readerSettings.fontSize - 2).ssp,
                    color = readerSettings.textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}