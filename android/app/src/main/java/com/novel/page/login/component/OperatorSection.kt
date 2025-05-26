package com.novel.page.login.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 运营商
 * @param name 运营商名称
 */
@Composable
fun OperatorSection(name: String) {
    NovelText(
        text = name,
        fontSize = 12.ssp,
        lineHeight = 15.ssp,
        color = NovelColors.NovelTextGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.5.wdp, top = 25.wdp)
    )
}