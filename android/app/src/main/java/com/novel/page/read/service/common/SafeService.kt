package com.novel.page.read.service.common

import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * 安全服务抽象基类
 * 
 * 提供统一的异步调度和错误处理机制：
 * - 统一协程调度策略
 * - 标准化错误处理流程
 * - 性能监控和日志记录
 * - 可测试的设计
 */
abstract class SafeService(
    protected val dispatchers: DispatcherProvider,
    protected val logger: ServiceLogger
) {
    
    /**
     * 安全的IO操作执行器
     * 
     * 自动处理协程调度和异常捕获
     * @param operation 要执行的IO操作
     * @return 操作结果，失败时返回null
     */
    protected suspend fun <T> safeIo(
        operation: suspend () -> T
    ): T? = withContext(dispatchers.io) {
        runCatching {
            operation()
        }.onFailure { error ->
            logger.logError("IO operation failed", error)
        }.getOrNull()
    }
    
    /**
     * 安全的IO操作执行器（带默认值）
     * 
     * @param defaultValue 操作失败时的默认返回值
     * @param operation 要执行的IO操作
     * @return 操作结果或默认值
     */
    protected suspend fun <T> safeIoWithDefault(
        defaultValue: T,
        operation: suspend () -> T
    ): T = withContext(dispatchers.io) {
        runCatching {
            operation()
        }.onFailure { error ->
            logger.logError("IO operation failed, using default value", error)
        }.getOrDefault(defaultValue)
    }
    
    /**
     * 安全的计算操作执行器
     * 
     * 用于CPU密集型任务
     * @param operation 要执行的计算操作
     * @return 操作结果，失败时返回null
     */
    protected suspend fun <T> safeComputation(
        operation: suspend () -> T
    ): T? = withContext(dispatchers.default) {
        runCatching {
            operation()
        }.onFailure { error ->
            logger.logError("Computation operation failed", error)
        }.getOrNull()
    }
    
    /**
     * 带重试的安全操作执行器
     * 
     * @param maxRetries 最大重试次数
     * @param retryDelay 重试间隔（毫秒）
     * @param operation 要执行的操作
     * @return 操作结果，失败时返回null
     */
    protected suspend fun <T> safeIoWithRetry(
        maxRetries: Int = ReaderServiceConfig.MAX_RETRY_COUNT,
        retryDelay: Long = ReaderServiceConfig.RETRY_DELAY_MS,
        operation: suspend () -> T
    ): T? = withContext(dispatchers.io) {
        var lastError: Throwable? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return@withContext operation()
            } catch (e: Exception) {
                lastError = e
                logger.logWarning("Operation failed on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(retryDelay)
                }
            }
        }
        
        logger.logError("Operation failed after ${maxRetries + 1} attempts", lastError!!)
        null
    }
    
    /**
     * 安全的IO操作执行器（带超时保护）
     * 
     * 自动处理协程调度和异常捕获，防止ANR
     * @param operation 要执行的IO操作
     * @param timeoutMs 超时时间（毫秒）
     * @return 操作结果，失败时返回null
     */
    protected suspend fun <T> safeIoWithTimeout(
        operation: suspend () -> T,
        timeoutMs: Long = ReaderServiceConfig.CACHE_OPERATION_TIMEOUT_MS
    ): T? = withContext(dispatchers.io) {
        runCatching {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                operation()
            }
        }.onFailure { error ->
            when (error) {
                is kotlinx.coroutines.TimeoutCancellationException -> {
                    logger.logWarning("IO operation timeout after ${timeoutMs}ms")
                }
                else -> {
                    logger.logError("IO operation failed", error)
                }
            }
        }.getOrNull()
    }
    
    /**
     * 性能监控包装器（suspend版本，优化版）
     * 
     * 记录操作耗时，仅在必要时记录以避免性能开销
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    protected suspend fun <T> withPerformanceMonitoring(
        operationName: String,
        operation: suspend () -> T
    ): T {
        if (!ReaderServiceConfig.ENABLE_PERFORMANCE_LOGGING) {
            return operation()
        }
        
        val startTime = System.currentTimeMillis()
        val result = operation()
        val duration = System.currentTimeMillis() - startTime
        
        // 只记录耗时较长的操作，避免日志泛滥
        if (duration > 100) {
            logger.logPerformance(operationName, duration)
        }
        return result
    }
    
    /**
     * 性能监控包装器（同步版本，优化版）
     * 
     * 记录操作耗时，仅在必要时记录以避免性能开销
     * @param operationName 操作名称
     * @param operation 要执行的操作
     * @return 操作结果
     */
    protected fun <T> withSyncPerformanceMonitoring(
        operationName: String,
        operation: () -> T
    ): T {
        if (!ReaderServiceConfig.ENABLE_PERFORMANCE_LOGGING) {
            return operation()
        }
        
        val startTime = System.currentTimeMillis()
        val result = operation()
        val duration = System.currentTimeMillis() - startTime
        
        // 只记录耗时较长的操作，避免日志泛滥
        if (duration > 50) {
            logger.logPerformance(operationName, duration)
        }
        return result
    }
    
    /**
     * 获取服务标签，用于日志记录
     */
    protected abstract fun getServiceTag(): String
}
/**
 * 服务日志记录器接口
 * 
 * 抽象日志记录，便于测试和切换日志框架
 */
interface ServiceLogger {
    fun logDebug(message: String, tag: String? = null)
    fun logInfo(message: String, tag: String? = null)
    fun logWarning(message: String, tag: String? = null)
    fun logError(message: String, error: Throwable? = null, tag: String? = null)
    fun logPerformance(operationName: String, durationMs: Long, tag: String? = null)
}

/**
 * 默认Android日志记录器实现（使用Timber）
 */
class AndroidServiceLogger : ServiceLogger {
    
    override fun logDebug(message: String, tag: String?) {
        if (ReaderServiceConfig.ENABLE_VERBOSE_LOGGING) {
            if (tag != null) {
                Timber.tag(tag).d(message)
            } else {
                Timber.d(message)
            }
        }
    }
    
    override fun logInfo(message: String, tag: String?) {
        if (tag != null) {
            Timber.tag(tag).i(message)
        } else {
            Timber.i(message)
        }
    }
    
    override fun logWarning(message: String, tag: String?) {
        if (tag != null) {
            Timber.tag(tag).w(message)
        } else {
            Timber.w(message)
        }
    }
    
    override fun logError(message: String, error: Throwable?, tag: String?) {
        if (tag != null) {
            if (error != null) {
                Timber.tag(tag).e(error, message)
            } else {
                Timber.tag(tag).e(message)
            }
        } else {
            if (error != null) {
                Timber.e(error, message)
            } else {
                Timber.e(message)
            }
        }
    }
    
    override fun logPerformance(operationName: String, durationMs: Long, tag: String?) {
        if (ReaderServiceConfig.ENABLE_PERFORMANCE_LOGGING) {
            val performanceMessage = "Performance: $operationName took ${durationMs}ms"
            if (tag != null) {
                Timber.tag(tag).d(performanceMessage)
            } else {
                Timber.d(performanceMessage)
            }
        }
    }
}
