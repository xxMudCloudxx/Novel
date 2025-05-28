package com.novel.utils.network

import android.util.Log
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import com.novel.utils.Store.NovelKeyChain.NovelKeyChainType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider @Inject constructor(
    private val keyChain: NovelKeyChain
) {
    private val mutex = Mutex()

    suspend fun accessToken(): String = mutex.withLock {
        keyChain.read(NovelKeyChainType.TOKEN) ?: ""
    }

    suspend fun saveToken(access: String, refresh: String) = mutex.withLock {
        Log.d("TokenProvider", "saveToken: $access, $refresh")
        keyChain.saveToken(access, refresh)
    }

    fun clear() {
        keyChain.delete(NovelKeyChainType.TOKEN)
        keyChain.delete(NovelKeyChainType.REFRESH_TOKEN)
    }
}
