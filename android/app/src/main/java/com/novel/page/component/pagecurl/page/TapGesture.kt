package com.novel.page.component.pagecurl.page

import com.novel.utils.TimberLogger
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
 * 为PageCurl组件添加点击翻页功能
 * 支持区域检测、自定义点击处理和触摸阈值判断
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

        TimberLogger.d("TapGesture", "配置点击手势 - 向前: ${config.tapForwardEnabled}, 向后: ${config.tapBackwardEnabled}")

        awaitEachGesture {
            // 等待按下事件
            val down = awaitFirstDown().also { it.consume() }
            // 等待抬起或取消事件
            val up = waitForUpOrCancellation() ?: return@awaitEachGesture

            // 检查是否是有效的点击（移动距离不超过触摸阈值）
            val moveDistance = (down.position - up.position).getDistance()
            if (moveDistance > viewConfiguration.touchSlop) {
                TimberLogger.v("TapGesture", "移动距离过大，不是有效点击: $moveDistance")
                return@awaitEachGesture
            }

            TimberLogger.v("TapGesture", "检测到点击事件，位置: ${up.position}")

            // 优先处理自定义点击
            if (config.tapCustomEnabled && config.onCustomTap(this, size, up.position)) {
                TimberLogger.d("TapGesture", "自定义点击处理完成")
                return@awaitEachGesture
            }

            // 检查向前点击区域
            if (config.tapForwardEnabled &&
                tapInteraction.forward.target.multiply(size).contains(up.position)
            ) {
                TimberLogger.d("TapGesture", "触发向前点击")
                scope.launch {
                    onTapForward()
                }
                return@awaitEachGesture
            }

            // 检查向后点击区域
            if (config.tapBackwardEnabled &&
                tapInteraction.backward.target.multiply(size).contains(up.position)
            ) {
                TimberLogger.d("TapGesture", "触发向后点击")
                scope.launch {
                    onTapBackward()
                }
                return@awaitEachGesture
            }

            TimberLogger.v("TapGesture", "点击位置不在任何有效区域内")
        }
    }
}
