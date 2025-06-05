package com.novel.page.home.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import com.novel.page.component.NovelDivider
import com.novel.page.component.NovelText
import com.novel.page.component.NovelImageView
import com.novel.page.home.utils.HomePerformanceOptimizer
import com.novel.ui.theme.NovelColors
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.wdp
import com.novel.utils.ssp
import com.novel.utils.HtmlTextUtil
import com.novel.utils.debounceClickable
import com.novel.page.component.FlipBookAnimationController
import androidx.compose.ui.graphics.Color

/**
 * 首页推荐书籍瀑布流网格组件 - 真正的参差不齐瀑布流布局
 */
@Composable
fun HomeRecommendGrid(
    books: List<SearchService.BookInfo> = emptyList(),
    homeBooks: List<HomeService.HomeBook> = emptyList(),
    onBookClick: (Long) -> Unit,
    onBookClickWithPosition: ((Long, Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    onLoadMore: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    fixedHeight: Boolean = false,  // 新增参数，用于在 LazyColumn 中使用
    flipBookController: FlipBookAnimationController? = null  // 添加动画控制器参数
) {
    if (fixedHeight) {
        // 在 LazyColumn 中使用的固定高度版本
        FixedHeightHomeRecommendGrid(
            books = books,
            homeBooks = homeBooks,
            onBookClick = onBookClick,
            onBookClickWithPosition = onBookClickWithPosition,
            onLoadMore = onLoadMore,
            modifier = modifier,
            flipBookController = flipBookController
        )
    } else {
        // 独立使用的完整瀑布流版本
        FullHeightHomeRecommendGrid(
            books = books,
            homeBooks = homeBooks,
            onBookClick = onBookClick,
            onBookClickWithPosition = onBookClickWithPosition,
            onLoadMore = onLoadMore,
            modifier = modifier,
            flipBookController = flipBookController
        )
    }
}

/**
 * 独立使用的完整瀑布流版本
 */
@Composable
private fun FullHeightHomeRecommendGrid(
    books: List<SearchService.BookInfo>,
    homeBooks: List<HomeService.HomeBook>,
    onBookClick: (Long) -> Unit,
    onBookClickWithPosition: ((Long, Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    val staggeredGridState = rememberLazyStaggeredGridState()

    // 监听滚动到底部，触发加载更多
    LaunchedEffect(staggeredGridState) {
        snapshotFlow { staggeredGridState.layoutInfo }
            .collect { layoutInfo ->
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount > 0 &&
                    visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
                ) {
                    onLoadMore()
                }
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2), // 固定2列
        state = staggeredGridState,
        modifier = modifier.padding(horizontal = 15.wdp),
        horizontalArrangement = Arrangement.spacedBy(10.wdp),
        verticalItemSpacing = 10.wdp,
        contentPadding = PaddingValues(vertical = 10.wdp)
    ) {
        // 根据数据源选择显示内容
        if (homeBooks.isNotEmpty()) {
            items(homeBooks, key = { it.bookId }) { book ->
                HomeBookStaggeredItem(
                    book = book,
                    onClick = { onBookClick(book.bookId) },
                    onClickWithPosition = onBookClickWithPosition?.let { callback ->
                        { offset, size -> callback(book.bookId, offset, size) }
                    },
                    flipBookController = flipBookController
                )
            }
        } else {
            items(books, key = { it.id }) { book ->
                SearchBookStaggeredItem(
                    book = book,
                    onClick = { onBookClick(book.id) },
                    onClickWithPosition = onBookClickWithPosition?.let { callback ->
                        { offset, size -> callback(book.id, offset, size) }
                    }
                )
            }
        }
    }
}

/**
 * 在 LazyColumn 中使用的固定高度版本 - 避免嵌套滚动
 */
@Composable
private fun FixedHeightHomeRecommendGrid(
    books: List<SearchService.BookInfo>,
    homeBooks: List<HomeService.HomeBook>,
    onBookClick: (Long) -> Unit,
    onBookClickWithPosition: ((Long, Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    // 当前显示的总数量
    val totalItems = if (homeBooks.isNotEmpty()) homeBooks.size else books.size

    Row(
        modifier = modifier.padding(horizontal = 15.wdp),
        horizontalArrangement = Arrangement.spacedBy(10.wdp)
    ) {
        if (homeBooks.isNotEmpty()) {
            // 处理首页推荐书籍 - 显示所有已加载的书籍
            val leftColumnBooks = homeBooks.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumnBooks = homeBooks.filterIndexed { index, _ -> index % 2 == 1 }

            // 左列
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.wdp)
            ) {
                leftColumnBooks.forEach { book ->
                    HomeBookStaggeredItem(
                        book = book,
                        onClick = { onBookClick(book.bookId) },
                        onClickWithPosition = onBookClickWithPosition?.let { callback ->
                            { offset, size -> callback(book.bookId, offset, size) }
                        },
                        flipBookController = flipBookController
                    )
                }
            }

            // 右列
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.wdp)
            ) {
                rightColumnBooks.forEach { book ->
                    HomeBookStaggeredItem(
                        book = book,
                        onClick = { onBookClick(book.bookId) },
                        onClickWithPosition = onBookClickWithPosition?.let { callback ->
                            { offset, size -> callback(book.bookId, offset, size) }
                        },
                        flipBookController = flipBookController
                    )
                }
            }
        } else {
            // 处理搜索结果书籍 - 显示所有已加载的书籍
            val leftColumnBooks = books.filterIndexed { index, _ -> index % 2 == 0 }
            val rightColumnBooks = books.filterIndexed { index, _ -> index % 2 == 1 }

            // 左列
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.wdp)
            ) {
                leftColumnBooks.forEach { book ->
                    SearchBookStaggeredItem(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onClickWithPosition = onBookClickWithPosition?.let { callback ->
                            { offset, size -> callback(book.id, offset, size) }
                        }
                    )
                }
            }

            // 右列
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.wdp)
            ) {
                rightColumnBooks.forEach { book ->
                    SearchBookStaggeredItem(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onClickWithPosition = onBookClickWithPosition?.let { callback ->
                            { offset, size -> callback(book.id, offset, size) }
                        }
                    )
                }
            }
        }
    }

    // 当显示的书籍数量是8的倍数且不为0时，自动触发加载更多
    LaunchedEffect(totalItems) {
        if (totalItems > 0 && totalItems % 8 == 0) {
            onLoadMore()
        }
    }
}

/**
 * 首页推荐书籍瀑布流项 - 自适应高度
 */
@Composable
private fun HomeBookStaggeredItem(
    book: HomeService.HomeBook,
    onClick: () -> Unit,
    onClickWithPosition: ((Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    modifier: Modifier = Modifier,
    flipBookController: FlipBookAnimationController? = null
) {
    // 使用缓存的自适应高度，增加变化范围
    val imageHeight = HomePerformanceOptimizer.getOptimizedImageHeight(
        bookId = book.bookId.toString(),
        minHeight = 200,
        maxHeight = 280
    ).wdp

    // 根据描述长度动态调整文本区域高度
    val descriptionLines = remember(book.bookDesc) {
        when {
            book.bookDesc.length > 80 -> 3
            book.bookDesc.length > 40 -> 2
            else -> 1
        }
    }

    // 位置追踪状态
    var positionInfo by remember {
        mutableStateOf(Pair(Offset.Zero, androidx.compose.ui.geometry.Size.Zero))
    }
    
    // 优化：检查是否当前书籍正在进行动画，减少重复计算
    val isCurrentBookAnimating = remember(flipBookController?.animationState) {
        flipBookController?.animationState?.let { animState ->
            animState.isAnimating && 
            animState.hideOriginalImage && 
            animState.bookId == book.bookId.toString()
        } ?: false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NovelColors.NovelBackground, RoundedCornerShape(8.wdp))
            .debounceClickable(onClick = {
                // 如果有位置回调，先调用位置回调，否则调用常规点击
                if (onClickWithPosition != null) {
                    onClickWithPosition(positionInfo.first, positionInfo.second)
                } else {
                    onClick()
                }
            })
            .clip(RoundedCornerShape(8.wdp))
    ) {
        // 书籍封面 - 自适应高度，支持动画状态隐藏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(if (isCurrentBookAnimating) Color.Transparent else NovelColors.NovelMain)
                .onGloballyPositioned { coordinates ->
                    // 追踪位置和尺寸
                    val windowRect = coordinates.boundsInWindow()
                    val newPosition = Offset(windowRect.left, windowRect.top)
                    val newSize = androidx.compose.ui.geometry.Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )

                    val currentInfo = positionInfo
                    if ((newPosition - currentInfo.first).getDistanceSquared() > 1f ||
                        kotlin.math.abs(newSize.width - currentInfo.second.width) > 1f ||
                        kotlin.math.abs(newSize.height - currentInfo.second.height) > 1f
                    ) {

                        positionInfo = newPosition to newSize
                    }
                }
        ) {
            if (!isCurrentBookAnimating) {
                // 正常状态：显示图片
                NovelImageView(
                    imageUrl = book.picUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            NovelText(
                                text = "暂无封面",
                                fontSize = 12.ssp,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                )
            }
        }

        // 书籍信息 - 动态高度
        Column(
            modifier = Modifier.padding(10.wdp)
        ) {
            // 书名
            NovelText(
                text = book.bookName,
                fontSize = 14.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.wdp))

            // 作者
            NovelText(
                text = book.authorName,
                fontSize = 12.ssp,
                color = NovelColors.NovelTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.wdp))

            // 描述 - 根据内容长度动态显示行数
            NovelText(
                text = HtmlTextUtil.cleanHtml(book.bookDesc),
                fontSize = 12.ssp,
                color = NovelColors.NovelTextGray,
                maxLines = descriptionLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 搜索结果书籍瀑布流项 - 自适应高度
 */
@Composable
private fun SearchBookStaggeredItem(
    book: SearchService.BookInfo,
    onClick: () -> Unit,
    onClickWithPosition: ((Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 使用缓存的自适应高度，增加变化范围
    val imageHeight = HomePerformanceOptimizer.getOptimizedImageHeight(
        bookId = book.id.toString(),
        minHeight = 200,
        maxHeight = 280
    ).wdp

    // 根据书名长度动态调整
    val titleLines = remember(book.bookName) {
        if (book.bookName.length > 12) 2 else 1
    }

    // 位置追踪状态
    var positionInfo by remember {
        mutableStateOf(Pair(Offset.Zero, androidx.compose.ui.geometry.Size.Zero))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NovelColors.NovelBackground, RoundedCornerShape(8.wdp))
            .debounceClickable(onClick = {
                // 如果有位置回调，先调用位置回调，否则调用常规点击
                if (onClickWithPosition != null) {
                    onClickWithPosition(positionInfo.first, positionInfo.second)
                } else {
                    onClick()
                }
            })
            .clip(RoundedCornerShape(8.wdp)),
    ) {
        // 书籍封面 - 自适应高度
        NovelImageView(
            imageUrl = book.picUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .background(NovelColors.NovelMain)
                .onGloballyPositioned { coordinates ->
                    // 追踪位置和尺寸
                    val windowRect = coordinates.boundsInWindow()
                    val newPosition = Offset(windowRect.left, windowRect.top)
                    val newSize = androidx.compose.ui.geometry.Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )

                    val currentInfo = positionInfo
                    if ((newPosition - currentInfo.first).getDistanceSquared() > 1f ||
                        kotlin.math.abs(newSize.width - currentInfo.second.width) > 1f ||
                        kotlin.math.abs(newSize.height - currentInfo.second.height) > 1f
                    ) {

                        positionInfo = newPosition to newSize
                    }
                },
            contentScale = ContentScale.Crop,
            placeholderContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    NovelText(
                        text = "暂无封面",
                        fontSize = 12.ssp,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        )

        // 书籍信息 - 动态高度
        Column(
            modifier = Modifier.padding(10.wdp)
        ) {
            // 书名 - 动态行数
            NovelText(
                text = book.bookName,
                fontSize = 14.ssp,
                fontWeight = FontWeight.Medium,
                color = NovelColors.NovelText,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.wdp))

            // 作者和分类
            NovelText(
                text = "${book.authorName} · ${book.categoryName}",
                fontSize = 12.ssp,
                color = NovelColors.NovelTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.wdp))

            // 字数和状态
            val statusText = if (book.bookStatus == 1) "完结" else "连载中"
            NovelText(
                text = "${formatWordCount(book.wordCount)} · $statusText",
                fontSize = 12.ssp,
                color = NovelColors.NovelTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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

/**
 * 加载更多指示器组件 - 可以在 LazyColumn 中独立使用
 */
@Composable
fun HomeRecommendLoadMoreIndicator(
    isLoading: Boolean,
    hasMoreData: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.wdp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                // 显示加载中状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                ) {
                    CircularProgressIndicator(
                        color = NovelColors.NovelMain,
                        modifier = Modifier.size(20.wdp),
                        strokeWidth = 2.wdp
                    )
                    NovelText(
                        text = "加载中...",
                        fontSize = 14.ssp,
                        color = NovelColors.NovelTextGray
                    )
                }
            }

            hasMoreData -> {
                // 显示加载中状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                ) {
                    CircularProgressIndicator(
                        color = NovelColors.NovelMain,
                        modifier = Modifier.size(20.wdp),
                        strokeWidth = 2.wdp
                    )
                    NovelText(
                        text = "加载中...",
                        fontSize = 14.ssp,
                        color = NovelColors.NovelTextGray
                    )
                }
            }

            else -> {
                // 没有更多数据 - 显示已加载全部
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.wdp)
                ) {
                    NovelDivider(
                        modifier = Modifier.width(60.wdp),
                        thickness = 1.wdp,
                    )
                    NovelText(
                        text = "已加载全部",
                        fontSize = 12.ssp,
                        color = NovelColors.NovelTextGray
                    )
                    NovelDivider(
                        modifier = Modifier.width(60.wdp),
                        thickness = 1.wdp,
                    )
                }
            }
        }
    }
} 