package com.novel.utils.network.interceptor

import android.util.Log
import com.novel.utils.network.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val token = tokenProvider.accessToken()
        Log.d("AuthInterceptor", "token: $token")

        val req = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        Log.d("AuthInterceptor", "Final headers: ${req.headers}")
        chain.proceed(req)
    }
}
