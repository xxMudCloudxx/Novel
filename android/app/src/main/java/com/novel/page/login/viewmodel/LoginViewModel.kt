package com.novel.page.login.viewmodel

import android.annotation.SuppressLint
import android.util.Log
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

// === UI 状态定义 ===

/**
 * 登录页面UI状态
 * 
 * 包含所有页面显示所需的状态数据：
 * - 表单输入状态和验证结果
 * - 验证码相关状态
 * - 加载状态和错误信息
 * - 模式切换状态（登录/注册）
 */
data class LoginUiState(
    /** 用户协议是否已勾选 */
    val isAgreementChecked: Boolean = false,
    /** 手机号码 */
    val phoneNumber: String = "",
    /** 运营商名称（用于客服电话） */
    val operatorName: String = "",
    /** 用户输入的验证码 */
    val verifyCode: String = "",
    /** Base64编码的验证码图片 */
    val verifyImage: String = "",
    /** 验证码对应的会话ID */
    val sessionId: String = "",
    /** 验证码加载状态 */
    val isCaptchaLoading: Boolean = false,
    /** 验证码加载错误信息 */
    val captchaError: String? = null,
    /** 用户名 */
    val username: String = "",
    /** 密码 */
    val password: String = "",
    /** 确认密码 */
    val passwordConfirm: String = "",
    /** 是否为注册模式 */
    val isRegisterMode: Boolean = false,
    /** 整体加载状态 */
    val isLoading: Boolean = false,
    /** 用户名验证错误 */
    val usernameError: String? = null,
    /** 密码验证错误 */
    val passwordError: String? = null,
    /** 确认密码验证错误 */
    val passwordConfirmError: String? = null,
    /** 验证码验证错误 */
    val verifyCodeError: String? = null,
) {
    /**
     * 计算按钮是否可用
     * 基于表单完整性和协议勾选状态
     */
    val isActionButtonEnabled: Boolean
        get() = isAgreementChecked &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                (!isRegisterMode || (passwordConfirm.isNotBlank() && verifyCode.isNotBlank()))
}

// === 事件定义 ===

/**
 * 登录页面一次性事件
 * 用于触发导航、显示提示等副作用
 */
sealed class LoginEvent {
    /** 显示Toast提示 */
    data class ShowToast(val message: String) : LoginEvent()
    /** 导航到指定路由 */
    data class Navigate(val route: String) : LoginEvent()
    /** 启动电话服务 */
    data class LaunchTelService(val number: String) : LoginEvent()
    /** 打开用户协议页面 */
    data object OpenUserAgreementPage : LoginEvent()
    /** 打开注册协议页面 */
    data object OpenRegisterAgreementPage : LoginEvent()
}

// === 用户操作定义 ===

/**
 * 登录页面用户操作意图
 * 采用MVI架构模式，封装所有用户交互
 */
sealed class LoginAction {
    /** 执行登录操作 */
    data class DoLogin(val username: String, val password: String) : LoginAction()
    /** 打开电话客服 */
    data object OpenTelService : LoginAction()
    /** 打开用户协议 */
    data object OpenUserAgreement : LoginAction()
    /** 打开注册协议 */
    data object OpenRegisterAgreement : LoginAction()
    /** 切换协议勾选状态 */
    data class ToggleAgreement(val checked: Boolean) : LoginAction()
    /** 输入验证码 */
    data class InputVerifyCode(val code: String) : LoginAction()
    /** 刷新验证码 */
    data object RefreshCaptcha : LoginAction()
    /** 输入用户名 */
    data class InputUsername(val value: String) : LoginAction()
    /** 输入密码 */
    data class InputPassword(val value: String) : LoginAction()
    /** 输入确认密码 */
    data class InputPasswordConfirm(val value: String) : LoginAction()
    /** 切换登录/注册模式 */
    data object ToggleRegisterMode : LoginAction()
    /** 执行注册操作 */
    data class DoRegister(
        val username: String,
        val password: String,
        val sessionId: String,
        val velCode: String
    ) : LoginAction()
}

/**
 * 登录页面ViewModel
 * 
 * 职责：
 * - 管理登录/注册页面的UI状态
 * - 处理用户交互和表单验证
 * - 协调认证服务和验证码服务
 * - 管理页面间导航和事件通知
 * 
 * 架构特点：
 * - 采用MVI架构模式，单向数据流
 * - 业务逻辑委托给领域服务
 * - 使用Channel处理一次性事件
 * - 响应式状态管理，自动UI更新
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    /** 认证服务，处理登录/注册逻辑 */
    private val authService: AuthService,
    /** 表单验证服务 */
    private val validationService: ValidationService,
    /** 验证码服务 */
    private val captchaService: CaptchaService,
    /** 手机信息工具类 */
    @SuppressLint("StaticFieldLeak") private val phoneInfoUtil: PhoneInfoUtil
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    /** UI状态流，响应式更新UI */
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** 事件通道，处理一次性事件 */
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        Log.d(TAG, "LoginViewModel初始化")
        // 初始化页面数据
        initializePageData()
        // 监听验证码状态变化
        observeCaptchaState()
    }

    /**
     * 统一入口，处理所有UI交互
     * 
     * 采用when表达式分发不同的用户操作
     * 确保所有操作都在viewModelScope中执行
     */
    fun onAction(action: LoginAction) {
        Log.d(TAG, "处理用户操作: ${action::class.simpleName}")
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
                    Log.d(TAG, "切换到${if (!uiState.value.isRegisterMode) "注册" else "登录"}模式")
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
     * 预加载手机信息和验证码
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
