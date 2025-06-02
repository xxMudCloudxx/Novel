package com.novel.page.read.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 背景颜色控制组件
 */
@Composable
fun BackgroundColorControl(
    backgroundThemes: List<BackgroundTheme>,
    currentBackgroundColor: Color,
    onThemeChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        NovelText(text = "背景", fontSize = 14.ssp)
        Spacer(modifier = Modifier.width(15.wdp))
        backgroundThemes.forEach { theme ->
            val isSelected = currentBackgroundColor == theme.backgroundColor

            Card(
                modifier = Modifier
                    .padding(end = 15.wdp)
                    .size(28.wdp)
                    .clickable {
                        onThemeChange(theme.backgroundColor)
                    },
                colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                shape = CircleShape,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, NovelColors.NovelText)
                } else null
            ){}
        }
        Spacer(modifier = Modifier.width(15.wdp))
    }
}