package com.novel.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.novel.ui.theme.NovelColors

/**
 * 侧滑状态数据类 - 用于传递拖拽状态信息
 */
data class SwipeBackState(
    val isDragging: Boolean = false,
    val offsetX: Float = 0f,
    val hintText: String = "",
    val hintAlpha: Float = 0f,
    val isOverThreshold: Boolean = false
)

/**
 * 高性能的 iOS 式侧滑返回手势处理 Modifier
 *
 * @param edgeWidthDp 检测手势的左侧热区宽度
 * @param firstThreshold 第一阶段阈值（屏宽百分比）- 显示"右滑退出阅读器"
 * @param completeThreshold 完成返回的阈值（屏宽百分比）- 显示"松开退出阅读器"
 * @param onSwipeStateChange 拖拽状态变化回调
 */
@SuppressLint("RememberReturnType")
fun Modifier.iosSwipeBack(
    edgeWidthDp: Dp = 300.wdp,
    firstThreshold: Float = 0.15f,
    completeThreshold: Float = 0.33f,
    onSwipeStateChange: ((SwipeBackState) -> Unit)? = null
): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val edgeWidthPx = with(density) { edgeWidthDp.toPx() }

    // 性能优化：使用单个 Animatable 而不是多个状态
    val offsetX = remember { Animatable(0f) }

    // 拖拽状态管理，避免频繁状态更新
    var isDragging by remember { mutableStateOf(false) }

    // 性能优化：预计算阈值像素值，避免重复计算
    val firstThresholdPx = remember(widthPx, firstThreshold) { widthPx * firstThreshold }
    val completeThresholdPx = remember(widthPx, completeThreshold) { widthPx * completeThreshold }

    // 提示文字状态 - 使用 derivedStateOf 优化性能
    val hintText by remember {
        derivedStateOf {
            when {
                !isDragging || offsetX.value < firstThresholdPx -> ""
                offsetX.value >= completeThresholdPx -> "松\n开\n退\n出\n阅\n读\n器"
                else -> "右\n滑\n退\n出\n阅\n读\n器"
            }
        }
    }

    // 提示透明度 - 使用 derivedStateOf 优化性能
    val hintAlpha by remember {
        derivedStateOf {
            when {
                !isDragging -> 0f
                offsetX.value < firstThresholdPx -> 0f
                offsetX.value >= completeThresholdPx -> 1f
                else -> (offsetX.value - firstThresholdPx) / (completeThresholdPx - firstThresholdPx) * 0.8f + 0.2f
            }
        }
    }

    // 判断是否超过完成阈值
    val isOverThreshold by remember {
        derivedStateOf {
            offsetX.value >= completeThresholdPx
        }
    }

    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

// 记录上次状态
    var prevOverThreshold by remember { mutableStateOf(false) }

    LaunchedEffect(isOverThreshold) {
        if (isOverThreshold && !prevOverThreshold) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    50, // 时长 50ms
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
        prevOverThreshold = isOverThreshold
    }
    // 状态变化回调
    remember(isDragging, offsetX.value, hintText, hintAlpha, isOverThreshold) {
        onSwipeStateChange?.invoke(
            SwipeBackState(
                isDragging = isDragging,
                offsetX = offsetX.value,
                hintText = hintText,
                hintAlpha = hintAlpha,
                isOverThreshold = isOverThreshold
            )
        )
    }

    // 性能优化：NestedScroll 连接复用，避免重复创建
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 只在水平滑动且向右时拦截
                val dx = available.x
                return if (isDragging && dx > 0 && offsetX.value > 0) {
                    scope.launch {
                        offsetX.snapTo((offsetX.value + dx).coerceAtMost(widthPx))
                    }
                    Offset(dx, 0f)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isDragging) {
                    isDragging = false
                    NavViewModel.navController.value?.let {
                        decideFinish(offsetX, completeThresholdPx, it)
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    this
        .pointerInput(Unit) {
            // 性能优化：只在边缘检测时才开启拖拽监听
            detectHorizontalDragGestures(
                onDragStart = { pos ->
                    if (pos.x <= edgeWidthPx) {
                        isDragging = true
                    }
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (isDragging) {
                        val newOffset = (offsetX.value + dragAmount).coerceIn(0f, widthPx)
                        scope.launch { offsetX.snapTo(newOffset) }
                    }
                },
                onDragEnd = {
                    if (isDragging) {
                        isDragging = false
                        scope.launch {
                            NavViewModel.navController.value?.let {
                                decideFinish(offsetX, completeThresholdPx, it)
                            }
                        }
                    }
                },
                onDragCancel = {
                    if (isDragging) {
                        isDragging = false
                        scope.launch {
                            offsetX.animateTo(
                                0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                        }
                    }
                }
            )
        }
        // 性能关键：使用 graphicsLayer 做硬件加速的位移
        .graphicsLayer {
            translationX = offsetX.value
            // 轻微阴影效果提升视觉体验
            shadowElevation = if (isDragging && offsetX.value > 0) 8f else 0f
        }
        .nestedScroll(connection)
}

/**
 * iOS 风格的侧滑返回背景指示器
 * 显示在被滑出的背景区域，而不是当前页面上
 */
@Composable
fun SwipeBackIndicator(
    swipeState: SwipeBackState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(end = 8.wdp)
            .fillMaxHeight()
            .width(20.wdp), // 宽度跟随滑动距离
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = swipeState.hintText,
            lineHeight = 10.sp,
            color = NovelColors.NovelTextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 带有背景指示器的侧滑返回容器组件
 * 指示器显示在被滑出的背景区域
 *
 * @param modifier 修饰符
 * @param edgeWidthDp 检测手势的左侧热区宽度
 * @param firstThreshold 第一阶段阈值
 * @param completeThreshold 完成返回的阈值
 * @param backgroundColor 背景颜色
 * @param content 子内容
 */
@Composable
fun SwipeBackContainer(
    modifier: Modifier = Modifier,
    edgeWidthDp: Dp = 300.wdp,
    firstThreshold: Float = 0.05f,
    completeThreshold: Float = 0.15f,
    backgroundColor: Color = NovelColors.NovelBookBackground.copy(alpha = 0.7f), // 默认浅灰色背景
    content: @Composable BoxScope.() -> Unit
) {
    var swipeState by remember { mutableStateOf(SwipeBackState()) }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxSize()
    ) {
        // 背景层 - 显示指示器的地方
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(swipeState.offsetX.dp / 3)
        ) {
            // 背景指示器 - 显示在被露出的区域
            SwipeBackIndicator(
                swipeState = swipeState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 前景内容层 - 可以滑动的页面
        Box(
            modifier = Modifier
                .fillMaxSize()
                .iosSwipeBack(
                    edgeWidthDp = edgeWidthDp,
                    firstThreshold = firstThreshold,
                    completeThreshold = completeThreshold,
                    onSwipeStateChange = { swipeState = it }
                )
        ) {
            content()
        }
    }
}

/**
 * 简化版本：为任意 Composable 增加基础的 iOS 式侧滑返回手势
 * 不包含提示UI，性能更优
 */
fun Modifier.iosSwipeBackBasic(
    edgeWidthDp: Dp = 300.wdp,
    completeThreshold: Float = 0.33f
): Modifier = iosSwipeBack(
    edgeWidthDp = edgeWidthDp,
    firstThreshold = 0.15f,
    completeThreshold = completeThreshold,
    onSwipeStateChange = null
)

/**
 * 性能优化版本：决定是否完成返回操作
 */
private suspend fun decideFinish(
    anim: Animatable<Float, AnimationVector1D>,
    thresholdPx: Float,
    navController: NavController
) {
    if (anim.value >= thresholdPx) {
        // 快速完成动画并执行返回
        anim.animateTo(
            anim.upperBound ?: Float.MAX_VALUE,
            animationSpec = tween(200) // 更快的动画提升体验
        )
        navController.popBackStack()
    } else {
        // 弹性回弹动画
        anim.animateTo(
            0f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        )
    }
}
