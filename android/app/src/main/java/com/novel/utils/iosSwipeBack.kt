package com.novel.utils

import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * 为任意 Composable 增加 iOS 式侧滑返回手势与动画。
 *
 * @param edgeWidthDp 检测手势的左侧热区宽度
 * @param completeThreshold 完成返回的阈值（屏宽百分比）
 */
fun Modifier.iosSwipeBack(
    edgeWidthDp: Dp = 24.wdp,
    completeThreshold: Float = 0.33f
): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val edgeWidthPx = with(density) { edgeWidthDp.toPx() }

    // 只负责位移，不引起重组
    val offsetX = remember { Animatable(0f) }

    // NestedScroll 优雅处理子组件（如 ScrollableColumn）与侧滑的冲突
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset.Zero

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 手指向右拖为负数，available.x > 0
                val dx = available.x.coerceAtLeast(0f)
                if (dx != 0f) {
                    scope.launch { offsetX.snapTo((offsetX.value + dx).coerceAtMost(widthPx)) }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                NavViewModel.navController.value?.let {
                    decideFinish(offsetX, widthPx, completeThreshold,
                        it
                    )
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    pointerInput(Unit) {
        // 只在边缘检测 DOWN，避免误触
        detectHorizontalDragGestures(
            onDragStart = { pos ->
                if (pos.x > edgeWidthPx){}  // 非边缘直接忽略
            },
            onHorizontalDrag = { _, dragAmount ->
                val newOffset = (offsetX.value + dragAmount).coerceIn(0f, widthPx)
                scope.launch { offsetX.snapTo(newOffset) }
            },
            onDragEnd = {
                scope.launch {
                    NavViewModel.navController.value?.let {
                        decideFinish(offsetX, widthPx, completeThreshold,
                            it
                        )
                    }
                }
            },
            onDragCancel = {
                scope.launch {
                    offsetX.animateTo(
                        0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                }
            }
        )
    }
        // 真正做动画的是 graphicsLayer，不会重新组合
        .graphicsLayer { translationX = offsetX.value }
        .nestedScroll(connection)
}

private suspend fun decideFinish(
    anim: Animatable<Float, AnimationVector1D>,
    widthPx: Float,
    threshold: Float,
    navController: NavController
) {
    if (anim.value > widthPx * threshold) {
        // 补到整屏然后 pop
        anim.animateTo(widthPx, animationSpec = tween(240))
        navController.popBackStack()
    } else {
        // 回弹
        anim.animateTo(
            0f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    }
}
