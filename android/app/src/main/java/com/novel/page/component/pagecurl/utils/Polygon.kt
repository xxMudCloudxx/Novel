package com.novel.page.component.pagecurl.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * 多边形工具类
 * 
 * 用于创建和操作多边形路径，主要用于PageCurl的卷曲效果绘制
 *
 * @param points 多边形的顶点列表
 */
internal class Polygon(private val points: List<Offset>) {

    /**
     * 将多边形转换为Compose Path对象
     * 
     * @return 可用于绘制的Path对象
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
     * @return 包含所有顶点的最小矩形
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
     * @param offset 平移偏移量
     * @return 新的平移后的多边形
     */
    fun translate(offset: Offset): Polygon {
        return Polygon(points.map { it + offset })
    }

    /**
     * 向外扩展多边形
     * 
     * @param value 扩展值
     * @return 新的扩展后的多边形
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
 * @return 标准化后的向量
 */
private fun Offset.normalized(): Offset {
    val magnitude = kotlin.math.sqrt(x * x + y * y)
    return if (magnitude > 0f) Offset(x / magnitude, y / magnitude) else Offset.Zero
}
