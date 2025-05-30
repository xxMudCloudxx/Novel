package com.novel.utils

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// 扩展：带防抖的点击 Modifier
@Composable
fun Modifier.debounceClickable(
    intervalMillis: Long = 500L,
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    var lastTime by remember { mutableLongStateOf(0L) }
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTime > intervalMillis) {
            lastTime = now
            onClick()
        }
    }
}
