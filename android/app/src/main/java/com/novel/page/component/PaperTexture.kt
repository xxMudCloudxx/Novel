package com.novel.page.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 纸张纹理组件
 * 
 * 为阅读页面添加真实的纸张噪点纹理效果，提升阅读体验的真实感
 *
 * @param modifier 修饰符
 * @param alpha 纹理透明度，默认0.03f（很微弱的效果）
 * @param density 噪点密度，值越大噪点越多
 * @param seed 随机种子，用于生成一致的纹理图案
 * @param content 内容组合项
 */
@Composable
fun PaperTexture(
    modifier: Modifier = Modifier,
    alpha: Float = 0.03f,
    density: Float = 1.0f,
    seed: Long = 42L,
    content: @Composable () -> Unit
) {
    LocalDensity.current
    
    // 记住纹理点，避免重复计算
    val texturePoints = remember(seed, density) {
        generateTexturePoints(seed, density)
    }
    
    Box(modifier = modifier) {
        // 背景内容
        content()
        
        // 纸张纹理覆盖层
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        ) {
            drawPaperTexture(texturePoints, size.width, size.height)
        }
    }
}

/**
 * 生成纸张纹理点
 * 
 * @param seed 随机种子
 * @param density 密度系数
 * @return 纹理点列表
 */
private fun generateTexturePoints(seed: Long, density: Float): List<TexturePoint> {
    val random = Random(seed)
    val points = mutableListOf<TexturePoint>()
    
    // 基础点数量，可以根据需要调整
    val basePointCount = (2000 * density).toInt()
    
    repeat(basePointCount) {
        points.add(
            TexturePoint(
                x = random.nextFloat(),
                y = random.nextFloat(),
                size = random.nextFloat() * 2f + 0.5f,
                brightness = random.nextFloat() * 0.8f + 0.2f,
                type = TextureType.entries.toTypedArray().random(random)
            )
        )
    }
    
    return points
}

/**
 * 绘制纸张纹理
 * 
 * @param points 纹理点列表
 * @param width 画布宽度
 * @param height 画布高度
 */
private fun DrawScope.drawPaperTexture(
    points: List<TexturePoint>,
    width: Float,
    height: Float
) {
    points.forEach { point ->
        val x = point.x * width
        val y = point.y * height
        
        when (point.type) {
            TextureType.DOT -> {
                // 圆形噪点
                drawCircle(
                    color = Color.Black.copy(alpha = point.brightness),
                    radius = point.size,
                    center = Offset(x, y)
                )
            }
            TextureType.FIBER -> {
                // 纤维纹理（短线条）
                val angle = point.brightness * Math.PI.toFloat() * 2
                val length = point.size * 3f
                val endX = x + cos(angle) * length
                val endY = y + sin(angle) * length
                
                drawLine(
                    color = Color.Black.copy(alpha = point.brightness * 0.6f),
                    start = Offset(x, y),
                    end = Offset(endX, endY),
                    strokeWidth = 0.5f
                )
            }
            TextureType.GRAIN -> {
                // 颗粒纹理（小方块）
                val halfSize = point.size * 0.5f
                drawRect(
                    color = Color.Black.copy(alpha = point.brightness * 0.4f),
                    topLeft = Offset(x - halfSize, y - halfSize),
                    size = androidx.compose.ui.geometry.Size(point.size, point.size)
                )
            }
        }
    }
}

/**
 * 纹理点数据类
 * 
 * @param x X坐标（0-1范围的比例值）
 * @param y Y坐标（0-1范围的比例值）
 * @param size 点的大小
 * @param brightness 亮度/透明度
 * @param type 纹理类型
 */
private data class TexturePoint(
    val x: Float,
    val y: Float,
    val size: Float,
    val brightness: Float,
    val type: TextureType
)

/**
 * 纹理类型枚举
 */
private enum class TextureType {
    DOT,    // 圆形噪点
    FIBER,  // 纤维纹理
    GRAIN   // 颗粒纹理
}
