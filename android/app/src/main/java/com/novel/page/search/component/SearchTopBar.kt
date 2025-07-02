package com.novel.page.search.component

import com.novel.utils.TimberLogger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.novel.page.component.NovelTextField
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

private const val TAG = "SearchTopBar"

/**
 * 搜索页面顶部导航栏组件
 * 
 * 提供返回按钮、搜索输入框和搜索按钮的统一界面
 * 
 * @param query 当前搜索关键词
 * @param onQueryChange 搜索词变更回调
 * @param onBackClick 返回按钮点击回调
 * @param onSearchClick 搜索按钮点击回调
 */
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.wdp, vertical = 12.wdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.wdp)
    ) {
        // 返回按钮
        IconButton(
            onClick = {
                TimberLogger.d(TAG, "返回按钮被点击")
                onBackClick()
            },
            modifier = Modifier.size(40.wdp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = NovelColors.NovelText
            )
        }

        // 搜索输入框
        NovelTextField(
            value = query,
            onValueChange = { newQuery ->
                TimberLogger.v(TAG, "搜索词变更: '$newQuery'")
                onQueryChange(newQuery)
            },
            placeText = "搜索小说或作者",
            modifier = Modifier
                .weight(1f)
                .height(40.wdp)
                .clip(RoundedCornerShape(5.dp))
        )

        // 搜索按钮
        IconButton(
            onClick = {
                TimberLogger.d(TAG, "搜索按钮被点击, 关键词: '$query'")
                onSearchClick()
            },
            modifier = Modifier.size(40.wdp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = NovelColors.NovelMain
            )
        }
    }
}