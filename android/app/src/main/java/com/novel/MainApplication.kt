package com.novel

import android.app.Application
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import com.novel.ui.theme.ThemeManager
import com.novel.utils.network.RetrofitClient
import com.novel.utils.network.TokenProvider
import com.novel.utils.network.interceptor.AuthInterceptor
import com.novel.utils.rn.NavigationPackage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 主应用类
 * 
 * 架构特点：
 * - Hilt依赖注入管理
 * - React Native新架构支持
 * - 全局单例组件初始化
 * 
 * 初始化流程：
 * 1. ThemeManager全局主题管理
 * 2. RetrofitClient网络服务
 * 3. SoLoader原生库加载
 * 4. React Native引擎初始化
 */
@HiltAndroidApp
class MainApplication : Application(), ReactApplication {
    
    companion object {
        private const val TAG = "MainApplication"
        private var instance: MainApplication? = null
        
        fun getInstance(): MainApplication? = instance
    }

    @Inject
    lateinit var authInterceptor: AuthInterceptor

    @Inject
    lateinit var tokenProvider: TokenProvider
    
    @Inject
    lateinit var settingsUtils: com.novel.utils.SettingsUtils

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                    add(NavigationPackage())
                }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        }

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "===== MainApplication 初始化开始 =====")
        
        instance = this
        
        // 初始化全局主题管理器
        Log.d(TAG, "初始化ThemeManager...")
        ThemeManager.initialize(this)
        
        // 初始化网络服务
        Log.d(TAG, "初始化RetrofitClient...")
        RetrofitClient.init(
            authInterceptor = authInterceptor,
            tokenProvider = tokenProvider
        )
        
        // 初始化SoLoader
        Log.d(TAG, "初始化SoLoader...")
        SoLoader.init(this, OpenSourceMergedSoMapping)
        
        // 启用新架构支持
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            Log.d(TAG, "启用React Native新架构...")
            load()
        }
        
        Log.d(TAG, "✅ MainApplication 初始化完成")
        Log.d(TAG, "====================================")
    }
}
