package com.novel.page.login.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
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
 * - 优化的@Composable状态访问方法，提升skippable比例
 */
@Stable
class LoginStateAdapter(
    stateFlow: StateFlow<LoginState>
) : StateAdapter<LoginState>(stateFlow) {
    
    // region Composable 状态访问方法 (用于提升 skippable 比例)
    
    /**
     * 是否为登录模式 - 优化版本
     * 替代 isLoginMode.collectAsState() 以提升性能
     */
    @Composable
    fun isLoginModeState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isLoginMode }
    }

    /**
     * 是否为注册模式 - 优化版本
     */
    @Composable
    fun isRegisterModeState(): State<Boolean> = remember {
        derivedStateOf { !getCurrentSnapshot().isLoginMode }
    }

    /**
     * 是否正在提交 - 优化版本
     */
    @Composable
    fun isSubmittingState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isSubmitting }
    }

    /**
     * 提交错误信息 - 优化版本
     */
    @Composable
    fun submitErrorState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().submitError }
    }

    /**
     * 协议是否已同意 - 优化版本
     */
    @Composable
    fun isAgreementAcceptedState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isAgreementAccepted }
    }

    /**
     * 当前激活的表单数据 - 优化版本
     */
    @Composable
    fun activeFormState(): State<FormData> = remember {
        derivedStateOf { getCurrentSnapshot().activeForm }
    }

    /**
     * 登录表单 - 优化版本
     */
    @Composable
    fun loginFormState(): State<LoginForm> = remember {
        derivedStateOf { getCurrentSnapshot().loginForm }
    }

    /**
     * 注册表单 - 优化版本
     */
    @Composable
    fun registerFormState(): State<RegisterForm> = remember {
        derivedStateOf { getCurrentSnapshot().registerForm }
    }

    /**
     * 验证结果 - 优化版本
     */
    @Composable
    fun validationResultsState(): State<ValidationResults> = remember {
        derivedStateOf { getCurrentSnapshot().validationResults }
    }

    /**
     * 验证码状态 - 优化版本
     */
    @Composable
    fun captchaStateState(): State<CaptchaState> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState }
    }

    /**
     * 验证码图片路径 - 优化版本
     */
    @Composable
    fun captchaImagePathState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState.imagePath }
    }

    /**
     * 验证码会话ID - 优化版本
     */
    @Composable
    fun captchaSessionIdState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState.sessionId }
    }

    /**
     * 验证码是否正在加载 - 优化版本
     */
    @Composable
    fun isCaptchaLoadingState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState.isLoading }
    }

    /**
     * 验证码错误信息 - 优化版本
     */
    @Composable
    fun captchaErrorState(): State<String?> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState.error }
    }

    /**
     * 是否有有效验证码 - 优化版本
     */
    @Composable
    fun hasValidCaptchaState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().captchaState.hasValidCaptcha }
    }

    /**
     * 手机信息 - 优化版本
     */
    @Composable
    fun phoneInfoState(): State<PhoneInfo> = remember {
        derivedStateOf { getCurrentSnapshot().phoneInfo }
    }

    /**
     * 手机号码 - 优化版本
     */
    @Composable
    fun phoneNumberState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().phoneInfo.phoneNumber }
    }

    /**
     * 运营商名称 - 优化版本
     */
    @Composable
    fun operatorNameState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().phoneInfo.operatorName }
    }

    /**
     * 脱敏手机号 - 优化版本
     */
    @Composable
    fun maskedPhoneNumberState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().phoneInfo.maskedPhoneNumber }
    }

    /**
     * 提交按钮是否可用 - 优化版本
     */
    @Composable
    fun isSubmitEnabledState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().isSubmitEnabled }
    }

    /**
     * 获取提交按钮文字 - 优化版本
     */
    @Composable
    fun submitButtonTextState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().submitButtonText }
    }

    /**
     * 获取模式切换按钮文字 - 优化版本
     */
    @Composable
    fun switchModeButtonTextState(): State<String> = remember {
        derivedStateOf { getCurrentSnapshot().switchModeButtonText }
    }

    /**
     * 是否有验证错误 - 优化版本
     */
    @Composable
    fun hasValidationErrorsState(): State<Boolean> = remember {
        derivedStateOf { getCurrentSnapshot().validationResults.hasErrors }
    }

    // endregion

    // region 过时的StateFlow映射方法 (标记为废弃)
    
    /** 
     * 是否为登录模式
     * @deprecated 使用 isLoginModeState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 isLoginModeState() 替代以提升性能",
        replaceWith = ReplaceWith("isLoginModeState()")
    )
    @Stable
    val isLoginMode = mapState { it.isLoginMode }
    
    /** 
     * 是否为注册模式
     * @deprecated 使用 isRegisterModeState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 isRegisterModeState() 替代以提升性能",
        replaceWith = ReplaceWith("isRegisterModeState()")
    )
    @Stable
    val isRegisterMode = mapState { !it.isLoginMode }
    
    /** 
     * 是否正在提交
     * @deprecated 使用 isSubmittingState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 isSubmittingState() 替代以提升性能",
        replaceWith = ReplaceWith("isSubmittingState()")
    )
    @Stable
    val isSubmitting = mapState { it.isSubmitting }
    
    /** 
     * 提交错误信息
     * @deprecated 使用 submitErrorState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 submitErrorState() 替代以提升性能",
        replaceWith = ReplaceWith("submitErrorState()")
    )
    @Stable
    val submitError = mapState { it.submitError }
    
    /** 
     * 协议是否已同意
     * @deprecated 使用 isAgreementAcceptedState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 isAgreementAcceptedState() 替代以提升性能",
        replaceWith = ReplaceWith("isAgreementAcceptedState()")
    )
    @Stable
    val isAgreementAccepted = mapState { it.isAgreementAccepted }
    
    // endregion
    
    // region 表单状态适配
    
    /** 
     * 当前激活的表单数据
     * @deprecated 使用 activeFormState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 activeFormState() 替代以提升性能",
        replaceWith = ReplaceWith("activeFormState()")
    )
    @Stable
    val activeForm = mapState { it.activeForm }
    
    /** 
     * 登录表单
     * @deprecated 使用 loginFormState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 loginFormState() 替代以提升性能",
        replaceWith = ReplaceWith("loginFormState()")
    )
    @Stable
    val loginForm = mapState { it.loginForm }
    
    /** 
     * 注册表单
     * @deprecated 使用 registerFormState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 registerFormState() 替代以提升性能",
        replaceWith = ReplaceWith("registerFormState()")
    )
    @Stable
    val registerForm = mapState { it.registerForm }
    
    /** 
     * 验证结果
     * @deprecated 使用 validationResultsState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 validationResultsState() 替代以提升性能",
        replaceWith = ReplaceWith("validationResultsState()")
    )
    @Stable
    val validationResults = mapState { it.validationResults }
    
    // endregion
    
    // region 验证码状态适配
    
    /** 
     * 验证码状态
     * @deprecated 使用 captchaStateState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 captchaStateState() 替代以提升性能",
        replaceWith = ReplaceWith("captchaStateState()")
    )
    @Stable
    val captchaState = mapState { it.captchaState }
    
    /** 
     * 验证码图片路径
     * @deprecated 使用 captchaImagePathState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 captchaImagePathState() 替代以提升性能",
        replaceWith = ReplaceWith("captchaImagePathState()")
    )
    @Stable
    val captchaImagePath = mapState { it.captchaState.imagePath }
    
    /** 
     * 验证码会话ID
     * @deprecated 使用 captchaSessionIdState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 captchaSessionIdState() 替代以提升性能",
        replaceWith = ReplaceWith("captchaSessionIdState()")
    )
    @Stable
    val captchaSessionId = mapState { it.captchaState.sessionId }
    
    /** 
     * 验证码是否正在加载
     * @deprecated 使用 isCaptchaLoadingState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 isCaptchaLoadingState() 替代以提升性能",
        replaceWith = ReplaceWith("isCaptchaLoadingState()")
    )
    @Stable
    val isCaptchaLoading = mapState { it.captchaState.isLoading }
    
    /** 
     * 验证码错误信息
     * @deprecated 使用 captchaErrorState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 captchaErrorState() 替代以提升性能",
        replaceWith = ReplaceWith("captchaErrorState()")
    )
    @Stable
    val captchaError = mapState { it.captchaState.error }
    
    /** 
     * 是否有有效验证码
     * @deprecated 使用 hasValidCaptchaState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 hasValidCaptchaState() 替代以提升性能",
        replaceWith = ReplaceWith("hasValidCaptchaState()")
    )
    @Stable
    val hasValidCaptcha = createConditionFlow { it.captchaState.hasValidCaptcha }
    
    // endregion
    
    // region 手机信息状态适配
    
    /** 
     * 手机信息
     * @deprecated 使用 phoneInfoState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 phoneInfoState() 替代以提升性能",
        replaceWith = ReplaceWith("phoneInfoState()")
    )
    @Stable
    val phoneInfo = mapState { it.phoneInfo }
    
    /** 
     * 手机号码
     * @deprecated 使用 phoneNumberState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 phoneNumberState() 替代以提升性能",
        replaceWith = ReplaceWith("phoneNumberState()")
    )
    @Stable
    val phoneNumber = mapState { it.phoneInfo.phoneNumber }
    
    /** 
     * 运营商名称
     * @deprecated 使用 operatorNameState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 operatorNameState() 替代以提升性能",
        replaceWith = ReplaceWith("operatorNameState()")
    )
    @Stable
    val operatorName = mapState { it.phoneInfo.operatorName }
    
    /** 
     * 脱敏手机号
     * @deprecated 使用 maskedPhoneNumberState() 替代以提升性能
     */
    @Deprecated(
        message = "使用 maskedPhoneNumberState() 替代以提升性能",
        replaceWith = ReplaceWith("maskedPhoneNumberState()")
    )
    @Stable
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
 * @deprecated 建议使用@Composable状态访问方法替代Flow监听
 */
@Deprecated("建议使用@Composable状态访问方法替代Flow监听")
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
 * @deprecated 建议使用@Composable状态访问方法替代
 */
@Deprecated("建议使用@Composable状态访问方法替代")
fun LoginStateAdapter.createLoginListener(): LoginStateListener {
    return LoginStateListener(this)
}