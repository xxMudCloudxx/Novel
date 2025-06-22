package com.novel.utils.rn

import android.util.Log
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * React Native导航模块包
 * 
 * 提供Android原生导航功能的RN桥接模块
 * 包含导航工具模块，支持页面跳转、返回等导航操作
 * 
 * 特性：
 * - 提供NavigationUtilModule原生模块
 * - 支持RN侧调用Android原生导航API
 * - 无自定义ViewManager组件
 */
class NavigationPackage : ReactPackage {
    
    companion object {
        private const val TAG = "NavigationPackage"
    }
    
    /**
     * 创建原生模块列表
     * 
     * @param reactContext RN应用上下文
     * @return 原生模块列表，包含NavigationUtilModule
     */
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        Log.d(TAG, "创建导航原生模块")
        
        return listOf(NavigationUtilModule(reactContext)).also { modules ->
            Log.d(TAG, "导航模块创建完成，包含${modules.size}个模块")
        }
    }

    /**
     * 创建ViewManager列表
     * 
     * @param reactContext RN应用上下文
     * @return 空列表，本包不提供自定义ViewManager
     */
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        Log.v(TAG, "导航包无自定义ViewManager")
        return emptyList()
    }
}