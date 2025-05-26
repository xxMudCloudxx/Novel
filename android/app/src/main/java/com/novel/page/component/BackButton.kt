package com.novel.page.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.novel.R
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel
import com.novel.utils.wdp

@Composable
fun BackButton() {
    // 返回按钮
    Image(
        painter = painterResource(id = R.drawable.round_arrow_back_ios_new_24),
        contentDescription = "back",
        modifier = Modifier
            .size(23.wdp, 23.wdp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { NavViewModel.navController.value?.popBackStack() }, // 点击返回上一页
        colorFilter = ColorFilter.tint(NovelColors.NovelText)
    )
}