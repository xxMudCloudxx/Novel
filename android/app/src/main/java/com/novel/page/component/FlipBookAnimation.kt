package com.novel.page.component

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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.novel.ui.theme.NovelColors
import com.novel.page.component.NovelText
import com.novel.page.component.NovelImageView
import com.novel.utils.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.novel.utils.wdp

/**
 * 翻书动画状态
 */
data class FlipBookState(
    val isAnimating: Boolean = false,
    val progress: Float = 0f,
    val bookId: String? = null,
    val originalImageUrl: String? = null,
    val originalPosition: Offset = Offset.Zero,
    val originalSize: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero
)

/**
 * 翻书动画控制器
 */
@Composable
fun rememberFlipBookAnimationController(): FlipBookAnimationController {
    return remember { FlipBookAnimationController() }
}

class FlipBookAnimationController {
    private var _animationState by mutableStateOf(FlipBookState())
    val animationState: FlipBookState get() = _animationState

    private var onNavigate: ((String) -> Unit)? = null

    fun setNavigationCallback(callback: (String) -> Unit) {
        onNavigate = callback
    }

    suspend fun startFlipAnimation(
        bookId: String,
        imageUrl: String,
        originalPosition: Offset,
        originalSize: androidx.compose.ui.geometry.Size
    ) {
        Log.d(
            "FlipBookAnimation",
            "开始动画: bookId=$bookId, position=$originalPosition, size=$originalSize"
        )

        _animationState = FlipBookState(
            isAnimating = true,
            bookId = bookId,
            originalImageUrl = imageUrl,
            originalPosition = originalPosition,
            originalSize = originalSize
        )

        // 创建动画 - 使用更流畅的动画
        val animatable = Animatable(0f)

        // 启动一个协程来实时更新进度
        val updateJob = kotlinx.coroutines.GlobalScope.launch {
            while (animatable.isRunning) {
                _animationState = _animationState.copy(progress = animatable.value)
                kotlinx.coroutines.delay(16) // 约60fps更新
            }
        }

        // 启动动画到80% - 使用更平滑的缓动
        animatable.animateTo(
            targetValue = 0.8f,
            animationSpec = tween(
                durationMillis = 600, // 减少时长让动画更快
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) // 更平滑的缓动
            )
        )

        // 当动画到达80%时触发页面跳转
        Log.d("FlipBookAnimation", "动画到达80%，触发导航")
        onNavigate?.invoke(bookId)

        // 继续动画到100%
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 150, // 缩短最后阶段
                easing = LinearEasing
            )
        )

        // 停止更新协程
        updateJob.cancel()

        _animationState = _animationState.copy(progress = 1f)
        delay(50) // 大幅减少延迟
        _animationState = FlipBookState()
    }

    suspend fun startReverseAnimation(bookId: String, imageUrl: String) {
        Log.d("FlipBookAnimation", "开始倒放动画: bookId=$bookId")

        // 倒放动画，从最终状态回到初始状态
        _animationState = FlipBookState(
            isAnimating = true,
            progress = 1f,
            bookId = bookId,
            originalImageUrl = imageUrl,
            originalPosition = Offset(0f, 300f), // 从屏幕左侧开始倒放
            originalSize = androidx.compose.ui.geometry.Size(50f, 65f)
        )

        val animatable = Animatable(1f)

        // 启动一个协程来实时更新进度
        val updateJob = kotlinx.coroutines.GlobalScope.launch {
            while (animatable.isRunning) {
                _animationState = _animationState.copy(progress = animatable.value)
                kotlinx.coroutines.delay(16) // 约60fps更新
            }
        }

        // 倒放动画 - 使用快速平滑的缓动
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500, // 倒放更快
                easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f) // 平滑缓动
            )
        )

        // 停止更新协程
        updateJob.cancel()

        // 清理状态
        _animationState = FlipBookState()
    }
}

/**
 * 书籍动画位置追踪器
 * 追踪书籍在屏幕中的位置，供动画使用
 */
@Composable
fun BookPositionTracker(
    controller: FlipBookAnimationController,
    bookId: String,
    imageUrl: String,
    onBookClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var bookPosition by remember { mutableStateOf(Offset.Zero) }
    var bookSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                bookPosition = Offset(bounds.left, bounds.top)
                bookSize = androidx.compose.ui.geometry.Size(bounds.width, bounds.height)
            }
    ) {
        // 如果当前书籍正在动画中，隐藏原始图片
        val isCurrentBookAnimating = controller.animationState.isAnimating &&
                controller.animationState.bookId == bookId

        Box(
            modifier = Modifier
                .then(
                    if (isCurrentBookAnimating) {
                        Modifier // 动画期间隐藏原始图片，或者设置为透明
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // 点击处理
        LaunchedEffect(Unit) {
            // 这里可以处理点击事件，但实际点击应该从外部传入
        }
    }

    // 设置点击回调，传递位置信息
    LaunchedEffect(bookPosition, bookSize) {
        // 这个effect会在位置更新时触发，但我们需要在点击时才启动动画
        // 所以这里先不做任何事，等待外部调用
    }

    // 提供一个函数供外部调用来启动动画
    LaunchedEffect(controller) {
        controller.setNavigationCallback { bookId ->
            onBookClick()
        }
    }

    // 暴露启动动画的方法
    DisposableEffect(bookId) {
        val startAnimation = {
            coroutineScope.launch {
                controller.startFlipAnimation(bookId, imageUrl, bookPosition, bookSize)
            }
        }

        onDispose { }
    }
}

/**
 * 全局翻书动画覆盖层
 * 在最顶层显示动画图片，可以突破所有组件边界
 */
@Composable
fun GlobalFlipBookOverlay(
    controller: FlipBookAnimationController,
    getBookImageUrl: ((String) -> String)? = null
) {
    val animationState = controller.animationState
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // 添加独立的宽度动画
    val widthAnimation = remember { Animatable(100f) }

    // 当进度超过0.15时启动独立宽度动画
    LaunchedEffect(animationState.progress) {
        if (animationState.progress > 0.15f) {
            widthAnimation.animateTo(
                targetValue = 400f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    if (animationState.isAnimating &&
        animationState.bookId != null
    ) {

        Log.d("FlipBookAnimation", "显示全局覆盖层动画，进度: ${animationState.progress}")

        // 尝试获取真实的图片URL，如果没有提供获取函数则使用默认URL
        val imageUrl = getBookImageUrl?.invoke(animationState.bookId)
            ?: animationState.originalImageUrl
            ?: "https://via.placeholder.com/50x65" // 默认占位图

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f), // 确保在最顶层
        ) {
            // 动画中的书籍图片
            Box(
                modifier = Modifier
                    .offset {
                        val baseX = animationState.originalPosition.x
                        val baseY = animationState.originalPosition.y

                        // 简化位置计算 - 直接使用您的逻辑
                        val progress = animationState.progress
                        val targetX = 0f
                        val targetY = (screenHeight.toPx() * 0.5f)

                        IntOffset(
                            x = (baseX * (1 - progress * 1.1)).toInt(),
                            y = (baseY + ((targetY - baseY) * progress)).toInt()
                        )
                    }
                    .graphicsLayer {
                        val progress = animationState.progress
                        val bookWidthDp = 50.wdp
                        val bookHeightDp = 65.wdp
                        val targetScaleX =
                            (screenWidth.value * 0.8f - 40.wdp.value) / bookWidthDp.value // 屏幕80%
                        val targetScaleY =
                            (screenHeight.value - 40.wdp.value) / bookHeightDp.value // 屏幕100%

                        // 简化3D变换计算
                        cameraDistance = 8f * density.density // 减少透视距离
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)

                        // 简化旋转 - 使用线性插值
                        rotationY = -120f * progress // 减少旋转角度

                        // 简化缩放 - 使用您调整后的参数
                        scaleX = 1f + (targetScaleX - 1f) * animationState.progress
                        scaleY = 1f + (targetScaleY - 1f) * animationState.progress

                        // 边界检查
                        if (scaleX > targetScaleX) scaleX = targetScaleX
                        if (scaleY > targetScaleY) scaleY = targetScaleY

                        // 保持透明度稳定，减少计算
                        alpha = 1f

                        // 简化阴影
                        shadowElevation = (4 + 6 * progress).dp.toPx()
                    }
            ) {
                if (animationState.progress > 0.15f) {
                    Box(
                        modifier = Modifier
                            .height(65.wdp)
                            .width(300.wdp)
                            .clip(RoundedCornerShape(4.wdp))
                            .background(NovelColors.NovelBookBackground)
                    )
                }

                Box {
                    // 翻书效果：背景的"下一页"方框

                    // 书籍封面 - 使用图片URL重新渲染
                    NovelImageView(
                        imageUrl = imageUrl,
                        modifier = Modifier
                            .size(50.dp, 65.dp) // 固定尺寸
                            .clip(RoundedCornerShape(4.dp))
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
}

/**
 * 简化的书籍点击处理器
 * 提供启动动画的接口
 */
@Composable
fun rememberBookClickHandler(
    controller: FlipBookAnimationController,
    bookId: String,
    imageUrl: String
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()

    return remember(bookId) {
        {
            coroutineScope.launch {
                // 使用默认位置启动动画，因为获取实际位置比较复杂
                // 这里简化处理，使用屏幕中的一个位置
                controller.startFlipAnimation(
                    bookId = bookId,
                    imageUrl = imageUrl,
                    originalPosition = Offset(200f, 300f),
                    originalSize = androidx.compose.ui.geometry.Size(50f, 65f)
                )
            }
        }
    }
}

/**
 * 翻书动画触发器
 */
@Composable
fun FlipBookTrigger(
    controller: FlipBookAnimationController,
    onNavigate: (String) -> Unit
) {
    LaunchedEffect(controller) {
        controller.setNavigationCallback(onNavigate)
    }
} 