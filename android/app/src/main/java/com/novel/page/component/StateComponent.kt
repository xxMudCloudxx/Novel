package com.novel.page.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.novel.core.StableThrowable
import com.novel.utils.debounceClickable

@Stable
sealed class ViewState {

    data object Idle : ViewState()

    data object Empty : ViewState()

    data class Error(@Stable val ex: StableThrowable) : ViewState()
}

@Stable
interface StateComponent {

    val viewState: ViewState

    fun showViewState(viewState: ViewState)

    fun retry()
}

@Stable
interface IStateComponentDefaults {

    val empty: @Composable BoxScope.() -> Unit
    val error: @Composable BoxScope.() -> Unit

    @Composable
    fun StateComponent(
        component: StateComponent,
        modifier: Modifier,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    )
}

open class StateComponentDefaults : Defaults(), IStateComponentDefaults {

    companion object : Target<StateComponentDefaults>(StateComponentDefaults())

    final override val empty: @Composable BoxScope.() -> Unit = { /*EMPTY*/ }
    final override val error: @Composable BoxScope.() -> Unit = { /*EMPTY*/ }

    @Composable
    override fun StateComponent(
        component: StateComponent,
        modifier: Modifier,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    ) {
        StateComponentImpl(
            component = component,
            modifier = modifier,
            empty = empty,
            error = error,
            content = content
        )
    }

    @Composable
    private fun StateComponentImpl(
        component: StateComponent,
        modifier: Modifier,
        empty: @Composable (BoxScope.() -> Unit)?,
        error: @Composable (BoxScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    ) {
        val errorContent = if (error !== this.error) error else {
            {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .debounceClickable(onClick = {component.retry()}),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "请求错误")
                }
            }
        }
        val emptyContent = if (empty !== this.empty) empty else {
            {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无数据")
                }
            }
        }
        Box(modifier = modifier) {
            val contentEnabled by remember(component) {
                derivedStateOf {
                    val result = component.viewState
                    result == ViewState.Idle ||
                            (result is ViewState.Empty && emptyContent == null) ||
                            (result is ViewState.Error && errorContent == null)
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = 1f.takeIf { contentEnabled } ?: 0f
                    }
                    .pointerInteropFilter { !contentEnabled }
            ) {
                content()
            }
            if (!contentEnabled) {
                Box(modifier = Modifier.matchParentSize()) {
                    when (component.viewState) {
                        is ViewState.Error -> {
                            if (errorContent != null) {
                                errorContent()
                            }
                        }
                        is ViewState.Empty -> {
                            if (emptyContent != null) {
                                emptyContent()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun StateComponent(
    component: StateComponent,
    modifier: Modifier = Modifier,
    empty: @Composable (BoxScope.() -> Unit)? = StateComponentDefaults.instance.empty,
    error: @Composable (BoxScope.() -> Unit)? = StateComponentDefaults.instance.error,
    content: @Composable BoxScope.() -> Unit
) {
    StateComponentDefaults.instance.StateComponent(
        component = component,
        modifier = modifier,
        empty = empty,
        error = error,
        content = content
    )
}
