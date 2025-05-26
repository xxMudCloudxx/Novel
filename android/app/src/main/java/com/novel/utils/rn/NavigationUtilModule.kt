package com.novel.utils.rn

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import android.os.Handler
import android.os.Looper
import com.novel.utils.NavViewModel

class NavigationUtilModule(ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {

    override fun getName(): String = "NavigationUtil"

    @ReactMethod
    fun goToLogin() {
        // 确保在主线程执行导航
        Handler(Looper.getMainLooper()).post {
            NavViewModelHolder.navController.value?.navigate("login")
        }
    }
}

// 利用单例持有 ViewModel，避免跨包引用问题
object NavViewModelHolder {
    val navController = NavViewModel.navController
}