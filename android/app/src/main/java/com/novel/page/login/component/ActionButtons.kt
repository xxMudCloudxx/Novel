package com.novel.page.login.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * @param firstText 登录按钮文字
 * @param secondText 注册按钮文字
 * @param onFirstClick 登录按钮点击事件
 * @param onSecondClick 注册按钮点击事件
 */
@Composable
fun ActionButtons(
    modifier: Modifier = Modifier,
    firstText: String = "登录",
    secondText: String = "暂无账号，进行注册",
    onFirstClick: () -> Unit,
    isFirstEnabled: Boolean = true,
    onSecondClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        NovelMainButton(
            content = {
                NovelText(
                    firstText,
                    fontSize = 16.ssp,
                    fontWeight = FontWeight.Bold,
                    color = NovelColors.NovelSecondaryBackground
                )
            },
            modifier = Modifier
                .padding(vertical = 16.wdp)
                .width(330.wdp)
                .height(48.wdp),
            enabldeClicke = isFirstEnabled,
            onClick = onFirstClick
        )
        NovelWeakenButton(
            content = {
                NovelText(
                    secondText,
                    fontSize = 16.ssp,
                    fontWeight = FontWeight.Bold,
                    color = NovelColors.NovelText
                )
            },
            modifier = Modifier
                .width(330.wdp)
                .height(48.wdp),
            onClick = onSecondClick
        )
    }
}