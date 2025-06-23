package com.novel.page

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import com.facebook.react.ReactInstanceManager
import com.novel.MainApplication
import com.novel.ui.theme.ThemeManager

/**
 * 通用React Native 页面容器组件
 * 
 * 使用缓存的ReactRootView实例，避免重复创建导致状态丢失
 * 支持页面状态保持和快速切换
 * 在RN上下文就绪时主动同步主题信息
 * 
 * @param componentName RN组件名称，决定加载哪个RN页面
 * @param initialProps 传递给RN组件的初始属性
 */
@SuppressLint("VisibleForTests")
@Composable
fun ReactNativePage(
    componentName: String = "Novel",
    initialProps: Map<String, Any> = mapOf("nativeMessage" to "ProfilePage")
) {
    
    val TAG = "ReactNativePage"
    val context = LocalContext.current
    val mainApplication = context.applicationContext as MainApplication
    
    // 跟踪RN上下文初始化状态
    val reactInstanceManager = remember { mainApplication.reactNativeHost.reactInstanceManager }
    var isContextReady by remember { mutableStateOf(reactInstanceManager.currentReactContext != null) }

    Log.d(TAG, "组件渲染 - componentName: $componentName, isContextReady: $isContextReady")

    // 获取缓存的ReactRootView实例
    val rootView = remember(componentName, initialProps) {
        Log.d(TAG, "获取缓存的ReactRootView for $componentName")
        
        // 将Map转换为Bundle
        val bundle = bundleOf().apply {
            initialProps.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        
        mainApplication.getOrCreateReactRootView(componentName, bundle)
    }

    // 管理RN上下文监听器的生命周期
    DisposableEffect(reactInstanceManager, componentName) {
        Log.d(TAG, "DisposableEffect启动 for $componentName")
        
        val contextListener = if (!isContextReady) {
            Log.d(TAG, "添加RN上下文监听器 for $componentName")
            ReactInstanceManager.ReactInstanceEventListener { 
                Log.d(TAG, "RN上下文状态变更为就绪 for $componentName")
                isContextReady = true 
                
                // 🎯 RN上下文就绪时，主动同步主题信息
                syncThemeToRN(componentName)
            }.also { listener ->
                reactInstanceManager.addReactInstanceEventListener(listener)
            }
        } else {
            // 如果RN上下文已经就绪，直接同步主题
            syncThemeToRN(componentName)
            null
        }
        
        onDispose {
            contextListener?.let { listener ->
                Log.d(TAG, "移除RN上下文监听器，防止内存泄漏 for $componentName")
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        }
    }

    // 使用AndroidView嵌入缓存的ReactRootView
    AndroidView(
        factory = { 
            Log.d(TAG, "AndroidView factory返回缓存的ReactRootView for $componentName")
            rootView 
        },
        modifier = Modifier.fillMaxSize()
    )

    // 在RN未就绪时显示加载指示器
    if (!isContextReady) {
        Log.v(TAG, "显示加载指示器 for $componentName")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

/**
 * 同步当前主题信息到RN端
 * @param componentName 组件名称，用于日志标识
 */
private fun syncThemeToRN(componentName: String) {
    try {
        Log.d("ReactNativePage", "🎯 开始同步主题信息到RN for $componentName")
        val themeManager = ThemeManager.getInstance()
        val actualTheme = themeManager.getCurrentActualThemeMode()
        Log.d("ReactNativePage", "当前实际主题: $actualTheme for $componentName")
        
        // 通过ThemeManager发送主题变更事件到RN
        themeManager.notifyThemeChangedToRN(actualTheme)
        Log.d("ReactNativePage", "✅ 主题信息已同步到RN: $actualTheme for $componentName")
    } catch (e: Exception) {
        Log.e("ReactNativePage", "❌ 同步主题信息到RN失败 for $componentName", e)
    }
}