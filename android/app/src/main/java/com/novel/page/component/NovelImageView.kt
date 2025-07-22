package com.novel.page.component

import android.annotation.SuppressLint
import com.novel.utils.TimberLogger
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.novel.utils.debounceClickable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 图片加载策略枚举
 * 根据不同使用场景优化加载行为
 */
enum class ImageLoadingStrategy {
    /**
     * 高性能模式 - 适用于瀑布流、列表等大量图片场景
     * 启用内存缓存、磁盘缓存、Bitmap复用池、内存压力管理
     */
    HIGH_PERFORMANCE,
    
    /**
     * 标准模式 - 适用于常规图片加载场景
     * 标准的内存和磁盘缓存策略
     */
    STANDARD,
    
    /**
     * 临时模式 - 适用于验证码等临时图片
     * 最小缓存策略，避免占用过多内存
     */
    TEMPORARY,
    
    /**
     * 高质量模式 - 适用于书籍详情封面等重要图片
     * 高质量缓存，优先保证图片质量
     */
    HIGH_QUALITY,
    
    /**
     * 动画模式 - 适用于动画场景
     * 快速加载和复用，减少动画卡顿
     */
    ANIMATION
}

/**
 * 小说应用专用异步图片加载组件 - 优化版
 *
 * 集成NovelCachedImageView的高级缓存功能，根据使用场景提供不同的加载策略
 *
 * 🔥 核心特性：
 * - 📱 响应式布局：支持固定尺寸和自适应布局
 * - 🎭 优雅降级：提供加载中、错误状态的精美占位
 * - 🔄 智能重试：支持用户手动重试和防抖机制
 * - 🚀 高性能缓存：多级缓存+Bitmap复用+内存压力管理
 * - ✨ 场景优化：根据使用场景选择最佳加载策略
 * - 🛡️ 健壮性：空URL、异常状态的完善处理
 *
 * 📊 适用场景：
 * - 书籍封面展示（列表、详情页）
 * - 用户头像加载
 * - 轮播图、推荐位图片
 * - 验证码等临时图片
 * - 动画场景图片
 *
 * @param imageUrl 网络图片地址，支持http/https协议和相对路径
 * @param loadingStrategy 加载策略，根据使用场景选择
 * @param needToken 是否需要在请求头中附带访问令牌
 * @param useAdvancedCache 是否启用高级缓存功能（NovelCachedImageView）
 * @param isLoading 外部加载状态，用于统一控制显示逻辑
 * @param error 外部错误信息，非空时显示错误占位
 * @param widthDp 固定宽度(dp)，<=0时使用fillMaxWidth
 * @param heightDp 固定高度(dp)，<=0时使用wrap_content
 * @param contentScale 图片缩放模式，默认Fit适应容器
 * @param crossfadeDuration 淡入动画时长(毫秒)，0则无动画
 * @param cachePolicy 缓存策略(内存+磁盘)，默认根据loadingStrategy自动选择
 * @param retryDebounceMs 重试按钮防抖时间，防止用户误触
 * @param modifier 额外的修饰符，用于外部样式定制
 * @param onRetry 重试操作回调，由外部决定重试逻辑
 * @param placeholderContent 加载中的占位组件，可自定义样式
 * @param errorContent 加载失败的占位组件，包含重试按钮
 */
@Composable
fun NovelImageView(
    imageUrl: String?,
    loadingStrategy: ImageLoadingStrategy = ImageLoadingStrategy.STANDARD,
    needToken: Boolean = false,
    useAdvancedCache: Boolean = true,
    isLoading: Boolean = false,
    error: String? = null,
    widthDp: Int = 0,
    heightDp: Int = 0,
    contentScale: ContentScale = ContentScale.Fit,
    crossfadeDuration: Int = 300,
    cachePolicy: Pair<CachePolicy, CachePolicy>? = null,
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
    TimberLogger.d("NovelImageView", "imageUrl: $imageUrl, strategy: $loadingStrategy")
    
    // 根据加载策略决定是否使用高级缓存
    val shouldUseAdvancedCache = useAdvancedCache && loadingStrategy != ImageLoadingStrategy.TEMPORARY
    
    // 预处理图片URL，过滤空值和无效URL
    val processedImageUrl by remember(imageUrl) {
        derivedStateOf {
            // 移除测试URL的硬编码，使用实际传入的URL
            imageUrl?.takeIf { it.isNotEmpty() && it.isNotBlank() }
        }
    }

    // 根据加载策略自动选择缓存策略
    val finalCachePolicy by remember(loadingStrategy, cachePolicy) {
        derivedStateOf {
            cachePolicy ?: when (loadingStrategy) {
                ImageLoadingStrategy.HIGH_PERFORMANCE -> CachePolicy.ENABLED to CachePolicy.ENABLED
                ImageLoadingStrategy.HIGH_QUALITY -> CachePolicy.ENABLED to CachePolicy.ENABLED
                ImageLoadingStrategy.ANIMATION -> CachePolicy.ENABLED to CachePolicy.READ_ONLY
                ImageLoadingStrategy.TEMPORARY -> CachePolicy.DISABLED to CachePolicy.DISABLED
                ImageLoadingStrategy.STANDARD -> CachePolicy.ENABLED to CachePolicy.ENABLED
            }
        }
    }

    // 预计算图片修饰符，避免重复创建
    val imgModifier by remember(widthDp, heightDp, modifier) {
        derivedStateOf {
            modifier.let {
                var m = it
                if (widthDp > 0) m = m.width(widthDp.wdp)
                if (heightDp > 0) m = m.height(heightDp.wdp)
                if (widthDp <= 0 && heightDp <= 0) m = m.fillMaxWidth()
                m
            }
        }
    }

    // 使用 key 确保 imageUrl 变化时重新加载
    key(processedImageUrl, loadingStrategy) {
        when {
            isLoading -> {
                TimberLogger.d("NovelImageView", "显示加载中状态")
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    placeholderContent()
                }
            }

            error != null -> {
                TimberLogger.e("NovelImageView", "显示错误状态: $error")
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            processedImageUrl == null -> {
                TimberLogger.w("NovelImageView", "图片URL为空，显示错误占位")
                Box(
                    modifier = imgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    errorContent(onRetry)
                }
            }

            shouldUseAdvancedCache -> {
                // 使用高级缓存加载
                TimberLogger.d("NovelImageView", "使用高级缓存加载: $processedImageUrl")
                AdvancedCachedImageLoader(
                    url = processedImageUrl!!,
                    needToken = needToken,
                    modifier = imgModifier,
                    contentScale = contentScale,
                    placeholderContent = placeholderContent,
                    errorContent = { errorContent(onRetry) }
                )
            }

            else -> {
                // 使用标准Coil加载
                TimberLogger.d("NovelImageView", "使用标准加载: $processedImageUrl")
                StandardImageLoader(
                    url = processedImageUrl!!,
                    modifier = imgModifier,
                    contentScale = contentScale,
                    crossfadeDuration = crossfadeDuration,
                    cachePolicy = finalCachePolicy,
                    placeholderContent = placeholderContent,
                    errorContent = { errorContent(onRetry) }
                )
            }
        }
    }
}

/**
 * 高级缓存图片加载器
 * 使用NovelCachedImageView的多级缓存功能
 */
@Composable
private fun AdvancedCachedImageLoader(
    url: String,
    needToken: Boolean,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderContent: @Composable () -> Unit,
    errorContent: @Composable () -> Unit
) {
    NovelCachedImageView(
        url = url,
        needToken = needToken,
        modifier = modifier,
        content = { imageBitmap ->
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        },
        placeholder = placeholderContent
    )
}

/**
 * 标准图片加载器
 * 使用Coil的标准加载功能
 */
@Composable
private fun StandardImageLoader(
    url: String,
    modifier: Modifier,
    contentScale: ContentScale,
    crossfadeDuration: Int,
    cachePolicy: Pair<CachePolicy, CachePolicy>,
    placeholderContent: @Composable () -> Unit,
    errorContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val imageRequest by remember(url, crossfadeDuration, cachePolicy) {
        derivedStateOf {
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(crossfadeDuration)
                .memoryCachePolicy(cachePolicy.first)
                .diskCachePolicy(cachePolicy.second)
                .build()
        }
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                TimberLogger.v("NovelImageView", "图片加载中: $url")
                placeholderContent()
            }
            is AsyncImagePainter.State.Error -> {
                TimberLogger.e("NovelImageView", "图片加载失败: $url")
                errorContent()
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}