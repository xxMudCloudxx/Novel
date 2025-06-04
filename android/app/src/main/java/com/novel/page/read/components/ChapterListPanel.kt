package com.novel.page.read.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp

/**
 * 章节信息数据类
 */
data class Chapter(
    val id: String,
    val chapterName: String,
    val chapterNum: String? = null,
    val isVip: String = "0"
)

/**
 * 章节列表侧滑面板
 * @param chapters 章节列表
 * @param currentChapterId 当前章节ID
 * @param backgroundColor 背景颜色
 * @param onChapterSelected 章节选择回调
 * @param onDismiss 关闭面板回调
 * @param modifier 修饰符
 */
@Composable
fun ChapterListPanel(
    chapters: List<Chapter>,
    currentChapterId: String,
    backgroundColor: Color,
    onChapterSelected: (Chapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 自动滚动到当前章节
    LaunchedEffect(currentChapterId, chapters) {
        val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIndex >= 0) {
            listState.scrollToItem(currentIndex)
        }
    }
    
    // 改为从下方弹起的布局
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f) // 占据屏幕高度的75%
            .background(
                backgroundColor,
                RoundedCornerShape(topStart = 16.wdp, topEnd = 16.wdp)
            )
            .clip(RoundedCornerShape(topStart = 16.wdp, topEnd = 16.wdp))
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.wdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "目录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NovelColors.NovelText
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.wdp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )
            
            // 章节列表
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.wdp)
            ) {
                items(chapters) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        isSelected = chapter.id == currentChapterId,
                        onClick = { onChapterSelected(chapter) }
                    )
                }
            }
        }
    }
}

/**
 * 章节项组件
 */
@Composable
private fun ChapterItem(
    chapter: Chapter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        NovelColors.NovelMain.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (isSelected) {
        NovelColors.NovelMain
    } else {
        NovelColors.NovelText
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.wdp, vertical = 12.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VIP标识
        if (chapter.isVip == "1") {
            Card(
                modifier = Modifier
                    .size(20.wdp)
                    .clip(RoundedCornerShape(4.wdp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIP",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.wdp))
        }
        
        // 章节名称
        Text(
            text = chapter.chapterName,
            fontSize = 14.sp,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // 当前章节指示器
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.wdp)
                    .background(
                        NovelColors.NovelMain,
                        RoundedCornerShape(4.wdp)
                    )
            )
        }
    }
} 