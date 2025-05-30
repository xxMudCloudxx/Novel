package com.novel.page.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.novel.ui.theme.NovelColors
import com.novel.utils.wdp
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.novel.utils.debounceClickable

/**
 * NovelImageView：异步加载网络图片，支持自定义尺寸、加载中、错误占位和重试逻辑。
 *
 * @param imageUrl 网络图片地址
 * @param isLoading 是否处于加载中状态
 * @param error 错误信息（非空表示加载失败）
 * @param widthDp 宽度，单位 dp；<=0 时不限制宽度
 * @param heightDp 高度，单位 dp；<=0 时不限制高度
 * @param contentScale 图片缩放模式
 * @param crossfadeDuration 过渡动画时长，单位毫秒
 * @param cachePolicy 图片缓存策略
 * @param retryDebounceMs 重试按钮防抖时间，单位毫秒
 * @param modifier 额外 Modifier
 * @param onRetry 重试回调
 * @param placeholderContent 加载中占位 Composable
 * @param errorContent 加载失败占位 Composable
 */
@Composable
fun NovelImageView(
    imageUrl: String?,
    isLoading: Boolean = false,
    error: String? = null,
    widthDp: Int = 0,
    heightDp: Int = 0,
    contentScale: ContentScale = ContentScale.Fit,
    crossfadeDuration: Int = 300,
    cachePolicy: Pair<CachePolicy, CachePolicy> = CachePolicy.ENABLED to CachePolicy.ENABLED,
    retryDebounceMs: Long = 500,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    placeholderContent: @Composable () -> Unit = {
        Box(
            modifier.background(NovelColors.NovelMainLight).wrapContentSize(Alignment.Center)
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.wdp,
                modifier = Modifier
                    .size(24.wdp),
                color = NovelColors.NovelTextGray
            )
        }
    },
    errorContent: @Composable (retry: () -> Unit) -> Unit = { retry ->
        Box(
            modifier.background(NovelColors.NovelMainLight).wrapContentSize (Alignment.Center)
        ) {
        IconButton(onClick = { retry() }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "加载失败，点击重试",
                tint = NovelColors.NovelText,
                modifier = Modifier
                    .size(24.wdp)
                    .debounceClickable(retryDebounceMs) { onRetry() }
            )
        }
    }
    }
) {
    // 构造 Modifier
    val imgModifier = remember(widthDp, heightDp, modifier) {
        modifier.let {
            var m = it
            if (widthDp > 0) m = m.width(widthDp.wdp)
            if (heightDp > 0) m = m.height(heightDp.wdp)
            if (widthDp <= 0 && heightDp <= 0) m = m.fillMaxWidth()
            m
        }
    }
    Log.d("NovelImageView", "imageUrl: $imageUrl, isLoading: $isLoading, error: $error")

    // 使用 key 确保 imageUrl 变化时重新加载
    key(imageUrl) {
        when {
            isLoading -> {
                // 加载中状态
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    placeholderContent()
                }
            }

            error != null -> {
                // 错误状态
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            imageUrl.isNullOrEmpty() -> {
                // 空 URL 显示错误占位
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            else -> {
                // 正常加载图片
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(crossfadeDuration)
                        .memoryCachePolicy(cachePolicy.first)
                        .diskCachePolicy(cachePolicy.second)
                        .build(),
                    contentDescription = null,
                    modifier = imgModifier,
                    contentScale = contentScale,
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> placeholderContent()
                        is AsyncImagePainter.State.Error -> {
                            val errorMessage =
                                "图片加载失败: ${(painter.state as AsyncImagePainter.State.Error).result.throwable}"
                            Log.e(
                                "NovelImageView",
                                errorMessage,
                                (painter.state as AsyncImagePainter.State.Error).result.throwable
                            )
                            errorContent(onRetry)
                        }

                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}