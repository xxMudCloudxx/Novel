package com.novel.page.component.pagecurl.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.novel.page.component.pagecurl.config.PageCurlConfig
import com.novel.page.component.pagecurl.config.rememberPageCurlConfig

/**
 * 显示可以通过拖拽或点击手势翻页的页面组件
 * 
 * 这是一个高性能的仿真翻页组件，支持真实的书页卷曲效果
 *
 * @param count 页面总数
 * @param modifier 修饰符
 * @param state PageCurl的状态，用于程序化改变当前页面或观察变化
 * @param config PageCurl的配置
 * @param content 内容lambda，提供页面可组合项，接收页面编号参数
 */
@ExperimentalPageCurlApi
@Composable
public fun PageCurl(
    count: Int,
    modifier: Modifier = Modifier,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier) {
        // 设置状态：页面数量和约束条件
        state.setup(count, constraints)

        // 获取更新的状态值，确保状态同步
        val updatedCurrent by rememberUpdatedState(state.current)
        val internalState by rememberUpdatedState(state.internalState ?: return@BoxWithConstraints)
        val updatedConfig by rememberUpdatedState(config)

        // 根据配置选择拖拽手势类型
        val dragGestureModifier = when (val interaction = updatedConfig.dragInteraction) {
            is PageCurlConfig.GestureDragInteraction ->
                Modifier
                    .dragGesture(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )

            is PageCurlConfig.StartEndDragInteraction ->
                Modifier
                    .dragStartEnd(
                        dragInteraction = interaction,
                        state = internalState,
                        enabledForward = updatedConfig.dragForwardEnabled && updatedCurrent < state.max - 1,
                        enabledBackward = updatedConfig.dragBackwardEnabled && updatedCurrent > 0,
                        scope = scope,
                        onChange = { state.current = updatedCurrent + it }
                    )
        }

        Box(
            Modifier
                .then(dragGestureModifier)
                .tapGesture(
                    config = updatedConfig,
                    scope = scope,
                    onTapForward = state::next,
                    onTapBackward = state::prev,
                )
        ) {
            // 使用key来同步状态更新，确保UI与状态保持一致
            key(updatedCurrent, internalState.forward.value, internalState.backward.value) {
                // 绘制下一页（背景页）- 在翻页时显示
                if (updatedCurrent + 1 < state.max) {
                    content(updatedCurrent + 1)
                }

                // 绘制当前页（前景页）- 带有卷曲效果
                if (updatedCurrent < state.max) {
                    val forward = internalState.forward.value
                    Box(Modifier.drawCurl(updatedConfig, forward.top, forward.bottom)) {
                        content(updatedCurrent)
                    }
                }

                // 绘制上一页（向后翻页时显示）
                if (updatedCurrent > 0) {
                    val backward = internalState.backward.value
                    Box(Modifier.drawCurl(updatedConfig, backward.top, backward.bottom)) {
                        content(updatedCurrent - 1)
                    }
                }
            }
        }
    }
}

/**
 * 带稳定key的PageCurl组件
 * 
 * 用于在当前页面前添加和删除项目时提供稳定的key
 *
 * @param count 页面总数
 * @param key 为每个项目提供稳定key的lambda函数
 * @param modifier 修饰符
 * @param state PageCurl状态
 * @param config PageCurl配置
 * @param content 内容lambda
 */
@ExperimentalPageCurlApi
@Composable
public fun PageCurl(
    count: Int,
    key: (Int) -> Any,
    modifier: Modifier = Modifier,
    state: PageCurlState = rememberPageCurlState(),
    config: PageCurlConfig = rememberPageCurlConfig(),
    content: @Composable (Int) -> Unit
) {
    // 记录最后一个key值，用于检测变化
    var lastKey by remember(state.current) { mutableStateOf(if (count > 0) key(state.current) else null) }

    // 当count变化时，检查key是否变化，如果变化则更新当前页面索引
    remember(count) {
        val newKey = if (count > 0) key(state.current) else null
        if (newKey != lastKey) {
            val index = List(count, key).indexOf(lastKey).coerceIn(0, count - 1)
            lastKey = newKey
            state.current = index
        }
        count
    }

    PageCurl(
        count = count,
        state = state,
        config = config,
        content = content,
        modifier = modifier,
    )
}

/**
 * 旧版本兼容API（已弃用）
 * 
 * @param state PageCurl状态
 * @param modifier 修饰符
 * @param content 内容lambda
 */
@ExperimentalPageCurlApi
@Composable
@Deprecated("请在PageCurl组件中指定'count'参数来替代'max'")
public fun PageCurl(
    state: PageCurlState,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    PageCurl(
        count = state.max,
        state = state,
        modifier = modifier,
        content = content,
    )
}
