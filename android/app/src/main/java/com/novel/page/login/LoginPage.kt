package com.novel.page.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.novel.page.component.NovelMainButton
import com.novel.page.component.NovelText
import com.novel.page.component.NovelWeakenButton
import com.novel.page.login.component.LoginAppBar
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
import com.novel.utils.ssp
import com.novel.utils.wdp

@Composable
fun LoginPage() {
    Column(
        modifier = Modifier.fillMaxSize().background(color = NovelColors.NovelBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginAppBar();
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
                .width(330.wdp)
                .height(44.wdp),
            onClick = {}
        )
        NovelWeakenButton(
            content = {
                NovelText(
                    "其他号码一键登录",
                    fontSize = 16.ssp,
                    fontWeight = FontWeight.Bold,
                    color = NovelColors.NovelText
                )
            },
            modifier = Modifier
                .width(330.wdp)
                .height(44.wdp),
            onClick = {}
        )
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    NovelTheme {
        AdaptiveScreen {
            Box(modifier = Modifier.fillMaxSize()) {
                LoginPage()
            }
        }
    }
}