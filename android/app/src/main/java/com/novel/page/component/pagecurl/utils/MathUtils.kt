package com.novel.page.component.pagecurl.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * 线段相交工具函数
 * 
 * 计算两条线段的交点坐标
 *
 * @param p1 第一条线的起点
 * @param p2 第一条线的终点
 * @param p3 第二条线的起点
 * @param p4 第二条线的终点
 * @return 交点坐标，如果没有交点则返回null
 */
fun lineLineIntersection(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Offset? {
    val denominator = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
    
    // 如果分母为0，说明两条线平行，没有交点
    if (denominator == 0f) return null
    
    val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / denominator
    
    return Offset(
        p1.x + t * (p2.x - p1.x),
        p1.y + t * (p2.y - p1.y)
    )
}

/**
 * 点旋转工具函数
 * 
 * 将一个点围绕另一个点旋转指定角度
 *
 * @param center 旋转中心点
 * @param angle 旋转角度（弧度）
 * @return 旋转后的点坐标
 */
fun Offset.rotate(center: Offset, angle: Float): Offset {
    val cosA = cos(angle)
    val sinA = sin(angle)
    val dx = x - center.x
    val dy = y - center.y
    
    return Offset(
        center.x + dx * cosA - dy * sinA,
        center.y + dx * sinA + dy * cosA
    )
}

/**
 * 点围绕原点旋转工具函数
 * 
 * 将一个点围绕原点(0,0)旋转指定角度
 *
 * @param angle 旋转角度（弧度）
 * @return 旋转后的点坐标
 */
fun Offset.rotate(angle: Float): Offset {
    val cosA = cos(angle)
    val sinA = sin(angle)
    
    return Offset(
        x * cosA - y * sinA,
        x * sinA + y * cosA
    )
}
