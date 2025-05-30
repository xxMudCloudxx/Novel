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

/**
 * 导航设置
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
        composable("book_detail/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailPage(bookId = bookId)
        }
    }
}

object NavViewModel : ViewModel() {
    val navController = MutableLiveData<NavHostController>()
    
    /**
     * 导航到书籍详情页
     */
    fun navigateToBookDetail(bookId: String) {
        navController.value?.navigate("book_detail/$bookId")
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
    fun navigateBack() {
        navController.value?.popBackStack()
    }
}