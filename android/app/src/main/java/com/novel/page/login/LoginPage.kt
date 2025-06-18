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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.novel.page.login.viewmodel.LoginAction
import com.novel.page.login.viewmodel.LoginEvent
import com.novel.page.login.viewmodel.LoginViewModel
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
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
 * 登录页面
 */
@SuppressLint("UnrememberedMutableState", "UseOfNonLambdaOffsetOverload")
@Composable
fun LoginPage() {
    val vm: LoginViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()

    // 收集一次性事件
    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is LoginEvent.ShowToast -> {}
                is LoginEvent.Navigate ->
                    navController.value?.navigate(event.route)

                is LoginEvent.LaunchTelService -> {}
                is LoginEvent.OpenUserAgreementPage ->
                    navController.value?.navigate("userAgreement")

                is LoginEvent.OpenRegisterAgreementPage ->
                    navController.value?.navigate("registerAgreement")
            }
        }
    }

    // 显示骨架屏或正常内容
    if (uiState.isLoading) {
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
                OperatorSection(uiState.operatorName + "认证")

                // 脱敏后手机号
                PhoneSection(uiState.phoneNumber)

                // 输入框
                InputSection(
                    uiState = uiState,
                    onAction = vm::onAction
                )

                // 1. 构建注册模式切换的 Transition
                val transition =
                    updateTransition(
                        targetState = uiState.isRegisterMode,
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
                    firstText = if (uiState.isRegisterMode) "注册" else "登录",
                    secondText = if (uiState.isRegisterMode) "返回登录" else "暂无账号，去注册",
                    onFirstClick = {
                        if (uiState.isRegisterMode) {
                            vm.onAction(
                                LoginAction.DoRegister(
                                    username = uiState.username,
                                    password = uiState.password,
                                    sessionId = uiState.sessionId,
                                    velCode = uiState.verifyCode
                                )
                            )
                        } else {
                            vm.onAction(LoginAction.DoLogin(uiState.username, uiState.password))
                        }
                    },
                    isFirstEnabled = uiState.isActionButtonEnabled,
                    onSecondClick = { vm.onAction(LoginAction.ToggleRegisterMode) },
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
                        operator = uiState.operatorName,
                        isChecked = uiState.isAgreementChecked,
                        onCheckedChange = { vm.onAction(LoginAction.ToggleAgreement(it)) },
                        onTelServiceClick = { vm.onAction(LoginAction.OpenTelService) },
                        onUserAgreementClick = { vm.onAction(LoginAction.OpenUserAgreement) },
                        onRegisterAgreementClick = { vm.onAction(LoginAction.OpenRegisterAgreement) },
                        modifier = Modifier
                            .offset(y = buttonOffset)
                            .padding(top = 16.wdp)
                    )
                }
            }
        }
    }
}