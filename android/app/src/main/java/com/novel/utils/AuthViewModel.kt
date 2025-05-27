package com.novel.utils

import androidx.lifecycle.ViewModel
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import com.novel.utils.Store.NovelKeyChain.NovelKeyChainType
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val keyChain: NovelKeyChain,
    private val userDefaults: NovelUserDefaults
) : ViewModel() {

    /**
     * true: 本地有 token 且还没过期
     * false: token 不存在或已过期，需要重新登录
     */
    val isTokenValid: Boolean
        get() {
            val token = keyChain.read(NovelKeyChainType.TOKEN)
            val expiresAt = userDefaults.get<Long>(NovelUserDefaultsKey.TOKEN_EXPIRES_AT) ?: 0L
            return token != null && System.currentTimeMillis() < expiresAt
        }
}
