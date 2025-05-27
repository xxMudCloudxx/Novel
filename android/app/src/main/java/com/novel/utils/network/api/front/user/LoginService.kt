package com.novel.utils.network.api.front.user

import android.util.Log
import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_USER
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginService @Inject constructor() {
    // region 数据结构
    // 登录请求数据类
    data class LoginRequest(
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String
    )

    // 登录响应数据结构
    data class LoginAPIResponse(
        @SerializedName("code") val code: Int?,
        @SerializedName("msg") val msg: String?,
        @SerializedName("data") val data: LoginResponseData?
    )

    data class LoginResponseData(
        @SerializedName("uid") val uid: Int,
        @SerializedName("nickName") val nickName: String,
        @SerializedName("token") val token: String,
    )
    // endregion

    // region 网络请求
    fun login(
        request: LoginRequest,
        callback: (LoginAPIResponse?, Throwable?) -> Unit
    ) {
        // 构建 JSON 请求体
        val requestBody = mapOf(
            "username" to request.username,
            "password" to request.password
        )
        Log.d("LoginService", "开始 login()，参数：$request")

        ApiService.post(
            baseUrl = BASE_URL_USER,
            endpoint = "login",
            params = requestBody, // 直接传递数据对象
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            Log.d("LoginService", "post 回调，response=$response, error=$error")  // <<< 新增
            handleResponse(response, error, callback)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun loginBlocking(request: LoginRequest): LoginAPIResponse {
        return suspendCancellableCoroutine { cont ->
            login(request) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }
    // endregion

    // region 响应处理
    private fun handleResponse(
        response: String?,
        error: Throwable?,
        callback: (LoginAPIResponse?, Throwable?) -> Unit
    ) {
        when {
            error != null -> {
                callback(null, error)
            }

            response != null -> {
                try {
                    callback(Gson().fromJson(response, LoginAPIResponse::class.java), null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }

            else -> {
                callback(null, Exception("Response is null"))
            }
        }
    }
}