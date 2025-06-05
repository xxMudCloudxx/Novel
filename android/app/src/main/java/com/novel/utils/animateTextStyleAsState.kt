package com.novel.utils

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 自定义动画：在两个 TextStyle 之间做插值
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun animateTextStyleAsState(
    targetValue: TextStyle,
    animationSpec: AnimationSpec<Float> = spring(),
    label: String = "TextStyleAnimation"
): State<TextStyle> {
    // Animatable 只支持数值，需要将 TextStyle 转换为 Float（这里只演示字号插值，忽略其它属性）
    val fontSizeAnim = remember { Animatable(targetValue.fontSize.value) }

    LaunchedEffect(targetValue) {
        fontSizeAnim.animateTo(
            targetValue = targetValue.fontSize.value,
            animationSpec = animationSpec
        )
    }

    // 返回的状态：把当前数值再套回 TextStyle，保持字体粗细/颜色等与 targetValue 一致
    return derivedStateOf {
        TextStyle(
            fontSize = fontSizeAnim.value.sp,
            fontWeight = targetValue.fontWeight ?: FontWeight.Normal,
            color = targetValue.color
            // 如有其他属性（fontFamily、lineHeight 等），可一并 copy
        )
    }
}
