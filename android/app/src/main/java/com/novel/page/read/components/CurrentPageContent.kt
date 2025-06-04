package com.novel.page.read.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.novel.page.component.NovelText
import com.novel.page.component.PaperTexture
import com.novel.page.read.viewmodel.PageData
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 当前页面内容 - 章节标题只在第一页显示，包含纸张纹理
 */
@Composable
fun CurrentPageContent(
    pageData: PageData,
    pageIndex: Int,
    readerSettings: ReaderSettings,
    isFirstPage: Boolean,
    modifier: Modifier = Modifier
) {
    PaperTexture(
        modifier = modifier.background(readerSettings.backgroundColor),
        alpha = 0.04f, // 轻微的纸张纹理效果
        density = 1.0f,
        seed = pageIndex.toLong() * 42L // 每页使用不同的种子
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.wdp, vertical = 10.wdp),
        ) {
            Column {
                // 只在第一页显示章节标题
                if (isFirstPage) {
                    NovelText(
                        text = pageData.chapterName,
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
                if (pageIndex in pageData.pages.indices) {
                    NovelText(
                        text = HtmlTextUtil.cleanHtml(pageData.pages[pageIndex]),
                        fontSize = readerSettings.fontSize.ssp,
                        color = readerSettings.textColor,
                        lineHeight = (readerSettings.fontSize * 1.5).ssp
                    )
                }
            }
        }
    }
}