package com.novel.page.home.dao

import android.util.Log
import com.novel.repository.CachedBookRepository
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.cache.NetworkCacheManager
import com.novel.utils.network.cache.CacheStrategy
import com.novel.utils.network.cache.getHomeBookseCached
import com.novel.utils.network.cache.getFriendLinksCached
import com.novel.utils.network.cache.onSuccess
import com.novel.utils.network.cache.onError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页数据仓库
 * 负责协调网络数据和本地缓存，集成缓存优先策略
 */
@Singleton
class HomeRepository @Inject constructor(
    private val homeDao: HomeDao,
    private val homeService: HomeService,
    private val cachedBookRepository: CachedBookRepository,
    private val cacheManager: NetworkCacheManager
) {
    
    companion object {
        private const val TAG = "HomeRepository"
        
        // 书籍类型常量
        const val TYPE_CAROUSEL = "carousel"
        const val TYPE_HOT = "hot"
        const val TYPE_NEW = "new"
        const val TYPE_VIP = "vip"
        const val TYPE_WEEKLY = "weekly"
        
        // 榜单类型常量
        const val RANK_TYPE_VISIT = "点击榜"
        const val RANK_TYPE_UPDATE = "更新榜"
        const val RANK_TYPE_NEWEST = "新书榜"
        
        // 榜单显示数量
        const val RANK_BOOK_LIMIT = 16
    }
    
    // region 榜单相关 - 使用缓存策略
    
    /**
     * 获取榜单书籍数据 - 缓存优先
     */
    suspend fun getRankBooks(
        rankType: String,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<com.novel.utils.network.api.front.BookService.BookRank> {
        return try {
            when (rankType) {
                RANK_TYPE_VISIT -> cachedBookRepository.getVisitRankBooks(strategy)
                RANK_TYPE_UPDATE -> cachedBookRepository.getUpdateRankBooks(strategy)
                RANK_TYPE_NEWEST -> {
                    try {
                        cachedBookRepository.getNewestRankBooks(strategy)
                    } catch (e: Exception) {
                        Log.w(TAG, "新书榜首次加载失败，清理缓存后重试: ${e.message}")
                        // 清理新书榜缓存
                        cachedBookRepository.clearNewestRankCache()
                        // 使用网络优先策略重试
                        cachedBookRepository.getNewestRankBooks(CacheStrategy.NETWORK_ONLY)
                    }
                }
                else -> {
                    Log.w(TAG, "未知榜单类型: $rankType, 使用默认点击榜")
                    cachedBookRepository.getVisitRankBooks(strategy)
                }
            }.take(RANK_BOOK_LIMIT)
        } catch (e: Exception) {
            Log.e(TAG, "获取榜单数据失败: $rankType", e)
            emptyList()
        }
    }
    
    /**
     * 清理榜单缓存
     */
    suspend fun clearRankCache() {
        cachedBookRepository.clearRankCache()
        Log.d(TAG, "榜单缓存已清理")
    }
    
    // region 首页推荐数据 - 使用缓存策略
    
    /**
     * 获取首页推荐书籍 - 缓存优先
     */
    suspend fun getHomeBooks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<HomeService.HomeBook> {
        return try {
            homeService.getHomeBookseCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    Log.d(TAG, "首页推荐数据缓存已更新")
                }
            ).onSuccess { response, fromCache ->
                Log.d(TAG, "首页推荐数据加载成功，来源: ${if (fromCache) "缓存" else "网络"}")
            }.onError { error, cachedData ->
                Log.e(TAG, "首页推荐数据加载失败", error)
            }.let { result ->
                when (result) {
                    is com.novel.utils.network.cache.CacheResult.Success -> result.data.data ?: emptyList()
                    is com.novel.utils.network.cache.CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取首页推荐数据异常", e)
            emptyList()
        }
    }
    
    /**
     * 获取友情链接 - 缓存优先
     */
    suspend fun getFriendLinks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<HomeService.FriendLink> {
        return try {
            homeService.getFriendLinksCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = { response ->
                    Log.d(TAG, "友情链接缓存已更新")
                }
            ).onSuccess { response, fromCache ->
                Log.d(TAG, "友情链接加载成功，来源: ${if (fromCache) "缓存" else "网络"}")
            }.onError { error, cachedData ->
                Log.e(TAG, "友情链接加载失败", error)
            }.let { result ->
                when (result) {
                    is com.novel.utils.network.cache.CacheResult.Success -> result.data.data ?: emptyList()
                    is com.novel.utils.network.cache.CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取友情链接异常", e)
            emptyList()
        }
    }
    
    // region 书籍分类 - 使用缓存策略
    
    /**
     * 获取书籍分类 - 缓存优先
     */
    fun getBookCategories(
        workDirection: Int = 0,
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): Flow<List<com.novel.utils.network.api.front.BookService.BookCategory>> = flow {
        try {
            // 先从本地数据库查询
            val localCategories = homeDao.getCategories().first()
            
            if (localCategories.isNotEmpty()) {
                // 转换为API格式
                val apiCategories = localCategories.map { entity ->
                    com.novel.utils.network.api.front.BookService.BookCategory(
                        id = entity.id,
                        name = entity.name
                    )
                }
                emit(apiCategories)
            }
            
            // 使用缓存策略获取网络数据
            val networkCategories = cachedBookRepository.getBookCategories(
                workDirection = workDirection,
                strategy = strategy
            )
            
            if (networkCategories.isNotEmpty()) {
                emit(networkCategories)
                
                // 更新本地数据库
                val entities = networkCategories.map { category ->
                    HomeCategoryEntity(
                        id = category.id,
                        name = category.name,
                        iconUrl = null,
                        sortOrder = 0
                    )
                }
                homeDao.insertCategories(entities)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "获取书籍分类失败", e)
            // 发射空列表作为最后的备选
            emit(emptyList())
        }
    }
    
    // region 书籍数据获取方法
    
    /**
     * 获取分类数据
     */
    fun getCategories(forceRefresh: Boolean = false): Flow<List<com.novel.utils.network.api.front.BookService.BookCategory>> {
        val strategy = if (forceRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
        return getBookCategories(workDirection = 0, strategy = strategy)
    }
    
    /**
     * 获取轮播图书籍
     */
    fun getCarouselBooks(forceRefresh: Boolean = false): Flow<List<HomeService.HomeBook>> = flow {
        try {
            val strategy = if (forceRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
            val books = getHomeBooks(strategy)
            emit(books.filter { it.type == 0 }) // 轮播图类型
        } catch (e: Exception) {
            Log.e(TAG, "获取轮播图书籍失败", e)
            emit(emptyList())
        }
    }
    
    /**
     * 获取热门书籍
     */
    fun getHotBooks(forceRefresh: Boolean = false): Flow<List<HomeService.HomeBook>> = flow {
        try {
            val strategy = if (forceRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
            val books = getHomeBooks(strategy)
            emit(books.filter { it.type == 3 }) // 热门推荐类型
        } catch (e: Exception) {
            Log.e(TAG, "获取热门书籍失败", e)
            emit(emptyList())
        }
    }
    
    /**
     * 获取最新书籍
     */
    fun getNewBooks(forceRefresh: Boolean = false): Flow<List<HomeService.HomeBook>> = flow {
        try {
            val strategy = if (forceRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
            val books = getHomeBooks(strategy)
            emit(books.filter { it.type == 4 }) // 精品推荐类型
        } catch (e: Exception) {
            Log.e(TAG, "获取最新书籍失败", e)
            emit(emptyList())
        }
    }
    
    /**
     * 获取VIP书籍
     */
    fun getVipBooks(forceRefresh: Boolean = false): Flow<List<HomeService.HomeBook>> = flow {
        try {
            val strategy = if (forceRefresh) CacheStrategy.NETWORK_ONLY else CacheStrategy.CACHE_FIRST
            val books = getHomeBooks(strategy)
            emit(books.filter { it.type == 2 }) // 本周强推类型
        } catch (e: Exception) {
            Log.e(TAG, "获取VIP书籍失败", e)
            emit(emptyList())
        }
    }
    
    // region 缓存管理
    
    /**
     * 清理所有首页相关缓存
     */
    suspend fun clearAllCache() {
        try {
            cacheManager.clearCache("home_books")
            cacheManager.clearCache("friend_links")
            cachedBookRepository.clearRankCache()
            Log.d(TAG, "所有首页缓存已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理缓存失败", e)
        }
    }
    
    /**
     * 强制刷新所有数据（绕过缓存）
     */
    suspend fun refreshAllData(): Triple<
        List<HomeService.HomeBook>,
        List<HomeService.FriendLink>, 
        List<com.novel.utils.network.api.front.BookService.BookCategory>
    > {
        return try {
            val homeBooks = getHomeBooks(CacheStrategy.NETWORK_ONLY)
            val friendLinks = getFriendLinks(CacheStrategy.NETWORK_ONLY)
            val categories = cachedBookRepository.getBookCategories(
                workDirection = 0,
                strategy = CacheStrategy.NETWORK_ONLY
            )
            
            Triple(homeBooks, friendLinks, categories)
        } catch (e: Exception) {
            Log.e(TAG, "强制刷新数据失败", e)
            Triple(emptyList(), emptyList(), emptyList())
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    suspend fun isCacheAvailable(): Boolean {
        return try {
            cacheManager.isCacheExists("home_books") &&
            cacheManager.isCacheExists("visit_rank_books")
        } catch (e: Exception) {
            Log.e(TAG, "检查缓存状态失败", e)
            false
        }
    }
} 