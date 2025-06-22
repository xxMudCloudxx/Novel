package com.novel.page.book.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 书评区域组件
 * 展示热门书评列表，包含评分和阅读时长
 */
@Composable
fun BookReviewsSection(reviews: List<BookDetailUiState.BookReview>) {
    val TAG = "BookReviewsSection"
    
    // 记录书评数量
    if (reviews.isNotEmpty()) {
        Log.d(TAG, "展示 ${reviews.size} 条书评")
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.wdp)
    ) {
        NovelText(
            text = "热门书评",
            fontSize = 18.ssp,
            fontWeight = FontWeight.Bold,
            color = NovelColors.NovelText
        )

        reviews.forEach { review ->
            BookReviewItem(review = review)
        }
    }
}

/**
 * 单个书评项组件
 * 包含头像、评论内容、星级评分和阅读时长
 */
@Composable
private fun BookReviewItem(review: BookDetailUiState.BookReview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.wdp),
        horizontalArrangement = Arrangement.spacedBy(7.wdp)
    ) {
        // 评论者头像（实心圆圈）
        Box(
            modifier = Modifier
                .padding(top = 2.wdp)
                .size(30.wdp)
                .background(NovelColors.NovelTextGray, CircleShape)
        )

        // 评论内容
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 评论文本
            NovelText(
                text = review.content,
                fontSize = 14.ssp,
                color = NovelColors.NovelText.copy(alpha = 0.8f) ,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 星级评分
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.wdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 渲染星级评分
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < review.rating) NovelColors.NovelText.copy(alpha = 0.8f) else NovelColors.NovelTextGray,
                        modifier = Modifier.size(12.wdp)
                    )
                }
                // 阅读时长
                NovelText(
                    text = review.readTime,
                    fontSize = 12.ssp,
                    color = NovelColors.NovelTextGray,
                    modifier = Modifier.padding(start = 4.wdp)
                )
            }
        }
    }
}
