package com.novel.page.home.dao

import com.novel.utils.TimberLogger
import com.novel.utils.network.repository.CachedBookRepository
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
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页数据仓库类
 * 
 * 职责：
 * - 协调网络数据和本地缓存的访问
 * - 实现缓存优先的数据获取策略
 * - 统一管理首页相关的所有数据源
 * - 提供一致的错误处理和重试机制
 * 
 * 数据来源：
 * - 本地Room数据库（离线优先）
 * - 网络API（实时数据）
 * - 内存缓存（性能优化）
 * 
 * @param homeDao 本地数据访问对象
 * @param homeService 首页网络服务
 * @param cachedBookRepository 带缓存的书籍数据仓库
 * @param cacheManager 网络缓存管理器
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
        
        /** 榜单类型常量 - 点击榜（按阅读量排序） */
        const val RANK_TYPE_VISIT = "点击榜"
        
        /** 榜单类型常量 - 更新榜（按更新时间排序） */
        const val RANK_TYPE_UPDATE = "更新榜"
        
        /** 榜单类型常量 - 新书榜（按发布时间排序） */
        const val RANK_TYPE_NEWEST = "新书榜"
        
        /** 榜单书籍显示数量限制 - 控制界面展示的书籍数量 */
        const val RANK_BOOK_LIMIT = 16
    }
    
    // region 榜单相关 - 使用缓存策略
    
    /**
     * 获取榜单书籍数据
     * 
     * 实现缓存优先策略：
     * 1. 首先尝试从本地缓存获取数据
     * 2. 缓存无效或过期时从网络加载
     * 3. 网络请求失败时降级到缓存数据
     * 4. 自动处理新书榜的特殊重试逻辑
     * 
     * @param rankType 榜单类型（点击榜/更新榜/新书榜）
     * @param strategy 缓存策略（默认缓存优先）
     * @return 榜单书籍列表，最多包含RANK_BOOK_LIMIT本书
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
                        TimberLogger.w(TAG, "新书榜首次加载失败，清理缓存后重试: ${e.message}")
                        // 清理新书榜缓存
                        cachedBookRepository.clearNewestRankCache()
                        // 使用网络优先策略重试
                        cachedBookRepository.getNewestRankBooks(CacheStrategy.NETWORK_ONLY)
                    }
                }
                else -> {
                    TimberLogger.w(TAG, "未知榜单类型: $rankType, 使用默认点击榜")
                    cachedBookRepository.getVisitRankBooks(strategy)
                }
            }.take(RANK_BOOK_LIMIT)
        } catch (e: Exception) {
            TimberLogger.e(TAG, "获取榜单数据失败: $rankType", e)
            emptyList()
        }
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
                onCacheUpdate = {
                    TimberLogger.d(TAG, "首页推荐数据缓存已更新")
                }
            ).onSuccess { _, fromCache ->
                // 首页推荐数据加载成功
            }.onError { error, _ ->
                TimberLogger.e(TAG, "首页推荐数据加载失败", error)
            }.let { result ->
                when (result) {
                    is com.novel.utils.network.cache.CacheResult.Success -> result.data.data ?: emptyList()
                    is com.novel.utils.network.cache.CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "获取首页推荐数据异常", e)
            emptyList()
        }
    }
    
    /**
     * 获取友情链接 - 缓存优先
     */
    private suspend fun getFriendLinks(
        strategy: CacheStrategy = CacheStrategy.CACHE_FIRST
    ): List<HomeService.FriendLink> {
        return try {
            homeService.getFriendLinksCached(
                cacheManager = cacheManager,
                strategy = strategy,
                onCacheUpdate = {
                    TimberLogger.d(TAG, "友情链接缓存已更新")
                }
            ).onSuccess { _, fromCache ->
                // 友情链接加载成功
            }.onError { error, _ ->
                TimberLogger.e(TAG, "友情链接加载失败", error)
            }.let { result ->
                when (result) {
                    is com.novel.utils.network.cache.CacheResult.Success -> result.data.data ?: emptyList()
                    is com.novel.utils.network.cache.CacheResult.Error -> result.cachedData?.data ?: emptyList()
                }
            }
        } catch (e: Exception) {
            TimberLogger.e(TAG, "获取友情链接异常", e)
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
    }.catch { e ->
        TimberLogger.e(TAG, "获取书籍分类失败", e)
        // 发射空列表作为最后的备选
        emit(emptyList())
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
            TimberLogger.e(TAG, "获取轮播图书籍失败", e)
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
            TimberLogger.e(TAG, "获取热门书籍失败", e)
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
            TimberLogger.e(TAG, "获取最新书籍失败", e)
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
            TimberLogger.e(TAG, "获取VIP书籍失败", e)
            emit(emptyList())
        }
    }
    
    // region 缓存管理
    
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
            TimberLogger.e(TAG, "强制刷新数据失败", e)
            Triple(emptyList(), emptyList(), emptyList())
        }
    }
} 