package com.novel.page.login

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelDivider
import com.novel.page.login.component.ActionButtons
import com.novel.page.login.component.AgreementSection
import com.novel.page.login.component.LoginAppBar
import com.novel.page.login.component.OperatorSection
import com.novel.page.login.component.PhoneSection
import com.novel.page.login.component.TitleSection
import com.novel.page.login.skeleton.LoginPageSkeleton
import com.novel.page.login.viewmodel.CaptchaState
import com.novel.page.login.viewmodel.LoginEffect
import com.novel.page.login.viewmodel.LoginForm
import com.novel.page.login.viewmodel.LoginIntent
import com.novel.page.login.viewmodel.LoginViewModel
import com.novel.page.login.viewmodel.RegisterForm
import com.novel.page.login.viewmodel.ValidationResults
import com.novel.ui.theme.NovelColors
import com.novel.utils.NavViewModel.navController
import com.novel.utils.wdp
import kotlinx.coroutines.flow.collectLatest
import com.novel.page.login.component.InputSection

// 动画常量
private object AnimationConfig {
    const val DURATION_MS = 600
    const val DELAY_MS = 200
    const val BUTTON_DURATION_MS = 800
    val SPRING_SPEC = spring<Dp>(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
}

/**
 * 登录页面 - MVI架构版本
 */
@SuppressLint("UnrememberedMutableState", "UseOfNonLambdaOffsetOverload")
@Composable
fun LoginPage() {
    val vm: LoginViewModel = hiltViewModel()
    val adapter = vm.adapter

    // 收集状态
    val isLoading by adapter.isLoading.collectAsState(initial = false)
    val isLoginMode by adapter.isLoginMode.collectAsState(initial = true)
    val isAgreementAccepted by adapter.isAgreementAccepted.collectAsState(initial = false)
    val operatorName by adapter.operatorName.collectAsState(initial = "")
    val maskedPhoneNumber by adapter.maskedPhoneNumber.collectAsState(initial = "")
    val loginForm by adapter.loginForm.collectAsState(initial = LoginForm())
    val registerForm by adapter.registerForm.collectAsState(initial = RegisterForm())
    val validationResults by adapter.validationResults.collectAsState(initial = ValidationResults())
    val captchaState by adapter.captchaState.collectAsState(initial = CaptchaState())

    // 收集副作用
    LaunchedEffect(Unit) {
        vm.effect.collectLatest { effect ->
            when (effect) {
                is LoginEffect.ShowToast -> {}
                is LoginEffect.NavigateToHome ->
                    navController.value?.navigate("home")
                is LoginEffect.LaunchTelService -> {}
                is LoginEffect.NavigateToPrivacyPolicy ->
                    navController.value?.navigate("userAgreement")
                is LoginEffect.NavigateToTermsOfService ->
                    navController.value?.navigate("registerAgreement")
                is LoginEffect.FocusSmsCodeInput -> {}
                is LoginEffect.NavigateBack -> {}
                is LoginEffect.ShowSmsCodeSentDialog -> {}
                is LoginEffect.TriggerHapticFeedback -> {}
            }
        }
    }

    // 显示骨架屏或正常内容
    if (isLoading) {
        LoginPageSkeleton()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = NovelColors.NovelBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginAppBar()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(animationSpec = tween(durationMillis = 300))
                    .padding(top = 60.wdp)
                    .background(color = NovelColors.NovelBackground),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                TitleSection()

                NovelDivider(
                    modifier = Modifier.padding(horizontal = 22.5.wdp)
                )

                //  运营商
                OperatorSection(operatorName + "认证")

                // 脱敏后手机号
                PhoneSection(maskedPhoneNumber)

                // 输入框
                InputSection(
                    isLoginMode = isLoginMode,
                    loginForm = loginForm,
                    registerForm = registerForm,
                    validationResults = validationResults,
                    captchaState = captchaState,
                    onPhoneInput = { vm.sendIntent(LoginIntent.InputPhone(it)) },
                    onPasswordInput = { vm.sendIntent(LoginIntent.InputPassword(it)) },
                    onPasswordConfirmInput = { vm.sendIntent(LoginIntent.InputPasswordConfirm(it)) },
                    onVerifyCodeInput = { vm.sendIntent(LoginIntent.InputVerifyCode(it)) },
                    onRefreshCaptcha = { vm.sendIntent(LoginIntent.RefreshCaptcha) }
                )

                // 构建注册模式切换的 Transition
                val transition =
                    updateTransition(
                        targetState = !isLoginMode,
                        label = "registerTransition"
                    )

                // 按钮区也用同样的弹性位移 + 透明度
                val buttonOffset by transition.animateDp(
                    transitionSpec = {
                        AnimationConfig.SPRING_SPEC
                    },
                    label = "buttonOffset"
                ) { state ->
                    if (state) 20.wdp else 0.wdp
                }
                val buttonAlpha by transition.animateFloat(
                    transitionSpec = {
                        tween(
                            durationMillis = AnimationConfig.BUTTON_DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    },
                    label = "buttonAlpha"
                ) { state ->
                    if (state) 1f else 1f
                }

                // 按钮区：在登录/注册两种模式下都保持可见，但切换时做淡入淡出+展开收缩
                ActionButtons(
                    firstText = if (!isLoginMode) "注册" else "登录",
                    secondText = if (!isLoginMode) "返回登录" else "暂无账号，去注册",
                    onFirstClick = {
                        if (!isLoginMode) {
                            vm.sendIntent(LoginIntent.SubmitRegister)
                        } else {
                            vm.sendIntent(LoginIntent.SubmitLogin)
                        }
                    },
                    isFirstEnabled = adapter.canSubmit(),
                    onSecondClick = { 
                        if (isLoginMode) {
                            vm.sendIntent(LoginIntent.SwitchToRegister)
                        } else {
                            vm.sendIntent(LoginIntent.SwitchToLogin)
                        }
                    },
                    modifier = Modifier
                        .offset(y = buttonOffset)
                        .alpha(buttonAlpha)
                        .fillMaxWidth()
                        .padding(horizontal = 22.5.wdp)
                )

                //  协议
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(
                        animationSpec = tween(
                            AnimationConfig.DURATION_MS,
                            AnimationConfig.DELAY_MS,
                            FastOutSlowInEasing
                        )
                    ) +
                            fadeIn(
                                animationSpec = tween(
                                    AnimationConfig.DURATION_MS,
                                    AnimationConfig.DELAY_MS,
                                    FastOutSlowInEasing
                                )
                            ),
                    exit = shrinkVertically(
                        animationSpec = tween(
                            AnimationConfig.DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    ) +
                            fadeOut(
                                animationSpec = tween(
                                    AnimationConfig.DURATION_MS,
                                    easing = FastOutSlowInEasing
                                )
                            )
                ) {
                    AgreementSection(
                        operator = operatorName,
                        isChecked = isAgreementAccepted,
                        onCheckedChange = { vm.sendIntent(LoginIntent.ToggleAgreement(it)) },
                        onTelServiceClick = { vm.sendIntent(LoginIntent.NavigateToTelService) },
                        onUserAgreementClick = { vm.sendIntent(LoginIntent.NavigateToPrivacyPolicy) },
                        onRegisterAgreementClick = { vm.sendIntent(LoginIntent.NavigateToTermsOfService) },
                        modifier = Modifier
                            .offset(y = buttonOffset)
                            .padding(top = 16.wdp)
                    )
                }
            }
        }
    }
}