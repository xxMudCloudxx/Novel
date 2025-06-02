package com.novel.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novel.page.MainPage
import com.novel.page.login.LoginPage
import com.novel.page.book.BookDetailPage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * 导航设置 - 简化版本，翻书动画在HomePage内部处理
 */
@Composable
fun NavigationSetup() {
    // 创建 NavController
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        NavViewModel.navController.value = navController
    }

    NavHost(
        navController = NavViewModel.navController.value ?: navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainPage()
        }
        composable("login") {
            LoginPage()
        }
        composable("book_detail/{bookId}?fromRank={fromRank}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val fromRank = backStackEntry.arguments?.getString("fromRank")?.toBoolean() ?: false
            BookDetailPage(
                bookId = bookId,
                fromRank = fromRank
            )
        }
    }
}

/**
 * 返回事件数据类
 */
data class BackNavigationEvent(
    val fromRoute: String,
    val bookId: String? = null,
    val fromRank: Boolean = false
)

object NavViewModel : ViewModel() {
    val navController = MutableLiveData<NavHostController>()
    
    // 返回事件流
    private val _backNavigationEvents = MutableSharedFlow<BackNavigationEvent>(replay = 0)
    val backNavigationEvents: SharedFlow<BackNavigationEvent> = _backNavigationEvents.asSharedFlow()
    
    // 当前书籍信息（用于返回动画）
    private var currentBookInfo: Pair<String, Boolean>? = null
    
    /**
     * 导航到书籍详情页
     * @param bookId 书籍ID
     * @param fromRank 是否来自榜单（用于识别但不影响动画，动画在HomePage处理）
     */
    fun navigateToBookDetail(bookId: String, fromRank: Boolean = false) {
        Log.d("NavViewModel", "===== 导航到书籍详情页 =====")
        Log.d("NavViewModel", "bookId: $bookId")
        Log.d("NavViewModel", "fromRank: $fromRank")
        
        // 记录当前书籍信息
        currentBookInfo = bookId to fromRank
        Log.d("NavViewModel", "保存书籍信息: $currentBookInfo")
        
        navController.value?.navigate("book_detail/$bookId?fromRank=$fromRank")
        Log.d("NavViewModel", "✅ 导航命令已发送")
        Log.d("NavViewModel", "==============================")
    }
    
    /**
     * 导航到登录页
     */
    fun navigateToLogin() {
        navController.value?.navigate("login")
    }
    
    /**
     * 返回
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun navigateBack() {
        navController.value?.popBackStack()
    }
}