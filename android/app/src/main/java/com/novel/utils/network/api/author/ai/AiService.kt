package com.novel.utils.network.api.author.ai

import android.util.Log
import com.novel.utils.network.ApiService
import com.novel.utils.network.ApiService.BASE_URL_AI
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor() {
    
    // region 数据结构
    data class AiResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: String?, // AI处理后的文本
        @SerializedName("ok") val ok: Boolean?
    )

    data class PolishRequest(
        val text: String
    )

    data class ExpandRequest(
        val text: String,
        val ratio: Double
    )

    data class ContinueRequest(
        val text: String,
        val length: Int
    )

    data class CondenseRequest(
        val text: String,
        val ratio: Int
    )
    // endregion

    // region 网络请求方法
    
    /**
     * AI润色接口
     * @param text 需要润色的文本
     */
    private fun polishText(
        text: String,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        Log.d("AiService", "开始 polishText()，文本长度：${text.length}")
        
        val params = mapOf("text" to text)
        
        ApiService.post(
            baseUrl = BASE_URL_AI,
            endpoint = "polish",
            params = params,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            handleResponse(response, error, AiResponse::class.java, callback)
        }
    }

    /**
     * AI扩写接口
     * @param text 需要扩写的文本
     * @param ratio 扩写比例
     */
    private fun expandText(
        text: String,
        ratio: Double,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        Log.d("AiService", "开始 expandText()，文本长度：${text.length}，扩写比例：$ratio")
        
        val params = mapOf(
            "text" to text,
            "ratio" to ratio.toString()
        )
        
        ApiService.post(
            baseUrl = BASE_URL_AI,
            endpoint = "expand",
            params = params,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            handleResponse(response, error, AiResponse::class.java, callback)
        }
    }

    /**
     * AI续写接口
     * @param text 需要续写的文本
     * @param length 续写长度
     */
    private fun continueText(
        text: String,
        length: Int,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        Log.d("AiService", "开始 continueText()，文本长度：${text.length}，续写长度：$length")
        
        val params = mapOf(
            "text" to text,
            "length" to length.toString()
        )
        
        ApiService.post(
            baseUrl = BASE_URL_AI,
            endpoint = "continue",
            params = params,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            handleResponse(response, error, AiResponse::class.java, callback)
        }
    }

    /**
     * AI缩写接口
     * @param text 需要缩写的文本
     * @param ratio 缩写比例
     */
    private fun condenseText(
        text: String,
        ratio: Int,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        Log.d("AiService", "开始 condenseText()，文本长度：${text.length}，缩写比例：$ratio")
        
        val params = mapOf(
            "text" to text,
            "ratio" to ratio.toString()
        )
        
        ApiService.post(
            baseUrl = BASE_URL_AI,
            endpoint = "condense",
            params = params,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "*/*"
            )
        ) { response, error ->
            handleResponse(response, error, AiResponse::class.java, callback)
        }
    }

    // endregion

    // region 便捷方法
    
    /**
     * AI润色接口（使用请求对象）
     */
    fun polishText(
        request: PolishRequest,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        polishText(request.text, callback)
    }

    /**
     * AI扩写接口（使用请求对象）
     */
    fun expandText(
        request: ExpandRequest,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        expandText(request.text, request.ratio, callback)
    }

    /**
     * AI续写接口（使用请求对象）
     */
    fun continueText(
        request: ContinueRequest,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        continueText(request.text, request.length, callback)
    }

    /**
     * AI缩写接口（使用请求对象）
     */
    fun condenseText(
        request: CondenseRequest,
        callback: (AiResponse?, Throwable?) -> Unit
    ) {
        condenseText(request.text, request.ratio, callback)
    }

    // endregion

    // region 协程版本
    suspend fun polishTextBlocking(text: String): AiResponse {
        return suspendCancellableCoroutine { cont ->
            polishText(text) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    suspend fun polishTextBlocking(request: PolishRequest): AiResponse {
        return polishTextBlocking(request.text)
    }

    suspend fun expandTextBlocking(text: String, ratio: Double): AiResponse {
        return suspendCancellableCoroutine { cont ->
            expandText(text, ratio) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    suspend fun expandTextBlocking(request: ExpandRequest): AiResponse {
        return expandTextBlocking(request.text, request.ratio)
    }

    suspend fun continueTextBlocking(text: String, length: Int): AiResponse {
        return suspendCancellableCoroutine { cont ->
            continueText(text, length) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    suspend fun continueTextBlocking(request: ContinueRequest): AiResponse {
        return continueTextBlocking(request.text, request.length)
    }

    suspend fun condenseTextBlocking(text: String, ratio: Int): AiResponse {
        return suspendCancellableCoroutine { cont ->
            condenseText(text, ratio) { response, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(error))
                } else {
                    response?.let { cont.resumeWith(Result.success(it)) }
                        ?: cont.resumeWith(Result.failure(Exception("Response is null")))
                }
            }
        }
    }

    suspend fun condenseTextBlocking(request: CondenseRequest): AiResponse {
        return condenseTextBlocking(request.text, request.ratio)
    }

    // endregion

    // region 响应处理
    private fun <T> handleResponse(
        response: String?,
        error: Throwable?,
        clazz: Class<T>,
        callback: (T?, Throwable?) -> Unit
    ) {
        when {
            error != null -> {
                Log.e("AiService", "请求失败", error)
                callback(null, error)
            }
            response != null -> {
                try {
                    val result = Gson().fromJson(response, clazz)
                    Log.d("AiService", "请求成功，响应：$response")
                    callback(result, null)
                } catch (e: Exception) {
                    Log.e("AiService", "解析响应失败", e)
                    callback(null, e)
                }
            }
            else -> {
                Log.e("AiService", "响应为空")
                callback(null, Exception("Response is null"))
            }
        }
    }
    // endregion
} 