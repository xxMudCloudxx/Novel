package com.novel.page.home.usecase

import com.novel.core.domain.BaseUseCase
import com.novel.core.domain.ComposeUseCase
import com.novel.page.home.dao.HomeRepository
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.toEntity
import com.novel.page.home.viewmodel.CategoryInfo
import com.novel.page.home.viewmodel.HomeViewModel
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.network.cache.CacheStrategy
import com.novel.utils.network.repository.CachedBookRepository
import com.novel.utils.ReactNativeBridge
import com.novel.utils.TimberLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Home模块组合UseCase
 * 
 * 处理复杂的业务逻辑组合，整合所有Home相关的UseCase
 */
class HomeCompositeUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val cachedBookRepository: CachedBookRepository,
    private val composeUseCase: ComposeUseCase
) : BaseUseCase<HomeCompositeUseCase.Params, HomeCompositeUseCase.Result>() {
    
    companion object {
        private const val TAG = "HomeCompositeUseCase"
        private const val RECOMMEND_PAGE_SIZE = 8
    }
    
    data class Params(
        val loadInitialData: Boolean = false,
        val refreshData: Boolean = false,
        val categoryFilter: String? = null,
        val rankType: String? = null,
        val loadMoreRecommend: Boolean = false,
        val categoryFilters: List<CategoryInfo> = emptyList(),
        val currentPage: Int = 1
    )
    
    data class Result(
        val categories: List<HomeCategoryEntity> = emptyList(),
        val categoryFilters: List<CategoryInfo> = emptyList(),
        val carouselBooks: List<HomeBookEntity> = emptyList(),
        val hotBooks: List<HomeBookEntity> = emptyList(),
        val newBooks: List<HomeBookEntity> = emptyList(),
        val vipBooks: List<HomeBookEntity> = emptyList(),
        val rankBooks: List<BookService.BookRank> = emptyList(),
        val recommendBooks: List<SearchService.BookInfo> = emptyList(),
        val homeRecommendBooks: List<HomeService.HomeBook> = emptyList(),
        val hasMoreRecommend: Boolean = true,
        val totalPages: Int = 1,
        val isSuccess: Boolean = true,
        val errorMessage: String? = null
    )
    
    // 创建内部UseCase实例
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase by lazy {
        GetHomeCategoriesUseCase(homeRepository)
    }
    private val getHomeRecommendBooksUseCase: GetHomeRecommendBooksUseCase by lazy {
        GetHomeRecommendBooksUseCase(homeRepository)
    }
    private val getRankingBooksUseCase: GetRankingBooksUseCase by lazy {
        GetRankingBooksUseCase(homeRepository)
    }
    private val refreshHomeDataUseCase: RefreshHomeDataUseCase by lazy {
        RefreshHomeDataUseCase(homeRepository)
    }
    private val sendReactNativeDataUseCase: SendReactNativeDataUseCase by lazy {
        SendReactNativeDataUseCase()
    }
    private val getCategoryRecommendBooksUseCase: GetCategoryRecommendBooksUseCase by lazy {
        GetCategoryRecommendBooksUseCase(cachedBookRepository)
    }
    private val getCategoriesUseCase: GetCategoriesUseCase by lazy {
        GetCategoriesUseCase(homeRepository)
    }
    private val getBooksDataUseCase: GetBooksDataUseCase by lazy {
        GetBooksDataUseCase(homeRepository)
    }
    
    override suspend fun execute(params: Params): Result {
        TimberLogger.d(TAG, "开始执行组合操作: $params")
        
        return try {
            when {
                params.loadInitialData -> loadInitialData()
                params.refreshData -> refreshAllData()
                params.categoryFilter != null -> loadCategoryData(params.categoryFilter, params.currentPage, params.categoryFilters)
                params.rankType != null -> loadRankingData(params.rankType)
                params.loadMoreRecommend -> loadMoreRecommendData(params.currentPage)
                else -> Result(isSuccess = false, errorMessage = "未知操作类型")
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "组合操作执行失败", e)
            Result(isSuccess = false, errorMessage = e.message ?: "未知错误")
        }
    }
    
    /**
     * 加载初始数据
     */
    private suspend fun loadInitialData(): Result {
        TimberLogger.d(TAG, "加载初始数据")
        
        return try {
            // 使用并发方式获取数据，但对Flow类型的数据使用first()确保获取到实际数据
            val categoryFilters = try {
                getHomeCategoriesUseCase(
                    GetHomeCategoriesUseCase.Params()
                ).first() // 使用first()确保获取到数据
            } catch (e: Exception) {
                TimberLogger.e(TAG, "获取分类筛选器失败", e)
                listOf(CategoryInfo("0", "推荐")) // 返回默认数据
            }
            
            val categories = try {
                getCategoriesUseCase(
                    GetCategoriesUseCase.Params(forceRefresh = false)
                ).first() // 使用first()确保获取到数据
            } catch (e: Exception) {
                TimberLogger.e(TAG, "获取分类数据失败", e)
                emptyList()
            }
            
            val booksData = try {
                getBooksDataUseCase(
                    GetBooksDataUseCase.Params(forceRefresh = false)
                ).first() // 使用first()确保获取到数据
            } catch (e: Exception) {
                TimberLogger.e(TAG, "获取书籍数据失败", e)
                GetBooksDataUseCase.BooksData(emptyList(), emptyList(), emptyList(), emptyList())
            }
            
            val rankBooksResult = getRankingBooksUseCase(
                GetRankingBooksUseCase.Params(HomeRepository.RANK_TYPE_VISIT)
            )
            
            val homeRecommendResult = getHomeRecommendBooksUseCase(
                GetHomeRecommendBooksUseCase.Params()
            )
            
            // 转换书籍数据为Entity
            val carouselBooks = booksData.carouselBooks.map { it.toEntity("carousel") }
            val hotBooks = booksData.hotBooks.map { it.toEntity("hot") }
            val newBooks = booksData.newBooks.map { it.toEntity("new") }
            val vipBooks = booksData.vipBooks.map { it.toEntity("vip") }
            
            TimberLogger.d(TAG, "初始数据加载完成 - 分类筛选器数量: ${categoryFilters.size}, 分类数量: ${categories.size}")
            
            Result(
                categories = categories,
                categoryFilters = categoryFilters,
                carouselBooks = carouselBooks,
                hotBooks = hotBooks,
                newBooks = newBooks,
                vipBooks = vipBooks,
                rankBooks = rankBooksResult,
                homeRecommendBooks = homeRecommendResult,
                isSuccess = true
            )
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加载初始数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
    
    /**
     * 刷新所有数据
     */
    private suspend fun refreshAllData(): Result {
        TimberLogger.d(TAG, "刷新所有数据")
        
        return try {
            // 强制刷新
            refreshHomeDataUseCase(Unit)
            
            // 重新加载数据
            loadInitialData()
        } catch (e: Exception) {
            TimberLogger.e(TAG, "刷新数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
    
    /**
     * 加载分类数据
     */
    private suspend fun loadCategoryData(categoryName: String, page: Int, categoryFilters: List<CategoryInfo>): Result {
        TimberLogger.d(TAG, "加载分类数据: categoryName=$categoryName, page=$page")
        
        return try {
            if (categoryName == "推荐") {
                // 加载首页推荐
                val homeRecommendBooks = getHomeRecommendBooksUseCase(
                    GetHomeRecommendBooksUseCase.Params()
                )
                
                Result(
                    homeRecommendBooks = homeRecommendBooks,
                    hasMoreRecommend = homeRecommendBooks.size >= RECOMMEND_PAGE_SIZE,
                    isSuccess = true
                )
            } else {
                // 加载分类推荐 - 修复分类ID映射
                val categoryId = getCurrentCategoryId(categoryName, categoryFilters)
                TimberLogger.d(TAG, "分类名称映射: $categoryName -> $categoryId")
                
                val booksData = getCategoryRecommendBooksUseCase(
                    GetCategoryRecommendBooksUseCase.Params(
                        categoryId = categoryId,
                        pageNum = page,
                        pageSize = RECOMMEND_PAGE_SIZE
                    )
                )
                
                Result(
                    recommendBooks = booksData.list,
                    hasMoreRecommend = booksData.list.size >= RECOMMEND_PAGE_SIZE,
                    totalPages = booksData.pages.toInt(),
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加载分类数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
    
    /**
     * 根据分类名称获取分类ID
     */
    /**
     * 获取当前分类ID
     */
    private fun getCurrentCategoryId(categoryName: String, categoryFilters: List<CategoryInfo>): Int {

        // 打印调试信息
        TimberLogger.d(TAG, "获取分类ID - 分类名称: $categoryName")
        TimberLogger.d(TAG, "可用分类筛选器: ${categoryFilters.map { "${it.name}(${it.id})" }}")

        // 特殊处理推荐模式
        if (categoryName == "推荐") {
            TimberLogger.d(TAG, "推荐模式，返回categoryId: 0")
            return 0
        }

        // 查找对应的分类ID
        val categoryInfo = categoryFilters.find { it.name == categoryName }
        val categoryId = categoryInfo?.id?.toIntOrNull() ?: run {
            // 如果找不到，尝试用分类名称创建一个映射
            val mappedId = when (categoryName) {
                "玄幻奇幻" -> 1
                "武侠仙侠" -> 2
                "都市言情" -> 3
                "历史军情" -> 4
                "科幻灵异" -> 5
                "网游竞技" -> 6
                else -> 1 // 默认返回1
            }
            TimberLogger.w(TAG, "未找到分类 '$categoryName' 的ID，使用映射值: $mappedId")
            mappedId
        }

        TimberLogger.d(TAG, "分类 '$categoryName' 对应ID: $categoryId")
        return categoryId
    }
    
    /**
     * 加载榜单数据
     */
    private suspend fun loadRankingData(rankType: String): Result {
        TimberLogger.d(TAG, "加载榜单数据: rankType=$rankType")
        
        return try {
            val rankBooks = getRankingBooksUseCase(
                GetRankingBooksUseCase.Params(rankType)
            )
            
            Result(
                rankBooks = rankBooks,
                isSuccess = true
            )
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加载榜单数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
    
    /**
     * 加载更多推荐数据
     */
    private suspend fun loadMoreRecommendData(page: Int): Result {
        TimberLogger.d(TAG, "加载更多推荐数据: page=$page")
        
        return try {
            // 获取新的首页推荐数据（强制从网络获取以确保数据更新）
            val homeRecommendBooks = getHomeRecommendBooksUseCase(
                GetHomeRecommendBooksUseCase.Params(strategy = CacheStrategy.NETWORK_ONLY)
            )
            
            // 计算分页数据
            val startIndex = (page - 1) * RECOMMEND_PAGE_SIZE
            val endIndex = startIndex + RECOMMEND_PAGE_SIZE
            val pagedBooks = homeRecommendBooks.drop(startIndex).take(RECOMMEND_PAGE_SIZE)
            
            TimberLogger.d(TAG, "加载更多推荐数据完成 - 页码: $page, 获取数量: ${pagedBooks.size}, 总数据量: ${homeRecommendBooks.size}")
            
            Result(
                homeRecommendBooks = pagedBooks,
                hasMoreRecommend = endIndex < homeRecommendBooks.size,
                isSuccess = true
            )
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加载更多推荐数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
    
    /**
     * 加载更多分类推荐数据
     */
    private suspend fun loadMoreCategoryData(categoryId: Int, page: Int): Result {
        TimberLogger.d(TAG, "加载更多分类推荐数据: categoryId=$categoryId, page=$page")
        
        return try {
            val booksData = getCategoryRecommendBooksUseCase(
                GetCategoryRecommendBooksUseCase.Params(
                    categoryId = categoryId,
                    pageNum = page,
                    pageSize = RECOMMEND_PAGE_SIZE,
                    strategy = CacheStrategy.CACHE_FIRST
                )
            )
            
            TimberLogger.d(TAG, "加载更多分类推荐数据完成 - 分类ID: $categoryId, 页码: $page, 获取数量: ${booksData.list.size}")
            
            Result(
                recommendBooks = booksData.list,
                hasMoreRecommend = booksData.list.size >= RECOMMEND_PAGE_SIZE,
                totalPages = booksData.pages.toInt(),
                isSuccess = true
            )
        } catch (e: Exception) {
            TimberLogger.e(TAG, "加载更多分类推荐数据失败", e)
            Result(isSuccess = false, errorMessage = e.message)
        }
    }
}

/**
 * Home模块状态检查UseCase
 * 
 * 检查Home模块的各种状态
 */
class HomeStatusCheckUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) : BaseUseCase<HomeStatusCheckUseCase.Params, HomeStatusCheckUseCase.Result>() {
    
    companion object {
        private const val TAG = "HomeStatusCheckUseCase"
    }
    
    data class Params(
        val checkCategories: Boolean = true,
        val checkRecommend: Boolean = true,
        val checkRanking: Boolean = true
    )
    
    data class Result(
        val hasCategoriesData: Boolean,
        val hasRecommendData: Boolean,
        val hasRankingData: Boolean
    )
    
    override suspend fun execute(params: Params): Result {
        TimberLogger.d(TAG, "检查Home模块状态")
        
        return try {
            var hasCategoriesData = true
            var hasRecommendData = true
            var hasRankingData = true
            
            if (params.checkCategories) {
                // TODO: 检查分类数据状态
            }
            
            if (params.checkRecommend) {
                // TODO: 检查推荐数据状态
            }
            
            if (params.checkRanking) {
                // TODO: 检查榜单数据状态
            }
            
            Result(
                hasCategoriesData = hasCategoriesData,
                hasRecommendData = hasRecommendData,
                hasRankingData = hasRankingData
            )
        } catch (e: Exception) {
            TimberLogger.e(TAG, "状态检查失败", e)
            Result(
                hasCategoriesData = false,
                hasRecommendData = false,
                hasRankingData = false
            )
        }
    }
} 