package com.novel.page.login.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.utils.PhoneInfoUtil
import com.novel.utils.maskPhoneNumber
import com.novel.utils.network.api.front.user.UserService
import com.novel.utils.network.api.front.resource.ResourceService
import com.novel.utils.network.TokenProvider
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// 错误类型
sealed class LoginError {
    data class InputError(val field: String, val message: String) : LoginError()
    data class NetworkError(val message: String) : LoginError()
    data class CaptchaError(val message: String) : LoginError()
}

// —— UI 状态 ——
data class LoginUiState(
    val isAgreementChecked: Boolean = false,
    val phoneNumber: String = "",
    val operatorName: String = "",
    val verifyCode: String = "",       // 用户输入的验证码
    val verifyImage: String = "",      // Base64 Data URI
    val sessionId: String = "",         // 验证码对应的会话 ID
    val isCaptchaLoading: Boolean = true, // 新增：验证码加载状态
    val captchaError: String? = null,      // 新增：验证码加载错误信息
    val username: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,      // 新增：整体加载状态
    val usernameError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null,
    val verifyCodeError: String? = null,
)

// —— 一次性事件 ——
sealed class LoginEvent {
    data class ShowToast(val message: String) : LoginEvent()
    data class Navigate(val route: String) : LoginEvent()
    data class LaunchTelService(val number: String) : LoginEvent()
    data object OpenUserAgreementPage : LoginEvent()
    data object OpenRegisterAgreementPage : LoginEvent()
}

// —— 所有用户操作 ——
sealed class LoginAction {
    data class DoLogin(val username: String, val password: String) : LoginAction()
    data object ToRegister : LoginAction()
    data object OpenTelService : LoginAction()
    data object OpenUserAgreement : LoginAction()
    data object OpenRegisterAgreement : LoginAction()
    data class ToggleAgreement(val checked: Boolean) : LoginAction()
    data class InputVerifyCode(val code: String) : LoginAction() // 用户输入验证码
    data object RefreshCaptcha : LoginAction()                        // 刷新验证码
    data class InputUsername(val value: String) : LoginAction()
    data class InputPassword(val value: String) : LoginAction()
    data class InputPasswordConfirm(val value: String) : LoginAction()
    data object ToggleRegisterMode : LoginAction()
    data class DoRegister(
        val username: String,
        val password: String,
        val sessionId: String,
        val velCode: String
    ) : LoginAction()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userService: UserService,
    private val resourceService: ResourceService,
    private val tokenProvider: TokenProvider,
    private val userDefaults: NovelUserDefaults,
    @SuppressLint("StaticFieldLeak") private val context: Context,
    phoneInfoUtil: PhoneInfoUtil
) : ViewModel() {

    // 状态流
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 事件通道
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // 预加载手机号和运营商
        viewModelScope.launch {
            val info = phoneInfoUtil.fetch()
            _uiState.update {
                it.copy(
                    phoneNumber = maskPhoneNumber(info.phoneNumber),
                    operatorName = info.operatorName
                )
            }
            loadCaptcha()
        }
    }

    /** 统一入口，处理所有 UI 交互  */
    fun onAction(action: LoginAction) {
        viewModelScope.launch {
            when (action) {
                is LoginAction.ToggleAgreement -> {
                    _uiState.update { it.copy(isAgreementChecked = action.checked) }
                }

                LoginAction.OpenTelService -> {
                    // 假设运营商服务客服电话存在于 uiState.operatorName 对应的映射
                    val serviceNumber = mapOperatorToTel(uiState.value.operatorName)
                    _events.send(LoginEvent.LaunchTelService(serviceNumber))
                }

                LoginAction.OpenUserAgreement -> {
                    _events.send(LoginEvent.OpenUserAgreementPage)
                }

                LoginAction.OpenRegisterAgreement -> {
                    _events.send(LoginEvent.OpenRegisterAgreementPage)
                }

                is LoginAction.DoLogin -> {
                    if (validateInputs()) {
                        performLogin()
                    }
                }

                is LoginAction.ToRegister -> {
                    _events.send(LoginEvent.Navigate("register"))
                }

                is LoginAction.InputVerifyCode -> {
                    _uiState.update { it.copy(verifyCode = action.code) }
                }

                is LoginAction.RefreshCaptcha -> {
                    loadCaptcha()
                }

                is LoginAction.InputUsername -> {
                    _uiState.update { it.copy(username = action.value) }
                }

                is LoginAction.InputPassword -> {
                    _uiState.update { it.copy(password = action.value) }
                }

                is LoginAction.InputPasswordConfirm -> {
                    _uiState.update {
                        it.copy(
                            passwordConfirm = action.value
                        )
                    }
                }

                is LoginAction.ToggleRegisterMode -> {
                    _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode) }
                }

                is LoginAction.DoRegister -> {
                    if (validateInputs()) {
                        performRegister(action)
                    }
                }
            }
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isBlank() -> "手机号不能为空"
            !username.matches(Regex("^1[3-9]\\d{9}$")) -> "请输入有效的手机号"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "密码不能为空"
            password.length < 6 -> "密码长度需至少6位"
            else -> null
        }
    }

    private fun validatePasswordConfirm(confirm: String, password: String): String? {
        return when {
            confirm != password -> "两次输入的密码不一致"
            else -> null
        }
    }

    private fun validateVerifyCode(code: String): String? {
        return if (uiState.value.isRegisterMode && code.length < 4) "验证码输入错误" else null
    }

    private fun validateInputs(): Boolean {
        val state = uiState.value
        // 1. 逐项校验
        val usernameError = validateUsername(state.username)
        val passwordError = validatePassword(state.password)
        val passwordConfirmError = if (state.isRegisterMode)
            validatePasswordConfirm(state.passwordConfirm, state.password)
        else null
        val verifyCodeError = validateVerifyCode(state.verifyCode)

        // 2. 如果有错，清空该字段并保留 error 作为 placeText
        _uiState.update {
            it.copy(
                username = if (usernameError != null) "" else it.username,
                usernameError = usernameError,

                password = if (passwordError != null) "" else it.password,
                passwordError = passwordError,

                passwordConfirm = if (passwordConfirmError != null) "" else it.passwordConfirm,
                passwordConfirmError = passwordConfirmError,

                verifyCode = if (verifyCodeError != null) "" else it.verifyCode,
                verifyCodeError = verifyCodeError
            )
        }

        // 3. 仅所有字段无错才返回 true
        return listOf(usernameError, passwordError, passwordConfirmError, verifyCodeError)
            .all { it == null }
    }


    /** 拉取并更新验证码图片和 sessionId */
    private fun loadCaptcha() {
        viewModelScope.launch {
            Log.d("LoginViewModel", "loadCaptcha started")
            _uiState.update {
                it.copy(
                    isCaptchaLoading = true,
                    captchaError = null,
                    verifyImage = "",
                    sessionId = ""
                )
            } // 重置图片和错误
            runCatching {
                resourceService.getImageVerifyCodeBlocking()
            }.onSuccess { resp ->
                resp.data?.let { data ->
                    // 解码 Base64 数据
                    val imageBytes = Base64.decode(data.imgBase64, Base64.DEFAULT)
                    // 将字节数组写入临时文件
                    val tempFile = withContext(Dispatchers.IO) {
                        val file = File(context.cacheDir, "captcha_${data.sessionId}.jpg")
                        file.writeBytes(imageBytes)
                        file
                    }
                    Log.d(
                        "LoginViewModel",
                        "loadCaptcha success, uri: ${tempFile.absolutePath}, sessionId: ${data.sessionId}"
                    )
                    _uiState.update {
                        it.copy(
                            verifyImage = tempFile.absolutePath,
                            sessionId = data.sessionId,
                            isCaptchaLoading = false,
                            captchaError = null
                        )
                    }
                } ?: run {
                    Log.d("LoginViewModel", "loadCaptcha failed: data is null")
                    _uiState.update {
                        it.copy(
                            isCaptchaLoading = false,
                            captchaError = "验证码数据为空"
                        )
                    }
                    _events.send(LoginEvent.ShowToast("验证码加载失败"))
                }
            }.onFailure { e ->
                Log.d("LoginViewModel", "loadCaptcha error: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isCaptchaLoading = false,
                        captchaError = e.localizedMessage ?: "验证码加载失败"
                    )
                }
                _events.send(LoginEvent.ShowToast("验证码加载失败：${e.localizedMessage}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 清理临时文件
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.filter { it.name.startsWith("captcha_") }?.forEach { it.delete() }
        }
    }

    private fun performRegister(action: LoginAction.DoRegister) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }    // 显示加载状态
            try {
                val resp = withContext(Dispatchers.IO) {
                    userService.registerBlocking(
                        UserService.RegisterRequest(
                            username = action.username,
                            password = action.password,
                            sessionId = action.sessionId,
                            velCode = action.velCode
                        )
                    )
                }
                if (resp.ok == true && resp.data != null) {
                    // 存 token、过期时间、uid
                    tokenProvider.saveToken(resp.data.token, "")
                    userDefaults.set(
                        System.currentTimeMillis() + 3600_000,
                        NovelUserDefaultsKey.TOKEN_EXPIRES_AT
                    )
                    userDefaults.set(true, NovelUserDefaultsKey.IS_LOGGED_IN)
                    userDefaults.set(resp.data.uid, NovelUserDefaultsKey.USER_ID)
                    launch(Dispatchers.IO) { runCatching { userService.getUserInfoBlocking() } }
                    _events.send(LoginEvent.ShowToast("注册成功"))
                    _events.send(LoginEvent.Navigate("main"))
                } else {
                    val msg = resp.msg.orEmpty().ifBlank { "注册失败" }
                    _events.send(LoginEvent.ShowToast(msg))
                }
            } catch (e: Exception) {
                _events.send(LoginEvent.ShowToast("网络异常：${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) } // 隐藏加载状态
            }
        }
    }

    /** 登录逻辑复用函数  */
    private fun performLogin() {
        viewModelScope.launch {
            _events.send(LoginEvent.ShowToast("开始登录..."))
            try {
                val resp = withContext(Dispatchers.IO) {
                    userService.loginBlocking(
                        UserService.LoginRequest(
                            uiState.value.username,
                            uiState.value.password
                        )
                    )
                }
                Log.d("LoginVM", "loginBlocking 返回: $resp")
                if (resp.ok == true && resp.data != null) {
                    val data = resp.data
                    // 存 token、过期时间、uid
                    tokenProvider.saveToken(data.token, "")
                    userDefaults.set(
                        System.currentTimeMillis() + 3600L * 1000,
                        NovelUserDefaultsKey.TOKEN_EXPIRES_AT
                    )
                    userDefaults.set(true, NovelUserDefaultsKey.IS_LOGGED_IN)
                    userDefaults.set(data.uid, NovelUserDefaultsKey.USER_ID)
                    // 异步拿用户详情
                    launch(Dispatchers.IO) { runCatching { userService.getUserInfoBlocking() } }
                    _events.send(LoginEvent.ShowToast("登录成功"))
                    _events.send(LoginEvent.Navigate("main"))
                } else {
                    val msg = resp.msg.orEmpty().ifBlank { "登录失败" }
                    _events.send(LoginEvent.ShowToast(msg))
                }
            } catch (e: Exception) {
                _events.send(LoginEvent.ShowToast("网络异常：${e.localizedMessage}"))
            }
        }
    }

    /** 根据运营商名称映射客服电话，示例写法  */
    private fun mapOperatorToTel(name: String): String = when (name) {
        "移动" -> "10086"
        "联通" -> "10010"
        "电信" -> "10000"
        else -> "10000"
    }
}
