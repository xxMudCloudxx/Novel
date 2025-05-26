package com.novel.page.login.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.novel.page.component.NovelMainButton
import com.novel.page.component.NovelText
import com.novel.page.component.NovelWeakenButton
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 登录按钮
 * @param onOneClick 本机一键登录点击事件
 * @param onOther 其他手机号登录点击事件
 */
@Composable
fun ActionButtons(
    onOneClick: () -> Unit,
    onOther: () -> Unit
) {
    NovelMainButton(
        content = {
            NovelText(
                "本机号码一键登录",
                fontSize = 16.ssp,
                fontWeight = FontWeight.Bold,
                color = NovelColors.NovelSecondaryBackground
            )
        },
        modifier = Modifier
            .padding(vertical = 16.wdp)
            .width(330.wdp)
            .height(48.wdp),
        onClick = onOneClick
    )
    NovelWeakenButton(
        content = {
            NovelText(
                "其他手机号码登录",
                fontSize = 16.ssp,
                fontWeight = FontWeight.Bold,
                color = NovelColors.NovelText
            )
        },
        modifier = Modifier
            .width(330.wdp)
            .height(48.wdp),
        onClick = onOther
    )
}