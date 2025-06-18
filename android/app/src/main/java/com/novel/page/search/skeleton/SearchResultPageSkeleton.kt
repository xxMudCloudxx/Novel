package com.novel.page.search.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.wdp
import com.valentinilk.shimmer.shimmer

/**
 * 搜索结果页面骨架屏
 */
@Composable
fun SearchResultPageSkeleton() {
    val skeletonColor = NovelColors.NovelTextGray.copy(alpha = 0.2f)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovelColors.NovelBackground)
            .shimmer()
    ) {
        // 顶部搜索栏骨架
        SearchResultTopBarSkeleton(skeletonColor)
        
        // 分类筛选骨架
        CategoryFilterRowSkeleton(skeletonColor)
        
        // 搜索结果列表骨架
        SearchResultListSkeleton(skeletonColor)
    }
}

@Composable
private fun SearchResultTopBarSkeleton(skeletonColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.wdp)
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 16.wdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.wdp)
    ) {
        // 返回按钮骨架
        Box(
            modifier = Modifier
                .size(24.wdp)
                .background(skeletonColor, CircleShape)
        )
        
        // 搜索框骨架
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.wdp)
                .clip(RoundedCornerShape(20.wdp))
                .background(skeletonColor)
        )
        
        // 搜索按钮骨架
        Box(
            modifier = Modifier
                .width(48.wdp)
                .height(32.wdp)
                .clip(RoundedCornerShape(16.wdp))
                .background(skeletonColor)
        )
    }
}

@Composable
private fun CategoryFilterRowSkeleton(skeletonColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.wdp)
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 16.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类标签骨架
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            items(5) { index ->
                Box(
                    modifier = Modifier
                        .width((60 + index * 10).wdp)
                        .height(32.wdp)
                        .clip(RoundedCornerShape(16.wdp))
                        .background(skeletonColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.wdp))
        
        // 筛选按钮骨架
        Box(
            modifier = Modifier
                .width(48.wdp)
                .height(32.wdp)
                .clip(RoundedCornerShape(16.wdp))
                .background(skeletonColor)
        )
    }
}

@Composable
private fun SearchResultListSkeleton(skeletonColor: Color) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.wdp)
    ) {
        items(10) { // 显示10个骨架项目
            SearchResultItemSkeleton(skeletonColor)
        }
    }
}

@Composable
private fun SearchResultItemSkeleton(skeletonColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.wdp, vertical = 12.wdp),
        horizontalArrangement = Arrangement.spacedBy(12.wdp)
    ) {
        // 书籍封面骨架
        Box(
            modifier = Modifier
                .width(75.wdp)
                .height(100.wdp)
                .clip(RoundedCornerShape(8.wdp))
                .background(skeletonColor)
        )
        
        // 书籍信息骨架
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.wdp)
        ) {
            // 书名骨架
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(18.wdp)
                    .clip(RoundedCornerShape(2.wdp))
                    .background(skeletonColor)
            )
            
            // 作者骨架
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.wdp)
                    .clip(RoundedCornerShape(2.wdp))
                    .background(skeletonColor)
            )
            
            // 简介骨架（多行）
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (index == 2) it.fillMaxWidth(0.7f) else it }
                        .height(12.wdp)
                        .clip(RoundedCornerShape(2.wdp))
                        .background(skeletonColor)
                )
            }
            
            // 标签和统计信息骨架
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.wdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类标签骨架
                Box(
                    modifier = Modifier
                        .width(40.wdp)
                        .height(20.wdp)
                        .clip(RoundedCornerShape(10.wdp))
                        .background(skeletonColor)
                )
                
                // 字数骨架
                Box(
                    modifier = Modifier
                        .width(50.wdp)
                        .height(12.wdp)
                        .clip(RoundedCornerShape(2.wdp))
                        .background(skeletonColor)
                )
                
                // 状态骨架
                Box(
                    modifier = Modifier
                        .width(30.wdp)
                        .height(12.wdp)
                        .clip(RoundedCornerShape(2.wdp))
                        .background(skeletonColor)
                )
            }
        }
        
        // 更多按钮骨架
        Box(
            modifier = Modifier
                .size(24.wdp)
                .background(skeletonColor, CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultPageSkeletonPreview() {
    NovelTheme {
        SearchResultPageSkeleton()
    }
} 