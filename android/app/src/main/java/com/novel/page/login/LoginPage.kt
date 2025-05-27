package com.novel.page.login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelDivider
import com.novel.page.component.NovelImageView
import com.novel.page.component.NovelTextField
import com.novel.page.login.component.ActionButtons
import com.novel.page.login.component.AgreementSection
import com.novel.page.login.component.LoginAppBar
import com.novel.page.login.component.OperatorSection
import com.novel.page.login.component.PhoneSection
import com.novel.page.login.component.TitleSection
import com.novel.page.login.viewmodel.LoginAction
import com.novel.page.login.viewmodel.LoginEvent
import com.novel.page.login.viewmodel.LoginViewModel
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
import com.novel.utils.NavViewModel.navController
import com.novel.utils.wdp
import kotlinx.coroutines.flow.collectLatest

/**
 * 登录页面
 */
@Composable
fun LoginPage() {
    val vm: LoginViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = NovelColors.NovelBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginAppBar();
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.wdp)
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

            NovelTextField(
                value = username, // 绑定到状态
                onValueChange = { username = it }, // 更新状态
                modifier = Modifier
                    .height(45.wdp)
                    .width(329.wdp),
                placeText = "请输入手机号"
            )

            Spacer(modifier = Modifier.padding(top = 19.wdp))

            NovelTextField(
                value = password, // 绑定到状态
                onValueChange = { password = it }, // 状态更新
                modifier = Modifier
                    .height(45.wdp)
                    .width(329.wdp),
                placeText = "请输入密码",
                isPassword = true
            )

            Spacer(modifier = Modifier.padding(top = 19.wdp))

            NovelTextField(
                value = passwordConfirm, // 绑定到状态
                onValueChange = { passwordConfirm = it }, // 状态更新
                modifier = Modifier
                    .height(45.wdp)
                    .width(329.wdp),
                placeText = "再次输入密码",
                isPassword = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(top = 19.wdp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.5.wdp)
            ) {
                NovelTextField(
                    value = uiState.verifyCode,
                    onValueChange = { vm.onAction(LoginAction.InputVerifyCode(it)) },
                    modifier = Modifier
                        .height(45.wdp)
                        .width(180.wdp),
                    placeText = "输入验证码",
                    isPassword = false
                )
                Spacer(Modifier.width(8.wdp))
                Log.d(
                    "LoginPage",
                    "Rendering NovelImageView with verifyImage: ${uiState.verifyImage}"
                )
                NovelImageView(
                    imageUrl = uiState.verifyImage,
                    isLoading = uiState.isCaptchaLoading,
                    error = uiState.captchaError,
                    widthDp = 100,
                    heightDp = 45,
                    modifier = Modifier.clickable { vm.onAction(LoginAction.RefreshCaptcha) },
                    onRetry = { vm.onAction(LoginAction.RefreshCaptcha) }
                )
            }

            //  登录按钮
            ActionButtons(
                onOneClick = { vm.onAction(LoginAction.DoLogin(username, password)) },
                onOther = {}
            )

            //  协议
            AgreementSection(
                operator = uiState.operatorName,
                isChecked = uiState.isAgreementChecked,
                onCheckedChange = { vm.onAction(LoginAction.ToggleAgreement(it)) },
                onTelServiceClick = { vm.onAction(LoginAction.OpenTelService) },
                onUserAgreementClick = { vm.onAction(LoginAction.OpenUserAgreement) },
                onRegisterAgreementClick = { vm.onAction(LoginAction.OpenRegisterAgreement) }
            )
        }
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    NovelTheme {
        AdaptiveScreen {
            Box(modifier = Modifier.fillMaxSize()) {
                LoginPage()
            }
        }
    }
}