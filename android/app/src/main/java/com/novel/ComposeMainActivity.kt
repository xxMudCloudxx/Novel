package com.novel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.novel.page.component.ImageLoaderService
import com.novel.page.component.LocalImageLoaderService
import com.novel.ui.theme.NovelTheme
import com.novel.utils.AdaptiveScreen
import com.novel.utils.NavigationSetup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var imageLoaderService: ImageLoaderService
    
    private val rim by lazy {
        (application as MainApplication).reactNativeHost.reactInstanceManager
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rim.createReactContextInBackground()
        setContent {
            NovelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalImageLoaderService provides imageLoaderService
                    ) {
                        AdaptiveScreen {
                            Box(modifier = Modifier.fillMaxSize()) {
                                NavigationSetup()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rim.onHostResume(this)                         // 传递 Activity
    }

    override fun onPause() {
        super.onPause()
        rim.onHostPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        rim.onHostDestroy(this)
    }
}
