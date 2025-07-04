package com.novel.page.home.viewmodel

import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService

/**
 * Home状态适配器
 * 将新的HomeState转换为UI层期望的HomeUiState格式
 * 确保UI层完全兼容，无需修改现有组件
 */
class HomeStateAdapter {
    
    /**
     * 将HomeState转换为HomeUiState
     */
    fun toHomeUiState(homeState: HomeState): HomeUiState {
        return HomeUiState(
            // 基础状态
            version = homeState.version,
            isLoading = homeState.isLoading,
            error = homeState.error,
            isRefreshing = homeState.isRefreshing,
            
            // 分类数据相关
            categories = homeState.categories,
            categoryLoading = homeState.categoryLoading,
            
            // 书籍推荐数据相关
            carouselBooks = homeState.carouselBooks,
            hotBooks = homeState.hotBooks,
            newBooks = homeState.newBooks,
            vipBooks = homeState.vipBooks,
            booksLoading = homeState.booksLoading,
            
            // 搜索相关
            searchQuery = homeState.searchQuery,
            
            // 分类筛选器状态
            selectedCategoryFilter = homeState.selectedCategoryFilter,
            categoryFilters = homeState.categoryFilters,
            categoryFiltersLoading = homeState.categoryFiltersLoading,
            
            // 榜单状态
            selectedRankType = homeState.selectedRankType,
            rankBooks = homeState.rankBooks,
            rankLoading = homeState.rankLoading,
            
            // 推荐书籍状态
            recommendBooks = homeState.recommendBooks,
            homeRecommendBooks = homeState.homeRecommendBooks,
            recommendLoading = homeState.recommendLoading,
            hasMoreRecommend = homeState.hasMoreRecommend,
            recommendPage = homeState.recommendPage,
            totalRecommendPages = homeState.totalRecommendPages,
            
            // 首页推荐分页状态
            homeRecommendLoading = homeState.homeRecommendLoading,
            hasMoreHomeRecommend = homeState.hasMoreHomeRecommend,
            homeRecommendPage = homeState.homeRecommendPage,
            
            // 显示模式控制
            isRecommendMode = homeState.isRecommendMode
        )
    }
    
    /**
     * 将HomeUiState转换为HomeState（如果需要的话）
     */
    fun toHomeState(homeUiState: HomeUiState): HomeState {
        return HomeState(
            version = homeUiState.version,
            isLoading = homeUiState.isLoading,
            error = homeUiState.error,
            
            isRefreshing = homeUiState.isRefreshing,
            
            // 分类数据相关
            categories = homeUiState.categories,
            categoryLoading = homeUiState.categoryLoading,
            
            // 书籍推荐数据相关
            carouselBooks = homeUiState.carouselBooks,
            hotBooks = homeUiState.hotBooks,
            newBooks = homeUiState.newBooks,
            vipBooks = homeUiState.vipBooks,
            booksLoading = homeUiState.booksLoading,
            
            // 搜索相关
            searchQuery = homeUiState.searchQuery,
            
            // 分类筛选器状态
            selectedCategoryFilter = homeUiState.selectedCategoryFilter,
            categoryFilters = homeUiState.categoryFilters,
            categoryFiltersLoading = homeUiState.categoryFiltersLoading,
            
            // 榜单状态
            selectedRankType = homeUiState.selectedRankType,
            rankBooks = homeUiState.rankBooks,
            rankLoading = homeUiState.rankLoading,
            
            // 推荐书籍状态
            recommendBooks = homeUiState.recommendBooks,
            homeRecommendBooks = homeUiState.homeRecommendBooks,
            recommendLoading = homeUiState.recommendLoading,
            hasMoreRecommend = homeUiState.hasMoreRecommend,
            recommendPage = homeUiState.recommendPage,
            totalRecommendPages = homeUiState.totalRecommendPages,
            
            // 首页推荐分页状态
            homeRecommendLoading = homeUiState.homeRecommendLoading,
            hasMoreHomeRecommend = homeUiState.hasMoreHomeRecommend,
            homeRecommendPage = homeUiState.homeRecommendPage,
            
            // 显示模式控制
            isRecommendMode = homeUiState.isRecommendMode
        )
    }
}

/**
 * 原有的HomeUiState数据类
 * 保持UI层兼容性，避免大规模重构
 */
data class HomeUiState(
    // 基础状态
    val version: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    
    // 分类数据相关
    val categories: List<HomeCategoryEntity> = emptyList(),
    val categoryLoading: Boolean = false,
    
    // 书籍推荐数据相关
    val carouselBooks: List<HomeBookEntity> = emptyList(),
    val hotBooks: List<HomeBookEntity> = emptyList(),
    val newBooks: List<HomeBookEntity> = emptyList(),
    val vipBooks: List<HomeBookEntity> = emptyList(),
    val booksLoading: Boolean = false,
    
    // 搜索相关
    val searchQuery: String = "",
    
    // 分类筛选器状态
    val selectedCategoryFilter: String = "推荐",
    val categoryFilters: List<CategoryInfo> = listOf(CategoryInfo("0", "推荐")),
    val categoryFiltersLoading: Boolean = false,
    
    // 榜单状态
    val selectedRankType: String = "点击榜",
    val rankBooks: List<BookService.BookRank> = emptyList(),
    val rankLoading: Boolean = false,
    
    // 推荐书籍状态
    val recommendBooks: List<SearchService.BookInfo> = emptyList(),
    val homeRecommendBooks: List<HomeService.HomeBook> = emptyList(),
    val recommendLoading: Boolean = false,
    val hasMoreRecommend: Boolean = true,
    val recommendPage: Int = 1,
    val totalRecommendPages: Int = 1,
    
    // 首页推荐分页状态
    val homeRecommendLoading: Boolean = false,
    val hasMoreHomeRecommend: Boolean = true,
    val homeRecommendPage: Int = 1,
    
    // 显示模式控制
    val isRecommendMode: Boolean = true
)

// CategoryInfo已在HomeMvi.kt中定义，此处删除重复定义

/**
 * Home页面的Action定义（保持原有定义以确保兼容性）
 */
sealed class HomeAction {
    data object LoadInitialData : HomeAction()
    data object RefreshData : HomeAction()
    data class UpdateSearchQuery(val query: String) : HomeAction()
    data class SelectCategoryFilter(val categoryName: String) : HomeAction()
    data class SelectRankType(val rankType: String) : HomeAction()
    data object LoadMoreRecommend : HomeAction()
    data object LoadMoreHomeRecommend : HomeAction()
    data class NavigateToSearch(val query: String) : HomeAction()
    data class NavigateToBookDetail(val bookId: Long) : HomeAction()
    data class NavigateToCategory(val categoryId: Long) : HomeAction()
    data class NavigateToFullRanking(val rankType: String) : HomeAction()
    data object RestoreData : HomeAction()
    data object ClearError : HomeAction()
}

/**
 * Home页面的Event定义（保持原有定义以确保兼容性）
 */
sealed class HomeEvent {
    data class NavigateToBook(val bookId: Long) : HomeEvent()
    data class NavigateToCategory(val categoryId: Long) : HomeEvent()
    data class NavigateToSearch(val query: String = "") : HomeEvent()
    data object NavigateToCategoryPage : HomeEvent()
    data class ShowToast(val message: String) : HomeEvent()
    data class NavigateToBookDetail(val bookId: Long) : HomeEvent()
    data class NavigateToFullRanking(val rankType: String) : HomeEvent()
    data class SendToReactNative(val data: Any) : HomeEvent()
} 