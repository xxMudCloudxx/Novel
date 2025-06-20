package com.novel

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.novel.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class MainActivity : ReactActivity() {

    /**
     * Returns the name of the main component registered from JavaScript. This is used to schedule
     * rendering of the component.
     */
    override fun getMainComponentName(): String = "Novel"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 监听主题变更并应用到Activity
        lifecycleScope.launch {
            val themeManager = ThemeManager.getInstance()
            themeManager.isDarkMode.collect { isDarkMode ->
                // 这里可以添加应用级别的主题变更逻辑
                // 例如状态栏、导航栏的颜色调整
            }
        }
    }

    /**
     * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
     * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
     */
    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return object : DefaultReactActivityDelegate(
            this,
            mainComponentName,
            fabricEnabled
        ) {
            // ★ 在这里 override，而不是在 Activity 本身
            override fun getLaunchOptions(): Bundle {
                // 启动时就发送 nativeMessage
                return Bundle().apply {
                    putString(
                        "nativeMessage",
                        intent?.getStringExtra("nativeMessage") ?: "默认消息"
                    )
                }
            }
        }
    }
}
