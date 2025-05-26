package com.novel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NovelColors {
    val NovelMain: Color
        @Composable get() = Color(0xFFFF995D)

    val NovelMainLight: Color
        @Composable get() = Color(0xFFF86827)

    val NovelDivider: Color
        @Composable get() = dynamicColor(light = Color(0xFFF7F7F8), dark = Color(0xFF1C1C1E))

    val NovelSecondaryBackground: Color
        @Composable get() = dynamicColor(light = Color(0xFFF7F7F8), dark = Color(0xFF1C1C1E))

    val NovelBackground: Color
        @Composable get() = dynamicColor(light = Color(0xFFFFFFFF), dark = Color(0xFF000000))

    val NovelTextGray: Color
        @Composable get() = dynamicColor(light = Color(0xFF7F7F7F), dark = Color(0xFF97989F))

    val NovelText: Color
        @Composable get() = dynamicColor(light = Color(0xFF000000), dark = Color(0xFFFFFFFF))

    val NovelLightGray: Color
        @Composable get() = dynamicColor(light = Color(0xFFDDDDDD), dark = Color(0xFF1C1C1E))

    val NovelChipBackground: Color
        @Composable get() = dynamicColor(light = Color(0xFFEBEDF0), dark = Color(0xFF23242B))
}

@Composable
fun dynamicColor(light: Color, dark: Color): Color {
    return if (isSystemInDarkTheme()) dark else light
}