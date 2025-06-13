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
    NETWORK_ONLY    // 只从网络获取
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
     * 获取缓存数据（支持cache-first策略）
     * @param key 缓存键
     * @param config 缓存配置
     * @param networkCall 网络请求函数
     * @param strategy 缓存策略
     * @param onCacheUpdate 缓存更新回调
     */
    suspend inline fun <reified T> getCachedData(
        key: String,
        config: CacheConfig = CacheConfig(),
        noinline networkCall: suspend () -> T,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST,
        noinline onCacheUpdate: ((T) -> Unit)? = null
    ): CacheResult<T> = withContext(Dispatchers.IO) {
        
        when (strategy) {
            CacheStrategy.CACHE_FIRST -> {
                // 先从缓存获取
                val cachedData = getCachedDataInternal<T>(key, config)
                
                if (cachedData != null) {
                    // 有缓存数据，异步更新网络数据
                    updateCacheAsync(key, config, networkCall, onCacheUpdate)
                    return@withContext CacheResult.Success(cachedData, true)
                } else {
                    // 无缓存数据，同步获取网络数据
                    try {
                        val networkData = networkCall()
                        saveCacheData(key, networkData, config)
                        return@withContext CacheResult.Success(networkData, false)
                    } catch (e: Exception) {
                        Log.e(TAG, "Network request failed for key: $key", e)
                        return@withContext CacheResult.Error(e)
                    }
                }
            }
            
            CacheStrategy.NETWORK_FIRST -> {
                try {
                    val networkData = networkCall()
                    saveCacheData(key, networkData, config)
                    return@withContext CacheResult.Success(networkData, false)
                } catch (e: Exception) {
                    Log.e(TAG, "Network request failed, trying cache for key: $key", e)
                    val cachedData = getCachedDataInternal<T>(key, config)
                    if (cachedData != null) {
                        return@withContext CacheResult.Success(cachedData, true)
                    } else {
                        return@withContext CacheResult.Error(e)
                    }
                }
            }
            
            CacheStrategy.CACHE_ONLY -> {
                val cachedData = getCachedDataInternal<T>(key, config)
                if (cachedData != null) {
                    return@withContext CacheResult.Success(cachedData, true)
                } else {
                    return@withContext CacheResult.Error(Exception("No cached data found"))
                }
            }
            
            CacheStrategy.NETWORK_ONLY -> {
                try {
                    val networkData = networkCall()
                    return@withContext CacheResult.Success(networkData, false)
                } catch (e: Exception) {
                    return@withContext CacheResult.Error(e)
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
     * 内部获取缓存数据
     */
    private suspend inline fun <reified T> getCachedDataInternal(
        key: String,
        config: CacheConfig
    ): T? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            val memoryCacheEntry = memoryCache[key] as? CacheEntry<T>
            if (memoryCacheEntry != null && !isCacheExpired(memoryCacheEntry.expiryTime)) {
                return@withContext memoryCacheEntry.data
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
    private suspend fun <T> saveCacheData(
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
                    .minByOrNull { (it.value as CacheEntry<*>).cacheTime }?.key
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
    private fun updateCacheState(key: String, isUpdating: Boolean) {
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
                .filter { isCacheExpired((it.value as CacheEntry<*>).expiryTime) }
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
            val memoryCacheEntry = memoryCache[key] as? CacheEntry<*>
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