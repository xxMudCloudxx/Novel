package com.novel.page.login.viewmodel

import com.novel.utils.TimberLogger

/**
 * Login状态更新器
 * 
 * 提供状态更新的便利方法，封装复杂的状态转换逻辑
 * 支持副作用的同时生成
 */
object LoginStateUpdater {
    
    private const val TAG = "LoginStateUpdater"
    
    /**
     * 状态更新结果
     */
    data class UpdateResult(
        val newState: LoginState,
        val effect: LoginEffect? = null
    )
    
    /**
     * 更新手机信息
     */
    fun updatePhoneInfo(currentState: LoginState, phoneInfo: PhoneInfo): LoginState {
        TimberLogger.d(TAG, "更新手机信息: ${phoneInfo.operatorName}")
        
        return currentState.copy(
            version = currentState.version + 1,
            phoneInfo = phoneInfo,
            // 页面初始化完成，关闭加载状态，隐藏骨架屏
            isLoading = false
        )
    }
    
    /**
     * 更新验证结果
     */
    fun updateValidationResults(currentState: LoginState, validationResults: ValidationResults): LoginState {
        TimberLogger.d(TAG, "更新验证结果，有错误: ${validationResults.hasErrors}")
        
        return currentState.copy(
            version = currentState.version + 1,
            validationResults = validationResults,
            isSubmitting = false
        )
    }
    
    /**
     * 更新验证码状态
     */
    fun updateCaptchaState(currentState: LoginState, captchaState: CaptchaState): LoginState {
        TimberLogger.d(TAG, "更新验证码状态")
        
        return currentState.copy(
            version = currentState.version + 1,
            captchaState = captchaState
        )
    }
    
    /**
     * 更新登录成功
     */
    fun updateLoginSuccess(currentState: LoginState, message: String): UpdateResult {
        TimberLogger.d(TAG, "登录成功: $message")
        
        val newState = currentState.copy(
            version = currentState.version + 1,
            isSubmitting = false,
            submitError = null
        )
        
        val effect = LoginEffect.NavigateToHome
        
        return UpdateResult(newState, effect)
    }
    
    /**
     * 更新登录失败
     */
    fun updateLoginFailure(currentState: LoginState, message: String): UpdateResult {
        TimberLogger.d(TAG, "登录失败: $message")
        
        val newState = currentState.copy(
            version = currentState.version + 1,
            isSubmitting = false,
            submitError = message
        )
        
        val effect = LoginEffect.ShowToast(message)
        
        return UpdateResult(newState, effect)
    }
    
    /**
     * 更新注册成功
     */
    fun updateRegisterSuccess(currentState: LoginState, message: String): UpdateResult {
        TimberLogger.d(TAG, "注册成功: $message")
        
        val newState = currentState.copy(
            version = currentState.version + 1,
            isSubmitting = false,
            submitError = null,
            // 注册成功后切换到登录模式
            isLoginMode = true
        )
        
        val effect = LoginEffect.ShowToast("注册成功，请登录")
        
        return UpdateResult(newState, effect)
    }
    
    /**
     * 更新注册失败
     */
    fun updateRegisterFailure(currentState: LoginState, message: String): UpdateResult {
        TimberLogger.d(TAG, "注册失败: $message")
        
        val newState = currentState.copy(
            version = currentState.version + 1,
            isSubmitting = false,
            submitError = message
        )
        
        val effect = LoginEffect.ShowToast(message)
        
        return UpdateResult(newState, effect)
    }
} 