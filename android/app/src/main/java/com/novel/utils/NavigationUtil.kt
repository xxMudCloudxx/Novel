package com.novel.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novel.page.MainPage

@Composable
fun NavigationSetup() {
    // 创建 NavController
    val navController = rememberNavController()
//
//    val authVm: AuthViewModel = hiltViewModel()
//
//    // 立即读取一次 isTokenValid，决定起点
//    val start = remember { if (authVm.isTokenValid) "main" else "login" }


    LaunchedEffect(navController) {
        NavViewModel.navController.value = navController
    }
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainPage()
        }
    }
}

object NavViewModel : ViewModel() {
    val navController = MutableLiveData<NavController>()
}