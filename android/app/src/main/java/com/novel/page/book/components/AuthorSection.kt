package com.novel.page.book.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.component.NovelMainButton
import com.novel.page.component.NovelText
import com.novel.page.component.NovelWeakenButton
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
import com.novel.utils.ssp
import com.novel.utils.wdp

@Composable
fun AuthorSection(bookInfo: BookDetailUiState.BookInfo?) {
    if (bookInfo == null) return
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 15.wdp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 作者头像（实心圆圈）
        Box(
            modifier = Modifier
                .size(25.wdp)
                .background(NovelColors.NovelTextGray, CircleShape)
        )
        
        // 作者名字
        NovelText(
            text = bookInfo.authorName,
            fontSize = 14.ssp,
            color = NovelColors.NovelText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.wdp)
        )
        
        // 关注按钮
        NovelWeakenButton(
            content = {
                NovelText(
                    text = "关注",
                    fontSize = 14.ssp,
                    color = NovelColors.NovelText.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 12.wdp)
                )
            },
            onClick = { /* TODO: 实现关注功能 */ },
            round = 18.wdp,
            color = NovelColors.NovelTextGray
        )
    }
}