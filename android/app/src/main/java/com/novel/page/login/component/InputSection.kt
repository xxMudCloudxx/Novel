package com.novel.page.login.component

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.novel.page.component.NovelImageView
import com.novel.page.component.NovelTextField
import com.novel.page.login.viewmodel.LoginAction
import com.novel.page.login.viewmodel.LoginUiState
import com.novel.utils.wdp

// 动画常量
private object AnimationConfig {
    const val DURATION_MS = 600
    const val DELAY_MS = 200
}

/**
 * 输入区域，包含用户名、密码、确认密码和验证码
 */
@Composable
fun InputSection(
    uiState: LoginUiState,
    onAction: (LoginAction) -> Unit
) {
    // 1. 构建注册模式切换的 Transition
    val transition =
        updateTransition(targetState = uiState.isRegisterMode, label = "registerTransition")

    // 2. 弹性位移动画：从 -40.wdp 到 0.wdp，下滑时带弹性回弹
    val sectionOffset by transition.animateDp(
        transitionSpec = {
            spring(
                stiffness = Spring.StiffnessLow,              // 更柔软的弹簧
                dampingRatio = Spring.DampingRatioMediumBouncy // 适度的回弹
            )
        },
        label = "sectionOffset"
    ) { state ->
        if (state) 0.wdp else (-40).wdp
    }

    // 3. 透明度动画：用 tween 控制总时长，并加一点延迟
    val sectionAlpha by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 800,
                delayMillis = 200,
                easing = FastOutSlowInEasing
            )
        },
        label = "sectionAlpha"
    ) { state ->
        if (state) 1f else 0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.5.wdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NovelTextField(
            value = uiState.username,
            onValueChange = { onAction(LoginAction.InputUsername(it)) },
            modifier = Modifier
                .height(45.wdp)
                .width(329.wdp),
            placeText = "请输入手机号",
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError
        )

        Spacer(modifier = Modifier.height(19.wdp))

        NovelTextField(
            value = uiState.password,
            onValueChange = { onAction(LoginAction.InputPassword(it)) },
            modifier = Modifier
                .height(45.wdp)
                .width(329.wdp),
            placeText = "请输入密码",
            isPassword = true,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError
        )
        if (uiState.isRegisterMode)
            AnimatedVisibility(
                visible = uiState.isRegisterMode,
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
                Column(
                    modifier = Modifier
                        .offset(y = sectionOffset)
                        .alpha(sectionAlpha)
                ) {
                    Spacer(modifier = Modifier.height(19.wdp))

                    NovelTextField(
                        value = uiState.passwordConfirm,
                        onValueChange = { onAction(LoginAction.InputPasswordConfirm(it)) },
                        modifier = Modifier
                            .height(45.wdp)
                            .width(329.wdp),
                        placeText = "再次输入密码",
                        isPassword = true,
                        isError = uiState.passwordConfirmError != null,
                        errorMessage = uiState.passwordConfirmError
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(top = 19.wdp)
                            .fillMaxWidth()
                    ) {
                        NovelTextField(
                            value = uiState.verifyCode,
                            onValueChange = { onAction(LoginAction.InputVerifyCode(it)) },
                            modifier = Modifier
                                .height(45.wdp)
                                .width(180.wdp),
                            placeText = "输入验证码",
                            isError = uiState.verifyCodeError != null,
                            errorMessage = uiState.verifyCodeError
                        )
                        Spacer(modifier = Modifier.width(8.wdp))
                        NovelImageView(
                            imageUrl = uiState.verifyImage,
                            isLoading = uiState.isCaptchaLoading,
                            error = uiState.captchaError,
                            widthDp = 100,
                            heightDp = 45,
                            modifier = Modifier.clickable { onAction(LoginAction.RefreshCaptcha) },
                            onRetry = { onAction(LoginAction.RefreshCaptcha) }
                        )
                    }
                }
            }
    }
}