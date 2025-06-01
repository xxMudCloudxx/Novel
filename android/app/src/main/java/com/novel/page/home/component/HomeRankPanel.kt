package com.novel.page.home.component

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.novel.page.component.NovelText
import com.novel.page.component.NovelImageView
import com.novel.page.component.FlipBookAnimationController
import com.novel.page.home.dao.HomeRepository
import com.novel.page.home.viewmodel.CategoryInfo
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.network.api.front.BookService
import com.novel.utils.wdp
import com.novel.utils.ssp
import kotlinx.coroutines.launch
import com.novel.page.component.rememberBookClickHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 首页榜单面板组件 - 连续滚动但带精确距离控制，支持3D翻书动画
 */
@Composable
fun HomeRankPanel(
    rankBooks: List<BookService.BookRank>,
    selectedRankType: String,
    onRankTypeSelected: (String) -> Unit,
    onBookClick: (Long, Offset, Size) -> Unit,
    modifier: Modifier = Modifier,
    // 翻书动画控制器
    flipBookController: FlipBookAnimationController? = null
) {
    val rankTypes = listOf(
        CategoryInfo(HomeRepository.RANK_TYPE_VISIT, "点击榜"),
        CategoryInfo(HomeRepository.RANK_TYPE_UPDATE, "更新榜"),
        CategoryInfo(HomeRepository.RANK_TYPE_NEWEST, "新书榜")
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(370.wdp),
        shape = RoundedCornerShape(8.wdp),
        colors = CardDefaults.cardColors(
            containerColor = NovelColors.NovelBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.wdp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 榜单类型筛选器
            RankFilterBar(
                rankTypes = rankTypes,
                selectedRankType = selectedRankType,
                onRankTypeSelected = onRankTypeSelected,
                modifier = Modifier.padding(top = 10.wdp)
            )

            // 榜单书籍连续滚动列表 - 最多16本书，每列4本
            val limitedBooks = rankBooks.take(16)

            if (limitedBooks.isNotEmpty()) {
                RankBooksScrollableGrid(
                    books = limitedBooks,
                    selectedRankType = selectedRankType,
                    onBookClick = onBookClick,
                    flipBookController = flipBookController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.wdp)
                )
            }
        }
    }
}

/**
 * 榜单书籍可滚动网格 - 精确距离控制，支持翻书动画
 */
@SuppressLint("RememberReturnType")
@Composable
private fun RankBooksScrollableGrid(
    books: List<BookService.BookRank>,
    selectedRankType: String,
    onBookClick: (Long, Offset, Size) -> Unit,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    // 将书籍按每列4本分组
    val bookColumns = books.chunked(4)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val snapFlingBehavior = rememberSnapFlingBehavior(listState)

    // 监听筛选器变化，重置滚动位置
    LaunchedEffect(selectedRankType) {
        coroutineScope.launch {
            listState.animateScrollToItem(0, 0)
        }
    }

    LazyRow(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.wdp),
        contentPadding = PaddingValues(horizontal = 4.wdp)
    ) {
        items(bookColumns.size) { columnIndex ->
            RankBookColumn(
                books = bookColumns[columnIndex],
                startRank = columnIndex * 4 + 1,
                onBookClick = onBookClick,
                flipBookController = flipBookController,
                modifier = Modifier.width(if (columnIndex != bookColumns.size - 1) 200.wdp else 312.wdp)
            )
        }
    }
}

/**
 * 榜单书籍列 - 每列显示4本书，支持翻书动画
 */
@Composable
private fun RankBookColumn(
    books: List<BookService.BookRank>,
    startRank: Int,
    onBookClick: (Long, Offset, Size) -> Unit,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    Column(
        modifier = modifier.padding(vertical = 8.wdp),
        verticalArrangement = Arrangement.spacedBy(8.wdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        books.forEachIndexed { index, book ->
            RankBookGridItem(
                book = book,
                rank = startRank + index,
                onClick = onBookClick,
                flipBookController = flipBookController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 榜单筛选器 - 与HomeFilterBar相同的UI风格
 */
@Composable
private fun RankFilterBar(
    rankTypes: List<CategoryInfo>,
    selectedRankType: String,
    onRankTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        rankTypes.forEach { rankType ->
            RankFilterChip(
                filter = rankType.name,
                isSelected = rankType.name == selectedRankType,
                onClick = { onRankTypeSelected(rankType.name) }
            )
        }
    }
}

/**
 * 榜单筛选按钮 - 与FilterChip相同的UI
 */
@Composable
private fun RankFilterChip(
    filter: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NovelText(
        text = filter,
        fontSize = 14.ssp,
        modifier = Modifier
            .debounceClickable(onClick = onClick)
            .padding(vertical = 8.wdp, horizontal = 12.wdp),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) NovelColors.NovelText else NovelColors.NovelTextGray
    )
}

/**
 * 榜单书籍网格项 - 垂直布局适应网格，支持翻书动画
 */
@Composable
private fun RankBookGridItem(
    book: BookService.BookRank,
    rank: Int,
    onClick: (Long, Offset, Size) -> Unit,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    var coverPosition by remember { mutableStateOf(Offset.Zero) }
    var coverSizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // 创建翻书动画点击处理器
    val bookClickHandler = if (flipBookController != null) {
        rememberBookClickHandler(
            controller = flipBookController,
            bookId = book.id.toString(),
            imageUrl = book.picUrl
        )
    } else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .debounceClickable(onClick = {
                // 先触发翻书动画，然后执行原有的点击逻辑
                bookClickHandler?.invoke()
                onClick(
                    book.id,
                    coverPosition,
                    coverSizePx
                )
            })
            .padding(2.wdp),
        verticalAlignment = Alignment.Top
    ) {
        // 书籍封面 - 直接显示，动画将在全局覆盖层进行
        Box(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    // boundsInWindow() 返回一个 Rect，单位默认是 px
                    val windowRect = coordinates.boundsInWindow()
                    coverPosition = Offset(
                        windowRect.left,
                        windowRect.top - 120.wdp.value // 去掉偏移调整，使用原始位置
                    )
                    coverSizePx = androidx.compose.ui.geometry.Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                    Log.d("RankPanel", "书籍位置: $coverPosition, 尺寸: $coverSizePx")
                }
        ) { BookCoverImage(book = book) }

        Spacer(modifier = Modifier.width(4.wdp))

        // 排名数字
        NovelText(
            modifier = Modifier.padding(top = 2.wdp),
            text = rank.toString(),
            fontSize = 16.ssp,
            fontWeight = FontWeight.Bold,
            color = NovelColors.NovelMain
        )

        Spacer(modifier = Modifier.width(4.wdp))

        // 书籍信息 - 垂直布局
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            // 书名
            NovelText(
                text = book.bookName,
                fontSize = 14.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelText,
                lineHeight = 15.ssp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.wdp))

            // 分类
            NovelText(
                text = book.categoryName,
                fontSize = 12.ssp,
                lineHeight = 13.ssp,
                color = NovelColors.NovelMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 书籍封面图片组件 - 提取为独立组件便于复用
 */
@Composable
private fun BookCoverImage(book: BookService.BookRank) {
    NovelImageView(
        imageUrl = book.picUrl,
        modifier = Modifier
            .width(50.wdp)
            .height(65.wdp)
            .clip(RoundedCornerShape(4.wdp))
            .background(NovelColors.NovelMain),
        contentScale = ContentScale.Crop,
        placeholderContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovelColors.NovelMain),
                contentAlignment = Alignment.Center
            ) {
                NovelText(
                    text = "暂无封面",
                    fontSize = 6.ssp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
        }
    )
}

/**
 * 格式化字数显示
 */
private fun formatWordCount(wordCount: Int): String {
    return when {
        wordCount >= 10000 -> "${wordCount / 10000}万字"
        wordCount >= 1000 -> "${wordCount / 1000}千字"
        else -> "${wordCount}字"
    }
} 