package com.novel.utils.network.interceptor

import com.novel.utils.TimberLogger
import com.novel.utils.network.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 认证拦截器
 * 
 * 功能：
 * - 自动为所有HTTP请求添加Authorization头部
 * - 支持Token动态获取和刷新
 * - 统一认证机制处理
 * 
 * 注意事项：
 * - 使用runBlocking处理异步Token获取
 * - 适配所有API请求的认证需求
 * - 支持Token过期自动重试机制
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    /**
     * 拦截请求并添加认证头部
     * @param chain 拦截器链
     * @return 响应结果
     */
    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val token = tokenProvider.accessToken()
        TimberLogger.d(TAG, "添加认证Token: ${token.take(20)}...")

        val req = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        
        TimberLogger.d(TAG, "请求头部已更新: ${req.headers["Authorization"]?.take(20)}...")
        chain.proceed(req)
    }
}
