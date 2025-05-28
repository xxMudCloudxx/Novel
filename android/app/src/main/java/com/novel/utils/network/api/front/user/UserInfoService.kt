package com.novel.utils.network.api.front.user

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.novel.utils.dao.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_FRONT

class UserInfoService @Inject constructor(
    private val repo: UserRepository      // 通过 Hilt 注入
) {
    // region 数据结构
    data class UserInfoResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("msg") val msg: String,
        @SerializedName("data") val data: UserInfoData?
    )

    data class UserInfoData(
        @SerializedName("nickName") val nickName: String,
        @SerializedName("userPhoto") val userPhoto: String,
        @SerializedName("userSex") val userSex: Int,
    )
    // endregion

    // region 网络请求
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getUserInfoBlocking(): UserInfoResponse? = withContext(Dispatchers.IO) {
        // 1. 先拿到原始响应（或本地 mock）
        val parsed: UserInfoResponse? = suspendCancellableCoroutine { cont ->
            ApiService.get(
                baseUrl = BASE_URL_FRONT,
                params = mapOf(),
                endpoint = "user",
                headers = mapOf("Accept" to "application/json")
            ) { response, error ->
                // 把 handleResponse 的逻辑内联，直接 resume
                if (error != null) {
                    Log.w("UserInfoService", error)
                    cont.resume(null, onCancellation = null)
                } else if (response != null) {
                    try {
                        val obj = Gson().fromJson(response, UserInfoResponse::class.java)
                        cont.resume(obj, onCancellation = null)
                    } catch (e: Exception) {
                        Log.e("UserInfoService", "JSON 解析失败，使用本地数据", e)
                        cont.resume(null, onCancellation = null)
                    }
                } else {
                    Log.w("UserInfoService", "收到空响应，使用本地数据")
                    cont.resume(null, onCancellation = null)
                }
            }
        }

        Log.d("UserInfoService", "获取用户信息成功: ${parsed?.data}")

        // 2. 挂起地写缓存
        parsed?.data?.let { repo.cacheUser(it) }

        // 3. 返回结果
        parsed
    }
    // endregion

    // region 响应处理
    private fun handleResponse(
        response: String?,
        error: Throwable?,
        callback: (UserInfoResponse?) -> Unit
    ) {
        when {
            error != null -> {
                Log.w("UserInfoService", "网络请求失败，使用本地数据")
                callback(null)
            }

            response != null -> {
                try {
                    callback(Gson().fromJson(response, UserInfoResponse::class.java))
                } catch (e: Exception) {
                    Log.e("UserInfoService", "JSON 解析失败", e)
                    callback(null)
                }
            }

            else -> {
                Log.w("UserInfoService", "收到空响应，使用本地数据")
                callback(null)
            }
        }
    }
    // endregion
}