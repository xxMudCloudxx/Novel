package com.novel.page.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

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
        LoadingComponent(
            component = component,
            modifier = modifier,
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
