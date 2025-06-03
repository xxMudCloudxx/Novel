package com.novel.page.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.novel.utils.wdp
import androidx.compose.ui.graphics.TransformOrigin
import com.facebook.react.uimanager.PixelUtil.dpToPx
import kotlinx.coroutines.coroutineScope
import com.novel.page.book.BookDetailPage
import com.novel.utils.NavViewModel

/**
 * 翻书动画状态
 */
@Stable
data class FlipBookState(
    val isAnimating: Boolean = false,
    val isOpening: Boolean = true, // true: 打开书籍, false: 合上书籍
    val coverRotationProgress: Float = 0f, // 封面旋转进度 [0f, 1f]
    val scaleProgress: Float = 0f,    // 缩放进度 [0f, 1f]
    val bookId: String? = null,
    val originalImageUrl: String? = null,
    val originalPosition: Offset = Offset.Zero,
    val originalSize: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero,
    val targetScale: Float = 1f,
    val showContent: Boolean = false, // 是否显示书籍内容页
    val hideOriginalImage: Boolean = false // 是否隐藏原始图片（用于共享元素动画）
)

/**
 * 翻书动画控制器 - 真正的3D翻书效果，使用共享元素动画
 */
@Composable
fun rememberFlipBookAnimationController(): FlipBookAnimationController {
    return remember { FlipBookAnimationController() }
}

class FlipBookAnimationController {
    private var _animationState by mutableStateOf(FlipBookState())
    val animationState: FlipBookState get() = _animationState

    // 动画完成回调
    private var onAnimationComplete: (() -> Unit)? = null

    /**
     * 开始翻书动画（打开书籍）
     * 使用共享元素动画：隐藏原始图片，在全局层显示动画
     */
    suspend fun startFlipAnimation(
        bookId: String,
        imageUrl: String,
        originalPosition: Offset,
        originalSize: androidx.compose.ui.geometry.Size,
        screenWidth: Float = 1080f,
        screenHeight: Float = 2400f
    ) {
        // 计算目标缩放比例
        val horScale = screenWidth / originalSize.width
        val verScale = screenHeight / originalSize.height
        val targetScale = maxOf(horScale, verScale)

        _animationState = FlipBookState(
            isAnimating = true,
            isOpening = true,
            bookId = bookId,
            originalImageUrl = imageUrl,
            originalPosition = originalPosition,
            originalSize = originalSize,
            targetScale = targetScale,
            showContent = true,
            hideOriginalImage = true // 隐藏原始图片，使用全局动画
        )

        coroutineScope {
            // 优化：使用单个动画状态对象减少状态更新
            val animationState = object {
                var coverRotation = 0f
                var scale = 0f
            }

            // 创建并行动画
            val coverRotationAnimatable = Animatable(0f)
            val scaleAnimatable = Animatable(0f)

            // 启动并行动画
            val coverRotationJob = launch {
                coverRotationAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                    )
                )
            }

            val scaleJob = launch {
                scaleAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                    )
                )
            }

            // 优化：减少更新频率，只在值变化时更新
            val updateJob = launch {
                var lastRotation = -1f
                var lastScale = -1f

                while (coverRotationAnimatable.isRunning || scaleAnimatable.isRunning) {
                    val currentRotation = coverRotationAnimatable.value
                    val currentScale = scaleAnimatable.value

                    // 只在值发生显著变化时更新状态（减少不必要的重组）
                    if (kotlin.math.abs(currentRotation - lastRotation) > 0.001f ||
                        kotlin.math.abs(currentScale - lastScale) > 0.001f
                    ) {

                        _animationState = _animationState.copy(
                            coverRotationProgress = currentRotation,
                            scaleProgress = currentScale
                        )

                        lastRotation = currentRotation
                        lastScale = currentScale
                    }

                    delay(16) // 约60fps更新
                }
            }

            // 等待动画完成
            coverRotationJob.join()
            scaleJob.join()
            updateJob.cancel()

            _animationState = _animationState.copy(
                coverRotationProgress = 1f,
                scaleProgress = 1f
            )

            onAnimationComplete?.invoke()
        }
    }

    /**
     * 开始倒放动画（合上书籍）
     * 恢复原始图片显示，隐藏全局动画
     */
    private suspend fun startReverseAnimation(
        bookId: String,
    ) {
        _animationState = _animationState.copy(
            isOpening = false,
            coverRotationProgress = 1f, // 封面从90度开始
            scaleProgress = 1f, // 从全屏开始
            hideOriginalImage = true // 继续隐藏原始图片直到动画结束
        )

        coroutineScope {
            // 创建倒放动画
            val coverRotationAnimatable = Animatable(1f) // 从90度回到0度
            val scaleAnimatable = Animatable(1f) // 从全屏缩小到原始大小

            // 启动并行倒放动画
            val coverRotationJob = launch {
                coverRotationAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                    )
                )
            }

            val scaleJob = launch {
                scaleAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                    )
                )
            }

            // 优化：减少更新频率
            val updateJob = launch {
                var lastRotation = -1f
                var lastScale = -1f

                while (coverRotationAnimatable.isRunning || scaleAnimatable.isRunning) {
                    val currentRotation = coverRotationAnimatable.value
                    val currentScale = scaleAnimatable.value

                    if (kotlin.math.abs(currentRotation - lastRotation) > 0.001f ||
                        kotlin.math.abs(currentScale - lastScale) > 0.001f
                    ) {

                        _animationState = _animationState.copy(
                            coverRotationProgress = currentRotation,
                            scaleProgress = currentScale
                        )

                        lastRotation = currentRotation
                        lastScale = currentScale
                    }

                    delay(16)
                }
            }

            // 等待动画完成
            coverRotationJob.join()
            scaleJob.join()
            updateJob.cancel()

            // 清理状态 - 恢复原始图片显示
            _animationState = FlipBookState() // 完全重置状态，恢复原始图片
        }
    }

    /**
     * 触发倒放动画的便捷方法
     */
    suspend fun triggerReverseAnimation() {
        if (_animationState.isAnimating) {
            startReverseAnimation(
                bookId = _animationState.bookId ?: "",
            )
        }
    }
}

/**
 * 全局翻书动画覆盖层 - 真正的3D翻书效果，性能优化版本
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GlobalFlipBookOverlay(
    controller: FlipBookAnimationController,
    getBookImageUrl: ((String) -> String)? = null
) {
    val animationState = controller.animationState
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // 预计算屏幕尺寸，避免重复计算
    val screenSize = remember(configuration) {
        androidx.compose.ui.geometry.Size(
            configuration.screenWidthDp.dp.value.dpToPx(),
            configuration.screenHeightDp.dp.value.dpToPx()
        )
    }

    // 性能关键：只有在动画进行时才渲染，完全避免无效渲染
    if (!animationState.isAnimating || animationState.bookId == null) {
        return
    }

    val imageUrl = remember(animationState.bookId, animationState.originalImageUrl) {
        getBookImageUrl?.invoke(animationState.bookId)
            ?: animationState.originalImageUrl
            ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
    ) {
        // 书籍内容视图 (BookDetailPage) - 直接放大动画
        if (animationState.showContent) {
            // 优化：预计算变换参数
            val (currentScale, transformOrigin) = remember(
                animationState.scaleProgress,
                animationState.originalPosition,
                screenSize
            ) {
                val progress = animationState.scaleProgress
                val scale = if (progress <= 0.5f) {
                    progress * 0.4f
                } else {
                    0.2f + (progress - 0.5f) * 1.6f
                }

                val origin = TransformOrigin(
                    pivotFractionX = animationState.originalPosition.x / screenSize.width,
                    pivotFractionY = animationState.originalPosition.y / screenSize.height
                )

                scale to origin
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        alpha = if (animationState.scaleProgress > 0.3f) 1f else 0.3f
                    }
            ) {
                BookDetailPage(
                    bookId = animationState.bookId,
                    fromRank = true,
                    flipBookController = controller,
                    onNavigateToReader = { bookId, chapterId ->
                        NavViewModel.navigateToReader(bookId, chapterId)
                    }
                )
            }
        }

        // 书籍封面视图 - 沿左边Y轴旋转90度，只有在需要显示封面时才渲染
        if (animationState.hideOriginalImage) {
            // 优化：预计算位置和变换参数
            val (offsetX, offsetY, rotationY, scale) = remember(
                animationState.scaleProgress,
                animationState.coverRotationProgress,
                animationState.originalPosition,
                animationState.targetScale,
                screenSize
            ) {
                val scaleProgress = animationState.scaleProgress
                val rotationProgress = animationState.coverRotationProgress
                val baseX = animationState.originalPosition.x
                val baseY = animationState.originalPosition.y - 120.wdp.value
                val targetY = screenSize.height * 0.5f

                val offsetX = (baseX * (1 - scaleProgress)).toInt()
                val offsetY = (baseY + ((targetY - baseY) * scaleProgress)).toInt()
                val rotationY = -90f * rotationProgress
                val scale = 1f + (animationState.targetScale - 1f) * scaleProgress

                Tuple4(offsetX, offsetY, rotationY, scale)
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                    .graphicsLayer {
                        this.rotationY = rotationY.toFloat()
                        scaleX = scale.toFloat()
                        scaleY = scale.toFloat()
                        cameraDistance = 12f * density.density
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        shadowElevation =
                            if (animationState.coverRotationProgress > 0) 12.dp.toPx() else 0f
                        alpha = 1f
                    }
            ) {
                // 书籍封面图片
                NovelImageView(
                    imageUrl = imageUrl,
                    modifier = Modifier
                        .size(
                            animationState.originalSize.width.wdp / density.density,
                            animationState.originalSize.height.wdp / density.density
                        )
                        .clip(RoundedCornerShape(4.wdp))
                        .background(NovelColors.NovelMain),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    placeholderContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NovelColors.NovelMain),
                            contentAlignment = Alignment.Center
                        ) {
                            NovelText(
                                text = "📖",
                                fontSize = 20.ssp,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                )
            }
        }
    }
}

// 辅助数据类，用于减少对象创建
private data class Tuple4<T>(val first: T, val second: T, val third: T, val fourth: T)

/**
 * 简化的书籍点击处理器
 * 提供启动动画的接口，支持精确位置
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberBookClickHandler(
    controller: FlipBookAnimationController,
    bookId: String,
    imageUrl: String,
    position: Offset = Offset.Zero,
    size: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 优化：预计算屏幕尺寸和默认值
    return remember(bookId, position, size, configuration, density) {
        val finalPosition = if (position != Offset.Zero) position else Offset(200f, 300f)
        val finalSize = if (size != androidx.compose.ui.geometry.Size.Zero) {
            size
        } else {
            androidx.compose.ui.geometry.Size(150f, 200f)
        }

        val screenWidthPx = configuration.screenWidthDp * density.density
        val screenHeightPx = configuration.screenHeightDp * density.density

        {
            coroutineScope.launch {
                controller.startFlipAnimation(
                    bookId = bookId,
                    imageUrl = imageUrl,
                    originalPosition = finalPosition,
                    originalSize = finalSize,
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx
                )
            }
        }
    }
}