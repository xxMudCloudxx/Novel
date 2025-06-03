package com.novel.page.component.pagecurl.page

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import com.novel.page.component.pagecurl.config.PageCurlConfig
import com.novel.page.component.pagecurl.utils.multiply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 点击手势修饰符
 *
 * 为PageCurl组件添加点击翻页功能，支持区域检测和自定义点击处理
 *
 * @param config PageCurl配置
 * @param scope 协程作用域
 * @param onTapForward 向前点击回调
 * @param onTapBackward 向后点击回调
 * @return 应用了点击手势的修饰符
 */
@ExperimentalPageCurlApi
internal fun Modifier.tapGesture(
    config: PageCurlConfig,
    scope: CoroutineScope,
    onTapForward: suspend () -> Unit,
    onTapBackward: suspend () -> Unit,
): Modifier = composed {
    val viewConfiguration = LocalViewConfiguration.current
    pointerInput(config) {
        val tapInteraction =
            config.tapInteraction as? PageCurlConfig.TargetTapInteraction ?: return@pointerInput

        awaitEachGesture {
            // 等待按下事件
            val down = awaitFirstDown().also { it.consume() }
            // 等待抬起或取消事件
            val up = waitForUpOrCancellation() ?: return@awaitEachGesture

            // 检查是否是有效的点击（移动距离不超过触摸阈值）
            if ((down.position - up.position).getDistance() > viewConfiguration.touchSlop) {
                return@awaitEachGesture
            }

            // 优先处理自定义点击
            if (config.tapCustomEnabled && config.onCustomTap(this, size, up.position)) {
                return@awaitEachGesture
            }

            // 检查向前点击区域
            if (config.tapForwardEnabled &&
                tapInteraction.forward.target.multiply(size).contains(up.position)
            ) {
                scope.launch {
                    onTapForward()
                }
                return@awaitEachGesture
            }

            // 检查向后点击区域
            if (config.tapBackwardEnabled &&
                tapInteraction.backward.target.multiply(size).contains(up.position)
            ) {
                scope.launch {
                    onTapBackward()
                }
                return@awaitEachGesture
            }
        }
    }
}

/**
 * 点击处理修饰符（内部使用）
 *
 * @param onTap 点击处理函数
 * @return 修饰符
 */
private fun Modifier.onTap(
    onTap: (offset: androidx.compose.ui.geometry.Offset, size: androidx.compose.ui.unit.IntSize) -> Boolean
): Modifier = this // 简化实现，实际使用中需要完整的点击检测逻辑
