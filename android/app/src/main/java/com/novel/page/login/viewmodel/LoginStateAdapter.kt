package com.novel.page.login.viewmodel

import androidx.compose.runtime.Stable
import com.novel.core.adapter.StateAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Login模块状态适配器
 * 
 * 为Login模块提供统一的状态适配功能：
 * - 细粒度状态订阅
 * - UI友好的便利方法
 * - 类型安全的状态访问
 */
@Stable
class LoginStateAdapter(
    stateFlow: StateFlow<LoginState>
) : StateAdapter<LoginState>(stateFlow) {
    
    // region 基础状态适配
    
    /** 是否为登录模式 */
    val isLoginMode = mapState { it.isLoginMode }
    
    /** 是否为注册模式 */
    val isRegisterMode = mapState { !it.isLoginMode }
    
    /** 是否正在提交 */
    val isSubmitting = mapState { it.isSubmitting }
    
    /** 提交错误信息 */
    val submitError = mapState { it.submitError }
    
    /** 协议是否已同意 */
    val isAgreementAccepted = mapState { it.isAgreementAccepted }
    
    // endregion
    
    // region 表单状态适配
    
    /** 当前激活的表单数据 */
    val activeForm = mapState { it.activeForm }
    
    /** 登录表单 */
    val loginForm = mapState { it.loginForm }
    
    /** 注册表单 */
    val registerForm = mapState { it.registerForm }
    
    /** 验证结果 */
    val validationResults = mapState { it.validationResults }
    
    // endregion
    
    // region 验证码状态适配
    
    /** 验证码状态 */
    val captchaState = mapState { it.captchaState }
    
    /** 验证码图片路径 */
    val captchaImagePath = mapState { it.captchaState.imagePath }
    
    /** 验证码会话ID */
    val captchaSessionId = mapState { it.captchaState.sessionId }
    
    /** 验证码是否正在加载 */
    val isCaptchaLoading = mapState { it.captchaState.isLoading }
    
    /** 验证码错误信息 */
    val captchaError = mapState { it.captchaState.error }
    
    /** 是否有有效验证码 */
    val hasValidCaptcha = createConditionFlow { it.captchaState.hasValidCaptcha }
    
    // endregion
    
    // region 手机信息状态适配
    
    /** 手机信息 */
    val phoneInfo = mapState { it.phoneInfo }
    
    /** 手机号码 */
    val phoneNumber = mapState { it.phoneInfo.phoneNumber }
    
    /** 运营商名称 */
    val operatorName = mapState { it.phoneInfo.operatorName }
    
    /** 脱敏手机号 */
    val maskedPhoneNumber = mapState { it.phoneInfo.maskedPhoneNumber }
    
    // endregion
    
    // region 便利方法
    
    /** 提交按钮是否可用 */
    fun canSubmit(): Boolean = getCurrentSnapshot().isSubmitEnabled
    
    /** 获取提交按钮文字 */
    fun getSubmitButtonText(): String = getCurrentSnapshot().submitButtonText
    
    /** 获取模式切换按钮文字 */
    fun getSwitchModeButtonText(): String = getCurrentSnapshot().switchModeButtonText
    
    /** 是否有验证错误 */
    fun hasValidationErrors(): Boolean = getCurrentSnapshot().validationResults.hasErrors
    
    /** 获取当前手机号（根据模式） */
    fun getCurrentPhone(): String {
        val state = getCurrentSnapshot()
        return if (state.isLoginMode) {
            state.loginForm.phone
        } else {
            state.registerForm.phone
        }
    }
    
    /** 获取当前密码（根据模式） */
    fun getCurrentPassword(): String {
        val state = getCurrentSnapshot()
        return if (state.isLoginMode) {
            state.loginForm.password
        } else {
            state.registerForm.password
        }
    }
    
    /** 检查是否可以切换到注册模式 */
    fun canSwitchToRegister(): Boolean = getCurrentSnapshot().isLoginMode
    
    /** 检查是否可以切换到登录模式 */
    fun canSwitchToLogin(): Boolean = !getCurrentSnapshot().isLoginMode
    
    /** 获取验证码提示文字 */
    fun getCaptchaHint(): String {
        val state = getCurrentSnapshot()
        return when {
            state.captchaState.isLoading -> "验证码加载中..."
            state.captchaState.error != null -> "验证码加载失败，点击重试"
            state.captchaState.hasValidCaptcha -> "点击刷新验证码"
            else -> "获取验证码"
        }
    }
    
    /** 获取运营商客服电话 */
    fun getOperatorServiceNumber(): String {
        val operatorName = getCurrentSnapshot().phoneInfo.operatorName
        return when (operatorName) {
            "移动" -> "10086"
            "联通" -> "10010"
            "电信" -> "10000"
            else -> "10000"
        }
    }
    
    /** 获取登录状态摘要 */
    fun getLoginStatusSummary(): String {
        val state = getCurrentSnapshot()
        return buildString {
            append("模式: ${if (state.isLoginMode) "登录" else "注册"}")
            if (state.isSubmitting) append(", 提交中")
            if (state.hasError) append(", 有错误")
            if (state.validationResults.hasErrors) append(", 验证失败")
        }
    }
    
    // endregion
}

/**
 * 状态组合器 - 登录页面专用
 */
@Stable
data class LoginScreenState(
    val isLoading: Boolean,
    val error: String?,
    val isLoginMode: Boolean,
    val canSubmit: Boolean,
    val submitButtonText: String,
    val switchModeButtonText: String,
    val currentPhone: String,
    val currentPassword: String,
    val hasValidationErrors: Boolean,
    val hasValidCaptcha: Boolean,
    val captchaHint: String,
    val operatorServiceNumber: String,
    val loginStatusSummary: String
)

/**
 * 状态监听器 - 登录页面专用
 */
class LoginStateListener(private val adapter: LoginStateAdapter) {
    
    /** 监听模式变化 */
    fun onModeChanged(action: (Boolean) -> Unit): Flow<Boolean> {
        return adapter.isLoginMode.map { isLoginMode ->
            action(isLoginMode)
            isLoginMode
        }
    }
    
    /** 监听提交状态变化 */
    fun onSubmitStateChanged(action: (Boolean) -> Unit): Flow<Boolean> {
        return adapter.isSubmitting.map { isSubmitting ->
            action(isSubmitting)
            isSubmitting
        }
    }
    
    /** 监听验证码状态变化 */
    fun onCaptchaStateChanged(action: (CaptchaState) -> Unit): Flow<CaptchaState> {
        return adapter.captchaState.map { captchaState ->
            action(captchaState)
            captchaState
        }
    }
    
    /** 监听表单验证状态变化 */
    fun onValidationChanged(action: (ValidationResults) -> Unit): Flow<ValidationResults> {
        return adapter.validationResults.map { validationResults ->
            action(validationResults)
            validationResults
        }
    }
}

/**
 * 扩展函数：为LoginStateAdapter创建状态组合器
 */
fun LoginStateAdapter.toScreenState(): LoginScreenState {
    return LoginScreenState(
        isLoading = isCurrentlyLoading(),
        error = getCurrentError(),
        isLoginMode = getCurrentSnapshot().isLoginMode,
        canSubmit = canSubmit(),
        submitButtonText = getSubmitButtonText(),
        switchModeButtonText = getSwitchModeButtonText(),
        currentPhone = getCurrentPhone(),
        currentPassword = getCurrentPassword(),
        hasValidationErrors = hasValidationErrors(),
        hasValidCaptcha = getCurrentSnapshot().captchaState.hasValidCaptcha,
        captchaHint = getCaptchaHint(),
        operatorServiceNumber = getOperatorServiceNumber(),
        loginStatusSummary = getLoginStatusSummary()
    )
}

/**
 * 扩展函数：为LoginStateAdapter创建状态监听器
 */
fun LoginStateAdapter.createLoginListener(): LoginStateListener {
    return LoginStateListener(this)
} 