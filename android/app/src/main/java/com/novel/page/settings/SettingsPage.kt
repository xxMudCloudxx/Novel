package com.novel.page.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.facebook.react.ReactRootView
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.ReactContext
import android.util.Log
import androidx.core.os.bundleOf
import com.novel.BuildConfig
import com.novel.MainApplication
import com.novel.page.component.BackButton
import com.novel.page.component.NovelText
import com.novel.ui.theme.NovelColors
import com.novel.utils.ssp
import com.novel.utils.wdp

/**
 * Android 设置页面
 * 上方显示导航栏（返回按钮 + 设置标题）
 * 下方嵌入RN的SettingsPage组件
 */
@Composable
fun SettingsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovelColors.NovelBackground)
    ) {
        // 顶部导航栏
        SettingsTopBar()
        
        // RN设置页面内容
        ReactNativeSettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

@Composable
private fun SettingsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.wdp, vertical = 16.wdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        BackButton()
        
        // 设置标题
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            NovelText(
                text = "设置",
                fontSize = 18.ssp,
                fontWeight = FontWeight.Bold,
                color = NovelColors.NovelText
            )
        }
        
        // 右侧占位，保持标题居中
        Spacer(modifier = Modifier.width(23.wdp))
    }
}

@SuppressLint("VisibleForTests")
@Composable
private fun ReactNativeSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 仿照ReactNativePage的实现
    val reactInstanceManager = remember {
        (context.applicationContext as MainApplication).reactNativeHost.reactInstanceManager
    }
    
    var isContextReady by remember { 
        mutableStateOf(reactInstanceManager.currentReactContext != null) 
    }
    
    Log.d("SettingsPage", "Composed SettingsPage, initial isContextReady: $isContextReady")
    Log.d("SettingsPage", "Has started creating initial context: ${reactInstanceManager.hasStartedCreatingInitialContext()}")

    val rootView = remember {
        Log.d("SettingsPage", "Creating ReactRootView for Settings")
        ReactRootView(context).apply {
            setIsFabric(BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
            
            // 当 RN Context 已准备好时立刻启动
            if (reactInstanceManager.currentReactContext != null) {
                Log.d("SettingsPage", "Context ready, starting SettingsPageComponent immediately")
                startReactApplication(
                    reactInstanceManager,
                    "SettingsPageComponent",
                    bundleOf("source" to "android_settings")
                )
            } else {
                Log.d("SettingsPage", "Context not ready, adding listener")
                reactInstanceManager.addReactInstanceEventListener(
                    object : ReactInstanceManager.ReactInstanceEventListener {
                        override fun onReactContextInitialized(context: ReactContext) {
                            Log.d("SettingsPage", "Context initialized, starting SettingsPageComponent")
                            startReactApplication(
                                reactInstanceManager,
                                "SettingsPageComponent",
                                bundleOf("source" to "android_settings")
                            )
                            reactInstanceManager.removeReactInstanceEventListener(this)
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(reactInstanceManager) {
        Log.d("SettingsPage", "DisposableEffect started")
        if (!isContextReady) {
            Log.d("SettingsPage", "Adding context ready listener")
            val listener = ReactInstanceManager.ReactInstanceEventListener { 
                isContextReady = true 
                Log.d("SettingsPage", "Context ready listener triggered")
            }
            reactInstanceManager.addReactInstanceEventListener(listener)
            onDispose {
                Log.d("SettingsPage", "Removing context ready listener")
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    // 使用AndroidView嵌入ReactRootView，仿照ReactNativePage的方式
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { rootView },
        modifier = modifier
    )

    // 如果上下文还没准备好，显示加载状态
    if (!isContextReady) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            NovelText(
                text = "加载中...",
                fontSize = 16.ssp,
                color = NovelColors.NovelText
            )
        }
    }
} 