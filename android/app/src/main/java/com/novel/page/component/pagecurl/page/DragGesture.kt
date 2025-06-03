package com.novel.page.component.pagecurl.page

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import com.novel.page.component.pagecurl.config.PageCurlConfig
import com.novel.page.component.pagecurl.config.PageCurlConfig.DragInteraction.PointerBehavior
import com.novel.page.component.pagecurl.utils.multiply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 手势拖拽修饰符
 * 
 * 基于拖拽方向的交互模式，根据手势方向自动判断前进或后退
 *
 * @param dragInteraction 拖拽交互配置
 * @param state PageCurl内部状态
 * @param enabledForward 是否启用向前拖拽
 * @param enabledBackward 是否启用向后拖拽
 * @param scope 协程作用域
 * @param onChange 页面变化回调
 * @return 应用了拖拽手势的修饰符
 */
@ExperimentalPageCurlApi
internal fun Modifier.dragGesture(
    dragInteraction: PageCurlConfig.GestureDragInteraction,
    state: PageCurlState.InternalState,
    enabledForward: Boolean,
    enabledBackward: Boolean,
    scope: CoroutineScope,
    onChange: (Int) -> Unit
): Modifier = this.composed {
    val isEnabledForward = rememberUpdatedState(enabledForward)
    val isEnabledBackward = rememberUpdatedState(enabledBackward)

    pointerInput(state) {
        // 计算目标区域的实际像素坐标
        val forwardTargetRect by lazy { dragInteraction.forward.target.multiply(size) }
        val backwardTargetRect by lazy { dragInteraction.backward.target.multiply(size) }

        // 向前拖拽配置
        val forwardConfig = DragConfig(
            edge = state.forward,
            start = state.rightEdge,
            end = state.leftEdge,
            isEnabled = { isEnabledForward.value },
            isDragSucceed = { start, end -> end.x < start.x }, // 向左拖拽
            onChange = { onChange(+1) }
        )
        
        // 向后拖拽配置
        val backwardConfig = DragConfig(
            edge = state.backward,
            start = state.leftEdge,
            end = state.rightEdge,
            isEnabled = { isEnabledBackward.value },
            isDragSucceed = { start, end -> end.x > start.x }, // 向右拖拽
            onChange = { onChange(-1) }
        )

        // 检测卷曲手势
        detectCurlGestures(
            scope = scope,
            newEdgeCreator = when (dragInteraction.pointerBehavior) {
                PointerBehavior.Default -> NewEdgeCreator.Default()
                PointerBehavior.PageEdge -> NewEdgeCreator.PageEdge()
            },
            getConfig = { start, end ->
                val config = if (forwardTargetRect.contains(start) && end.x < start.x) {
                    forwardConfig
                } else if (backwardTargetRect.contains(start) && end.x > start.x) {
                    backwardConfig
                } else {
                    null
                }

                // 如果找到配置，取消之前的动画并重置状态
                if (config != null) {
                    scope.launch {
                        state.animateJob?.cancel()
                        state.reset()
                    }
                }

                config
            },
        )
    }
}
