package com.novel.utils.Store.NovelKeyChain

import android.content.Context
import android.content.SharedPreferences
import com.novel.utils.TimberLogger
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * 密钥链存储类型枚举
 * 
 * 对应iOS的NovelKeyChainType，定义安全存储的键值类型
 */
enum class NovelKeyChainType(val key: String) {
    /** 访问令牌 */
    TOKEN("com.atcumt.kxq.token"),
    /** 刷新令牌 */
    REFRESH_TOKEN("com.atcumt.kxq.refreshToken")
}

/**
 * 密钥链存储接口
 * 
 * 提供安全的键值对存储功能，用于敏感信息如Token的保存
 * 抽象接口设计便于单元测试和Mock
 */
interface NovelKeyChain {
    /**
     * 保存Token对（访问令牌和刷新令牌）
     * @param accessToken 访问令牌（可为空）
     * @param refreshToken 刷新令牌（可为空）
     */
    fun saveToken(accessToken: String?, refreshToken: String?)
    
    /**
     * 保存单个键值对
     * @param key 存储键
     * @param value 存储值
     */
    fun save(key: NovelKeyChainType, value: String)
    
    /**
     * 读取指定键的值
     * @param key 存储键
     * @return 存储值，不存在时返回null
     */
    fun read(key: NovelKeyChainType): String?
    
    /**
     * 删除指定键的值
     * @param key 存储键
     */
    fun delete(key: NovelKeyChainType)
}

/**
 * 基于EncryptedSharedPreferences的加密存储实现
 * 
 * 核心特性：
 * - 使用Android安全框架进行数据加密
 * - AES256_GCM加密算法确保数据安全
 * - 自动处理加密密钥管理
 * - 异常安全处理，避免应用崩溃
 */
@Singleton
class EncryptedNovelKeyChain @Inject constructor(
    @ApplicationContext private val context: Context
) : NovelKeyChain {

    companion object {
        private const val TAG = "EncryptedNovelKeyChain"
        private const val PREFS_NAME = "Novel_keystore_prefs"
    }

    /** 延迟初始化的加密SharedPreferences实例 */
    private val prefs: SharedPreferences by lazy {
        try {
            TimberLogger.d(TAG, "初始化加密存储")
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                TimberLogger.d(TAG, "加密存储初始化成功")
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加密存储初始化失败", e)
            throw e
        }
    }

    /**
     * 保存Token对
     * 
     * 便捷方法，同时保存访问令牌和刷新令牌
     */
    override fun saveToken(accessToken: String?, refreshToken: String?) {
        TimberLogger.d(TAG, "保存Token对: accessToken=${accessToken != null}, refreshToken=${refreshToken != null}")
        accessToken?.let { save(NovelKeyChainType.TOKEN, it) }
        refreshToken?.let { save(NovelKeyChainType.REFRESH_TOKEN, it) }
    }

    /**
     * 保存单个键值对
     * 
     * 使用加密存储确保数据安全性
     */
    override fun save(key: NovelKeyChainType, value: String) {
        runCatching {
            TimberLogger.d(TAG, "保存数据: key=${key.key}")
            prefs.edit {
                putString(key.key, value)
            }
            TimberLogger.d(TAG, "数据保存成功: key=${key.key}")
        }.onFailure { 
            TimberLogger.e(TAG, "数据保存失败: key=${key.key}", it)
            it.printStackTrace() 
        }
    }

    /**
     * 读取指定键的值
     * 
     * @return 存储值，异常或不存在时返回null
     */
    override fun read(key: NovelKeyChainType): String? =
        runCatching { 
            val value = prefs.getString(key.key, null)
            TimberLogger.d(TAG, "读取数据: key=${key.key}, found=${value != null}")
            value
        }.getOrElse { 
            TimberLogger.e(TAG, "读取数据失败: key=${key.key}", it)
            null 
        }

    /**
     * 删除指定键的值
     * 
     * 安全删除，异常时记录日志但不影响应用运行
     */
    override fun delete(key: NovelKeyChainType) {
        runCatching {
            TimberLogger.d(TAG, "删除数据: key=${key.key}")
            prefs.edit {
                remove(key.key)
            }
            TimberLogger.d(TAG, "数据删除成功: key=${key.key}")
        }.onFailure { 
            TimberLogger.e(TAG, "数据删除失败: key=${key.key}", it)
            it.printStackTrace() 
        }
    }
}

/**
 * Token提供器接口
 * 
 * 抽象Token获取逻辑，便于依赖注入和单元测试
 */
interface TokenProvider {
    /**
     * 获取当前有效的访问令牌
     * @return 访问令牌，不存在时返回null
     */
    fun getToken(): String?
}

/**
 * 基于NovelKeyChain的Token提供器实现
 * 
 * 从安全存储中读取Token，为网络请求提供认证信息
 */
@Singleton
class KeyChainTokenProvider @Inject constructor(
    private val NovelKeyChain: NovelKeyChain
) : TokenProvider {
    
    companion object {
        private const val TAG = "KeyChainTokenProvider"
    }
    
    /**
     * 从密钥链中获取当前Token
     */
    override fun getToken(): String? {
        val token = NovelKeyChain.read(NovelKeyChainType.TOKEN)
        TimberLogger.d(TAG, "获取Token: found=${token != null}")
        return token
    }
}

/**
 * Hilt依赖注入模块
 * 
 * 绑定接口到具体实现，支持依赖注入和测试替换
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NovelKeyChainModule {
    
    /**
     * 绑定NovelKeyChain接口到加密实现
     */
    @Binds
    abstract fun bindNovelKeyChain(
        impl: EncryptedNovelKeyChain
    ): NovelKeyChain

    /**
     * 绑定TokenProvider接口到KeyChain实现
     */
    @Binds
    abstract fun bindTokenProvider(
        impl: KeyChainTokenProvider
    ): TokenProvider
}
