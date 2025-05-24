package com.novel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.novel.page.login.view.LoginPage.LoginPage
import com.novel.ui.theme.KxqTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KxqTheme {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(24.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    var clickCount by remember { mutableStateOf(0) }
//
//                    Button(onClick = {
//                        clickCount++
//                        // 构造要传给 RN 的消息
//                        val msg = "来自 Compose 的第 $clickCount 次问候 👋"
//                        startActivity(
//                            Intent(this@ComposeMainActivity, MainActivity::class.java)
//                                .putExtra("nativeMessage", msg)
//                        )
//                    }) {
//                        Text(text = "打开 React Native 页面")
//                    }
//                }
                LoginPage()
            }
        }
    }
}
