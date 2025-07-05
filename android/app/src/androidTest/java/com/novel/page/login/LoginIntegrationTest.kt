package com.novel.page.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novel.page.login.viewmodel.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Login模块集成测试
 * 
 * 测试内容：
 * 1. 状态管理集成
 * 2. Reducer逻辑
 * 3. 数据模型验证
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class LoginIntegrationTest {

    @Test
    fun loginState_计算属性_正确性() {
        // 测试登录模式状态
        val loginState = LoginState(
            isLoginMode = true,
            loginForm = LoginForm(phone = "13800138000", password = "password123"),
            isAgreementAccepted = true
        )
        
        assertTrue(loginState.isSubmitEnabled)
        assertEquals("登录", loginState.submitButtonText)
        assertEquals("暂无账号，去注册", loginState.switchModeButtonText)
        assertEquals("13800138000", loginState.activeForm.phone)

        // 测试注册模式状态
        val registerState = LoginState(
            isLoginMode = false,
            registerForm = RegisterForm(
                phone = "13800138000",
                password = "password123",
                passwordConfirm = "password123",
                verifyCode = "1234"
            ),
            isAgreementAccepted = true,
            captchaState = CaptchaState(imagePath = "test", sessionId = "session")
        )
        
        assertTrue(registerState.isSubmitEnabled)
        assertEquals("注册", registerState.submitButtonText)
        assertEquals("返回登录", registerState.switchModeButtonText)
        assertEquals("13800138000", registerState.activeForm.phone)
    }

    @Test
    fun loginReducer_状态转换_正确性() {
        val reducer = LoginReducer()
        var currentState = LoginState()

        // 测试输入手机号
        val phoneResult = reducer.reduce(currentState, LoginIntent.InputPhone("13800138000"))
        currentState = phoneResult.newState
        assertEquals("13800138000", currentState.loginForm.phone)
        assertEquals(1L, currentState.version)

        // 测试输入密码
        val passwordResult = reducer.reduce(currentState, LoginIntent.InputPassword("password123"))
        currentState = passwordResult.newState
        assertEquals("password123", currentState.loginForm.password)
        assertEquals(2L, currentState.version)

        // 测试切换协议
        val agreementResult = reducer.reduce(currentState, LoginIntent.ToggleAgreement(true))
        currentState = agreementResult.newState
        assertTrue(currentState.isAgreementAccepted)
        assertEquals(3L, currentState.version)

        // 测试模式切换
        val switchResult = reducer.reduce(currentState, LoginIntent.SwitchToRegister)
        currentState = switchResult.newState
        assertFalse(currentState.isLoginMode)
        assertEquals(4L, currentState.version)
    }

    @Test
    fun 完整登录流程_状态管理() {
        val reducer = LoginReducer()
        var currentState = LoginState()
        
        // 1. 输入手机号
        val phoneResult = reducer.reduce(currentState, LoginIntent.InputPhone("13800138000"))
        currentState = phoneResult.newState
        
        // 2. 输入密码
        val passwordResult = reducer.reduce(currentState, LoginIntent.InputPassword("password123"))
        currentState = passwordResult.newState
        
        // 3. 同意协议
        val agreementResult = reducer.reduce(currentState, LoginIntent.ToggleAgreement(true))
        currentState = agreementResult.newState
        
        // 4. 提交登录
        val submitResult = reducer.reduce(currentState, LoginIntent.SubmitLogin)
        currentState = submitResult.newState
        
        // 验证最终状态
        assertEquals("13800138000", currentState.loginForm.phone)
        assertEquals("password123", currentState.loginForm.password)
        assertTrue(currentState.isAgreementAccepted)
        assertTrue(currentState.isSubmitting)
        assertEquals(4L, currentState.version)
    }

    @Test
    fun 状态版本控制_正确递增() {
        val reducer = LoginReducer()
        var currentState = LoginState()
        val initialVersion = currentState.version
        
        // 执行多个操作
        val operations = listOf(
            LoginIntent.InputPhone("13800138000"),
            LoginIntent.InputPassword("password123"),
            LoginIntent.ToggleAgreement(true),
            LoginIntent.SwitchToRegister,
            LoginIntent.SwitchToLogin
        )
        
        operations.forEach { intent ->
            val result = reducer.reduce(currentState, intent)
            currentState = result.newState
            assertTrue("版本应该递增", currentState.version > initialVersion)
        }
        
        // 验证最终版本
        assertEquals(initialVersion + operations.size, currentState.version)
    }
} 