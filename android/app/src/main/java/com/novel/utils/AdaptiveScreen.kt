package com.novel.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 自适应屏幕组件
 * 
 * 核心功能：
 * - 基于设计稿尺寸进行屏幕适配
 * - 分别控制宽高缩放比例，避免变形
 * - 自动设置全局缩放因子
 * - 支持不同屏幕尺寸的UI一致性
 * 
 * 适配策略：
 * - 设计基准：375×852像素
 * - 动态计算：实际屏幕/设计尺寸
 * - 比例应用：通过全局变量控制UI元素缩放
 * - 居中对齐：确保内容在不同屏幕上居中显示
 * 
 * 使用场景：
 * - 根布局包装器
 * - 需要精确适配的页面
 * - 跨设备UI一致性保证
 *
 * @param modifier 修饰符
 * @param content Composable内容
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val TAG = "AdaptiveScreen"
    // 设计稿的基准尺寸
    val DESIGN_WIDTH = 375.dp
    val DESIGN_HEIGHT = 852.dp

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 计算缩放比例
        val scaleX = maxWidth / DESIGN_WIDTH
        val scaleY = maxHeight / DESIGN_HEIGHT
        
        // 设置全局比例
        globalScaleX = scaleX
        globalScaleY = scaleY
        
        // 记录适配信息（仅在调试模式下）
        LaunchedEffect(scaleX, scaleY) {
            TimberLogger.d(TAG, "屏幕适配 - 实际尺寸: ${maxWidth}×${maxHeight}, 设计尺寸: ${DESIGN_WIDTH}×${DESIGN_HEIGHT}")
            TimberLogger.d(TAG, "缩放比例 - X: $scaleX, Y: $scaleY")
        }

        content()
    }
}
