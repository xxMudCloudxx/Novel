@file:Suppress("ComplexMethod", "LongParameterList", "LongMethod")

package com.novel.page.component.pagecurl.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.novel.page.component.pagecurl.page.ExperimentalPageCurlApi

/**
 * 创建并记住PageCurlConfig配置
 * 
 * 这个函数提供了PageCurl组件的完整配置选项，包括视觉效果、交互方式等
 *
 * @param backPageColor 背页颜色。大多数情况下应设置为内容背景色
 * @param backPageContentAlpha 背页内容透明度，定义内容"透过"背页的可见程度。0（不可见）到1（完全可见）
 * @param shadowColor 阴影颜色。大多数情况下应设置为内容背景色的反色。应为纯色，使用shadowAlpha调整不透明度
 * @param shadowAlpha 阴影的透明度
 * @param shadowRadius 基础阴影大小
 * @param shadowOffset 阴影偏移。轻微的偏移可以增加真实感
 * @param thicknessDp 页面厚度效果的大小
 * @param highlightStrength 高光效果的强度，0到1之间
 * @param perspectiveTiltDeg 透视倾斜角度，增加3D感
 * @param dynamicShadowEnabled 是否启用动态阴影（阴影大小随拖拽距离变化）
 * @param creaseShadowStrength 折痕阴影强度，0到1之间
 * @param rimLightWidth 边缘光宽度
 * @param selfShadowStrength 卷曲正面朝里方向的自阴影强度，0到1之间
 * @param dragForwardEnabled 是否启用向前拖拽交互
 * @param dragBackwardEnabled 是否启用向后拖拽交互
 * @param tapForwardEnabled 是否启用向前点击交互
 * @param tapBackwardEnabled 是否启用向后点击交互
 * @param tapCustomEnabled 是否启用自定义点击交互，参见onCustomTap
 * @param dragInteraction 拖拽交互设置
 * @param tapInteraction 点击交互设置
 * @param onCustomTap 自定义点击处理lambda。接收密度作用域、PageCurl尺寸和点击位置。返回true表示点击已处理，false则使用默认处理
 */
@ExperimentalPageCurlApi
@Composable
fun rememberPageCurlConfig(
    backPageColor: Color = Color.White,
    backPageContentAlpha: Float = 0.1f,
    shadowColor: Color = Color.Black,
    shadowAlpha: Float = 0.2f,
    shadowRadius: Dp = 15.dp,
    shadowOffset: DpOffset = DpOffset((-10).dp, 10.dp),
    thicknessDp: Dp = 3.dp,
    highlightStrength: Float = 0.3f,
    perspectiveTiltDeg: Float = 10f,
    dynamicShadowEnabled: Boolean = true,
    creaseShadowStrength: Float = 0.6f,
    rimLightWidth: Dp = 1.dp,
    selfShadowStrength: Float = 0.15f,
    dragForwardEnabled: Boolean = true,
    dragBackwardEnabled: Boolean = true,
    tapForwardEnabled: Boolean = true,
    tapBackwardEnabled: Boolean = true,
    tapCustomEnabled: Boolean = true,
    dragInteraction: PageCurlConfig.DragInteraction = PageCurlConfig.StartEndDragInteraction(),
    tapInteraction: PageCurlConfig.TapInteraction = PageCurlConfig.TargetTapInteraction(),
    onCustomTap: Density.(IntSize, Offset) -> Boolean = { _, _ -> false },
): PageCurlConfig =
    rememberSaveable(
        saver = listSaver(
            save = {
                fun Rect.forSave(): List<Any> =
                    listOf(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)

                fun PageCurlConfig.DragInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.GestureDragInteraction ->
                            listOf(forward.target, backward.target)

                        is PageCurlConfig.StartEndDragInteraction ->
                            listOf(forward.start, forward.end, backward.start, backward.end)
                    }

                fun PageCurlConfig.TapInteraction.getRectList(): List<Rect> =
                    when (this) {
                        is PageCurlConfig.TargetTapInteraction ->
                            listOf(forward.target, backward.target)
                    }

                fun PageCurlConfig.DragInteraction.forSave(): List<Any> =
                    listOf(
                        this::class.java,
                        pointerBehavior.name
                    ) + getRectList().flatMap(Rect::forSave)

                fun PageCurlConfig.TapInteraction.forSave(): List<Any> =
                    listOf(this::class.java) + getRectList().flatMap(Rect::forSave)

                listOf(
                    (it.backPageColor.value shr 32).toInt(),
                    it.backPageContentAlpha,
                    (it.shadowColor.value shr 32).toInt(),
                    it.shadowAlpha,
                    it.shadowRadius.value,
                    it.shadowOffset.x.value,
                    it.shadowOffset.y.value,
                    it.thicknessDp.value,
                    it.highlightStrength,
                    it.perspectiveTiltDeg,
                    it.dynamicShadowEnabled,
                    it.creaseShadowStrength,
                    it.rimLightWidth.value,
                    it.selfShadowStrength,
                    it.dragForwardEnabled,
                    it.dragBackwardEnabled,
                    it.tapForwardEnabled,
                    it.tapBackwardEnabled,
                    it.tapCustomEnabled,
                    *it.dragInteraction.forSave().toTypedArray(),
                    *it.tapInteraction.forSave().toTypedArray(),
                )
            },
            restore = {
                val iterator = it.iterator()
                fun Iterator<Any>.nextRect(): Rect =
                    Rect(next() as Float, next() as Float, next() as Float, next() as Float)

                PageCurlConfig(
                    Color(iterator.next() as Int),
                    iterator.next() as Float,
                    Color(iterator.next() as Int),
                    iterator.next() as Float,
                    Dp(iterator.next() as Float),
                    DpOffset(Dp(iterator.next() as Float), Dp(iterator.next() as Float)),
                    Dp(iterator.next() as Float),
                    iterator.next() as Float,
                    iterator.next() as Float,
                    iterator.next() as Boolean,
                    iterator.next() as Float,
                    Dp(iterator.next() as Float),
                    iterator.next() as Float,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    iterator.next() as Boolean,
                    when (iterator.next()) {
                        PageCurlConfig.GestureDragInteraction::class.java -> {
                            PageCurlConfig.GestureDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.GestureDragInteraction.Config(iterator.nextRect()),
                            )
                        }

                        PageCurlConfig.StartEndDragInteraction::class.java -> {
                            PageCurlConfig.StartEndDragInteraction(
                                PageCurlConfig.DragInteraction.PointerBehavior.valueOf(iterator.next() as String),
                                PageCurlConfig.StartEndDragInteraction.Config(
                                    iterator.nextRect(),
                                    iterator.nextRect()
                                ),
                                PageCurlConfig.StartEndDragInteraction.Config(
                                    iterator.nextRect(),
                                    iterator.nextRect()
                                ),
                            )
                        }

                        else -> error("无法恢复PageCurlConfig")
                    },
                    when (iterator.next()) {
                        PageCurlConfig.TargetTapInteraction::class.java -> {
                            PageCurlConfig.TargetTapInteraction(
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                                PageCurlConfig.TargetTapInteraction.Config(iterator.nextRect()),
                            )
                        }

                        else -> error("无法恢复PageCurlConfig")
                    },
                    onCustomTap
                )
            }
        )
    ) {
        PageCurlConfig(
            backPageColor = backPageColor,
            backPageContentAlpha = backPageContentAlpha,
            shadowColor = shadowColor,
            shadowAlpha = shadowAlpha,
            shadowRadius = shadowRadius,
            shadowOffset = shadowOffset,
            thicknessDp = thicknessDp,
            highlightStrength = highlightStrength,
            perspectiveTiltDeg = perspectiveTiltDeg,
            dynamicShadowEnabled = dynamicShadowEnabled,
            creaseShadowStrength = creaseShadowStrength,
            rimLightWidth = rimLightWidth,
            selfShadowStrength = selfShadowStrength,
            dragForwardEnabled = dragForwardEnabled,
            dragBackwardEnabled = dragBackwardEnabled,
            tapForwardEnabled = tapForwardEnabled,
            tapBackwardEnabled = tapBackwardEnabled,
            tapCustomEnabled = tapCustomEnabled,
            dragInteraction = dragInteraction,
            tapInteraction = tapInteraction,
            onCustomTap = onCustomTap
        )
    }

/**
 * PageCurl组件的配置类
 *
 * @param backPageColor 背页颜色。大多数情况下应设置为内容背景色
 * @param backPageContentAlpha 背页内容透明度，定义内容"透过"背页的可见程度。0（不可见）到1（完全可见）
 * @param shadowColor 阴影颜色。大多数情况下应设置为内容背景色的反色。应为纯色，使用shadowAlpha调整不透明度
 * @param shadowAlpha 阴影透明度
 * @param shadowRadius 基础阴影大小
 * @param shadowOffset 阴影偏移。轻微的偏移可以增加真实感
 * @param thicknessDp 页面厚度效果的大小
 * @param highlightStrength 高光效果的强度，0到1之间
 * @param perspectiveTiltDeg 透视倾斜角度，增加3D感
 * @param dynamicShadowEnabled 是否启用动态阴影（阴影大小随拖拽距离变化）
 * @param creaseShadowStrength 折痕阴影强度，0到1之间
 * @param rimLightWidth 边缘光宽度
 * @param selfShadowStrength 卷曲正面朝里方向的自阴影强度，0到1之间
 * @param dragForwardEnabled 是否启用向前拖拽交互
 * @param dragBackwardEnabled 是否启用向后拖拽交互
 * @param tapForwardEnabled 是否启用向前点击交互
 * @param tapBackwardEnabled 是否启用向后点击交互
 * @param tapCustomEnabled 是否启用自定义点击交互，参见onCustomTap
 * @param dragInteraction 拖拽交互设置
 * @param tapInteraction 点击交互设置
 * @param onCustomTap 自定义点击处理lambda。接收密度作用域、PageCurl尺寸和点击位置。返回true表示点击已处理，false则使用默认处理
 */
@ExperimentalPageCurlApi
class PageCurlConfig(
    backPageColor: Color,
    backPageContentAlpha: Float,
    shadowColor: Color,
    shadowAlpha: Float,
    shadowRadius: Dp,
    shadowOffset: DpOffset,
    thicknessDp: Dp,
    highlightStrength: Float,
    perspectiveTiltDeg: Float,
    dynamicShadowEnabled: Boolean,
    creaseShadowStrength: Float,
    rimLightWidth: Dp,
    selfShadowStrength: Float,
    dragForwardEnabled: Boolean,
    dragBackwardEnabled: Boolean,
    tapForwardEnabled: Boolean,
    tapBackwardEnabled: Boolean,
    tapCustomEnabled: Boolean,
    dragInteraction: DragInteraction,
    tapInteraction: TapInteraction,
    val onCustomTap: Density.(IntSize, Offset) -> Boolean,
) {
    /**
     * 背页颜色。大多数情况下应设置为内容背景色
     */
    var backPageColor: Color by mutableStateOf(backPageColor)

    /**
     * 背页内容透明度，定义内容"透过"背页的可见程度。0（不可见）到1（完全可见）
     */
    var backPageContentAlpha: Float by mutableStateOf(backPageContentAlpha)

    /**
     * 阴影颜色。大多数情况下应设置为内容背景色的反色。应为纯色，使用shadowAlpha调整不透明度
     */
    var shadowColor: Color by mutableStateOf(shadowColor)

    /**
     * 阴影透明度
     */
    var shadowAlpha: Float by mutableStateOf(shadowAlpha)

    /**
     * 基础阴影大小
     */
    var shadowRadius: Dp by mutableStateOf(shadowRadius)

    /**
     * 阴影偏移。轻微的偏移可以增加真实感
     */
    var shadowOffset: DpOffset by mutableStateOf(shadowOffset)

    /**
     * 页面厚度效果的大小
     */
    var thicknessDp: Dp by mutableStateOf(thicknessDp)

    /**
     * 高光效果的强度，0到1之间
     */
    var highlightStrength: Float by mutableStateOf(highlightStrength)

    /**
     * 透视倾斜角度，增加3D感
     */
    var perspectiveTiltDeg: Float by mutableStateOf(perspectiveTiltDeg)

    /**
     * 是否启用动态阴影（阴影大小随拖拽距离变化）
     */
    var dynamicShadowEnabled: Boolean by mutableStateOf(dynamicShadowEnabled)

    /**
     * 折痕阴影强度，0到1之间
     */
    var creaseShadowStrength: Float by mutableStateOf(creaseShadowStrength)

    /**
     * 边缘光宽度
     */
    var rimLightWidth: Dp by mutableStateOf(rimLightWidth)

    /**
     * 卷曲正面朝里方向的自阴影强度，0到1之间
     */
    var selfShadowStrength: Float by mutableStateOf(selfShadowStrength)

    /**
     * 是否启用向前拖拽交互
     */
    var dragForwardEnabled: Boolean by mutableStateOf(dragForwardEnabled)

    /**
     * 是否启用向后拖拽交互
     */
    var dragBackwardEnabled: Boolean by mutableStateOf(dragBackwardEnabled)

    /**
     * 是否启用向前点击交互
     */
    var tapForwardEnabled: Boolean by mutableStateOf(tapForwardEnabled)

    /**
     * 是否启用向后点击交互
     */
    var tapBackwardEnabled: Boolean by mutableStateOf(tapBackwardEnabled)

    /**
     * 是否启用自定义点击交互，参见onCustomTap
     */
    var tapCustomEnabled: Boolean by mutableStateOf(tapCustomEnabled)

    /**
     * 拖拽交互设置
     */
    var dragInteraction: DragInteraction by mutableStateOf(dragInteraction)

    /**
     * 点击交互设置
     */
    var tapInteraction: TapInteraction by mutableStateOf(tapInteraction)

    /**
     * 拖拽交互设置接口
     */
    sealed interface DragInteraction {

        /**
         * 拖拽过程中的指针行为
         */
        val pointerBehavior: PointerBehavior

        /**
         * 可用指针行为的枚举
         */
        enum class PointerBehavior {
            /**
             * 默认行为是原始行为，其中"翻页"锚定到用户的手指。
             * 这里的"翻页"是指分隔当前页背面和下一页正面的线。
             * 这意味着当手指拖到左边缘时，下一页完全可见。
             */
            Default,

            /**
             * 在页面边缘行为中，当前页面的右边缘锚定到用户的手指。
             * 这意味着当手指拖到左边缘时，下一页只有一半可见。
             */
            PageEdge;
        }
    }

    /**
     * 基于用户在PageCurl内开始和结束拖拽手势位置的拖拽交互设置
     *
     * @property pointerBehavior 拖拽过程中的指针行为
     * @property forward 向前点击配置
     * @property backward 向后点击配置
     */
    data class StartEndDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(start = rightHalf(), end = leftHalf()),
        val backward: Config = Config(start = leftHalf(), end = rightHalf())
    ) : DragInteraction {

        /**
         * 前进或后退拖拽的交互配置
         *
         * @property start 定义交互应该开始的矩形。矩形坐标是相对的（从0到1），然后缩放到PageCurl边界
         * @property end 定义交互应该结束的矩形。矩形坐标是相对的（从0到1），然后缩放到PageCurl边界
         */
        data class Config(val start: Rect, val end: Rect)
    }

    /**
     * 基于拖拽开始方向的拖拽交互设置
     *
     * @property pointerBehavior 拖拽过程中的指针行为
     * @property forward 向前点击配置
     * @property backward 向后点击配置
     */
    data class GestureDragInteraction(
        override val pointerBehavior: DragInteraction.PointerBehavior = DragInteraction.PointerBehavior.Default,
        val forward: Config = Config(target = full()),
        val backward: Config = Config(target = full()),
    ) : DragInteraction {

        /**
         * 前进或后退拖拽的交互配置
         *
         * @property target 定义捕获交互的矩形。矩形坐标是相对的（从0到1），然后缩放到PageCurl边界
         */
        data class Config(val target: Rect)
    }

    /**
     * 点击交互设置接口
     */
    sealed interface TapInteraction

    /**
     * 基于用户在PageCurl内点击位置的点击交互设置
     *
     * @property forward 向前点击配置
     * @property backward 向后点击配置
     */
    data class TargetTapInteraction(
        val forward: Config = Config(target = rightHalf()),
        val backward: Config = Config(target = leftHalf())
    ) : TapInteraction {

        /**
         * 前进或后退点击的交互配置
         *
         * @property target 定义捕获交互的矩形。矩形坐标是相对的（从0到1），然后缩放到PageCurl边界
         */
        data class Config(val target: Rect)
    }
}

/**
 * PageCurl的完整尺寸
 */
private fun full(): Rect = Rect(0.0f, 0.0f, 1.0f, 1.0f)

/**
 * PageCurl的左半部分
 */
private fun leftHalf(): Rect = Rect(0.0f, 0.0f, 0.5f, 1.0f)

/**
 * PageCurl的右半部分
 */
private fun rightHalf(): Rect = Rect(0.5f, 0.0f, 1.0f, 1.0f)
