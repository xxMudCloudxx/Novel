package com.novel.page.book.components

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.HtmlTextUtil
import com.novel.utils.ssp
import com.novel.utils.wdp
import kotlinx.coroutines.launch

/**
 * 书籍简介底部弹窗 - 性能优化版本
 * @param description 书籍简介内容
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun BookDescriptionBottomSheet(
    description: String,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // 清理HTML内容 - 使用remember缓存
    val cleaned = remember(description) { HtmlTextUtil.cleanHtml(description) }
    
    // 动画状态管理
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    // 常量配置
    val maxOffset = remember { with(density) { 150.dp.toPx() } }
    val dismissThreshold = maxOffset * 0.4f
    
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
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetY > dismissThreshold) {
                                onDismiss()
                            } else {
                                offsetY = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        if (dragAmount.y > 0) {
                            offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f).coerceAtMost(maxOffset)
                        }
                    }
                }
        ) {
            // 弹窗内容
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .align(Alignment.BottomCenter)
                    .scale(scale)
                    .graphicsLayer {
                        translationY = offsetY
                        alpha = (1f - offsetY / maxOffset * 0.5f).coerceAtLeast(0.3f)
                    },
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
                    // 拖拽指示器 - 优化动画
                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (offsetY > 0) 0.8f else 0.3f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "indicatorAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(40.wdp)
                            .height(4.wdp)
                            .background(
                                NovelColors.NovelTextGray.copy(alpha = indicatorAlpha),
                                RoundedCornerShape(2.wdp)
                            )
                            .align(Alignment.CenterHorizontally)
                    )
                    
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