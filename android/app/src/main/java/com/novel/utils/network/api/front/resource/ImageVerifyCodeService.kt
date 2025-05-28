package com.novel.utils.network.api.front.resource

import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_RESOURCE
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.Exception

@Singleton
class ImageVerifyCodeService @Inject constructor() {

    // region 数据结构
    data class ImageVerifyCodeAPIResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: VerifyCodeData?,
        @SerializedName("ok") val ok: Boolean?
    )

    data class VerifyCodeData(
        @SerializedName("sessionId") val sessionId: String,
        @SerializedName("img") val imgBase64: String
    )
    // endregion

    // region 网络请求

    /**
     * 获取图片验证码（回调式）
     */
    private fun getImageVerifyCode(
        callback: (ImageVerifyCodeAPIResponse?, Throwable?) -> Unit
    ) {
        ApiService.get(
            baseUrl = BASE_URL_RESOURCE,
            endpoint = "img_verify_code",
            params = mapOf(),
            headers = mapOf("Accept" to "*/*")
        ) { response, error ->
            handleResponse(response, error, callback)
        }
    }

    /**
     * 获取图片验证码（挂起式）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getImageVerifyCodeBlocking(): ImageVerifyCodeAPIResponse {
        return suspendCancellableCoroutine { cont ->
            getImageVerifyCode { resp, err ->
                when {
                    err != null -> cont.resumeWith(Result.failure(err))
                    resp != null -> cont.resumeWith(Result.success(resp))
                    else -> cont.resumeWith(Result.failure(Exception("响应为空")))
                }
            }
        }
    }
    // endregion

    // region 响应处理
    private fun handleResponse(
        response: String?,
        error: Throwable?,
        callback: (ImageVerifyCodeAPIResponse?, Throwable?) -> Unit
    ) {
        when {
            error != null -> callback(null, error)
            response != null -> {
                try {
                    val parsed = Gson().fromJson(response, ImageVerifyCodeAPIResponse::class.java)
                    callback(parsed, null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }
            else -> callback(null, Exception("Response is null"))
        }
    }
    // endregion
}
