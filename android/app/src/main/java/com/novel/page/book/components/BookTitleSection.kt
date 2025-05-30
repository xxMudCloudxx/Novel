package com.novel.page.book.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp

@Composable
fun BookTitleSection(bookInfo: BookDetailUiState.BookInfo?) {
    if (bookInfo == null) return
    
    NovelText(
        text = bookInfo.bookName,
        fontSize = 20.ssp,
        fontWeight = FontWeight.Bold,
        color = NovelColors.NovelText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
} 