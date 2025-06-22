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
 * 下方嵌入缓存的RN SettingsPage组件，确保状态一致性
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
    val TAG = "SettingsPage"
    val context = LocalContext.current
    val mainApplication = context.applicationContext as MainApplication
    
    // 获取 ReactInstanceManager
    val reactInstanceManager = remember { mainApplication.reactNativeHost.reactInstanceManager }
    
    var isContextReady by remember { 
        mutableStateOf(reactInstanceManager.currentReactContext != null) 
    }
    
    Log.d(TAG, "组件渲染 - isContextReady: $isContextReady")

    // 获取缓存的ReactRootView实例
    val rootView = remember {
        Log.d(TAG, "获取缓存的ReactRootView for SettingsPageComponent")
        mainApplication.getOrCreateReactRootView(
            "SettingsPageComponent",
            bundleOf("source" to "android_settings")
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
                Log.d(TAG, "移除RN上下文监听器")
                reactInstanceManager.removeReactInstanceEventListener(listener)
            }
        }
    }

    // 使用AndroidView嵌入缓存的ReactRootView
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { 
            Log.d(TAG, "AndroidView factory返回缓存的ReactRootView")
            rootView 
        },
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