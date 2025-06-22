package com.novel

import android.app.Application
import android.os.Bundle
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
import com.facebook.react.ReactRootView
import androidx.core.os.bundleOf
import java.util.concurrent.ConcurrentHashMap
import com.facebook.react.bridge.ReactContext
import com.facebook.react.ReactInstanceManager

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

    // 添加ReactRootView缓存管理
    private val reactRootViewCache = ConcurrentHashMap<String, ReactRootView>()
    
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

    /**
     * 获取或创建ReactRootView实例
     * @param componentName React组件名称
     * @param initialProps 初始属性
     * @return 缓存的或新创建的ReactRootView
     */
    fun getOrCreateReactRootView(
        componentName: String, 
        initialProps: Bundle? = null
    ): ReactRootView {
        return reactRootViewCache.getOrPut(componentName) {
            Log.d("MainApplication", "创建新的ReactRootView: $componentName")
            ReactRootView(this).apply {
                setIsFabric(BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
                
                val rim = reactNativeHost.reactInstanceManager
                if (rim.currentReactContext != null) {
                    Log.d("MainApplication", "立即启动React应用: $componentName")
                    startReactApplication(rim, componentName, initialProps)
                } else {
                    Log.d("MainApplication", "等待React上下文初始化: $componentName")
                    rim.addReactInstanceEventListener(
                        object : ReactInstanceManager.ReactInstanceEventListener {
                            override fun onReactContextInitialized(context: ReactContext) {
                                Log.d("MainApplication", "React上下文就绪，启动应用: $componentName")
                                startReactApplication(rim, componentName, initialProps)
                                rim.removeReactInstanceEventListener(this)
                            }
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 清理指定的ReactRootView缓存
     */
    fun clearReactRootViewCache(componentName: String) {
        reactRootViewCache.remove(componentName)?.let {
            Log.d("MainApplication", "清理ReactRootView缓存: $componentName")
        }
    }
    
    /**
     * 清理所有ReactRootView缓存
     */
    fun clearAllReactRootViewCache() {
        reactRootViewCache.clear()
        Log.d("MainApplication", "清理所有ReactRootView缓存")
    }
}
