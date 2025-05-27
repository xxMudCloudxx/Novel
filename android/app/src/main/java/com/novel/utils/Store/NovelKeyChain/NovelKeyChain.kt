package com.novel.utils.Store.NovelKeyChain

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

// 对应 iOS 的 NovelKeyChainType
enum class NovelKeyChainType(val key: String) {
    TOKEN("com.atcumt.kxq.token"),
    REFRESH_TOKEN("com.atcumt.kxq.refreshToken")
}

// 抽象 KeyChain 接口，方便 Mock 测试
interface NovelKeyChain {
    fun saveToken(accessToken: String?, refreshToken: String?)
    fun save(key: NovelKeyChainType, value: String)
    fun read(key: NovelKeyChainType): String?
    fun delete(key: NovelKeyChainType)
}

/**
 * 基于 EncryptedSharedPreferences 的加密存储实现
 */
@Singleton
class EncryptedNovelKeyChain @Inject constructor(
    @ApplicationContext private val context: Context
) : NovelKeyChain {

    // 显式指定类型，确保 lazy delegate 正确推断
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "Novel_keystore_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveToken(accessToken: String?, refreshToken: String?) {
        accessToken?.let { save(NovelKeyChainType.TOKEN, it) }
        refreshToken?.let { save(NovelKeyChainType.REFRESH_TOKEN, it) }
    }

    override fun save(key: NovelKeyChainType, value: String) {
        runCatching {
            prefs.edit()
                .putString(key.key, value)
                .apply()
        }.onFailure { it.printStackTrace() }
    }

    override fun read(key: NovelKeyChainType): String? =
        runCatching { prefs.getString(key.key, null) }
            .getOrNull()

    override fun delete(key: NovelKeyChainType) {
        runCatching {
            prefs.edit()
                .remove(key.key)
                .apply()
        }.onFailure { it.printStackTrace() }
    }
}

// 抽象 TokenProvider，方便注入与 Mock
interface TokenProvider {
    fun getToken(): String?
}

/**
 * 从 NovelKeyChain 中读取当前 Token
 */
@Singleton
class KeyChainTokenProvider @Inject constructor(
    private val NovelKeyChain: NovelKeyChain
) : TokenProvider {
    override fun getToken(): String? =
        NovelKeyChain.read(NovelKeyChainType.TOKEN)
}

/**
 * Hilt Module：绑定接口到实现
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NovelKeyChainModule {
    @Binds
    abstract fun bindNovelKeyChain(
        impl: EncryptedNovelKeyChain
    ): NovelKeyChain

    @Binds
    abstract fun bindTokenProvider(
        impl: KeyChainTokenProvider
    ): TokenProvider
}
