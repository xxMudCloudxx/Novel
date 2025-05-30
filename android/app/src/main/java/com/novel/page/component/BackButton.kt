package com.novel.page.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.novel.R
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel
import com.novel.utils.debounceClickable
import com.novel.utils.wdp

/**
 * 返回按钮
 */
@Composable
fun BackButton() {
    // 返回按钮
    Image(
        painter = painterResource(id = R.drawable.round_arrow_back_ios_new_24),
        contentDescription = "back",
        modifier = Modifier
            .size(23.wdp, 23.wdp)
            .debounceClickable(onClick = { NavViewModel.navigateBack() }),
        colorFilter = ColorFilter.tint(NovelColors.NovelText)
    )
}