package com.novel.page.login.component

import com.novel.utils.TimberLogger
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.novel.page.component.NovelMainButton
import androidx.compose.ui.Alignment
import com.novel.page.component.NovelText
import com.novel.page.component.NovelWeakenButton
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 登录页面操作按钮组件
 *
 * 包含主要操作按钮（登录）和次要操作按钮（注册）
 * 支持按钮启用状态控制和自定义文案
 *
 * @param modifier 修饰符
 * @param firstText 主按钮文字，默认为"登录"
 * @param secondText 次按钮文字，默认为"暂无账号，进行注册"
 * @param onFirstClick 主按钮点击事件回调
 * @param isFirstEnabled 主按钮是否启用，默认为true
 * @param onSecondClick 次按钮点击事件回调
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
    val TAG = "ActionButtons"

    val firstClick = remember(onFirstClick) {
        {
            TimberLogger.d(TAG, "点击主操作按钮: $firstText")
            onFirstClick()
        }
    }
    val secondClick = remember(onSecondClick) {
        {
            TimberLogger.d(TAG, "点击次要操作按钮: $secondText")
            onSecondClick()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 主操作按钮（登录）
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
            onClick = firstClick
        )

        // 次要操作按钮（注册）
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
            onClick = secondClick
        )
    }
}