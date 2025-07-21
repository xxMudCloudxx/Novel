package com.novel.page.book.components

import com.novel.utils.TimberLogger
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.novel.page.book.viewmodel.BookDetailUiState
import com.novel.page.component.NovelText
import com.novel.page.component.NovelWeakenButton
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 作者信息展示组件
 * 包含作者头像、姓名和关注按钮
 */
@Composable
fun AuthorSection(
    bookInfo: BookDetailUiState.BookInfo?,
    onFollowAuthor: ((String) -> Unit)? = null
) {
    val TAG = "AuthorSection"
    
    // 空数据保护
    if (bookInfo == null) {
        TimberLogger.w(TAG, "BookInfo为空，跳过渲染")
        return
    }

    // 记忆化关注按钮点击事件
    val followClick = remember(bookInfo.authorName, onFollowAuthor) {
        {
            TimberLogger.d(TAG, "点击关注作者: ${'$'}{bookInfo.authorName}")
            onFollowAuthor?.invoke(bookInfo.authorName) ?: Unit
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 15.wdp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 作者头像（实心圆圈）
        Box(
            modifier = Modifier
                .size(25.wdp)
                .background(NovelColors.NovelTextGray, CircleShape)
        )
        
        // 作者名字
        NovelText(
            text = bookInfo.authorName,
            fontSize = 14.ssp,
            color = NovelColors.NovelText.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 8.wdp)
        )
        
        // 关注按钮
        NovelWeakenButton(
            content = {
                NovelText(
                    text = "关注",
                    fontSize = 14.ssp,
                    color = NovelColors.NovelText.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.wdp)
                )
            },
            onClick = followClick,
            round = 18.wdp,
            color = NovelColors.NovelTextGray.copy(alpha = 0.1f),
        )
    }
}