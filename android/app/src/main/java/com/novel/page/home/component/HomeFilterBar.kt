package com.novel.page.home.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.novel.page.component.NovelText
import com.novel.page.home.viewmodel.CategoryInfo
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import com.novel.utils.ssp
import com.novel.utils.animateTextStyleAsState
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList

/**
 * 首页筛选器组件
 */
@Composable
fun HomeFilterBar(
    filters: ImmutableList<CategoryInfo>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. 创建并记忆 LazyRow 的滚动状态
    val listState = rememberLazyListState()  // 控制 LazyRow 滚动的位置和动画 :contentReference[oaicite:0]{index=0}
    // 2. 创建 CoroutineScope，用于在点击时调用 animateScrollToItem
    val coroutineScope = rememberCoroutineScope()

    LazyRow(
        state = listState,  // 将滚动状态与 LazyRow 绑定
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(15.wdp),
        verticalAlignment = Alignment.Bottom,
        contentPadding = PaddingValues(horizontal = 15.wdp)
    ) {
        // 3. 使用 itemsIndexed，获取每个 FilterChip 的索引 index
        itemsIndexed(
            items = filters,
            key = { _, filter -> filter.id } // 为分类过滤器添加稳定 key
        ) { index, filter ->
            // 性能优化：使用 remember 缓存点击回调，避免每次重组都创建新 Lambda
            val onFilterClick = remember(filter.name, onFilterSelected, index, coroutineScope, listState) { {
                // 4. 点击时先回调外部状态，再决定是否要滚动
                onFilterSelected(filter.name)

                // 5. 当索引 >= 2（即第三个及之后的项），让列表滚动到 index-1
                if (index >= 3) {
                    coroutineScope.launch {
                        // 6. animateScrollToItem 会将目标 item 滚到视口最左侧（或根据偏移量滚动） :contentReference[oaicite:1]{index=1}
                        listState.animateScrollToItem(index = index - 2)
                    }
                }
            } }

            FilterChip(
                filter = filter.name,
                isSelected = (filter.name == selectedFilter),
                onClick = onFilterClick
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    // 1️⃣ 直接监听指针事件（比 InteractionSource 更早）
    val pressModifier = Modifier.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true          // 👉 立即进入放大态
                val up = waitForUpOrCancellation()
                pressed = false         // 👉 松手/取消复位
                if (up != null) onClick()   // 真·点击
            }
        }
    }

    // 2️⃣ 组合状态：pressed 优先级最高
    val big = pressed || isSelected
    val fontSize = if (big) 18.ssp else 16.ssp
    val weight   = if (big) FontWeight.Bold else FontWeight.Normal
    val color    = if (big) NovelColors.NovelText else NovelColors.NovelTextGray

    // 3️⃣ 极短 Tweed 动画，首帧几乎贴切
    val sizeAnim by animateTextStyleAsState(
        targetValue = TextStyle(fontSize = fontSize, fontWeight = weight, color = color),
        animationSpec = tween(durationMillis = 80)   // 很短的 tween，而非 spring
    )

    NovelText(
        text  = filter,
        style = sizeAnim,
        modifier = pressModifier.padding(vertical = 8.wdp),
        color = NovelColors.NovelText
    )
}