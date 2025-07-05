package com.novel.page.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novel.page.login.viewmodel.*
import com.novel.ui.theme.NovelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Login模块性能测试
 * 
 * 测试内容：
 * 1. 状态更新性能
 * 2. UI渲染性能
 * 3. Reducer处理性能
 */
@RunWith(AndroidJUnit4::class)
class LoginPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 状态更新性能_基准测试() {
        var state = LoginState()
        
        val time = measureTimeMillis {
            repeat(100) { index ->
                state = state.copy(
                    version = state.version + 1,
                    loginForm = state.loginForm.copy(phone = "1380013800$index")
                )
            }
        }
        
        // 验证性能在合理范围内（100次更新应该在100ms内完成）
        assert(time < 100) { "状态更新性能过慢: ${time}ms" }
    }

    @Test
    fun reducer处理性能_基准测试() {
        val reducer = LoginReducer()
        val initialState = LoginState()
        
        val intents = listOf(
            LoginIntent.InputPhone("13800138000"),
            LoginIntent.InputPassword("password123"),
            LoginIntent.ToggleAgreement(true),
            LoginIntent.SwitchToRegister,
            LoginIntent.SwitchToLogin
        )
        
        val time = measureTimeMillis {
            var currentState = initialState
            repeat(20) {
                intents.forEach { intent ->
                    val result = reducer.reduce(currentState, intent)
                    currentState = result.newState
                }
            }
        }
        
        // 验证Reducer处理性能
        assert(time < 50) { "Reducer处理性能过慢: ${time}ms" }
    }

    @Test
    fun UI渲染性能_初始渲染() {
        val time = measureTimeMillis {
            composeTestRule.setContent {
                NovelTheme {
                    LoginPage()
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // 验证UI渲染性能
        assert(time < 1000) { "UI渲染性能过慢: ${time}ms" }
    }

    @Test
    fun 计算属性性能_基准测试() {
        val state = LoginState(
            isLoginMode = true,
            loginForm = LoginForm(phone = "13800138000", password = "password123"),
            registerForm = RegisterForm(
                phone = "13800138001",
                password = "password456",
                passwordConfirm = "password456",
                verifyCode = "1234"
            ),
            isAgreementAccepted = true,
            captchaState = CaptchaState(imagePath = "test", sessionId = "session")
        )
        
        val time = measureTimeMillis {
            repeat(1000) {
                // 测试计算属性性能
                val isSubmitEnabled = state.isSubmitEnabled
                val submitButtonText = state.submitButtonText
                val switchModeButtonText = state.switchModeButtonText
                val activeForm = state.activeForm
                val isEmpty = state.isEmpty
                val isSuccess = state.isSuccess
            }
        }
        
        // 验证计算属性性能
        assert(time < 50) { "计算属性性能过慢: ${time}ms" }
    }

    @Test
    fun 完整登录流程性能_端到端() {
        val reducer = LoginReducer()
        
        val time = measureTimeMillis {
            repeat(10) {
                var currentState = LoginState()
                
                // 完整登录流程
                val phoneResult = reducer.reduce(currentState, LoginIntent.InputPhone("13800138000"))
                currentState = phoneResult.newState
                
                val passwordResult = reducer.reduce(currentState, LoginIntent.InputPassword("password123"))
                currentState = passwordResult.newState
                
                val agreementResult = reducer.reduce(currentState, LoginIntent.ToggleAgreement(true))
                currentState = agreementResult.newState
                
                val submitResult = reducer.reduce(currentState, LoginIntent.SubmitLogin)
                currentState = submitResult.newState
            }
        }
        
        // 验证完整流程性能
        assert(time < 100) { "完整登录流程性能过慢: ${time}ms" }
    }
} 