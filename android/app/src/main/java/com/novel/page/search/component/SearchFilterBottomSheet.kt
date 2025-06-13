package com.novel.page.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novel.page.component.NovelText
import com.novel.page.search.viewmodel.*
import com.novel.ui.theme.NovelColors
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * 搜索筛选弹窗
 */
@Composable
fun SearchFilterBottomSheet(
    filters: FilterState,
    onFiltersChange: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    var currentFilters by remember(filters) { mutableStateOf(filters) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // 弹窗内容
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NovelColors.NovelBookBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.wdp)
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NovelText(
                            text = "筛选",
                            fontSize = 18.ssp,
                            fontWeight = FontWeight.Bold,
                            color = NovelColors.NovelText
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = NovelColors.NovelTextGray,
                                modifier = Modifier.size(20.wdp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.wdp))
                    
                    // 筛选内容
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(20.wdp)
                    ) {
                        // 更新状态
                        item {
                            FilterSection(
                                title = "更新状态"
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                ) {
                                    UpdateStatus.values().take(3).forEach { status ->
                                        SearchFilterChip(
                                            text = status.displayName,
                                            selected = currentFilters.updateStatus == status,
                                            onClick = {
                                                currentFilters = currentFilters.copy(updateStatus = status)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.wdp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                ) {
                                    UpdateStatus.values().drop(3).forEach { status ->
                                        SearchFilterChip(
                                            text = status.displayName,
                                            selected = currentFilters.updateStatus == status,
                                            onClick = {
                                                currentFilters = currentFilters.copy(updateStatus = status)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // VIP状态
                        item {
                            FilterSection(
                                title = "是否VIP"
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                ) {
                                    VipStatus.values().forEach { vipStatus ->
                                        SearchFilterChip(
                                            text = vipStatus.displayName,
                                            selected = currentFilters.isVip == vipStatus,
                                            onClick = {
                                                currentFilters = currentFilters.copy(isVip = vipStatus)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 字数篇幅
                        item {
                            FilterSection(
                                title = "字数篇幅"
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                    ) {
                                        WordCountRange.values().take(4).forEach { range ->
                                            SearchFilterChip(
                                                text = range.displayName,
                                                selected = currentFilters.wordCountRange == range,
                                                onClick = {
                                                    currentFilters = currentFilters.copy(wordCountRange = range)
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.wdp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                    ) {
                                        WordCountRange.values().drop(4).forEach { range ->
                                            SearchFilterChip(
                                                text = range.displayName,
                                                selected = currentFilters.wordCountRange == range,
                                                onClick = {
                                                    currentFilters = currentFilters.copy(wordCountRange = range)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 排序方式
                        item {
                            FilterSection(
                                title = "排序方式"
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.wdp)
                                ) {
                                    SortBy.values().forEach { sortBy ->
                                        SearchFilterChip(
                                            text = sortBy.displayName,
                                            selected = currentFilters.sortBy == sortBy,
                                            onClick = {
                                                currentFilters = currentFilters.copy(sortBy = sortBy)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.wdp))
                    
                    // 底部按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.wdp)
                    ) {
                        // 清空按钮
                        OutlinedButton(
                            onClick = {
                                currentFilters = FilterState()
                                onClear()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NovelColors.NovelTextGray
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.foundation.BorderStroke(
                                    1.wdp,
                                    NovelColors.NovelTextGray.copy(alpha = 0.3f)
                                ).brush
                            )
                        ) {
                            NovelText(
                                text = "清空",
                                fontSize = 14.ssp,
                                color = NovelColors.NovelTextGray
                            )
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                onFiltersChange(currentFilters)
                                onApply()
                            },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NovelColors.NovelMain
                            ),
                            shape = RoundedCornerShape(8.wdp)
                        ) {
                            NovelText(
                                text = "确定",
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

/**
 * 筛选区块组件
 */
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
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

/**
 * 流式布局（简化版）
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // 使用简化的Row布局包装
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
} 