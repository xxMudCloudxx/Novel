package com.novel.page.login.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.novel.page.component.BackButton
import com.novel.page.component.NovelText
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 登录页面的AppBar
 */
@Composable
fun LoginAppBar(){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.wdp)
            .padding(horizontal = 16.wdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton()
        NovelText("帮助", fontSize = 16.ssp, lineHeight = 20.ssp, fontWeight = FontWeight.Bold)
    }
}