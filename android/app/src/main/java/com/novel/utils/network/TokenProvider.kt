package com.novel.utils.network

import com.novel.utils.TimberLogger
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import com.novel.utils.Store.NovelKeyChain.NovelKeyChainType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户令牌管理提供者
 * 
 * 负责访问令牌和刷新令牌的安全存储、读取和清理
 * 使用Mutex确保并发访问的线程安全性
 * 
 * @param keyChain 安全密钥链存储实例
 */
@Singleton
class TokenProvider @Inject constructor(
    private val keyChain: NovelKeyChain
) {
    
    val TAG = "TokenProvider"
    /** 访问令牌操作的互斥锁，确保线程安全 */
    private val mutex = Mutex()

    /**
     * 获取当前用户的访问令牌
     * 
     * @return 访问令牌字符串，如果不存在则返回空字符串
     */
    suspend fun accessToken(): String = mutex.withLock {
        val token = keyChain.read(NovelKeyChainType.TOKEN) ?: ""
        TimberLogger.d(TAG, "获取访问令牌: ${if (token.isNotEmpty()) "已存在" else "不存在"}")
        token
    }

    /**
     * 保存用户的访问令牌和刷新令牌
     * 
     * @param access 访问令牌
     * @param refresh 刷新令牌
     */
    suspend fun saveToken(access: String, refresh: String) = mutex.withLock {
        TimberLogger.d(TAG, "保存令牌 - 访问令牌长度: ${access.length}, 刷新令牌长度: ${refresh.length}")
        keyChain.saveToken(access, refresh)
        TimberLogger.d(TAG, "令牌保存完成")
    }

    /**
     * 清除所有存储的令牌
     * 
     * 用于用户登出或令牌失效时的清理操作
     */
    fun clear() {
        TimberLogger.d(TAG, "开始清除所有令牌")
        keyChain.delete(NovelKeyChainType.TOKEN)
        keyChain.delete(NovelKeyChainType.REFRESH_TOKEN)
        TimberLogger.d(TAG, "令牌清除完成")
    }
}
