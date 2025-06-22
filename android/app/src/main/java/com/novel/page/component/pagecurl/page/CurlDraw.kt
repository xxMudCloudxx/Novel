package com.novel.page.component.pagecurl.page

import android.graphics.Canvas
import android.graphics.Paint as AndroidPaint
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.novel.page.component.pagecurl.config.PageCurlConfig
import com.novel.page.component.pagecurl.utils.Polygon
import com.novel.page.component.pagecurl.utils.lineLineIntersection
import com.novel.page.component.pagecurl.utils.rotate
import java.lang.Float.max
import kotlin.math.atan2
import androidx.core.graphics.createBitmap

/**
 * 绘制卷曲效果的修饰符
 * 
 * 这是PageCurl组件的核心绘制逻辑，实现真实的书页卷曲视觉效果
 * 包含动态阴影、页面厚度和高光效果
 *
 * @param config PageCurl配置
 * @param posA 卷曲线的起始点（顶部）
 * @param posB 卷曲线的结束点（底部）
 * @return 应用了卷曲绘制效果的修饰符
 */
@ExperimentalPageCurlApi
internal fun Modifier.drawCurl(
    config: PageCurlConfig,
    posA: Offset,
    posB: Offset,
): Modifier = drawWithCache {
    // 快速检查：如果卷曲在最左侧位置（手势完全完成）
    // 在这种情况下不需要绘制任何内容
    if (posA == size.toRect().topLeft && posB == size.toRect().bottomLeft) {
        return@drawWithCache drawNothing()
    }

    // 快速检查：如果卷曲在最右侧位置（手势尚未开始）
    // 在这种情况下只绘制完整内容
    if (posA == size.toRect().topRight && posB == size.toRect().bottomRight) {
        return@drawWithCache drawOnlyContent()
    }

    // 找到卷曲线（[posA, posB]）与顶部和底部边的交点
    // 这样我们可以正确地裁剪和镜像内容
    val topIntersection = lineLineIntersection(
        Offset(0f, 0f), Offset(size.width, 0f),
        posA, posB
    )
    val bottomIntersection = lineLineIntersection(
        Offset(0f, size.height), Offset(size.width, size.height),
        posA, posB
    )

    // 通常不会发生，但如果没有交点（卷曲线是水平的），就绘制完整内容
    if (topIntersection == null || bottomIntersection == null) {
        return@drawWithCache drawOnlyContent()
    }

    // 限制两个交点的x坐标至少为0，这样页面看起来不会像从书中撕下来一样
    val topCurlOffset = Offset(max(0f, topIntersection.x), topIntersection.y)
    val bottomCurlOffset = Offset(max(0f, bottomIntersection.x), bottomIntersection.y)

    // 简单部分：准备一个lambda来绘制被卷曲线裁剪的内容
    val drawClippedContent = prepareClippedContent(topCurlOffset, bottomCurlOffset)
    // 复杂部分：准备一个lambda来绘制带阴影的背页
    val drawCurl = prepareCurl(config, topCurlOffset, bottomCurlOffset)

    onDrawWithContent {
        drawClippedContent()
        drawCurl()
    }
}

/**
 * 绘制完整未修改内容的简单方法
 */
private fun CacheDrawScope.drawOnlyContent(): DrawResult =
    onDrawWithContent {
        drawContent()
    }

/**
 * 不绘制任何内容的简单方法
 */
private fun CacheDrawScope.drawNothing(): DrawResult =
    onDrawWithContent {
        /* 空的 */
    }

/**
 * 准备裁剪内容的绘制方法
 * 
 * @param topCurlOffset 顶部卷曲偏移点
 * @param bottomCurlOffset 底部卷曲偏移点
 * @return 内容绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareClippedContent(
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // 从左侧到交点构建一个四边形
    val path = Path()
    path.lineTo(topCurlOffset.x, topCurlOffset.y)
    path.lineTo(bottomCurlOffset.x, bottomCurlOffset.y)
    path.lineTo(0f, size.height)
    return result@{
        // 绘制被构建路径裁剪的内容
        clipPath(path) {
            this@result.drawContent()
        }
    }
}

/**
 * 准备卷曲效果（背页、阴影、厚度和高光）的绘制方法
 * 
 * @param config PageCurl配置
 * @param topCurlOffset 顶部卷曲偏移点
 * @param bottomCurlOffset 底部卷曲偏移点
 * @return 卷曲效果绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareCurl(
    config: PageCurlConfig,
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
): ContentDrawScope.() -> Unit {
    // 构建要作为背页镜像的页面部分的四边形
    // 在所有情况下，多边形应该有4个点，即使背页只是一个小"角落"（有3个点）
    // 由于阴影渲染，否则在3点和4点多边形之间切换时会产生视觉伪影
    val polygon = Polygon(
        sequence {
            // 查找卷曲线与右侧的交点
            // 如果找到交点，添加到多边形点列表
            suspend fun SequenceScope<Offset>.yieldEndSideInterception() {
                val offset = lineLineIntersection(
                    topCurlOffset, bottomCurlOffset,
                    Offset(size.width, 0f), Offset(size.width, size.height)
                ) ?: return
                yield(offset)
                yield(offset)
            }

            // 如果顶部交点在页面卷曲边界内，从顶部取2个点，否则从右侧取交点
            if (topCurlOffset.x < size.width) {
                yield(topCurlOffset)
                yield(Offset(size.width, topCurlOffset.y))
            } else {
                yieldEndSideInterception()
            }

            // 如果底部交点在页面卷曲边界内，从底部取2个点，否则从右侧取交点
            if (bottomCurlOffset.x < size.width) {
                yield(Offset(size.width, size.height))
                yield(bottomCurlOffset)
            } else {
                yieldEndSideInterception()
            }
        }.toList()
    )

    // 计算X轴与卷曲线之间的弧度角，用于将镜像内容旋转到卷曲背页的正确位置
    val lineVector = topCurlOffset - bottomCurlOffset
    val angle = Math.PI.toFloat() - atan2(lineVector.y, lineVector.x) * 2

    // 计算翻页进度（0到1），用于动态效果
    val progress = ((bottomCurlOffset.x + topCurlOffset.x) * 0.5f) / size.width

    // 创建折痕路径
    val foldPath = Path().apply {
        moveTo(topCurlOffset.x, topCurlOffset.y)
        lineTo(bottomCurlOffset.x, bottomCurlOffset.y)
    }

    // 准备所有绘制层
    val drawShadow = prepareShadow(config, polygon, angle, progress)
    val drawEdgeShadow = prepareEdgeShadow(config, foldPath)
    val drawSelfShadow = prepareSelfShadow(config, foldPath, angle, progress)
    val drawAmbientFalloff = prepareAmbientFalloff(config, topCurlOffset, bottomCurlOffset, progress)
    val drawThickness = prepareThickness(config, polygon, angle)
    val drawSpecular = prepareSpecular(config, foldPath)

    return result@{
        withTransform({
            // 在X轴上镜像绘制，因为背页应该被镜像
            scale(-1f, 1f, pivot = bottomCurlOffset)
            // 根据卷曲线旋转绘制
            rotateRad(angle, pivot = bottomCurlOffset)
        }) {
            // 1 - 深折痕阴影（窄且暗）
            this@result.drawEdgeShadow()

            // 2 - 自阴影（放在正面内容之上、背页之下）
            this@result.drawSelfShadow()

            // 3 - 现有的广泛阴影
            this@result.drawShadow()

            // 4 - 环境衰减渐变
            this@result.drawAmbientFalloff()

            // 5 - 绘制背页内容
            clipPath(polygon.toPath()) {
                this@result.drawContent()

                val overlayAlpha = 1f - config.backPageContentAlpha
                drawRect(config.backPageColor.copy(alpha = overlayAlpha))
                
                // 添加径向高光效果
                if (config.highlightStrength > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = config.highlightStrength * 0.8f),
                                Color.Transparent
                            ),
                            center = bottomCurlOffset,
                            radius = size.minDimension * 0.4f
                        ),
                        blendMode = BlendMode.Softlight
                    )
                }
            }
            
            // 最后绘制厚度条（在背页之上）
            this@result.drawThickness()
            
            // 7 - 镜面边缘光（最顶层）
            this@result.drawSpecular()
        }
    }
}

/**
 * 准备边缘折痕阴影绘制
 * 
 * @param config PageCurl配置
 * @param foldPath 折痕路径
 * @return 边缘阴影绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareEdgeShadow(
    config: PageCurlConfig,
    foldPath: Path
): ContentDrawScope.() -> Unit {
    if (config.shadowAlpha == 0f || config.creaseShadowStrength == 0f) {
        return { /* 不需要折痕阴影 */ }
    }

    val radius = (config.shadowRadius * 0.4f * config.creaseShadowStrength).toPx()
    val paint = Paint().apply {
        asFrameworkPaint().setShadowLayer(
            radius,
            0f,
            0f,
            config.shadowColor.copy(
                alpha = config.shadowAlpha * config.creaseShadowStrength * 1.2f
            ).toArgb()
        )
        this.color = Color.Transparent  // 只显示阴影
    }

    return {
        drawIntoCanvas {
            it.nativeCanvas.drawPath(foldPath.asAndroidPath(), paint.asFrameworkPaint())
        }
    }
}

/**
 * 准备自阴影绘制（卷曲起始处的内阴影）
 * 
 * @param config PageCurl配置
 * @param foldPath 折痕路径
 * @param angle 角度
 * @param progress 翻页进度
 * @return 自阴影绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareSelfShadow(
    config: PageCurlConfig,
    foldPath: Path,
    angle: Float,
    progress: Float
): ContentDrawScope.() -> Unit {
    if (config.selfShadowStrength <= 0f) return { /* 不需要自阴影 */ }

    // 宽度随进度放大，阴影色随 config.selfShadowStrength
    val shadowW = lerp(8.dp.toPx(), 32.dp.toPx(), progress)
    val brush = Brush.linearGradient(
        0f to Color.Black.copy(alpha = config.selfShadowStrength),
        1f to Color.Transparent,
        start = Offset.Zero,
        end = Offset(shadowW, 0f).rotate(angle)
    )

    return {
        // 只对「未被卷起的可视区域」上色
        clipPath(foldPath, ClipOp.Difference) {
            drawRect(brush = brush, blendMode = BlendMode.Multiply)
        }
    }
}

/**
 * 准备环境衰减渐变绘制
 * 
 * @param config PageCurl配置
 * @param topCurlOffset 顶部卷曲偏移点
 * @param bottomCurlOffset 底部卷曲偏移点
 * @param progress 翻页进度
 * @return 环境衰减绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareAmbientFalloff(
    config: PageCurlConfig,
    topCurlOffset: Offset,
    bottomCurlOffset: Offset,
    progress: Float
): ContentDrawScope.() -> Unit {
    if (config.shadowAlpha == 0f) return { /* 不需要环境衰减 */ }

    val center = (topCurlOffset + bottomCurlOffset) * 0.5f
    val maxRadius = size.minDimension * 1.2f
    val radius = lerp(maxRadius * 0.3f, maxRadius, progress)
    
    val ambientBrush = Brush.radialGradient(
        colors = listOf(
            config.shadowColor.copy(alpha = config.shadowAlpha * 0.6f),
            config.shadowColor.copy(alpha = config.shadowAlpha * 0.3f),
            Color.Transparent
        ),
        center = center,
        radius = radius
    )

    return {
        drawRect(brush = ambientBrush, blendMode = BlendMode.Multiply)
    }
}

/**
 * 准备镜面边缘光绘制
 * 
 * @param config PageCurl配置
 * @param foldPath 折痕路径
 * @return 镜面光绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareSpecular(
    config: PageCurlConfig,
    foldPath: Path
): ContentDrawScope.() -> Unit {
    if (config.highlightStrength <= 0f) return { /* 不需要边缘光 */ }

    // 线宽取决于DPI
    val strokeW = config.rimLightWidth.toPx().coerceAtLeast(0.5f)
    val paint = Paint().apply {
        val frameworkPaint = asFrameworkPaint()
        frameworkPaint.strokeWidth = strokeW
        frameworkPaint.style = AndroidPaint.Style.STROKE
        frameworkPaint.strokeCap = AndroidPaint.Cap.ROUND
        this.blendMode = BlendMode.Screen   // 加法混合
        this.color = Color.White.copy(alpha = config.highlightStrength)
    }

    return {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(foldPath.asAndroidPath(), paint.asFrameworkPaint())
        }
    }
}

/**
 * 准备动态阴影绘制
 * 
 * @param config PageCurl配置
 * @param polygon 多边形
 * @param angle 角度
 * @param progress 翻页进度
 * @return 阴影绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareShadow(
    config: PageCurlConfig,
    polygon: Polygon,
    angle: Float,
    progress: Float
): ContentDrawScope.() -> Unit {
    // 如果不需要阴影则快速退出
    if (config.shadowAlpha == 0f || config.shadowRadius == 0.dp) {
        return { /* 不需要阴影 */ }
    }

    // 准备阴影参数 - 动态阴影大小
    val baseRadius = config.shadowRadius.toPx()
    val radius = if (config.dynamicShadowEnabled) {
        lerp(6.dp.toPx(), baseRadius * 1.5f, progress)
    } else {
        baseRadius
    }
    
    val shadowColor = config.shadowColor.copy(alpha = config.shadowAlpha).toArgb()
    val transparent = config.shadowColor.copy(alpha = 0f).toArgb()
    val shadowOffset = Offset(-config.shadowOffset.x.toPx(), config.shadowOffset.y.toPx())
        .rotate(angle = 2 * Math.PI.toFloat() - angle)

    // 准备带阴影层的阴影画笔
    val paint = Paint().apply {
        val frameworkPaint = asFrameworkPaint()
        frameworkPaint.color = transparent
        frameworkPaint.setShadowLayer(
            radius,
            shadowOffset.x,
            shadowOffset.y,
            shadowColor
        )
    }

    // 硬件加速仅在API 28及以上版本支持setShadowLayer()
    // 因此为了支持之前的API版本，需要在位图上绘制阴影
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        prepareShadowApi28(radius, paint, polygon)
    } else {
        prepareShadowImage(radius, paint, polygon)
    }
}

/**
 * 准备页面厚度效果
 * 
 * @param config PageCurl配置
 * @param polygon 多边形
 * @param angle 角度
 * @return 厚度绘制方法
 */
@ExperimentalPageCurlApi
private fun CacheDrawScope.prepareThickness(
    config: PageCurlConfig,
    polygon: Polygon,
    angle: Float
): ContentDrawScope.() -> Unit {
    if (config.thicknessDp == 0.dp) {
        return { /* 不需要厚度效果 */ }
    }

    val thickness = config.thicknessDp.toPx()
    
    // 计算厚度条的偏移向量（沿卷曲线法向量）
    val normalVector = Offset(-kotlin.math.sin(angle), kotlin.math.cos(angle))
    val thicknessOffset = normalVector * thickness
    
    // 创建厚度条多边形
    val thicknessPolygon = polygon.translate(thicknessOffset)
    
    // 创建厚度颜色渐变（从背页颜色到稍暗的颜色）
    val thicknessBrush = Brush.linearGradient(
        colors = listOf(
            config.backPageColor,
            config.backPageColor.copy(alpha = 0.7f)
        ),
        start = Offset.Zero,
        end = thicknessOffset
    )

    return {
        clipPath(thicknessPolygon.toPath()) {
            drawRect(brush = thicknessBrush)
        }
    }
}

/**
 * API 28+ 阴影绘制
 */
private fun prepareShadowApi28(
    radius: Float,
    paint: Paint,
    polygon: Polygon,
): ContentDrawScope.() -> Unit = {
    drawIntoCanvas {
        it.nativeCanvas.drawPath(
            polygon
                .offset(radius).toPath()
                .asAndroidPath(),
            paint.asFrameworkPaint()
        )
    }
}

/**
 * API 28以下阴影绘制（使用位图）
 */
private fun CacheDrawScope.prepareShadowImage(
    radius: Float,
    paint: Paint,
    polygon: Polygon,
): ContentDrawScope.() -> Unit {
    // 稍微增加大小以确保阴影不被裁剪
    val bitmap = createBitmap((size.width + radius * 4).toInt(), (size.height + radius * 4).toInt())
    Canvas(bitmap).apply {
        drawPath(
            polygon
                // 由于位图大小增加，应该平移多边形以使阴影保持在中心
                .translate(Offset(2 * radius, 2 * radius))
                .offset(radius).toPath()
                .asAndroidPath(),
            paint.asFrameworkPaint()
        )
    }

    return {
        drawIntoCanvas {
            // 由于位图大小增加，应该移动绘制以使阴影保持在中心
            it.nativeCanvas.drawBitmap(bitmap, -2 * radius, -2 * radius, null)
        }
    }
}
