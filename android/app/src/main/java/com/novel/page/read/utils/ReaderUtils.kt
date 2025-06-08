package com.novel.page.read.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * 结果包装类
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}

/**
 * 安全调用包装函数
 */
suspend inline fun <T> safeCall(crossinline action: suspend () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * 带超时的安全调用
 */
suspend inline fun <T> safeCallWithTimeout(
    timeoutMs: Long = 30000L,
    crossinline action: suspend () -> T
): Result<T> {
    return try {
        val result = withTimeoutOrNull(timeoutMs) { action() }
        if (result != null) {
            Result.Success(result)
        } else {
            Result.Error(Exception("Operation timeout"))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * 尺寸扩展函数
 */
val Int.wdp: Dp
    @Composable get() = with(LocalDensity.current) { (this@wdp * density).dp }

val Int.ssp: TextUnit
    @Composable get() = (this * 1.0f).sp

val Float.wdp: Dp
    @Composable get() = with(LocalDensity.current) { (this@wdp * density).dp }

val Float.ssp: TextUnit
    @Composable get() = this.sp

/**
 * 防抖点击扩展
 */
@Composable
fun rememberDebounceClickable(
    intervalMs: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    return remember(onClick) {
        var lastClickTime = 0L
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= intervalMs) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}

/**
 * 字符串工具扩展
 */
fun String.cleanHtml(): String {
    return this
        .replace("<br/><br/>", "\n\n")
        .replace("<br/>", "\n")
        .replace(Regex("<[^>]*>"), "")
        .trim()
}

fun String.isValidId(): Boolean {
    return this.isNotBlank() && this.all { it.isDigit() }
}

/**
 * 集合扩展
 */
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in 0 until size) this[index] else null
}

fun <T> List<T>.safeSubList(fromIndex: Int, toIndex: Int): List<T> {
    val safeFrom = fromIndex.coerceAtLeast(0)
    val safeTo = toIndex.coerceAtMost(size)
    return if (safeFrom < safeTo) subList(safeFrom, safeTo) else emptyList()
}

/**
 * 性能监控扩展 - 增强版本，支持性能统计和优化建议
 */
inline fun <T> measureTimeWithResult(
    tag: String = "Performance",
    enableLogging: Boolean = ReadConst.Debug.ENABLE_PERFORMANCE_LOGS,
    block: () -> T
): Pair<T, Long> {
    val startTime = System.nanoTime()
    val result = block()
    val duration = (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
    
    if (enableLogging && duration > 100) { // 只记录超过100ms的操作
        android.util.Log.d(tag, "Operation took ${duration}ms")
        
        // 性能优化建议
        when {
            duration > 1000 -> android.util.Log.w(tag, "SLOW: Operation took ${duration}ms - consider optimization")
            duration > 500 -> android.util.Log.i(tag, "MODERATE: Operation took ${duration}ms - monitor for optimization")
        }
    }
    
    return result to duration
}

/**
 * 内存使用监控
 */
fun checkMemoryUsage(tag: String = "Memory"): Long {
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
    
    if (ReadConst.Debug.ENABLE_PERFORMANCE_LOGS) {
        android.util.Log.d(tag, "Memory usage: ${usedMemory}MB")
        
        if (usedMemory > ReadConst.Performance.MEMORY_WARNING_THRESHOLD_MB) {
            android.util.Log.w(tag, "HIGH MEMORY USAGE: ${usedMemory}MB - consider cleanup")
        }
    }
    
    return usedMemory
}

/**
 * 性能优化的防抖函数 - 带内存清理
 */
fun <T> optimizedDebounce(
    delayMs: Long,
    scope: kotlinx.coroutines.CoroutineScope,
    onMemoryWarning: (() -> Unit)? = null
): (suspend (T) -> Unit) -> (T) -> Unit {
    var job: kotlinx.coroutines.Job? = null
    
    return { action ->
        { value ->
            job?.cancel()
            job = scope.launch {
                kotlinx.coroutines.delay(delayMs)
                
                // 检查内存使用情况
                val memoryUsage = checkMemoryUsage()
                if (memoryUsage > ReadConst.Performance.MEMORY_WARNING_THRESHOLD_MB) {
                    onMemoryWarning?.invoke()
                }
                
                action(value)
            }
        }
    }
}

/**
 * 内存优化扩展
 */
fun <T> weakReference(value: T): java.lang.ref.WeakReference<T> {
    return java.lang.ref.WeakReference(value)
}

/**
 * Flow 扩展
 */
fun <T> kotlinx.coroutines.flow.Flow<T>.throttleLatest(
    periodMs: Long
): kotlinx.coroutines.flow.Flow<T> {
    return kotlinx.coroutines.flow.flow {
        var lastEmissionTime = 0L
        collect { value ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastEmissionTime >= periodMs) {
                lastEmissionTime = currentTime
                emit(value)
            }
        }
    }
}

/**
 * 协程扩展
 */
suspend fun <T> T.delay(ms: Long): T {
    kotlinx.coroutines.delay(ms)
    return this
}

/**
 * 防抖函数
 */
fun <T> debounce(
    delayMs: Long,
    scope: kotlinx.coroutines.CoroutineScope
): (T) -> Unit {
    var job: kotlinx.coroutines.Job? = null
    return { value ->
        job?.cancel()
        job = scope.launch {
            kotlinx.coroutines.delay(delayMs)
            // 这里需要一个回调来处理防抖后的值
        }
    }
}

/**
 * 错误处理工具
 */
object ErrorHandler {
    fun handleException(throwable: Throwable): Exception {
        return when (throwable) {
            is Exception -> throwable
            else -> Exception(throwable.message ?: "Unknown error", throwable)
        }
    }
    
    fun getUserMessage(exception: Exception): String {
        return when (exception) {
            is java.net.UnknownHostException -> "网络连接不可用，请检查网络设置"
            is java.net.SocketTimeoutException -> "网络连接超时，请重试"
            is java.io.IOException -> "网络请求失败，请稍后重试"
            is IllegalArgumentException -> "参数错误：${exception.message}"
            is IllegalStateException -> "状态异常：${exception.message}"
            else -> exception.message ?: "操作失败，请重试"
        }
    }
}

/**
 * 常量配置
 */
object ReadConst {
    
    // 缓存配置
    object Cache {
        const val MAX_CHAPTER_CACHE_SIZE = 12
        const val PRELOAD_CHAPTER_RANGE = 2
        const val CACHE_EXPIRY_HOURS = 24L
    }
    
    // 翻页配置
    object Flip {
        const val FLIP_COOLDOWN_MS = 200L
        const val FLIP_THRESHOLD_PERCENTAGE = 0.15f
        const val MAX_DRAG_OFFSET_PERCENTAGE = 0.8f
    }
    
    // 分页配置
    object Pagination {
        const val DEFAULT_FONT_SIZE = 16
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 24
        const val DEFAULT_LINE_HEIGHT = 1.5f
        const val DEFAULT_PAGE_MARGIN = 16
        const val PAGINATION_TIMEOUT_MS = 30000L
    }
    
    // 网络配置
    object Network {
        const val REQUEST_TIMEOUT_MS = 30000L
        const val CONNECT_TIMEOUT_MS = 15000L
        const val READ_TIMEOUT_MS = 30000L
        const val MAX_RETRY_COUNT = 3
        const val RETRY_DELAY_MS = 1000L
    }
    
    // 性能配置
    object Performance {
        const val SCROLL_DEBOUNCE_MS = 100L
        const val CLICK_DEBOUNCE_MS = 500L
        const val FRAME_DROP_THRESHOLD = 16 // 60fps
        const val MEMORY_WARNING_THRESHOLD_MB = 100
    }
    
    // UI配置
    object UI {
        const val ANIMATION_DURATION_MS = 300L
        const val SETTINGS_PANEL_HEIGHT_RATIO = 0.6f
        const val CHAPTER_LIST_HEIGHT_RATIO = 0.8f
        const val BRIGHTNESS_STEP_SIZE = 0.05f
    }
    
    // 调试配置
    object Debug {
        const val ENABLE_PERFORMANCE_LOGS = true
        const val ENABLE_CACHE_LOGS = false
        const val ENABLE_FLIP_LOGS = false
        const val LOG_TAG = "NovelReader"
    }
}

/**
 * 操作包装函数
 */
inline fun <T> safeCall(
    operation: () -> T,
    onError: (Exception) -> Unit = {}
) {
    try {
        operation()
    } catch (e: Exception) {
        onError(e)
    }
} 