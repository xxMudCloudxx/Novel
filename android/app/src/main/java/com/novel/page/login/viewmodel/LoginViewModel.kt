package com.novel.page.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.utils.NavViewModel
import com.novel.utils.PhoneInfoUtil
import com.novel.utils.maskPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页面 ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val phoneInfoUtil: PhoneInfoUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val info = phoneInfoUtil.fetch()
            _uiState.update {
                it.copy(
                    phoneNumber = maskPhoneNumber(info.phoneNumber),
                    operatorName = info.operatorName
                )
            }
        }
    }

    fun onAgreementToggled(checked: Boolean) {
        _uiState.update { it.copy(isAgreementChecked = checked) }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OpenTelService -> {
                NavViewModel.navController.value?.popBackStack()
            }
            is LoginAction.OpenUserAgreement -> { /* 同上 */ }
            is LoginAction.OpenRegisterAgreement -> { /* 同上 */ }
        }
    }
}

data class LoginUiState(
    val isAgreementChecked: Boolean = false,
    val phoneNumber: String = "",     // 脱敏后号码
    val operatorName: String = ""     // 运营商
)

sealed class LoginAction {
    data object OpenTelService : LoginAction()
    data object OpenUserAgreement : LoginAction()
    data object OpenRegisterAgreement : LoginAction()
}
