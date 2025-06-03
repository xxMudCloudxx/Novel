package com.novel.page.component.pagecurl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novel.page.component.pagecurl.config.rememberPageCurlConfig
import com.novel.page.component.pagecurl.page.ExperimentalPageCurlApi
import com.novel.page.component.pagecurl.page.PageCurl
import com.novel.page.component.pagecurl.page.rememberPageCurlState

/**
 * PageCurl集成示例
 * 
 * 演示如何使用PageCurl组件实现翻页效果
 */
@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PageCurlExample() {
    var currentPage by remember { mutableIntStateOf(0) }
    val pages = remember {
        listOf(
            "第一页\n欢迎使用PageCurl翻页效果",
            "第二页\n这是一个仿真书本翻页",
            "第三页\n支持拖拽和点击翻页",
            "第四页\n具有真实的卷曲效果",
            "第五页\n最后一页了"
        )
    }
    
    val state = rememberPageCurlState()
    
    // PageCurl配置
    val config = rememberPageCurlConfig(
        backPageColor = Color.White,
        shadowColor = Color.Black,
        dragForwardEnabled = true,
        dragBackwardEnabled = true,
        tapForwardEnabled = true,
        tapBackwardEnabled = true
    )
    
    PageCurl(
        count = pages.size,
        state = state,
        config = config
    ) { page ->
        PageContent(
            page = pages.getOrElse(page) { "空白页" },
            pageNumber = page + 1,
            backgroundColor = if (page % 2 == 0) Color.White else Color(0xFFF5F5F5)
        )
    }
}

/**
 * 页面内容组件
 */
@Composable
private fun PageContent(
    page: String,
    pageNumber: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = page,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            
            Text(
                text = "页码: $pageNumber",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
} 