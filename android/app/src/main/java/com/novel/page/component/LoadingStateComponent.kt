package com.novel.page.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.novel.utils.SwipeBackContainer
import com.novel.utils.iosSwipeBackBasic
import com.novel.ui.theme.NovelColors

@Stable
interface LoadingStateComponent : LoadingComponent, StateComponent

interface ILoadingStateComponentDefaults : ILoadingComponentDefaults, IStateComponentDefaults {

    @Composable
    fun LoadingStateComponent(
        component: LoadingStateComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    )
}

open class LoadingStateComponentDefaults(
    private val loadingComponentDefaults: ILoadingComponentDefaults = LoadingComponentDefaults.instance,
    private val stateComponentDefaults: IStateComponentDefaults = StateComponentDefaults.instance
) : Defaults(), ILoadingStateComponentDefaults,
    ILoadingComponentDefaults by loadingComponentDefaults,
    IStateComponentDefaults by stateComponentDefaults {

    companion object : Target<LoadingStateComponentDefaults>(LoadingStateComponentDefaults())

    final override val loading: @Composable BoxScope.() -> Unit
        get() = loadingComponentDefaults.loading
    final override val empty: @Composable BoxScope.() -> Unit
        get() = stateComponentDefaults.empty
    final override val error: @Composable BoxScope.() -> Unit
        get() = stateComponentDefaults.error

    @Composable
    override fun LoadingStateComponent(
        component: LoadingStateComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    ) {
        LoadingStateComponentImpl(
            component = component,
            modifier = modifier,
            enabled = enabled,
            loading = loading,
            empty = empty,
            error = error,
            content = content
        )
    }

    @Composable
    private fun LoadingStateComponentImpl(
        component: LoadingStateComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    ) {
        // 使用新的SwipeBackContainer组件，指示器显示在背景区域
        SwipeBackContainer(
            modifier = modifier,
            backgroundColor = NovelColors.NovelBookBackground.copy(alpha = 0.7f) // 使用主题背景色
        ) {
            LoadingComponent(
                component = component,
                modifier = Modifier, // 避免重复应用 modifier
                enabled = enabled,
                loading = loading,
            ) {
                StateComponent(
                    component = component,
                    modifier = Modifier,
                    error = error,
                    empty = empty,
                    content = content
                )
            }
        }
    }
}

@Composable
fun LoadingStateComponent(
    component: LoadingStateComponent,
    modifier: Modifier = Modifier,
    enabled: Boolean = LoadingStateComponentDefaults.instance.enabled,
    loading: @Composable BoxScope.() -> Unit = LoadingStateComponentDefaults.instance.loading,
    error: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.error,
    empty: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.empty,
    content: @Composable BoxScope.() -> Unit
) {
    LoadingStateComponentDefaults.instance.LoadingStateComponent(
        component = component,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        error = error,
        empty = empty,
        content = content
    )
}

/**
 * 带自定义背景的LoadingStateComponent版本
 * 可以指定背景颜色来匹配不同的设计需求
 */
@Composable
fun LoadingStateComponent(
    component: LoadingStateComponent,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NovelColors.NovelBookBackground.copy(alpha = 0.7f),
    enabled: Boolean = LoadingStateComponentDefaults.instance.enabled,
    loading: @Composable BoxScope.() -> Unit = LoadingStateComponentDefaults.instance.loading,
    error: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.error,
    empty: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.empty,
    content: @Composable BoxScope.() -> Unit
) {
    SwipeBackContainer(
        modifier = modifier,
        backgroundColor = backgroundColor
    ) {
        LoadingComponent(
            component = component,
            modifier = Modifier,
            enabled = enabled,
            loading = loading,
        ) {
            StateComponent(
                component = component,
                modifier = Modifier,
                error = error,
                empty = empty,
                content = content
            )
        }
    }
}

/**
 * 基础版本的LoadingStateComponent - 不包含提示UI的侧滑返回
 * 性能更优，适用于不需要提示文字的场景
 */
@Composable
fun LoadingStateComponentBasic(
    component: LoadingStateComponent,
    modifier: Modifier = Modifier,
    enabled: Boolean = LoadingStateComponentDefaults.instance.enabled,
    loading: @Composable BoxScope.() -> Unit = LoadingStateComponentDefaults.instance.loading,
    error: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.error,
    empty: @Composable (BoxScope.() -> Unit)? = LoadingStateComponentDefaults.instance.empty,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.iosSwipeBackBasic()) {
        LoadingComponent(
            component = component,
            modifier = Modifier,
            enabled = enabled,
            loading = loading,
        ) {
            StateComponent(
                component = component,
                modifier = Modifier,
                error = error,
                empty = empty,
                content = content
            )
        }
    }
}
