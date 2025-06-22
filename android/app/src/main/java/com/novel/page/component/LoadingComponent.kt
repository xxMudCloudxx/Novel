package com.novel.page.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 加载组件默认接口
 * 定义加载组件的默认行为和样式
 */
interface ILoadingComponentDefaults {

    val enabled: Boolean

    val loading: @Composable BoxScope.() -> Unit

    @Composable
    fun LoadingComponent(
        component: LoadingComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.() -> Unit
    )
}

/**
 * 加载组件默认实现
 * 
 * 功能：
 * - 默认圆形进度指示器
 * - 阻止用户交互
 * - 支持返回键取消
 */
open class LoadingComponentDefaults : Defaults(), ILoadingComponentDefaults {

    companion object : Target<LoadingComponentDefaults>(LoadingComponentDefaults())

    override val enabled: Boolean = true

    final override val loading: @Composable BoxScope.() -> Unit = { /*EMPTY*/ }

    @Composable
    override fun LoadingComponent(
        component: LoadingComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.() -> Unit
    ) {
        LoadingComponentImpl(
            component = component,
            modifier = modifier,
            enabled = enabled,
            loading = loading,
            content = content
        )
    }

    /**
     * 加载组件内部实现
     * 处理加载状态显示和用户交互阻止
     */
    @Composable
    private fun LoadingComponentImpl(
        component: LoadingComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.() -> Unit
    ) {
        // 默认加载内容：居中的圆形进度指示器
        val loadingContent = if (loading !== this.loading) loading else {
            {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            // 阻止用户交互
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        Box(modifier = modifier) {
            content()

            val showLoading = component.loading
            val containsCancelable = component.containsCancelable
            
            // 支持返回键取消加载
            BackHandler(enabled = enabled && showLoading && containsCancelable) {
                component.cancelLoading()
            }
            
            if (enabled && showLoading) {
                loadingContent()
            }
        }
    }
}

/**
 * 加载组件接口
 * 定义加载状态管理的基本操作
 */
@Stable
interface LoadingComponent {

    /** 是否正在加载 */
    val loading: Boolean

    /** 是否支持取消 */
    val containsCancelable: Boolean

    /** 显示/隐藏加载状态 */
    fun showLoading(show: Boolean)

    /** 取消加载 */
    fun cancelLoading()
}

/**
 * 加载组件Composable函数
 * 
 * @param component 加载组件实例
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param loading 自定义加载内容
 * @param content 主要内容
 */
@Composable
fun LoadingComponent(
    component: LoadingComponent,
    modifier: Modifier = Modifier,
    enabled: Boolean = LoadingComponentDefaults.instance.enabled,
    loading: @Composable BoxScope.() -> Unit = LoadingComponentDefaults.instance.loading,
    content: @Composable BoxScope.() -> Unit
) {
    LoadingComponentDefaults.instance.LoadingComponent(
        component = component,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        content = content
    )
}
