package com.novel.page.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.novel.utils.Store.NovelKeyChain.NovelKeyChainType
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 提供者接口
 * 抽象Token获取逻辑，便于单元测试和依赖注入
 */
interface TokenProvider {
    fun getToken(): String?
}

/**
 * 基于KeyChain的Token提供者实现
 * 从安全存储中读取访问令牌
 */
@Singleton
class KeyChainTokenProvider @Inject constructor(
    private val novelKeyChain: NovelKeyChain
) : TokenProvider {
    override fun getToken(): String? =
        novelKeyChain.read(NovelKeyChainType.TOKEN)
}

/**
 * 内存缓存接口
 * 定义图片内存缓存的基本操作
 */
interface MemoryCache {
    fun get(key: String): Bitmap?
    fun put(key: String, bitmap: Bitmap)
}

/**
 * 基于LruCache的内存缓存实现
 * 使用最近最少使用算法管理内存，防止内存溢出
 */
@Singleton
class LruMemoryCache @Inject constructor(): MemoryCache {
    private val cache = LruCache<String, Bitmap>(20 * 1024 * 1024) // 20MB缓存
    override fun get(key: String) = cache[key]
    override fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

/**
 * 图片加载服务接口
 * 定义异步图片加载的契约
 */
interface ImageLoaderService {
    suspend fun loadImage(url: String, needToken: Boolean): Bitmap?
}

/**
 * HTTP图片加载服务实现
 * 
 * 采用三级缓存策略：内存缓存 -> OkHttp磁盘缓存 -> 网络请求
 * 提供高性能的图片加载体验
 */
@Singleton
class HttpImageLoaderService @Inject constructor(
    private val client: OkHttpClient,
    private val memoryCache: MemoryCache,
    private val tokenProvider: TokenProvider
) : ImageLoaderService {
    
    companion object {
        private const val BASE_URL = "http://47.110.147.60:8080" // 基础URL
        private const val TAG = "HttpImageLoaderService"
    }
    
    override suspend fun loadImage(url: String, needToken: Boolean): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // 1. 内存缓存命中检查
                memoryCache.get(url)?.let { 
                    Log.v(TAG, "内存缓存命中: $url")
                    return@withContext it 
                }
                
                // 2. URL处理 - 相对路径转绝对路径
                val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "$BASE_URL$url"
                }
                
                Log.d(TAG, "开始加载图片: $fullUrl")
                
                // 3. 构建HTTP请求
                val reqBuilder = Request.Builder()
                    .url(fullUrl)
                    .get()
                if (needToken) tokenProvider.getToken()?.let {
                    reqBuilder.header("Authorization", it)
                }
                
                // 4. 执行网络请求（自动利用OkHttp磁盘缓存）
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "网络请求失败: $fullUrl, 状态码: ${resp.code}")
                        return@withContext null
                    }
                    
                    resp.body?.byteStream()?.let { stream ->
                        BitmapFactory.decodeStream(stream)?.also { bitmap ->
                            // 5. 写入内存缓存
                            memoryCache.put(url, bitmap)
                            Log.d(TAG, "图片加载成功并缓存: $url")
                        }
                    }
                }
            } catch (e: Exception) {
                // 记录错误但不抛出异常，返回null让UI显示占位符
                Log.w(TAG, "加载图片失败: $url", e)
                null
            }
        }
}

/**
 * Hilt网络依赖提供模块
 * 配置图片加载所需的网络组件
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {
    @Provides @Singleton
    fun provideOkHttpCache(@ApplicationContext ctx: Context): Cache =
        Cache(ctx.cacheDir.resolve("http_cache"), 10L * 1024 * 1024)

    @Provides @Singleton
    fun provideOkHttpClient(cache: Cache): OkHttpClient =
        OkHttpClient.Builder()
            .cache(cache)
            .build()

    @Provides @Singleton
    fun provideMemoryCache(): MemoryCache = LruMemoryCache()

    @Provides @Singleton
    fun provideTokenProvider(keyChainTokenProvider: KeyChainTokenProvider): TokenProvider = keyChainTokenProvider

    @Provides @Singleton
    fun provideImageLoaderService(
        client: OkHttpClient,
        memCache: MemoryCache,
        tokenProv: TokenProvider
    ): ImageLoaderService = HttpImageLoaderService(client, memCache, tokenProv)
}

/**
 * 图片加载状态
 * 封装加载过程中的不同状态
 */
sealed class LoadState {
    data object Loading : LoadState()
    data object Error   : LoadState()
    data class Success(val image: ImageBitmap) : LoadState()
}

/**
 * CompositionLocal图片加载器
 * 简化在Composable中获取ImageLoaderService的方式
 */
val LocalImageLoaderService = staticCompositionLocalOf<ImageLoaderService> {
    error("请在CompositionLocalProvider中提供ImageLoaderService")
}

/**
 * 多级缓存图片加载组件
 * 
 * 提供高性能的图片加载体验，支持内存缓存、磁盘缓存和网络加载
 * 自动处理加载状态和错误情况
 *
 * @param url 图片URL，支持相对路径和绝对路径
 * @param needToken 是否需要在请求头中附带访问令牌
 * @param modifier 外部传入的修饰符
 * @param content 加载成功后展示ImageBitmap的Composable插槽
 * @param placeholder 加载中或失败时展示的Composable插槽
 * @param imageLoader 图片加载服务，默认从CompositionLocal获取
 */
@Composable
fun NovelCachedImageView(
    url: String?,
    needToken: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    content: @Composable (ImageBitmap) -> Unit,
    placeholder: @Composable () -> Unit,
    imageLoader: ImageLoaderService = LocalImageLoaderService.current
) {
    val state by produceState<LoadState>(initialValue = LoadState.Loading, url, needToken) {
        val bmp = url?.let { imageLoader.loadImage(it, needToken) }
        value = if (bmp != null) LoadState.Success(bmp.asImageBitmap()) else LoadState.Error
    }

    Box(modifier) {
        when (state) {
            is LoadState.Success -> {
                Log.v("NovelCachedImageView", "渲染图片成功: $url")
                content((state as LoadState.Success).image)
            }
            LoadState.Loading -> {
                Log.v("NovelCachedImageView", "图片加载中: $url")
                placeholder()
            }
            LoadState.Error -> {
                Log.w("NovelCachedImageView", "图片加载失败，显示占位符: $url")
                placeholder()
            }
        }
    }
}