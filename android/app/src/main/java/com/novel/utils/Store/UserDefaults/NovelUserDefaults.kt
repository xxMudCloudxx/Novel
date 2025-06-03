package com.novel.utils.Store.UserDefaults

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * 对应 Swift 里的 NovelUserDefaults.Key
 */
enum class NovelUserDefaultsKey(val key: String) {
    IS_LOGGED_IN("isLoggedIn"),
    NEWS_TYPE("newsType"),
    TOKEN_EXPIRES_AT("tokenExpiresAt"),
    USER_ID("uid"),
    PAGE_FLIP_EFFECT("pageFlipEffect"),
}

/**
 * 抽象接口，方便 Mock & 替换为 DataStore 实现
 */
interface NovelUserDefaults {
    // 原有的枚举 key
    fun <T> set(value: T, forKey: NovelUserDefaultsKey)
    fun <T> get(key: NovelUserDefaultsKey): T?
    fun remove(key: NovelUserDefaultsKey)
    fun contains(key: NovelUserDefaultsKey): Boolean
    fun clearAll()

    // 新增：字符串 key
    fun setString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}

/**
 * 基于 SharedPreferences 的实现
 */
@Singleton
class SharedPrefsUserDefaults @Inject constructor(
    private val prefs: SharedPreferences
) : NovelUserDefaults {

    override fun <T> set(value: T, forKey: NovelUserDefaultsKey) {
        prefs.edit {
            when (value) {
                is String -> putString(forKey.key, value)
                is Int -> putInt(forKey.key, value)
                is Boolean -> putBoolean(forKey.key, value)
                is Float -> putFloat(forKey.key, value)
                is Long -> putLong(forKey.key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST")
                putStringSet(forKey.key, value as Set<String>)

                else -> throw IllegalArgumentException("不支持的类型：${value}")
            }
        } // 异步提交
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: NovelUserDefaultsKey): T? {
        // 直接从 all 里拿，类型转换
        return prefs.all[key.key] as? T
    }

    override fun remove(key: NovelUserDefaultsKey) {
        prefs.edit { remove(key.key) }
    }

    override fun contains(key: NovelUserDefaultsKey): Boolean =
        prefs.contains(key.key)

    override fun clearAll() {
        prefs.edit {
            NovelUserDefaultsKey.entries.forEach { remove(it.key) }
        }
    }

    override fun setString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun getString(key: String): String? =
        prefs.getString(key, null)

    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }
}