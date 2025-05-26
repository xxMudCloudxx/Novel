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

    @Composable
    private fun LoadingComponentImpl(
        component: LoadingComponent,
        modifier: Modifier,
        enabled: Boolean,
        loading: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.() -> Unit
    ) {
        val loadingContent = if (loading !== this.loading) loading else {
            {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
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
            BackHandler(enabled = enabled && showLoading && containsCancelable) {
                component.cancelLoading()
            }
            if (enabled && showLoading) {
                loadingContent()
            }
        }
    }
}


@Stable
interface LoadingComponent {

    val loading: Boolean

    val containsCancelable: Boolean

    fun showLoading(show: Boolean)

    fun cancelLoading()
}


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
