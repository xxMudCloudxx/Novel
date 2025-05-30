package com.novel.page.book.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 书籍简介顶部弹窗
 * @param description 书籍简介内容
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun BookDescriptionBottomSheet(
    description: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val cleaned = HtmlTextUtil.cleanHtml(description)
    val density = LocalDensity.current
    
    // 弹窗偏移状态
    var offsetY by remember { mutableFloatStateOf(0f) }
    val maxOffset = with(density) { 200.dp.toPx() } // 最大可拖拽距离
    
    // 监听滚动状态，只有在顶部时才允许拖拽关闭
    val canDragToClose = scrollState.value == 0
    
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
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // 拖拽结束时判断是否关闭
                            if (offsetY > maxOffset * 0.3f && canDragToClose) {
                                onDismiss()
                            } else {
                                offsetY = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        // 只有在顶部且向下拖拽时才允许移动
                        if (canDragToClose && dragAmount.y > 0) {
                            offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f).coerceAtMost(maxOffset)
                        }
                    }
                }
        ) {
            // 背景遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ -> }
                    }
            )
            
            // 弹窗内容
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = offsetY
                        alpha = 1f - (offsetY / maxOffset * 0.3f)
                    },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NovelColors.NovelBookBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.wdp, vertical = 16.wdp)
                ) {
                    // 拖拽指示器
                    Box(
                        modifier = Modifier
                            .width(40.wdp)
                            .height(4.wdp)
                            .background(
                                NovelColors.NovelTextGray.copy(alpha = 0.3f),
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
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = NovelColors.NovelTextGray
                            )
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "关闭",
                                modifier = Modifier.size(24.wdp)
                            )
                        }
                    }

                    // 简介内容 - 可滚动
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        NovelText(
                            text = cleaned.repeat(10),
                            fontSize = 16.ssp,
                            lineHeight = 24.ssp,
                            color = NovelColors.NovelText.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(bottom = 20.wdp)
                        )
                    }
                }
            }
        }
    }
} 