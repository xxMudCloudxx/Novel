package com.novel.utils.network.cache

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存条目数据
 */
data class CacheEntry<T>(
    val key: String,
    val data: T,
    val cacheTime: Long,
    val expiryTime: Long
)

/**
 * 缓存配置
 */
data class CacheConfig(
    val maxAge: Long = TimeUnit.HOURS.toMillis(1), // 默认1小时过期
    val maxEntries: Int = 1000 // 最大缓存条目数
)

/**
 * 缓存策略
 */
enum class CacheStrategy {
    CACHE_FIRST,    // 先从缓存获取，然后异步更新
    NETWORK_FIRST,  // 先从网络获取，失败时从缓存获取
    CACHE_ONLY,     // 只从缓存获取
    NETWORK_ONLY,   // 只从网络获取
    SMART_FALLBACK  // 智能兜底：缓存优先，失败时网络重试，最后使用过期缓存
}

/**
 * 缓存结果
 */
sealed class CacheResult<T> {
    data class Success<T>(val data: T, val fromCache: Boolean) : CacheResult<T>()
    data class Error<T>(val error: Throwable, val cachedData: T? = null) : CacheResult<T>()
}

/**
 * 通用网络缓存管理器
 * 
 * 功能：
 * 1. 支持多种缓存策略
 * 2. 自动缓存过期管理
 * 3. 内存和磁盘双重缓存
 * 4. 泛型支持，可缓存任意类型数据
 */
@Singleton
class NetworkCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "NetworkCacheManager"
        private const val CACHE_DIR_NAME = "network_cache"
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
    
    // 内存缓存
    private val memoryCache = mutableMapOf<String, CacheEntry<*>>()
    
    // 缓存更新状态
    private val _cacheUpdateState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val cacheUpdateState: StateFlow<Map<String, Boolean>> = _cacheUpdateState.asStateFlow()
    
    init {
        cacheDir.mkdirs()
        // 清理过期缓存
        cleanExpiredCaches()
    }
    
    /**
     * 获取缓存数据（支持cache-first策略，增强兜底机制）
     * @param key 缓存键
     * @param config 缓存配置
     * @param networkCall 网络请求函数
     * @param strategy 缓存策略
     * @param onCacheUpdate 缓存更新回调
     */
    suspend fun <T> getCachedData(
        key: String,
        config: CacheConfig = CacheConfig(),
        networkCall: suspend () -> T,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
        onCacheUpdate: ((T) -> Unit)? = null
    ): CacheResult<T> = withContext(Dispatchers.IO) {
        
        when (strategy) {
            CacheStrategy.CACHE_FIRST -> {
                // 先从缓存获取
                val cachedData = getCachedDataInternal<T>(key)
                
                if (cachedData != null && isValidData(cachedData)) {
                    // 有有效缓存数据，异步更新网络数据
                    updateCacheAsync(key, config, networkCall, onCacheUpdate)
                    return@withContext CacheResult.Success(cachedData, true)
                } else {
                    // 缓存数据无效或不存在，同步获取网络数据
                    Log.d(TAG, "Cache data invalid or missing for key: $key, fetching from network")
                    try {
                        val networkData = networkCall()
                        // 检查网络数据是否为有效数据（兜底检查）
                        if (isValidData(networkData)) {
                            saveCacheData(key, networkData, config)
                            return@withContext CacheResult.Success(networkData, false)
                        } else {
                            Log.w(TAG, "Network data is invalid for key: $key, retrying...")
                            // 如果网络数据无效，稍作延迟后重试一次
                            kotlinx.coroutines.delay(1000)
                            val retryNetworkData = networkCall()
                            if (isValidData(retryNetworkData)) {
                                saveCacheData(key, retryNetworkData, config)
                                return@withContext CacheResult.Success(retryNetworkData, false)
                            } else {
                                // 如果网络重试仍失败，尝试返回过期的缓存数据作为兜底
                                if (cachedData != null) {
                                    Log.w(TAG, "Using expired cache data as fallback for key: $key")
                                    return@withContext CacheResult.Success(cachedData, true)
                                }
                                return@withContext CacheResult.Error(Exception("Network data is invalid after retry"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Network request failed for key: $key", e)
                        // 网络失败时，尝试返回缓存数据（即使可能过期）作为兜底
                        if (cachedData != null) {
                            Log.w(TAG, "Using cached data as fallback for key: $key")
                            return@withContext CacheResult.Error(e, cachedData)
                        }
                        return@withContext CacheResult.Error(e)
                    }
                }
            }
            
            CacheStrategy.NETWORK_FIRST -> {
                try {
                    val networkData = networkCall()
                    if (isValidData(networkData)) {
                        saveCacheData(key, networkData, config)
                        return@withContext CacheResult.Success(networkData, false)
                    } else {
                        Log.w(TAG, "Network data is invalid for key: $key, falling back to cache")
                        val cachedData = getCachedDataInternal<T>(key)
                        if (cachedData != null) {
                            return@withContext CacheResult.Success(cachedData, true)
                        } else {
                            return@withContext CacheResult.Error(Exception("Both network and cache data unavailable"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network request failed, trying cache for key: $key", e)
                    val cachedData = getCachedDataInternal<T>(key)
                    if (cachedData != null) {
                        return@withContext CacheResult.Success(cachedData, true)
                    } else {
                        return@withContext CacheResult.Error(e)
                    }
                }
            }
            
            CacheStrategy.CACHE_ONLY -> {
                val cachedData = getCachedDataInternal<T>(key)
                if (cachedData != null && isValidData(cachedData)) {
                    return@withContext CacheResult.Success(cachedData, true)
                } else {
                    return@withContext CacheResult.Error(Exception("No valid cached data found"))
                }
            }
            
            CacheStrategy.NETWORK_ONLY -> {
                try {
                    val networkData = networkCall()
                    if (isValidData(networkData)) {
                        return@withContext CacheResult.Success(networkData, false)
                    } else {
                        Log.w(TAG, "Network data is invalid for key: $key, retrying...")
                        kotlinx.coroutines.delay(1000)
                        val retryNetworkData = networkCall()
                        if (isValidData(retryNetworkData)) {
                            return@withContext CacheResult.Success(retryNetworkData, false)
                        } else {
                            return@withContext CacheResult.Error(Exception("Network data is invalid after retry"))
                        }
                    }
                } catch (e: Exception) {
                    return@withContext CacheResult.Error(e)
                }
            }
            
            CacheStrategy.SMART_FALLBACK -> {
                // 智能兜底策略：先尝试有效缓存，再尝试网络，最后使用任何可用缓存
                val cachedData = getCachedDataInternal<T>(key)
                
                // 如果有有效缓存，使用缓存并异步更新
                if (cachedData != null && isValidData(cachedData)) {
                    updateCacheAsync(key, config, networkCall, onCacheUpdate)
                    return@withContext CacheResult.Success(cachedData, true)
                }
                
                // 尝试网络请求
                try {
                    val networkData = networkCall()
                    if (isValidData(networkData)) {
                        saveCacheData(key, networkData, config)
                        return@withContext CacheResult.Success(networkData, false)
                    } else {
                        Log.w(TAG, "Network data invalid, retrying for key: $key")
                        kotlinx.coroutines.delay(1000)
                        val retryData = networkCall()
                        if (isValidData(retryData)) {
                            saveCacheData(key, retryData, config)
                            return@withContext CacheResult.Success(retryData, false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network failed in SMART_FALLBACK for key: $key", e)
                }
                
                // 最后兜底：尝试获取过期的缓存数据
                val expiredCachedData = getCachedDataInternal<T>(key, allowExpired = true)
                if (expiredCachedData != null) {
                    Log.w(TAG, "Using expired cache data as last resort for key: $key")
                    return@withContext CacheResult.Error(Exception("Using expired cache data as fallback"), expiredCachedData)
                }
                
                return@withContext CacheResult.Error(Exception("All fallback options exhausted"))
            }
        }
    }
    
    /**
     * 检查数据是否有效
     * 用于兜底检查，防止缓存或返回无效数据
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> isValidData(data: T): Boolean {
        return when (data) {
            null -> false
            is String -> data.isNotBlank()
            is Collection<*> -> data.isNotEmpty()
            is Array<*> -> data.isNotEmpty()
            is Map<*, *> -> data.isNotEmpty()
            // 对于自定义对象，检查关键字段
            else -> {
                try {
                    // 通过反射检查对象是否包含有效数据
                    val fields = data::class.java.declaredFields
                    fields.any { field ->
                        field.isAccessible = true
                        val value = field.get(data)
                        value != null && when (value) {
                            is String -> value.isNotBlank()
                            is Collection<*> -> value.isNotEmpty()
                            is Number -> value != 0
                            else -> true
                        }
                    }
                } catch (e: Exception) {
                    // 如果反射检查失败，假设数据有效
                    true
                }
            }
        }
    }
    
    /**
     * 异步更新缓存
     */
    private suspend fun <T> updateCacheAsync(
        key: String,
        config: CacheConfig,
        networkCall: suspend () -> T,
        onCacheUpdate: ((T) -> Unit)?
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 标记正在更新
                updateCacheState(key, true)
                
                val networkData = networkCall()
                saveCacheData(key, networkData, config)
                
                // 回调通知缓存已更新
                onCacheUpdate?.invoke(networkData)
                
                Log.d(TAG, "Cache updated for key: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update cache for key: $key", e)
            } finally {
                // 标记更新完成
                updateCacheState(key, false)
            }
        }
    }
    
    /**
     * 内部获取缓存数据（支持获取过期缓存）
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> getCachedDataInternal(
        key: String,
        allowExpired: Boolean = false
    ): T? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            val memoryCacheEntry = memoryCache[key] as? CacheEntry<T>
            if (memoryCacheEntry != null) {
                if (!isCacheExpired(memoryCacheEntry.expiryTime) || allowExpired) {
                    return@withContext memoryCacheEntry.data
                }
            }
            
            // 从磁盘缓存获取
            val diskCacheFile = File(cacheDir, "$key.json")
            if (diskCacheFile.exists()) {
                val cacheEntryJson = diskCacheFile.readText()
                val type = object : TypeToken<CacheEntry<T>>() {}.type
                val cacheEntry = gson.fromJson<CacheEntry<T>>(cacheEntryJson, type)
                
                if (!isCacheExpired(cacheEntry.expiryTime)) {
                    // 更新内存缓存
                    memoryCache[key] = cacheEntry
                    return@withContext cacheEntry.data
                } else if (allowExpired) {
                    // 允许过期缓存时，仍然返回数据但不更新内存缓存
                    Log.d(TAG, "Returning expired cache data for key: $key")
                    return@withContext cacheEntry.data
                } else {
                    // 过期则删除
                    diskCacheFile.delete()
                    memoryCache.remove(key)
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached data for key: $key", e)
            null
        }
    }
    
    /**
     * 保存缓存数据
     */
    internal suspend fun <T> saveCacheData(
        key: String,
        data: T,
        config: CacheConfig
    ) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val cacheEntry = CacheEntry(
                key = key,
                data = data,
                cacheTime = currentTime,
                expiryTime = currentTime + config.maxAge
            )
            
            // 保存到内存缓存
            memoryCache[key] = cacheEntry
            
            // 保存到磁盘缓存
            val diskCacheFile = File(cacheDir, "$key.json")
            val cacheEntryJson = gson.toJson(cacheEntry)
            diskCacheFile.writeText(cacheEntryJson)
            
            // 检查内存缓存大小，如果超过限制则清理最老的条目
            if (memoryCache.size > config.maxEntries) {
                val oldestKey = memoryCache.entries
                    .minByOrNull { it.value.cacheTime }?.key
                oldestKey?.let { memoryCache.remove(it) }
            }
            
            Log.d(TAG, "Cache saved for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache for key: $key", e)
        }
    }
    
    /**
     * 检查缓存是否过期
     */
    private fun isCacheExpired(expiryTime: Long): Boolean {
        return System.currentTimeMillis() > expiryTime
    }
    
    /**
     * 更新缓存状态
     */
    internal fun updateCacheState(key: String, isUpdating: Boolean) {
        val currentState = _cacheUpdateState.value.toMutableMap()
        if (isUpdating) {
            currentState[key] = true
        } else {
            currentState.remove(key)
        }
        _cacheUpdateState.value = currentState
    }
    
    /**
     * 清理过期缓存
     */
    private fun cleanExpiredCaches() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 清理磁盘缓存
            cacheDir.listFiles()?.forEach { file ->
                try {
                    val cacheEntryJson = file.readText()
                    val type = object : TypeToken<CacheEntry<Any?>>() {}.type
                    val cacheEntry = gson.fromJson<CacheEntry<Any?>>(cacheEntryJson, type)
                    
                    if (isCacheExpired(cacheEntry.expiryTime)) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // 解析失败的文件直接删除
                    file.delete()
                }
            }
            
            // 清理内存缓存
            val expiredKeys = memoryCache.entries
                .filter { isCacheExpired(it.value.expiryTime) }
                .map { it.key }
            
            expiredKeys.forEach { memoryCache.remove(it) }
            
            Log.d(TAG, "Expired cache cleaned")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean expired caches", e)
        }
    }
    
    /**
     * 清理指定key的缓存
     */
    suspend fun clearCache(key: String) = withContext(Dispatchers.IO) {
        try {
            memoryCache.remove(key)
            File(cacheDir, "$key.json").delete()
            Log.d(TAG, "Cache cleared for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache for key: $key", e)
        }
    }
    
    /**
     * 清理所有缓存
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            memoryCache.clear()
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "All cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all cache", e)
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    suspend fun isCacheExists(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查内存缓存
            val memoryCacheEntry = memoryCache[key]
            if (memoryCacheEntry != null && !isCacheExpired(memoryCacheEntry.expiryTime)) {
                return@withContext true
            }
            
            // 检查磁盘缓存
            val diskCacheFile = File(cacheDir, "$key.json")
            if (diskCacheFile.exists()) {
                val cacheEntryJson = diskCacheFile.readText()
                val type = object : TypeToken<CacheEntry<Any?>>() {}.type
                val cacheEntry = gson.fromJson<CacheEntry<Any?>>(cacheEntryJson, type)
                return@withContext !isCacheExpired(cacheEntry.expiryTime)
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cache existence for key: $key", e)
            false
        }
    }
} 