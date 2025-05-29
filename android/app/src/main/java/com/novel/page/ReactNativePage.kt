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
 * ReactNative 页面
 */
@SuppressLint("VisibleForTests")
@Composable
fun ReactNativePage() {
    val context = LocalContext.current
    val reactInstanceManager = remember {
        // 从 MainApplication 获取 ReactInstanceManager，确保单例
        (context.applicationContext as MainApplication).reactNativeHost.reactInstanceManager
    }
    var isContextReady by remember { mutableStateOf(reactInstanceManager.currentReactContext != null) }
    val isViewAttached by remember { mutableStateOf(false) }
    val isViewLaidOut by remember { mutableStateOf(false) }
    var reactRootView by remember { mutableStateOf<ReactRootView?>(null) }

    Log.d("ReactNativePage", "Composed ReactNativePage, initial isContextReady: $isContextReady")
    Log.d(
        "ReactNativePage",
        "Has started creating initial context: ${reactInstanceManager.hasStartedCreatingInitialContext()}"
    )

    val rootView = remember {
        Log.d("ReactNativePage", "Creating ReactRootView")
        Log.d("ReactNativePage", "Starting React application")
        ReactRootView(context).apply {
            setIsFabric(BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
            // 当 RN Context 已准备好时立刻启动
            if (reactInstanceManager.currentReactContext != null) {
                startReactApplication(
                    reactInstanceManager,
                    "Novel",
                    bundleOf("nativeMessage" to "sssss")
                )
            } else {
                reactInstanceManager.addReactInstanceEventListener(
                    object : ReactInstanceManager.ReactInstanceEventListener {
                        override fun onReactContextInitialized(context: ReactContext) {
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

    DisposableEffect(reactInstanceManager) {
        Log.d("ReactNativePage", "DisposableEffect started")
        if (!isContextReady) {
            Log.d("ReactNativePage", "Adding listener")
            // 添加监听器，等待 React 上下文初始化
            val listener = ReactInstanceManager.ReactInstanceEventListener { isContextReady = true }
            reactInstanceManager.addReactInstanceEventListener(listener)
            onDispose {
                Log.d("ReactNativePage", "Removing listener")
                // 清理监听器，防止内存泄漏
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    AndroidView(
        factory = { rootView },
        update = { view ->
            reactRootView = view
        },
        modifier = Modifier.fillMaxSize()
    )

//    LaunchedEffect(Unit) {
//        while (!isContextReady || !isViewAttached || !isViewLaidOut || reactRootView == null) {
//            delay(100) // 等待所有条件满足
//        }
    try {//        reactRootView!!.startReactApplication(reactInstanceManager, "Novel", null)
    } catch (e: Exception) {
        TODO("Not yet implemented")
    }
//    }

    if (!isContextReady || !isViewAttached || !isViewLaidOut) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}