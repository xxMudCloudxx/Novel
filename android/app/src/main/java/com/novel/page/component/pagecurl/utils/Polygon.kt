package com.novel.page.component.pagecurl.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * 多边形工具类
 * 
 * 用于创建和操作多边形路径，主要服务于PageCurl的卷曲效果绘制
 * 提供多边形的基本几何操作，如路径转换、边界计算、平移和扩展
 *
 * @param points 多边形的顶点列表，按顺序连接
 */
internal class Polygon(private val points: List<Offset>) {

    /**
     * 转换为Compose Path对象
     * 
     * 将多边形顶点转换为可绘制的Path路径
     * 
     * @return 闭合的Path对象，可用于Canvas绘制
     */
    fun toPath(): Path {
        val path = Path()
        
        if (points.isEmpty()) return path
        
        // 移动到第一个点
        path.moveTo(points.first().x, points.first().y)
        
        // 连接所有其他点
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        
        // 闭合路径
        path.close()
        
        return path
    }

    /**
     * 获取多边形的边界矩形
     * 
     * 计算包含所有顶点的最小矩形区域
     * 
     * @return 包围盒矩形
     */
    fun getBounds(): androidx.compose.ui.geometry.Rect {
        if (points.isEmpty()) {
            return androidx.compose.ui.geometry.Rect.Zero
        }
        
        var minX = points.first().x
        var maxX = points.first().x
        var minY = points.first().y
        var maxY = points.first().y
        
        points.forEach { point ->
            if (point.x < minX) minX = point.x
            if (point.x > maxX) maxX = point.x
            if (point.y < minY) minY = point.y
            if (point.y > maxY) maxY = point.y
        }
        
        return androidx.compose.ui.geometry.Rect(
            offset = Offset(minX, minY),
            size = androidx.compose.ui.geometry.Size(maxX - minX, maxY - minY)
        )
    }

    /**
     * 平移多边形
     * 
     * 将多边形的所有顶点按指定偏移量平移
     * 
     * @param offset 平移向量
     * @return 平移后的新多边形
     */
    fun translate(offset: Offset): Polygon {
        return Polygon(points.map { it + offset })
    }

    /**
     * 向外扩展多边形
     * 
     * 沿每个顶点的法向量方向扩展多边形
     * 用于创建阴影效果或边框
     * 
     * @param value 扩展距离，正值向外扩展，负值向内收缩
     * @return 扩展后的新多边形
     */
    fun offset(value: Float): Polygon {
        if (points.size < 3) return this
        
        val expandedPoints = mutableListOf<Offset>()
        val size = points.size
        
        for (i in points.indices) {
            val current = points[i]
            val prev = points[(i - 1 + size) % size]
            val next = points[(i + 1) % size]
            
            // 计算前一条边的法向量
            val prevEdge = current - prev
            val prevNormal = Offset(-prevEdge.y, prevEdge.x).normalized()
            
            // 计算下一条边的法向量
            val nextEdge = next - current
            val nextNormal = Offset(-nextEdge.y, nextEdge.x).normalized()
            
            // 计算平均法向量
            val avgNormal = (prevNormal + nextNormal).normalized()
            
            // 向外扩展
            expandedPoints.add(current + avgNormal * value)
        }
        
        return Polygon(expandedPoints)
    }
}

/**
 * 向量标准化扩展函数
 * 
 * 将向量转换为单位向量，保持方向不变
 * 
 * @return 长度为1的标准化向量，零向量返回零向量
 */
private fun Offset.normalized(): Offset {
    val magnitude = kotlin.math.sqrt(x * x + y * y)
    return if (magnitude > 0f) Offset(x / magnitude, y / magnitude) else Offset.Zero
}
