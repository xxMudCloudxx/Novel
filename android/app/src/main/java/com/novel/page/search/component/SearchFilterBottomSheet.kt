package com.novel.page.search.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novel.page.component.NovelText
import com.novel.page.search.viewmodel.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlin.math.roundToInt

/**
 * 搜索筛选弹窗 - 可在整卡下滑关闭；支持“可选可空”的筛选逻辑
 */
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun SearchFilterBottomSheet(
    filters: FilterState,
    onFiltersChange: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    var currentFilters by remember(filters) { mutableStateOf(filters) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .debounceClickable(onClick = onDismiss)
        ) {
            Column(Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))

                // 弹窗内容
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (dragOffset > dismissThresholdPx) {
                                        onDismiss()
                                    } else {
                                        dragOffset = 0f
                                    }
                                }
                            ) { _, dragAmount ->
                                // 允许上下拖拽，但不可高于初始位置（dragOffset >= 0）
                                dragOffset = (dragOffset + dragAmount.y).coerceAtLeast(0f)
                            }
                        }
                        .debounceClickable(onClick = {}),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NovelColors.NovelBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.wdp)
                    ) {
                        // 标题栏
                        Box(Modifier.fillMaxWidth()) {
                            NovelText(
                                text = "筛选",
                                fontSize = 18.ssp,
                                fontWeight = FontWeight.Bold,
                                color = NovelColors.NovelText,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "关闭",
                                tint = NovelColors.NovelText,
                                modifier = Modifier
                                    .size(40.wdp)
                                    .debounceClickable(onClick = onDismiss)
                                    .align(Alignment.CenterStart)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.wdp))

                        // 筛选内容
                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(20.wdp)
                        ) {
                            FilterSection("更新状态") {
                                ThreeColumnFilterGrid(
                                    items = UpdateStatus.entries.drop(1), // 不含 ALL
                                    selectedItem = currentFilters.updateStatus,
                                    defaultItem = UpdateStatus.ALL,
                                    onItemSelected = {
                                        currentFilters = currentFilters.copy(updateStatus = it)
                                    },
                                    itemDisplayName = { it.displayName }
                                )
                            }

                            FilterSection("是否VIP") {
                                ThreeColumnFilterGrid(
                                    items = VipStatus.entries.drop(1),
                                    selectedItem = currentFilters.isVip,
                                    defaultItem = VipStatus.ALL,
                                    onItemSelected = {
                                        currentFilters = currentFilters.copy(isVip = it)
                                    },
                                    itemDisplayName = { it.displayName }
                                )
                            }

                            FilterSection("字数篇幅") {
                                ThreeColumnFilterGrid(
                                    items = WordCountRange.entries.drop(1),
                                    selectedItem = currentFilters.wordCountRange,
                                    defaultItem = WordCountRange.ALL,
                                    onItemSelected = {
                                        currentFilters =
                                            currentFilters.copy(wordCountRange = it)
                                    },
                                    itemDisplayName = { it.displayName }
                                )
                            }

                            FilterSection("排序方式") {
                                ThreeColumnFilterGrid(
                                    items = SortBy.entries.drop(1),
                                    selectedItem = currentFilters.sortBy,
                                    defaultItem = SortBy.NULL,
                                    onItemSelected = {
                                        currentFilters = currentFilters.copy(sortBy = it)
                                    },
                                    itemDisplayName = { it.displayName }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.wdp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.wdp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentFilters = FilterState()
                                    onClear()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NovelColors.NovelTextGray),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.foundation.BorderStroke(
                                        1.wdp,
                                        NovelColors.NovelTextGray.copy(alpha = 0.3f)
                                    ).brush
                                )
                            ) {
                                NovelText(
                                    "清空",
                                    fontSize = 14.ssp,
                                    color = NovelColors.NovelTextGray
                                )
                            }

                            Button(
                                onClick = {
                                    onFiltersChange(currentFilters)
                                    onApply()
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(containerColor = NovelColors.NovelMain),
                                shape = RoundedCornerShape(8.wdp)
                            ) {
                                NovelText(
                                    "确定",
                                    fontSize = 14.ssp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 三列网格，可“可选可空”
 */
@Composable
private fun <T> ThreeColumnFilterGrid(
    items: List<T>,
    selectedItem: T,
    defaultItem: T,
    onItemSelected: (T) -> Unit,
    itemDisplayName: (T) -> String,
    modifier: Modifier = Modifier
) {
    val rows = items.chunked(3)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.wdp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.wdp)
            ) {
                rowItems.forEach { item ->
                    val selected = selectedItem == item
                    SearchFilterChip(
                        text = itemDisplayName(item),
                        selected = selected,
                        onClick = {
                            if (selected) onItemSelected(defaultItem) else onItemSelected(item)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column {
        NovelText(
            text = title,
            fontSize = 15.ssp,
            fontWeight = FontWeight.Medium,
            color = NovelColors.NovelText,
            modifier = Modifier.padding(bottom = 8.wdp)
        )
        content()
    }
}
