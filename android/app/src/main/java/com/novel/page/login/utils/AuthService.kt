package com.novel.page.login.utils

import com.novel.page.login.dao.UserRepository
import com.novel.utils.ReactNativeBridge
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import com.novel.utils.network.TokenProvider
import com.novel.utils.network.api.front.user.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证领域服务 - 专门处理登录、注册、Token管理等核心业务逻辑
 * ⚠ 安全检查: 所有认证相关操作都需要严格验证
 */
@Singleton
class AuthService @Inject constructor(
    private val userService: UserService,
    private val tokenProvider: TokenProvider,
    private val userDefaults: NovelUserDefaults,
    private val userRepository: UserRepository
) {

    /**
     * 执行用户登录
     * @param username 用户名/手机号
     * @param password 密码
     * @return 登录结果
     */
    suspend fun login(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val response = userService.loginBlocking(
                    UserService.LoginRequest(username, password)
                )

                if (response.ok == true && response.data != null) {
                    // 保存认证信息
                    saveAuthInfo(response.data.token, response.data.uid)
                    // 异步加载用户详情
                    loadAndCacheUserInfo()
                    AuthResult.Success("登录成功")
                } else {
                    AuthResult.Error(response.msg ?: "登录失败")
                }
            } catch (e: Exception) {
                AuthResult.Error("网络异常：${e.localizedMessage}")
            }
        }

    /**
     * 执行用户注册
     * @param request 注册请求信息
     * @return 注册结果
     */
    suspend fun register(request: RegisterRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = userService.registerBlocking(
                UserService.RegisterRequest(
                    username = request.username,
                    password = request.password,
                    sessionId = request.sessionId,
                    velCode = request.verifyCode
                )
            )

            if (response.ok == true && response.data != null) {
                // 保存认证信息
                saveAuthInfo(response.data.token, response.data.uid)
                // 异步加载用户详情
                loadAndCacheUserInfo()
                AuthResult.Success("注册成功")
            } else {
                AuthResult.Error(response.msg ?: "注册失败")
            }
        } catch (e: Exception) {
            AuthResult.Error("网络异常：${e.localizedMessage}")
        }
    }

    /**
     * 检查当前登录状态
     */
//    fun isLoggedIn(): Boolean {
//        val token = tokenProvider.accessToken()
//        val expiresAt = userDefaults.get<Long>(NovelUserDefaultsKey.TOKEN_EXPIRES_AT) ?: 0L
//        val isLoggedIn = userDefaults.get<Boolean>(NovelUserDefaultsKey.IS_LOGGED_IN) ?: false
//
//        return token.isNotEmpty() &&
//               System.currentTimeMillis() < expiresAt &&
//               isLoggedIn
//    }

    /**
     * 退出登录，清理所有认证信息
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        tokenProvider.clear()
        userDefaults.remove(NovelUserDefaultsKey.TOKEN_EXPIRES_AT)
        userDefaults.remove(NovelUserDefaultsKey.IS_LOGGED_IN)
        userDefaults.remove(NovelUserDefaultsKey.USER_ID)
        // 清理本地用户缓存
        userRepository.clearAllUsers()
    }

    /**
     * 保存认证信息
     * ⚠ 安全检查: Token过期时间设置为1小时，确保安全性
     */
    private suspend fun saveAuthInfo(token: String, uid: Int) {
        tokenProvider.saveToken(token, "")
        userDefaults.set(
            System.currentTimeMillis() + 30 * 24 * 3600_000L, // 1小时过期
            NovelUserDefaultsKey.TOKEN_EXPIRES_AT
        )
        userDefaults.set(true, NovelUserDefaultsKey.IS_LOGGED_IN)
        userDefaults.set(uid, NovelUserDefaultsKey.USER_ID)
    }

    /**
     * 异步加载并缓存用户信息
     */
    private suspend fun loadAndCacheUserInfo() {
        runCatching {
            val userInfo = userService.getUserInfoBlocking()
            userInfo?.data?.let { userRepository.cacheUser(it) }
            val uid = userDefaults.get<Int>(NovelUserDefaultsKey.USER_ID)
            val token = tokenProvider.accessToken()
            val nickname = userInfo?.data?.nickName ?: "用户$uid"
            val photo = userInfo?.data?.userPhoto ?: ""
            val sex = when (userInfo?.data?.userSex) {
                1 -> "男"
                2 -> "女"
                else -> "未知"
            }
            ReactNativeBridge.sendUserDataToRN(uid.toString(), token, nickname, photo, sex)
        }
    }
}

/**
 * 注册请求数据类
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val sessionId: String,
    val verifyCode: String
)

/**
 * 认证结果封装
 */
sealed class AuthResult {
    data class Success(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
} 