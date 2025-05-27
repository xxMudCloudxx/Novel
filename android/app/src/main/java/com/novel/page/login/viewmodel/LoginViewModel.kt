package com.novel.page.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novel.utils.PhoneInfoUtil
import com.novel.utils.maskPhoneNumber
import com.novel.utils.network.api.front.user.LoginService
import com.novel.utils.network.api.front.user.UserInfoService
import com.novel.utils.network.TokenProvider
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// —— UI 状态 ——
data class LoginUiState(
    val isAgreementChecked: Boolean = false,
    val phoneNumber: String = "",
    val operatorName: String = ""
)

// —— 一次性事件 ——
sealed class LoginEvent {
    data class ShowToast(val message: String) : LoginEvent()
    data class Navigate(val route: String) : LoginEvent()
    data class LaunchTelService(val number: String) : LoginEvent()
    data object OpenUserAgreementPage : LoginEvent()
    data object OpenRegisterAgreementPage : LoginEvent()
}

// —— 所有用户操作 ——
sealed class LoginAction {
    data class DoLogin(val username: String, val password: String) : LoginAction()
    data object ToRegister : LoginAction()
    data object OpenTelService : LoginAction()
    data object OpenUserAgreement : LoginAction()
    data object OpenRegisterAgreement : LoginAction()
    data class ToggleAgreement(val checked: Boolean) : LoginAction()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginService: LoginService,
    private val userInfoService: UserInfoService,
    private val tokenProvider: TokenProvider,
    private val userDefaults: NovelUserDefaults,
    phoneInfoUtil: PhoneInfoUtil
) : ViewModel() {

    // 状态流
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 事件通道
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // 预加载手机号和运营商
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

    /** 统一入口，处理所有 UI 交互  */
    fun onAction(action: LoginAction) {
        viewModelScope.launch {
            when (action) {
                is LoginAction.ToggleAgreement -> {
                    _uiState.update { it.copy(isAgreementChecked = action.checked) }
                }
                LoginAction.OpenTelService -> {
                    // 假设运营商服务客服电话存在于 uiState.operatorName 对应的映射
                    val serviceNumber = mapOperatorToTel(uiState.value.operatorName)
                    _events.send(LoginEvent.LaunchTelService(serviceNumber))
                }
                LoginAction.OpenUserAgreement -> {
                    _events.send(LoginEvent.OpenUserAgreementPage)
                }
                LoginAction.OpenRegisterAgreement -> {
                    _events.send(LoginEvent.OpenRegisterAgreementPage)
                }
                is LoginAction.DoLogin -> {
                    performLogin(action.username, action.password)
                }
                LoginAction.ToRegister -> {
                    _events.send(LoginEvent.Navigate("register"))
                }
            }
        }
    }

    /** 登录逻辑复用函数  */
    private fun performLogin(username: String, password: String) {
        viewModelScope.launch {
            _events.send(LoginEvent.ShowToast("开始登录..."))
            try {
                val resp = withContext(Dispatchers.IO) {
                    loginService.loginBlocking(LoginService.LoginRequest(username, password))
                }
                Log.d("LoginVM", "loginBlocking 返回: $resp")
                if (resp.code == 200 && resp.data != null) {
                    val data = resp.data
                    // 存 token、过期时间、uid
                    tokenProvider.saveToken(data.token, "")
                    userDefaults.set(System.currentTimeMillis() + 3600L * 1000, NovelUserDefaultsKey.TOKEN_EXPIRES_AT)
                    userDefaults.set(true, NovelUserDefaultsKey.IS_LOGGED_IN)
                    userDefaults.set(data.uid, NovelUserDefaultsKey.USER_ID)
                    // 异步拿用户详情
                    launch(Dispatchers.IO) { runCatching { userInfoService.getUserInfoBlocking() } }
                    _events.send(LoginEvent.ShowToast("登录成功"))
                    _events.send(LoginEvent.Navigate("main"))
                } else {
                    val msg = resp.msg.orEmpty().ifBlank { "登录失败" }
                    _events.send(LoginEvent.ShowToast(msg))
                }
            } catch (e: Exception) {
                _events.send(LoginEvent.ShowToast("网络异常：${e.localizedMessage}"))
            }
        }
    }

    /** 根据运营商名称映射客服电话，示例写法  */
    private fun mapOperatorToTel(name: String): String = when (name) {
        "移动" -> "10086"
        "联通" -> "10010"
        "电信" -> "10000"
        else    -> "10000"
    }
}
