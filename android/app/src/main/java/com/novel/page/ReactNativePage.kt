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
 * React Native 页面容器组件
 * 
 * 使用缓存的ReactRootView实例，避免重复创建导致状态丢失
 * 支持页面状态保持和快速切换
 * 在RN上下文就绪时主动同步主题信息
 */
@SuppressLint("VisibleForTests")
@Composable
fun ReactNativePage() {
    
    val TAG = "ReactNativePage"
    val context = LocalContext.current
    val mainApplication = context.applicationContext as MainApplication
    
    // 跟踪RN上下文初始化状态
    val reactInstanceManager = remember { mainApplication.reactNativeHost.reactInstanceManager }
    var isContextReady by remember { mutableStateOf(reactInstanceManager.currentReactContext != null) }

    Log.d(TAG, "组件渲染 - isContextReady: $isContextReady")

    // 获取缓存的ReactRootView实例
    val rootView = remember {
        Log.d(TAG, "获取缓存的ReactRootView")
        mainApplication.getOrCreateReactRootView(
            "Novel",
            bundleOf("nativeMessage" to "ProfilePage")
        )
    }

    // 管理RN上下文监听器的生命周期
    DisposableEffect(reactInstanceManager) {
        Log.d(TAG, "DisposableEffect启动")
        
        val contextListener = if (!isContextReady) {
            Log.d(TAG, "添加RN上下文监听器")
            ReactInstanceManager.ReactInstanceEventListener { 
                Log.d(TAG, "RN上下文状态变更为就绪")
                isContextReady = true 
                
                // 🎯 RN上下文就绪时，主动同步主题信息
                syncThemeToRN()
            }.also { listener ->
                reactInstanceManager.addReactInstanceEventListener(listener)
            }
        } else {
            // 如果RN上下文已经就绪，直接同步主题
            syncThemeToRN()
            null
        }
        
        onDispose {
            contextListener?.let { listener ->
                Log.d(TAG, "移除RN上下文监听器，防止内存泄漏")
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        }
    }

    // 使用AndroidView嵌入缓存的ReactRootView
    AndroidView(
        factory = { 
            Log.d(TAG, "AndroidView factory返回缓存的ReactRootView")
            rootView 
        },
        modifier = Modifier.fillMaxSize()
    )

    // 在RN未就绪时显示加载指示器
    if (!isContextReady) {
        Log.v(TAG, "显示加载指示器")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

/**
 * 同步当前主题信息到RN端
 */
private fun syncThemeToRN() {
    try {
        Log.d("ReactNativePage", "🎯 开始同步主题信息到RN")
        val themeManager = ThemeManager.getInstance()
        val actualTheme = themeManager.getCurrentActualThemeMode()
        Log.d("ReactNativePage", "当前实际主题: $actualTheme")
        
        // 通过ThemeManager发送主题变更事件到RN
        themeManager.notifyThemeChangedToRN(actualTheme)
        Log.d("ReactNativePage", "✅ 主题信息已同步到RN: $actualTheme")
    } catch (e: Exception) {
        Log.e("ReactNativePage", "❌ 同步主题信息到RN失败", e)
    }
}