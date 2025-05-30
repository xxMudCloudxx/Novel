package com.novel.page.book.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.component.NovelImageView
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

@Composable
fun BookCoverSection(bookInfo: BookDetailUiState.BookInfo?) {
    if (bookInfo == null) return

    Box(
        modifier = Modifier.padding(top = 55.wdp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        NovelImageView(
            imageUrl = bookInfo.picUrl,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(125.wdp)
                .height(190.wdp)
                .clip(RoundedCornerShape(5.wdp))
                .background(NovelColors.NovelMainLight),
        )
    }
}