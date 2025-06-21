package com.novel.page.book.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.HtmlTextUtil
import com.novel.utils.debounceClickable
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 书籍简介底部弹窗 - 性能优化版本，仿照SearchFilterBottomSheet实现拖动关闭
 * @param description 书籍简介内容
 * @param onDismiss 关闭弹窗回调
 */
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun BookDescriptionBottomSheet(
    description: String,
    onDismiss: () -> Unit
) {
    // 清理HTML内容 - 使用remember缓存
    val cleaned = remember(description) { HtmlTextUtil.cleanHtml(description) }
    
    // 拖动状态管理 - 仿照SearchFilterBottomSheet
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    
    // 动画状态管理
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
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

                // 弹窗内容 - 使用SearchFilterBottomSheet的拖动方式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .scale(scale)
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
                        .debounceClickable(onClick = {}), // 防止点击弹窗内容时关闭
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NovelColors.NovelBookBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.wdp, vertical = 16.wdp)
                    ) {
                        Spacer(modifier = Modifier.height(12.wdp))
                        
                        // 标题栏
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.wdp)
                        ) {
                            NovelText(
                                text = "简介",
                                fontSize = 18.ssp,
                                fontWeight = FontWeight.Bold,
                                color = NovelColors.NovelText,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "关闭",
                                    tint = NovelColors.NovelTextGray,
                                    modifier = Modifier.size(24.wdp)
                                )
                            }
                        }

                        // 简介内容 - 使用LazyColumn优化性能
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            contentPadding = PaddingValues(bottom = 20.wdp)
                        ) {
                            item {
                                NovelText(
                                    text = cleaned,
                                    fontSize = 16.ssp,
                                    lineHeight = 24.ssp,
                                    color = NovelColors.NovelText.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 