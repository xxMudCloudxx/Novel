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
import com.novel.page.read.ReaderPage
import com.novel.page.search.SearchPage
import com.novel.page.search.SearchResultPage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import com.novel.page.component.FlipBookAnimationController
import com.novel.page.search.FullRankingPage

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
        composable("search?query={query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchPage(
                onNavigateBack = {
                    NavViewModel.navigateBack()
                },
                onNavigateToBookDetail = { bookId ->
                    NavViewModel.navigateToBookDetail(bookId.toString(), fromRank = false)
                }
            )
        }
        composable("search_result?query={query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchResultPage(
                initialQuery = query
            )
        }
        composable("full_ranking/{rankingType}/{encodedData}") { backStackEntry ->
            val rankingType = backStackEntry.arguments?.getString("rankingType") ?: ""
            val encodedData = backStackEntry.arguments?.getString("encodedData") ?: ""
            
            // 解码榜单数据
            val rankingItems = try {
                NavViewModel.decodeRankingData(encodedData)
            } catch (e: Exception) {
                emptyList()
            }
            
            FullRankingPage(
                rankingType = rankingType,
                rankingItems = rankingItems,
                onNavigateBack = {
                    NavViewModel.navigateBack()
                },
                onNavigateToBookDetail = { bookId ->
                    NavViewModel.navigateToBookDetail(bookId.toString(), fromRank = true)
                }
            )
        }
        composable("book_detail/{bookId}?fromRank={fromRank}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val fromRank = backStackEntry.arguments?.getString("fromRank")?.toBoolean() ?: false
            BookDetailPage(
                bookId = bookId,
                fromRank = fromRank,
                onNavigateToReader = { bookId, chapterId ->
                    NavViewModel.navigateToReader(bookId, chapterId)
                }
            )
        }
        composable("reader/{bookId}?chapterId={chapterId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            ReaderPage(
                bookId = bookId,
                chapterId = chapterId
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
    
    /** 供 BookDetailPage 与 ReaderPage 共享的临时动画控制器 */
    var flipBookController: FlipBookAnimationController? = null
        private set

    fun setFlipBookController(controller: FlipBookAnimationController?) {
        flipBookController = controller
    }
    
    /**
     * 导航到搜索页面
     * @param query 搜索关键词（可选）
     */
    fun navigateToSearch(query: String = "") {
        Log.d("NavViewModel", "===== 导航到搜索页面 =====")
        Log.d("NavViewModel", "query: $query")
        
        val route = if (query.isNotBlank()) {
            "search?query=$query"
        } else {
            "search"
        }
        
        navController.value?.navigate(route)
        Log.d("NavViewModel", "✅ 导航到搜索页面命令已发送")
        Log.d("NavViewModel", "==============================")
    }
    
    /**
     * 导航到搜索结果页面
     * @param query 搜索关键词
     */
    fun navigateToSearchResult(query: String) {
        Log.d("NavViewModel", "===== 导航到搜索结果页面 =====")
        Log.d("NavViewModel", "query: $query")
        
        val route = "search_result?query=$query"
        navController.value?.navigate(route)
        
        Log.d("NavViewModel", "✅ 导航到搜索结果页面命令已发送")
        Log.d("NavViewModel", "==============================")
    }
    
    /**
     * 导航到完整榜单页面
     * @param rankingType 榜单类型
     * @param rankingItems 榜单数据
     */
    fun navigateToFullRanking(rankingType: String, rankingItems: List<com.novel.page.search.component.SearchRankingItem>) {
        Log.d("NavViewModel", "===== 导航到完整榜单页面 =====")
        Log.d("NavViewModel", "rankingType: $rankingType")
        Log.d("NavViewModel", "rankingItems count: ${rankingItems.size}")
        
        val encodedData = encodeRankingData(rankingItems)
        val route = "full_ranking/$rankingType/$encodedData"
        navController.value?.navigate(route)
        
        Log.d("NavViewModel", "✅ 导航到完整榜单页面命令已发送")
        Log.d("NavViewModel", "==============================")
    }
    
    /**
     * 编码榜单数据为URL安全的字符串
     */
    private fun encodeRankingData(items: List<com.novel.page.search.component.SearchRankingItem>): String {
        return try {
            val json = android.util.Base64.encodeToString(
                items.joinToString("|") { "${it.id},${it.title},${it.author},${it.rank}" }.toByteArray(),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )
            java.net.URLEncoder.encode(json, "UTF-8")
        } catch (e: Exception) {
            Log.e("NavViewModel", "编码榜单数据失败", e)
            ""
        }
    }
    
    /**
     * 解码榜单数据
     */
    fun decodeRankingData(encodedData: String): List<com.novel.page.search.component.SearchRankingItem> {
        return try {
            val decodedJson = java.net.URLDecoder.decode(encodedData, "UTF-8")
            val decodedBytes = android.util.Base64.decode(decodedJson, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val dataString = String(decodedBytes)
            
            dataString.split("|").mapNotNull { itemString ->
                val parts = itemString.split(",")
                if (parts.size >= 4) {
                    com.novel.page.search.component.SearchRankingItem(
                        id = parts[0].toLongOrNull() ?: 0L,
                        title = parts[1],
                        author = parts[2],
                        rank = parts[3].toIntOrNull() ?: 0
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("NavViewModel", "解码榜单数据失败", e)
            emptyList()
        }
    }
    
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
     * 导航到阅读器页面
     * @param bookId 书籍ID
     * @param chapterId 章节ID（可选）
     */
    fun navigateToReader(bookId: String, chapterId: String? = null) {
        Log.d("NavViewModel", "===== 导航到阅读器页面 =====")
        Log.d("NavViewModel", "bookId: $bookId")
        Log.d("NavViewModel", "chapterId: $chapterId")
        
        val route = if (chapterId != null) {
            "reader/$bookId?chapterId=$chapterId"
        } else {
            "reader/$bookId"
        }
        
        navController.value?.navigate(route)
        Log.d("NavViewModel", "✅ 导航到阅读器命令已发送")
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