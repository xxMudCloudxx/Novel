package com.novel.page.login.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.page.login.utils.AuthResult
import com.novel.page.login.utils.AuthService
import com.novel.page.login.utils.CaptchaService
import com.novel.page.login.utils.LoginForm
import com.novel.page.login.utils.RegisterForm
import com.novel.page.login.utils.RegisterRequest
import com.novel.page.login.utils.ValidationResult
import com.novel.page.login.utils.ValidationResults
import com.novel.page.login.utils.ValidationService
import com.novel.utils.PhoneInfoUtil
import com.novel.utils.maskPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isCaptchaLoading: Boolean = false, // 新增：验证码加载状态
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
) {
    /**
     * 计算按钮是否可用
     */
    val isActionButtonEnabled: Boolean
        get() = isAgreementChecked &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                (!isRegisterMode || (passwordConfirm.isNotBlank() && verifyCode.isNotBlank()))
}

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
    data object OpenTelService : LoginAction()
    data object OpenUserAgreement : LoginAction()
    data object OpenRegisterAgreement : LoginAction()
    data class ToggleAgreement(val checked: Boolean) : LoginAction()
    data class InputVerifyCode(val code: String) : LoginAction()
    data object RefreshCaptcha : LoginAction()
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

/**
 * 登录页面ViewModel - 专注于UI状态管理和用户交互
 * 将业务逻辑委托给相应的领域服务
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
    private val validationService: ValidationService,
    private val captchaService: CaptchaService,
    @SuppressLint("StaticFieldLeak") private val phoneInfoUtil: PhoneInfoUtil
) : ViewModel() {

    // UI状态流
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 事件通道
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // 初始化页面数据
        initializePageData()
        // 监听验证码状态变化
        observeCaptchaState()
    }

    /**
     * 统一入口，处理所有UI交互
     */
    fun onAction(action: LoginAction) {
        viewModelScope.launch {
            when (action) {
                is LoginAction.ToggleAgreement -> {
                    _uiState.update { it.copy(isAgreementChecked = action.checked) }
                }

                LoginAction.OpenTelService -> {
                    val serviceNumber = getOperatorServiceNumber(uiState.value.operatorName)
                    _events.send(LoginEvent.LaunchTelService(serviceNumber))
                }

                LoginAction.OpenUserAgreement -> {
                    _events.send(LoginEvent.OpenUserAgreementPage)
                }

                LoginAction.OpenRegisterAgreement -> {
                    _events.send(LoginEvent.OpenRegisterAgreementPage)
                }

                is LoginAction.DoLogin -> {
                    handleLogin()
                }

                is LoginAction.InputVerifyCode -> {
                    _uiState.update { 
                        it.copy(
                            verifyCode = action.code,
                            verifyCodeError = null // 清除错误状态
                        ) 
                    }
                }

                LoginAction.RefreshCaptcha -> {
                    handleRefreshCaptcha()
                }

                is LoginAction.InputUsername -> {
                    _uiState.update { 
                        it.copy(
                            username = action.value,
                            usernameError = null // 清除错误状态
                        ) 
                    }
                }

                is LoginAction.InputPassword -> {
                    _uiState.update { 
                        it.copy(
                            password = action.value,
                            passwordError = null // 清除错误状态
                        ) 
                    }
                }

                is LoginAction.InputPasswordConfirm -> {
                    _uiState.update {
                        it.copy(
                            passwordConfirm = action.value,
                            passwordConfirmError = null // 清除错误状态
                        )
                    }
                }

                LoginAction.ToggleRegisterMode -> {
                    _uiState.update { 
                        it.copy(
                            isRegisterMode = !it.isRegisterMode,
                            // 切换模式时清除所有错误状态
                            usernameError = null,
                            passwordError = null,
                            passwordConfirmError = null,
                            verifyCodeError = null
                        ) 
                    }
                }

                is LoginAction.DoRegister -> {
                    handleRegister()
                }
            }
        }
    }

    /**
     * 初始化页面数据
     */
    private fun initializePageData() {
        viewModelScope.launch {
            // 加载手机号和运营商信息
            val phoneInfo = phoneInfoUtil.fetch()
            _uiState.update {
                it.copy(
                    phoneNumber = maskPhoneNumber(phoneInfo.phoneNumber),
                    operatorName = phoneInfo.operatorName
                )
            }
            
            // 加载验证码
            captchaService.loadCaptcha()
        }
    }

    /**
     * 监听验证码状态变化
     */
    private fun observeCaptchaState() {
        viewModelScope.launch {
            captchaService.captchaState.collect { captchaState ->
                _uiState.update {
                    it.copy(
                        verifyImage = captchaState.imagePath,
                        sessionId = captchaState.sessionId,
                        isCaptchaLoading = captchaState.isLoading,
                        captchaError = captchaState.error
                    )
                }
            }
        }
    }

    /**
     * 处理登录
     */
    private suspend fun handleLogin() {
        val state = uiState.value
        
        // 验证输入
        val validationResult = validationService.validateLoginForm(
            LoginForm(phone = state.username, password = state.password)
        )
        
        if (!validationResult.isValid) {
            updateValidationErrors(validationResult)
            return
        }
        
        // 执行登录
        _uiState.update { it.copy(isLoading = true) }
        
        when (val result = authService.login(state.username, state.password)) {
            is AuthResult.Success -> {
                _events.send(LoginEvent.ShowToast(result.message))
                _events.send(LoginEvent.Navigate("main"))
            }
            is AuthResult.Error -> {
                _events.send(LoginEvent.ShowToast(result.message))
            }
        }
        
        _uiState.update { it.copy(isLoading = false) }
    }

    /**
     * 处理注册
     */
    private suspend fun handleRegister() {
        val state = uiState.value
        
        // 验证输入
        val validationResult = validationService.validateRegisterForm(
            RegisterForm(
                phone = state.username,
                password = state.password,
                passwordConfirm = state.passwordConfirm,
                verifyCode = state.verifyCode
            )
        )
        
        if (!validationResult.isValid) {
            updateValidationErrors(validationResult)
            return
        }
        
        // 执行注册
        _uiState.update { it.copy(isLoading = true) }
        
        val request = RegisterRequest(
            username = state.username,
            password = state.password,
            sessionId = state.sessionId,
            verifyCode = state.verifyCode
        )
        
        when (val result = authService.register(request)) {
            is AuthResult.Success -> {
                _events.send(LoginEvent.ShowToast(result.message))
                _events.send(LoginEvent.Navigate("main"))
            }
            is AuthResult.Error -> {
                _events.send(LoginEvent.ShowToast(result.message))
            }
        }
        
        _uiState.update { it.copy(isLoading = false) }
    }

    /**
     * 处理验证码刷新
     */
    private suspend fun handleRefreshCaptcha() {
        val success = captchaService.refreshCaptcha()
        if (!success) {
            _events.send(LoginEvent.ShowToast("验证码刷新失败"))
        }
    }

    /**
     * 更新验证错误状态
     */
    private fun updateValidationErrors(validationResult: ValidationResults) {
        _uiState.update {
            it.copy(
                usernameError = (validationResult.phoneError as? ValidationResult.Error)?.message,
                passwordError = (validationResult.passwordError as? ValidationResult.Error)?.message,
                passwordConfirmError = (validationResult.passwordConfirmError as? ValidationResult.Error)?.message,
                verifyCodeError = (validationResult.verifyCodeError as? ValidationResult.Error)?.message
            )
        }
    }

    /**
     * 获取运营商客服电话
     */
    private fun getOperatorServiceNumber(operatorName: String): String = when (operatorName) {
        "移动" -> "10086"
        "联通" -> "10010"
        "电信" -> "10000"
        else -> "10000"
    }

    override fun onCleared() {
        super.onCleared()
        // 清理验证码缓存
        viewModelScope.launch {
            captchaService.clearAllCaptchaFiles()
        }
    }
}
