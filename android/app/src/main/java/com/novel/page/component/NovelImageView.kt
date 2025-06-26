package com.novel.page.component

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.novel.utils.debounceClickable

/**
 * 小说应用专用异步图片加载组件
 *
 * 基于Coil库实现的高性能图片加载组件，专为小说应用的书籍封面、头像等场景优化
 *
 * 🔥 核心特性：
 * - 📱 响应式布局：支持固定尺寸和自适应布局
 * - 🎭 优雅降级：提供加载中、错误状态的精美占位
 * - 🔄 智能重试：支持用户手动重试和防抖机制
 * - 🚀 性能优化：内存+磁盘双级缓存，减少网络请求
 * - ✨ 平滑动画：支持淡入淡出过渡效果
 * - 🛡️ 健壮性：空URL、异常状态的完善处理
 *
 * 📊 适用场景：
 * - 书籍封面展示（列表、详情页）
 * - 用户头像加载
 * - 轮播图、推荐位图片
 * - 任何需要网络图片的场景
 *
 * @param imageUrl 网络图片地址，支持http/https协议
 * @param isLoading 外部加载状态，用于统一控制显示逻辑
 * @param error 外部错误信息，非空时显示错误占位
 * @param widthDp 固定宽度(dp)，<=0时使用fillMaxWidth
 * @param heightDp 固定高度(dp)，<=0时使用wrap_content
 * @param contentScale 图片缩放模式，默认Fit适应容器
 * @param crossfadeDuration 淡入动画时长(毫秒)，0则无动画
 * @param cachePolicy 缓存策略(内存+磁盘)，默认全部启用
 * @param retryDebounceMs 重试按钮防抖时间，防止用户误触
 * @param modifier 额外的修饰符，用于外部样式定制
 * @param onRetry 重试操作回调，由外部决定重试逻辑
 * @param placeholderContent 加载中的占位组件，可自定义样式
 * @param errorContent 加载失败的占位组件，包含重试按钮
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
            modifier
                .background(NovelColors.NovelMainLight)
                .wrapContentSize(Alignment.Center)
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
            modifier
                .background(NovelColors.NovelMainLight)
                .wrapContentSize(Alignment.Center)
        ) {
            IconButton(onClick = { retry() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "加载失败，点击重试",
                    tint = NovelColors.NovelText,
                    modifier = Modifier
                        .size(24.wdp)
                        .debounceClickable(onClick = { retry() })
                )
            }
        }
    }
) {
    val imageUrl = "https://img.picui.cn/free/2025/06/22/6857c4dee81d8.jpg"
    // 预处理图片URL，过滤空值和无效URL
    val processedImageUrl = remember(imageUrl) {
        imageUrl.takeIf { it?.isNotEmpty() ?: false }
    }

    // 预计算图片修饰符，避免重复创建
    val imgModifier = remember(widthDp, heightDp, modifier) {
        modifier.let {
            var m = it
            if (widthDp > 0) m = m.width(widthDp.wdp)
            if (heightDp > 0) m = m.height(heightDp.wdp)
            if (widthDp <= 0 && heightDp <= 0) m = m.fillMaxWidth()
            m
        }
    }

    // 使用 key 确保 imageUrl 变化时重新加载
    key(processedImageUrl) {
        when {
            isLoading -> {
                // 添加日志：加载中状态
                Log.d("NovelImageView", "显示加载中状态")
                // 加载中状态
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    placeholderContent()
                }
            }

            error != null -> {
                // 添加日志：错误状态
                Log.e("NovelImageView", "显示错误状态: $error")
                // 错误状态
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            processedImageUrl == null -> {
                // 添加日志：空URL状态
                Log.w("NovelImageView", "图片URL为空，显示错误占位")
                // 空 URL 显示错误占位
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            else -> {
                val current = LocalContext.current
                // 预构建图片请求，避免重复创建
                val imageRequest = remember(processedImageUrl, crossfadeDuration, cachePolicy) {
                    ImageRequest.Builder(current)
                        .data(processedImageUrl)
                        .crossfade(crossfadeDuration)
                        .memoryCachePolicy(cachePolicy.first)
                        .diskCachePolicy(cachePolicy.second)
                        .build()
                }

                // 正常加载图片
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = imgModifier,
                    contentScale = contentScale,
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            // 添加日志：图片加载中
                            Log.v("NovelImageView", "图片加载中: $processedImageUrl")
                            placeholderContent()
                        }
                        is AsyncImagePainter.State.Error -> {
                            // 添加日志：图片加载失败
                            Log.e("NovelImageView", "图片加载失败: $processedImageUrl")
                            // 图片加载失败，显示错误占位
                            errorContent(onRetry)
                        }

                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}