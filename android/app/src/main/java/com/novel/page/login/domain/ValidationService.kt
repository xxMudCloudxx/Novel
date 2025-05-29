package com.novel.page.login.domain

import com.novel.utils.security.SecurityConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 输入验证服务 - 统一管理所有输入验证逻辑
 * ⚠ 安全检查: 所有输入验证都需要严格按照业务规则执行
 */
@Singleton
class ValidationService @Inject constructor(
    private val securityConfig: SecurityConfig
) {
    
    /**
     * 验证手机号格式
     * @param phone 手机号
     * @return 验证结果，null表示验证通过
     */
    fun validatePhone(phone: String): ValidationResult? {
        return when {
            phone.isBlank() -> ValidationResult.Error("手机号不能为空")
            !phone.matches(Regex(SecurityConfig.PHONE_REGEX)) -> 
                ValidationResult.Error("请输入有效的手机号")
            else -> null
        }
    }
    
    /**
     * 验证密码强度
     * ⚠ 安全检查: 密码需要满足最低安全要求
     * @param password 密码
     * @return 验证结果，null表示验证通过
     */
    fun validatePassword(password: String): ValidationResult? {
        return when {
            password.isBlank() -> ValidationResult.Error("密码不能为空")
            password.length < SecurityConfig.MIN_PASSWORD_LENGTH -> 
                ValidationResult.Error("密码长度需至少${SecurityConfig.MIN_PASSWORD_LENGTH}位")
            password.length > SecurityConfig.MAX_PASSWORD_LENGTH -> 
                ValidationResult.Error("密码长度不能超过${SecurityConfig.MAX_PASSWORD_LENGTH}位")
            !password.matches(Regex(SecurityConfig.PASSWORD_REGEX)) -> 
                ValidationResult.Error("密码包含非法字符")
            else -> null
        }
    }
    
    /**
     * 验证密码确认
     * @param password 原密码
     * @param confirmPassword 确认密码
     * @return 验证结果，null表示验证通过
     */
    fun validatePasswordConfirm(password: String, confirmPassword: String): ValidationResult? {
        return when {
            confirmPassword.isBlank() -> ValidationResult.Error("确认密码不能为空")
            confirmPassword != password -> ValidationResult.Error("两次输入的密码不一致")
            else -> null
        }
    }
    
    /**
     * 验证验证码
     * @param verifyCode 验证码
     * @param isRequired 是否必填
     * @return 验证结果，null表示验证通过
     */
    fun validateVerifyCode(verifyCode: String, isRequired: Boolean = true): ValidationResult? {
        return when {
            isRequired && verifyCode.isBlank() -> ValidationResult.Error("验证码不能为空")
            isRequired && verifyCode.length < SecurityConfig.CAPTCHA_MIN_LENGTH -> 
                ValidationResult.Error("验证码格式错误")
            verifyCode.isNotBlank() && !verifyCode.matches(Regex(SecurityConfig.CAPTCHA_REGEX)) -> 
                ValidationResult.Error("验证码格式错误")
            else -> null
        }
    }
    
    /**
     * 验证用户名长度
     * @param username 用户名
     * @return 验证结果，null表示验证通过
     */
    fun validateUsernameLength(username: String): ValidationResult? {
        return when {
            !securityConfig.isInputLengthValid(
                username, 
                SecurityConfig.MIN_USERNAME_LENGTH, 
                SecurityConfig.MAX_USERNAME_LENGTH
            ) -> ValidationResult.Error("用户名长度需在${SecurityConfig.MIN_USERNAME_LENGTH}-${SecurityConfig.MAX_USERNAME_LENGTH}位之间")
            else -> null
        }
    }
    
    /**
     * 批量验证登录表单
     * @param form 登录表单数据
     * @return 验证结果映射
     */
    fun validateLoginForm(form: LoginForm): ValidationResults {
        return ValidationResults(
            phoneError = validatePhone(form.phone),
            passwordError = validatePassword(form.password)
        )
    }
    
    /**
     * 批量验证注册表单
     * @param form 注册表单数据
     * @return 验证结果映射
     */
    fun validateRegisterForm(form: RegisterForm): ValidationResults {
        return ValidationResults(
            phoneError = validatePhone(form.phone),
            passwordError = validatePassword(form.password),
            passwordConfirmError = validatePasswordConfirm(form.password, form.passwordConfirm),
            verifyCodeError = validateVerifyCode(form.verifyCode, isRequired = true)
        )
    }
    
    /**
     * 验证用户昵称
     * @param nickname 昵称
     * @return 验证结果，null表示验证通过
     */
    fun validateNickname(nickname: String): ValidationResult? {
        return when {
            nickname.isBlank() -> ValidationResult.Error("昵称不能为空")
            !securityConfig.isInputLengthValid(
                nickname, 
                1, 
                SecurityConfig.MAX_NICKNAME_LENGTH
            ) -> ValidationResult.Error("昵称长度不能超过${SecurityConfig.MAX_NICKNAME_LENGTH}位")
            else -> null
        }
    }
}

/**
 * 验证结果
 */
sealed class ValidationResult {
    data class Error(val message: String) : ValidationResult()
}

/**
 * 批量验证结果
 */
data class ValidationResults(
    val phoneError: ValidationResult? = null,
    val passwordError: ValidationResult? = null,
    val passwordConfirmError: ValidationResult? = null,
    val verifyCodeError: ValidationResult? = null
) {
    /**
     * 检查是否所有验证都通过
     */
    val isValid: Boolean
        get() = phoneError == null && 
                passwordError == null && 
                passwordConfirmError == null && 
                verifyCodeError == null
    
    /**
     * 获取所有错误信息
     */
    fun getAllErrors(): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        phoneError?.let { errors["phone"] = (it as ValidationResult.Error).message }
        passwordError?.let { errors["password"] = (it as ValidationResult.Error).message }
        passwordConfirmError?.let { errors["passwordConfirm"] = (it as ValidationResult.Error).message }
        verifyCodeError?.let { errors["verifyCode"] = (it as ValidationResult.Error).message }
        return errors
    }
}

/**
 * 登录表单数据
 */
data class LoginForm(
    val phone: String,
    val password: String
)

/**
 * 注册表单数据
 */
data class RegisterForm(
    val phone: String,
    val password: String,
    val passwordConfirm: String,
    val verifyCode: String
) 