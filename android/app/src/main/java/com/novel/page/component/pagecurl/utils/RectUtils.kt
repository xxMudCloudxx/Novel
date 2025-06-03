package com.novel.page.component.pagecurl.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * 矩形工具扩展函数
 * 
 * 提供便捷的矩形操作方法
 */

/**
 * 获取矩形的中心点
 * 
 * @return 矩形的中心点坐标
 */
//val Rect.center: Offset
//    get() = Offset(centerX, centerY)

/**
 * 根据容器尺寸缩放矩形
 * 
 * 将相对坐标（0-1范围）转换为实际像素坐标
 *
 * @param size 容器尺寸
 * @return 缩放后的矩形
 */
fun Rect.multiply(size: IntSize): Rect {
    return Rect(
        topLeft = Offset(left * size.width, top * size.height),
        bottomRight = Offset(right * size.width, bottom * size.height)
    )
}

/**
 * 创建相对坐标矩形
 * 
 * @param left 左边界（0-1）
 * @param top 顶边界（0-1）
 * @param right 右边界（0-1）
 * @param bottom 底边界（0-1）
 * @return 相对坐标矩形
 */
fun relativeRect(left: Float, top: Float, right: Float, bottom: Float): Rect {
    return Rect(
        topLeft = Offset(left, top),
        bottomRight = Offset(right, bottom)
    )
}
