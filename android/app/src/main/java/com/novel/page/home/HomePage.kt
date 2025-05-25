package com.novel.page.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.novel.utils.wdp
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.novel.MainActivity

@Composable
fun HomePage() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.wdp),
        contentAlignment = Alignment.Center
    ) {
        var clickCount by remember { mutableIntStateOf(0) }

        Button(onClick = {
            clickCount++
            val msg = "来自 Compose 的第 $clickCount 次问候 👋"
            // 使用正确的上下文和目标Activity类名
            val intent = Intent(context, MainActivity::class.java)
                .putExtra("nativeMessage", msg)
            context.startActivity(intent) // 直接启动Activity
        }) {
            Text(text = "打开 React Native 页面")
        }
    }

}