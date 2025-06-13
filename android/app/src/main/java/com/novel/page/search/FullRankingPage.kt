package com.novel.page.search

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelText
import com.novel.page.component.RankingNumber
import com.novel.page.search.component.SearchRankingItem
import com.novel.page.search.viewmodel.SearchViewModel
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 完整榜单页面
 */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullRankingPage(
    rankingType: String,
    rankingItems: List<SearchRankingItem>,
    onNavigateBack: () -> Unit,
    onNavigateToBookDetail: (Long) -> Unit
) {
    // 顶部栏状态和滚动行为 - 修复滚动问题
    val toolbarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(toolbarState)
    
    // 计算折叠进度
    val progress by derivedStateOf {
        if (toolbarState.heightOffsetLimit == 0f) 0f
        else (-toolbarState.heightOffset / toolbarState.heightOffsetLimit).coerceIn(0f, 1f)
    }
    
    // 当前日期
    val currentDate = remember {
        SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date())
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 自定义顶部栏 - 简化布局，使用固定高度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDF8F6), 
                                Color.White
                            )
                        )
                    )
            ) {
                // 返回按钮
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 8.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = NovelColors.NovelText
                    )
                }
                
                // 标题容器 - 简化布局
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // 主标题
                    NovelText(
                        text = rankingType,
                        fontSize = 20.ssp,
                        fontWeight = FontWeight.Bold,
                        color = NovelColors.NovelText
                    )
                    
                    Spacer(modifier = Modifier.height(8.wdp))
                    
                    // 副标题
                    NovelText(
                        text = "根据真实搜索更新",
                        fontSize = 12.ssp,
                        color = NovelColors.NovelTextGray
                    )
                }
            }
        }
    ) { innerPadding ->
        // 榜单内容
        if (rankingItems.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NovelText(
                        text = "📋",
                        fontSize = 48.ssp,
                        color = NovelColors.NovelTextGray
                    )
                    Spacer(modifier = Modifier.height(16.wdp))
                    NovelText(
                        text = "暂无榜单数据",
                        fontSize = 16.ssp,
                        color = NovelColors.NovelTextGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(2.wdp)
            ) {
                items(
                    items = rankingItems,
                    key = { it.id }
                ) { item ->
                    FullRankingItem(
                        item = item,
                        onClick = { onNavigateToBookDetail(item.id) }
                    )
                }
                
                // 底部空间
                item {
                    Spacer(modifier = Modifier.height(16.wdp))
                }
            }
        }
    }
}

/**
 * 完整榜单项
 */
@Composable
private fun FullRankingItem(
    item: SearchRankingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 16.wdp, vertical = 12.wdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.wdp)
    ) {
        // 排名序号
        RankingNumber(
            rank = item.rank
        )
        
        // 书籍信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 书名
            NovelText(
                text = item.title,
                fontSize = 15.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelText,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(2.wdp))
            
            // 作者
            NovelText(
                text = item.author,
                fontSize = 13.ssp,
                color = NovelColors.NovelTextGray,
                maxLines = 1
            )
        }
        
        // 热搜数字（模拟数据）
        NovelText(
            text = "${(item.rank * 1000 + kotlin.random.Random.nextInt(500))}热搜",
            fontSize = 12.ssp,
            color = NovelColors.NovelTextGray.copy(alpha = 0.7f)
        )
    }
} 