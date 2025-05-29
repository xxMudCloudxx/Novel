package com.novel.utils.network

import android.util.Log
import com.novel.utils.network.interceptor.AuthInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException

// Retrofit 服务接口，用于定义网络请求的接口方法
interface ApiServiceInterface {
    @GET
    fun get(
        @Url endpoint: String,  // 接口路径
        @QueryMap params: Map<String, String>? = null,  // 可选查询参数
        @HeaderMap headers: Map<String, String> = mapOf()  // 请求头部信息
    ): Call<ResponseBody>

    @POST
    fun post(
        @Url endpoint: String,
        @Body body: RequestBody,  // 请求体（JSON 格式）
        @HeaderMap headers: Map<String, String> = mapOf()  // 请求头部信息
    ): Call<ResponseBody>

    @DELETE
    fun delete(
        @Url endpoint: String,  // 接口路径
        @HeaderMap headers: Map<String, String> = mapOf()  // 请求头部信息
    ): Call<ResponseBody>

    @FormUrlEncoded
    @PATCH
    fun patch(
        @Url endpoint: String,
        @FieldMap params: Map<String, String>? = null,  // 表单参数
        @HeaderMap headers: Map<String, String> = mapOf()  // 请求头部信息
    ): Call<ResponseBody>

    @PUT
    fun put(
        @Url endpoint: String,
        @Body body: RequestBody,  // 请求体（JSON 格式）
        @HeaderMap headers: Map<String, String> = mapOf()  // 请求头部信息
    ): Call<ResponseBody>
}

// Retrofit 管理类，用于创建和缓存 Retrofit 实例
object RetrofitClient {

    private val retrofitInstances = mutableMapOf<String, Retrofit>()

    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var tokenProvider: TokenProvider

    // 初始化方法，在 Application 中调用
    fun init(
        authInterceptor: AuthInterceptor,
        tokenProvider: TokenProvider
    ) {
        this.authInterceptor = authInterceptor
        this.tokenProvider = tokenProvider
    }

    fun getRetrofit(baseUrl: String): Retrofit =
        retrofitInstances.getOrPut(baseUrl) {
            // --- 1. 组装 OkHttpClient ---
            val builder = OkHttpClient.Builder()
                .addInterceptor(authInterceptor) //自动加token

            // --- 2. 生成 Retrofit ---
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

    fun getService(baseUrl: String): ApiServiceInterface =
        getRetrofit(baseUrl).create(ApiServiceInterface::class.java)
}


// 网络请求服务类，用于处理不同 HTTP 方法的请求
object ApiService {
    // 各个接口的基础 URL
    const val BASE_URL_FRONT = "http://47.110.147.60:8080/api/front/"
    const val BASE_URL_USER = "http://47.110.147.60:8080/api/front/user/"
    const val BASE_URL_RESOURCE = "http://47.110.147.60:8080/api/front/resource/"
    const val BASE_URL_AUTHOR = "http://47.110.147.60:8080/api/author/"
    const val BASE_URL_AI = "http://47.110.147.60:8080/api/author/ai/"
    private const val TAG = "ApiServiceS"

    // 获取指定 baseUrl 的服务实例
    private fun getService(baseUrl: String) =
        RetrofitClient.getService(baseUrl)

    // GET 请求方法
    fun get(
        baseUrl: String,
        endpoint: String,
        params: Map<String, String>? = mapOf(),
        headers: Map<String, String> = mapOf(),
        callback: (String?, Throwable?) -> Unit
    ) {
        Log.d("NetworkLog", "Sending GET to: ${baseUrl + endpoint}")
        getService(baseUrl).get(endpoint, params, headers)
            .enqueue(createCallback(callback))
    }

    // POST 请求方法
    fun post(
        baseUrl: String,
        endpoint: String,
        params: Map<String, String> = mapOf(),
        headers: Map<String, String> = mapOf(),
        callback: (String?, Throwable?) -> Unit
    ) {
        Log.d("NetworkLog", "Sending POST to: ${baseUrl + endpoint}")
        getService(baseUrl).post(endpoint, createJsonBody(params), headers)
            .enqueue(createCallback(callback))
    }

    // DELETE 请求方法
    fun delete(
        baseUrl: String,
        endpoint: String,
        headers: Map<String, String> = mapOf(),
        callback: (String?, Throwable?) -> Unit
    ) {
        getService(baseUrl).delete(endpoint, headers).enqueue(createCallback(callback))
    }

    // PATCH 请求方法
    fun patch(
        baseUrl: String,
        endpoint: String,
        params: Map<String, String>? = mapOf(),
        headers: Map<String, String> = mapOf(),
        callback: (String?, Throwable?) -> Unit
    ) {
        getService(baseUrl).patch(endpoint, params, headers)
            .enqueue(createCallback(callback))
    }

    // PUT 请求方法
    fun put(
        baseUrl: String,
        endpoint: String,
        body: RequestBody,
        headers: Map<String, String> = mapOf(),
        callback: (String?, Throwable?) -> Unit
    ) {
        getService(baseUrl).put(endpoint, body, headers).enqueue(createCallback(callback))
    }

    // 创建回调函数，处理请求的响应和失败情况
    private fun createCallback(callback: (String?, Throwable?) -> Unit) =
        object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                Log.d("NetworkLog", "Received response from: ${call.request()}")
                Log.d("NetworkLog", "Response body: $response")
                try {
                    if (response.isSuccessful) {
                        val body = response.body()?.string()
                        Log.d("NetworkLog", "Response body: $body")
                        callback(body, null)  // 请求成功，返回响应体
                    } else {
                        Log.e("NetworkLog", "HTTP error: ${response.code()} / ${response.errorBody()?.string()}")
                        callback(
                            null,
                            IOException("HTTP error: ${response.code()}")
                        )  // 请求失败，返回错误信息
                    }
                } catch (e: Exception) {
                    Log.e("NetworkLog", "Response parse exception", e)
                    callback(null, e)  // 处理异常情况
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("NetworkLog", "onFailure: 请求失败", t)  // <<< 增加
                callback(null, t)  // 请求失败，返回异常
            }
        }

    // 创建 JSON 格式的请求体
    private fun createJsonRequestBody(json: String): RequestBody {
        return json.toRequestBody("application/json".toMediaTypeOrNull())  // 将 JSON 字符串转为 RequestBody
    }

    // 直接使用 JSON 格式的请求体，而不是使用 FormBody
    private fun createJsonBody(params: Map<String, String>): RequestBody {
        val json = params.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        val jsonBody = "{ $json }"
        return createJsonRequestBody(jsonBody)  // 创建 JSON 请求体
    }
}
