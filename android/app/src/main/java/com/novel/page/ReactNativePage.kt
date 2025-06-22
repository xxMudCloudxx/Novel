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
import com.facebook.react.ReactRootView
import com.facebook.react.bridge.ReactContext
import com.novel.BuildConfig
import com.novel.MainApplication

/**
 * React Native 页面容器组件
 * 
 * 使用缓存的ReactRootView实例，避免重复创建导致状态丢失
 * 支持页面状态保持和快速切换
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
            }.also { listener ->
                reactInstanceManager.addReactInstanceEventListener(listener)
            }
        } else null
        
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