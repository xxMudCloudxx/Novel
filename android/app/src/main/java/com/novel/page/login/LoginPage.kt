package com.novel.page.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.novel.page.component.NovelDivider
import com.novel.page.login.component.ActionButtons
import com.novel.page.login.component.AgreementSection
import com.novel.page.login.component.LoginAppBar
import com.novel.page.login.component.OperatorSection
import com.novel.page.login.component.PhoneSection
import com.novel.page.login.component.TitleSection
import com.novel.page.login.viewmodel.LoginAction
import com.novel.page.login.viewmodel.LoginViewModel
import com.novel.ui.theme.NovelColors
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
import com.novel.utils.wdp

/**
 * 登录页面
 * @param viewModel 登录页面的 ViewModel
 */
@Composable
fun LoginPage(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

            //  登录按钮
            ActionButtons(
                onOneClick = {},
                onOther = {}
            )

            //  协议
            AgreementSection(
                operator = uiState.operatorName,
                isChecked = uiState.isAgreementChecked,
                onCheckedChange = { viewModel.onAgreementToggled(it) },
                onTelServiceClick = { viewModel.onAction(LoginAction.OpenTelService) },
                onUserAgreementClick = { viewModel.onAction(LoginAction.OpenUserAgreement) },
                onRegisterAgreementClick = { viewModel.onAction(LoginAction.OpenRegisterAgreement) }
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