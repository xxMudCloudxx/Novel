package com.novel.page.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.novel.page.component.RankingNumber
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 推荐榜单区域
 */
@Composable
fun RankingSection(
    novelRanking: List<SearchRankingItem>,
    dramaRanking: List<SearchRankingItem>,
    newBookRanking: List<SearchRankingItem>,
    onRankingItemClick: (Long) -> Unit,
    onViewFullRanking: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.wdp),
    ) {
        item {
            Spacer(modifier = Modifier.width(8.wdp))
        }
        item {
            RankingSectionItem(
                title = "点击榜",
                items = novelRanking,
                onItemClick = onRankingItemClick,
                onViewFullRanking = { onViewFullRanking("点击榜") }
            )
        }
        item {
            RankingSectionItem(
                title = "推荐榜",
                items = dramaRanking,
                onItemClick = onRankingItemClick,
                onViewFullRanking = { onViewFullRanking("推荐榜") }
            )
        }
        item {
            RankingSectionItem(
                title = "新书榜",
                items = newBookRanking,
                onItemClick = onRankingItemClick,
                onViewFullRanking = { onViewFullRanking("新书榜") }
            )
        }
        item {
            Spacer(modifier = Modifier.width(8.wdp))
        }
    }
}

/**
 * 单个榜单区域
 */
@Composable
private fun RankingSectionItem(
    title: String,
    items: List<SearchRankingItem>,
    onItemClick: (Long) -> Unit,
    onViewFullRanking: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(225.wdp)
            .background(
                color = NovelColors.NovelBookBackground,
                shape = RoundedCornerShape(8.wdp)
            )
            .padding(vertical = 20.wdp, horizontal = 12.wdp)
    ) {
        // 榜单标题
        NovelText(
            text = title,
            fontSize = 16.ssp,
            fontWeight = FontWeight.Bold,
            color = NovelColors.NovelText,
            modifier = Modifier.padding(bottom = 12.wdp)
        )

        // 榜单列表
        RankingList(
            items = items,
            onItemClick = onItemClick,
            onViewFullRanking = onViewFullRanking
        )
    }
}

/**
 * 排行榜项数据类
 */
data class SearchRankingItem(
    val id: Long,
    val title: String,
    val author: String,
    val rank: Int
)


/**
 * 榜单列表组件
 * 显示文本列表格式：序号 + 标题 + 作者名字
 */
@Composable
fun RankingList(
    items: List<SearchRankingItem>,
    onItemClick: (Long) -> Unit,
    onViewFullRanking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.wdp),
        verticalArrangement = Arrangement.spacedBy(4.wdp)
    ) {
        items.take(15).forEach { item ->
            RankingListItem(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }
        
        // 如果超过15个且提供了完整榜单回调，显示"查看完整榜单>"链接
        if (items.size > 15) {
            Spacer(modifier = Modifier.height(4.wdp))
            NovelText(
                text = "查看完整榜单 >",
                fontSize = 13.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelMain,
                modifier = Modifier
                    .fillMaxWidth()
                    .debounceClickable(onClick = onViewFullRanking)
                    .padding(vertical = 8.wdp, horizontal = 4.wdp)
            )
        }
    }
}

/**
 * 单个榜单列表项
 */
@Composable
private fun RankingListItem(
    item: SearchRankingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .debounceClickable(onClick = onClick),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.wdp)
    ) {
        // 排名序号
        RankingNumber(
            rank = item.rank,
        )

        // 标题和作者信息
        Column {
            // 书籍标题
            NovelText(
                text = item.title,
                fontSize = 13.ssp,
                lineHeight = 13.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 作者名字
            NovelText(
                text = item.author,
                fontSize = 12.ssp,
                fontWeight = FontWeight.Normal,
                color = NovelColors.NovelTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}