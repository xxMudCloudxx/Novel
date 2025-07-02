package com.novel.utils

import com.novel.utils.TimberLogger
import androidx.lifecycle.ViewModel
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import com.novel.utils.Store.NovelKeyChain.NovelKeyChainType
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 认证状态管理ViewModel
 * 
 * 核心功能：
 * - 管理用户Token的有效性验证
 * - 提供统一的认证状态查询接口
 * - 集成安全存储和配置管理
 * 
 * 设计特点：
 * - 基于Hilt依赖注入
 * - 结合KeyChain安全存储和UserDefaults配置
 * - 实时Token过期检查
 * - 为全局认证逻辑提供统一入口
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    /** 安全密钥链存储，用于Token等敏感信息 */
    private val keyChain: NovelKeyChain,
    /** 用户配置存储，用于Token过期时间等配置 */
    private val userDefaults: NovelUserDefaults
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    /**
     * 检查Token是否有效
     * 
     * 验证逻辑：
     * 1. 检查Token是否存在
     * 2. 检查Token是否已过期
     * 
     * @return true: 本地有Token且未过期，用户已登录
     *         false: Token不存在或已过期，需要重新登录
     */
    val isTokenValid: Boolean
        get() {
            return try {
                val token = keyChain.read(NovelKeyChainType.TOKEN)
                val expiresAt = userDefaults.get<Long>(NovelUserDefaultsKey.TOKEN_EXPIRES_AT) ?: 0L
                val currentTime = System.currentTimeMillis()
                
                val isValid = token != null && currentTime < expiresAt
                
                TimberLogger.d(TAG, "Token验证结果: $isValid (Token存在: ${token != null}, 过期时间: $expiresAt, 当前时间: $currentTime)")
                
                isValid
            } catch (e: Exception) {
                TimberLogger.e(TAG, "Token验证失败", e)
                false
            }
        }
}
