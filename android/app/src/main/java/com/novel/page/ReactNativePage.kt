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
 * 负责在Compose中嵌入React Native页面，处理RN上下文初始化和页面渲染
 * 包含加载状态管理和实例监听器的生命周期管理
 */
@SuppressLint("VisibleForTests")
@Composable
fun ReactNativePage() {
    
    val TAG = "ReactNativePage"
    val context = LocalContext.current
    
    // 从 MainApplication 获取单例的 ReactInstanceManager
    val reactInstanceManager = remember {
        Log.d(TAG, "获取ReactInstanceManager实例")
        (context.applicationContext as MainApplication).reactNativeHost.reactInstanceManager
    }
    
    // 跟踪RN上下文初始化状态
    var isContextReady by remember { mutableStateOf(reactInstanceManager.currentReactContext != null) }
    val isViewAttached by remember { mutableStateOf(false) }
    val isViewLaidOut by remember { mutableStateOf(false) }
    var reactRootView by remember { mutableStateOf<ReactRootView?>(null) }

    Log.d(TAG, "组件渲染 - isContextReady: $isContextReady")
    Log.d(TAG, "已开始创建初始上下文: ${reactInstanceManager.hasStartedCreatingInitialContext()}")

    // 创建ReactRootView并配置应用启动
    val rootView = remember {
        Log.d(TAG, "创建ReactRootView")
        ReactRootView(context).apply {
            // 设置Fabric架构支持
            setIsFabric(BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
            Log.d(TAG, "Fabric架构已启用: ${BuildConfig.IS_NEW_ARCHITECTURE_ENABLED}")
            
            // 根据RN上下文状态决定启动时机
            if (reactInstanceManager.currentReactContext != null) {
                Log.d(TAG, "RN上下文已就绪，立即启动应用")
                startReactApplication(
                    reactInstanceManager,
                    "Novel",
                    bundleOf("nativeMessage" to "sssss")
                )
            } else {
                Log.d(TAG, "等待RN上下文初始化完成")
                reactInstanceManager.addReactInstanceEventListener(
                    object : ReactInstanceManager.ReactInstanceEventListener {
                        override fun onReactContextInitialized(context: ReactContext) {
                            Log.d(TAG, "RN上下文初始化完成，启动应用")
                            startReactApplication(
                                reactInstanceManager,
                                "Novel",
                                bundleOf("nativeMessage" to "sssss")
                            )
                            reactInstanceManager.removeReactInstanceEventListener(this)
                        }
                    }
                )
            }
        }
    }

    // 管理RN上下文监听器的生命周期
    DisposableEffect(reactInstanceManager) {
        Log.d(TAG, "DisposableEffect启动")
        if (!isContextReady) {
            Log.d(TAG, "添加RN上下文监听器")
            val listener = ReactInstanceManager.ReactInstanceEventListener { 
                Log.d(TAG, "RN上下文状态变更为就绪")
                isContextReady = true 
            }
            reactInstanceManager.addReactInstanceEventListener(listener)
            onDispose {
                Log.d(TAG, "移除RN上下文监听器，防止内存泄漏")
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    // 使用AndroidView嵌入ReactRootView
    AndroidView(
        factory = { 
            Log.d(TAG, "AndroidView factory执行")
            rootView 
        },
        update = { view ->
            reactRootView = view
            Log.v(TAG, "ReactRootView已更新")
        },
        modifier = Modifier.fillMaxSize()
    )

    // 注释掉的LaunchedEffect代码保留，用于未来可能的延迟启动逻辑
//    LaunchedEffect(Unit) {
//        while (!isContextReady || !isViewAttached || !isViewLaidOut || reactRootView == null) {
//            delay(100) // 等待所有条件满足
//        }
    try {
//        reactRootView!!.startReactApplication(reactInstanceManager, "Novel", null)
    } catch (e: Exception) {
        Log.e(TAG, "ReactNative应用启动异常", e)
        // TODO: 实现异常处理逻辑
    }
//    }

    // 在RN未就绪时显示加载指示器
    if (!isContextReady || !isViewAttached || !isViewLaidOut) {
        Log.v(TAG, "显示加载指示器")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}