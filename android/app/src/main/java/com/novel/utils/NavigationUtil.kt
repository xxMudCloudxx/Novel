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
    }
}

object NavViewModel : ViewModel() {
    val navController = MutableLiveData<NavHostController>()
}