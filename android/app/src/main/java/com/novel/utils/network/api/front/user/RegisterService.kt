package com.novel.utils.network.api.front.user

import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_USER
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception

class RegisterService {
    // region 数据结构
    data class RegisterRequest(
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String,
        @SerializedName("sessionId") val sessionId: String,
        @SerializedName("velCode") val velCode: String
    )

    data class RegisterResponse(
        @SerializedName("code") val code: Int?,
        @SerializedName("msg") val msg: String?,
        @SerializedName("data") val data: RegisterData?
    )

    data class RegisterData(
        @SerializedName("uid") val uid: String,
        @SerializedName("token") val token: String,
    )
    // endregion

    // region 网络请求
    fun register(
        request: RegisterRequest,
        callback: (RegisterResponse?, Throwable?) -> Unit
    ) {
        val requestBody = mutableMapOf<String, String>().apply {
            put("username", request.username)
            put("password", request.password)
        }

        ApiService.post(
            baseUrl = BASE_URL_USER,
            endpoint = "/register",
            params = requestBody, // 自动序列化对象
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            handleResponse(response, error, callback)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun registerBlocking(request: RegisterRequest): RegisterResponse {
        return suspendCancellableCoroutine { cont ->
            register(request) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    if (response == null) {
                        cont.resumeWith(Result.failure(Exception("Unknown error")))
                    } else {
                        cont.resumeWith(Result.success(response))
                    }
                }
            }
        }
    }
    // endregion

    // region 响应处理
    private fun handleResponse(
        response: String?,
        error: Throwable?,
        callback: (RegisterResponse?, Throwable?) -> Unit
    ) {
        when {
            error != null -> {
                callback(null, error)
            }

            response != null -> {
                try {
                    callback(Gson().fromJson(response, RegisterResponse::class.java), null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }

            else -> {
                callback(null, Exception("Unknown error"))
            }
        }
    }
}