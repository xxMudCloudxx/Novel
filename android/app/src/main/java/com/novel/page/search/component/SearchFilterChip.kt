package com.novel.page.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 搜索筛选标签组件
 */
@Composable
fun SearchFilterChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        NovelColors.NovelMain.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val borderColor = if (selected) {
        NovelColors.NovelMain
    } else {
        NovelColors.NovelTextGray.copy(alpha = 0.3f)
    }

    val textColor = if (selected) {
        NovelColors.NovelText
    } else {
        NovelColors.NovelTextGray
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.wdp)
            )
            .border(
                width = 1.wdp,
                color = borderColor,
                shape = RoundedCornerShape(16.wdp)
            )
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 12.wdp, vertical = 6.wdp),
        contentAlignment = Alignment.Center
    ) {
        NovelText(
            text = text,
            fontSize = 13.ssp,
            color = textColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.wdp, vertical = 6.wdp).debounceClickable(onClick = onClick),
        )
    }
} 