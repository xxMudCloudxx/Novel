package com.novel.page.home.dao

import android.util.Log
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.HomeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页数据仓库
 * 负责协调网络数据和本地缓存
 */
@Singleton
class HomeRepository @Inject constructor(
    private val homeDao: HomeDao,
    private val homeService: HomeService,
    private val bookService: BookService  // 添加BookService依赖
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
    
    // region 榜单相关
    
    /**
     * 获取榜单书籍数据
     */
    fun getRankBooks(rankType: String): Flow<List<BookService.BookRank>> = flow {
        try {
            Log.d(TAG, "开始获取榜单数据：$rankType")
            
            val response = when (rankType) {
                RANK_TYPE_VISIT -> bookService.getVisitRankBooksBlocking()
                RANK_TYPE_UPDATE -> bookService.getUpdateRankBooksBlocking()
                RANK_TYPE_NEWEST -> bookService.getNewestRankBooksBlocking()
                else -> bookService.getVisitRankBooksBlocking()
            }
            
            if (response.ok == true && response.data != null) {
                // 限制显示16本书
                val limitedBooks = response.data.take(RANK_BOOK_LIMIT)
                emit(limitedBooks)
                Log.d(TAG, "$rankType 数据获取成功，共${limitedBooks.size}本书")
            } else {
                Log.e(TAG, "$rankType 数据获取失败：${response.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 $rankType 数据异常", e)
            emit(emptyList())
        }
    }
    
    /**
     * 获取分类数据
     */
    fun getBookCategories(): Flow<List<BookService.BookCategory>> = flow {
        try {
            Log.d(TAG, "开始获取分类数据")
            
            val response = bookService.getBookCategoriesBlocking(0)
            if (response.ok == true && response.data != null) {
                emit(response.data)
                Log.d(TAG, "分类数据获取成功，共${response.data.size}个分类")
            } else {
                Log.e(TAG, "分类数据获取失败：${response.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取分类数据异常", e)
            emit(emptyList())
        }
    }
    
    // endregion

    // region 书籍相关
    
    /**
     * 获取轮播推荐书籍 (type=0)
     */
    fun getCarouselBooks(forceRefresh: Boolean = false): Flow<List<HomeBookEntity>> = flow {
        try {
            // 先发射本地缓存数据
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_CAROUSEL)
            if (cachedBooks.isNotEmpty() && !forceRefresh) {
                emit(cachedBooks)
            }
            
            // 使用首页书籍接口
            val response = homeService.getHomeBooksBlocking()
            if (response.ok == true && response.data != null) {
                // 过滤type为0的书籍（轮播图）
                val books = response.data.filter { it.type == 0 }
                val entities = books.map { homeBook ->
                    HomeBookEntity(
                        id = homeBook.bookId,
                        title = homeBook.bookName,
                        author = homeBook.authorName,
                        coverUrl = homeBook.picUrl,
                        description = homeBook.bookDesc,
                        category = "",
                        categoryId = 0,
                        rating = 0.0,
                        readCount = 0,
                        wordCount = 0,
                        commentCount = 0,
                        isCompleted = false,
                        isVip = false,
                        updateTime = System.currentTimeMillis(),
                        lastChapterName = null,
                        lastChapterUpdateTime = null,
                        type = TYPE_CAROUSEL
                    )
                }
                
                // 更新本地缓存
                homeDao.deleteBooksByType(TYPE_CAROUSEL)
                homeDao.insertBooks(entities)
                
                // 发射最新数据
                emit(entities)
                Log.d(TAG, "轮播书籍数据更新成功，共${entities.size}本")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "获取轮播书籍失败", e)
            // 异常时尝试使用缓存
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_CAROUSEL)
            if (cachedBooks.isNotEmpty()) {
                emit(cachedBooks)
            }
        }
    }
    
    /**
     * 获取热门推荐书籍 (type=3)
     */
    fun getHotBooks(forceRefresh: Boolean = false): Flow<List<HomeBookEntity>> = flow {
        try {
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_HOT)
            if (cachedBooks.isNotEmpty() && !forceRefresh) {
                emit(cachedBooks)
            }
            
            // 使用首页书籍接口
            val response = homeService.getHomeBooksBlocking()
            if (response.ok == true && response.data != null) {
                // 过滤type为3的书籍（热门推荐）
                val books = response.data.filter { it.type == 3 }
                val entities = books.map { homeBook ->
                    HomeBookEntity(
                        id = homeBook.bookId,
                        title = homeBook.bookName,
                        author = homeBook.authorName,
                        coverUrl = homeBook.picUrl,
                        description = homeBook.bookDesc,
                        category = "",
                        categoryId = 0,
                        rating = 0.0,
                        readCount = 0,
                        wordCount = 0,
                        commentCount = 0,
                        isCompleted = false,
                        isVip = false,
                        updateTime = System.currentTimeMillis(),
                        lastChapterName = null,
                        lastChapterUpdateTime = null,
                        type = TYPE_HOT
                    )
                }
                
                homeDao.deleteBooksByType(TYPE_HOT)
                homeDao.insertBooks(entities)
                
                emit(entities)
                Log.d(TAG, "热门书籍数据更新成功，共${entities.size}本")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热门书籍失败", e)
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_HOT)
            if (cachedBooks.isNotEmpty()) {
                emit(cachedBooks)
            }
        }
    }
    
    /**
     * 获取本周强推书籍 (type=2)
     */
    fun getNewBooks(forceRefresh: Boolean = false): Flow<List<HomeBookEntity>> = flow {
        try {
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_NEW)
            if (cachedBooks.isNotEmpty() && !forceRefresh) {
                emit(cachedBooks)
            }
            
            // 使用首页书籍接口
            val response = homeService.getHomeBooksBlocking()
            if (response.ok == true && response.data != null) {
                // 过滤type为2的书籍（本周强推）
                val books = response.data.filter { it.type == 2 }
                val entities = books.map { homeBook ->
                    HomeBookEntity(
                        id = homeBook.bookId,
                        title = homeBook.bookName,
                        author = homeBook.authorName,
                        coverUrl = homeBook.picUrl,
                        description = homeBook.bookDesc,
                        category = "",
                        categoryId = 0,
                        rating = 0.0,
                        readCount = 0,
                        wordCount = 0,
                        commentCount = 0,
                        isCompleted = false,
                        isVip = false,
                        updateTime = System.currentTimeMillis(),
                        lastChapterName = null,
                        lastChapterUpdateTime = null,
                        type = TYPE_NEW
                    )
                }
                
                homeDao.deleteBooksByType(TYPE_NEW)
                homeDao.insertBooks(entities)
                
                emit(entities)
                Log.d(TAG, "本周强推书籍数据更新成功，共${entities.size}本")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取本周强推书籍失败", e)
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_NEW)
            if (cachedBooks.isNotEmpty()) {
                emit(cachedBooks)
            }
        }
    }
    
    /**
     * 获取精品推荐书籍 (type=4)
     */
    fun getVipBooks(forceRefresh: Boolean = false): Flow<List<HomeBookEntity>> = flow {
        try {
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_VIP)
            if (cachedBooks.isNotEmpty() && !forceRefresh) {
                emit(cachedBooks)
            }
            
            // 使用首页书籍接口
            val response = homeService.getHomeBooksBlocking()
            if (response.ok == true && response.data != null) {
                // 过滤type为4的书籍（精品推荐）
                val books = response.data.filter { it.type == 4 }
                val entities = books.map { homeBook ->
                    HomeBookEntity(
                        id = homeBook.bookId,
                        title = homeBook.bookName,
                        author = homeBook.authorName,
                        coverUrl = homeBook.picUrl,
                        description = homeBook.bookDesc,
                        category = "",
                        categoryId = 0,
                        rating = 0.0,
                        readCount = 0,
                        wordCount = 0,
                        commentCount = 0,
                        isCompleted = false,
                        isVip = true, // 精品推荐标记为VIP
                        updateTime = System.currentTimeMillis(),
                        lastChapterName = null,
                        lastChapterUpdateTime = null,
                        type = TYPE_VIP
                    )
                }
                
                homeDao.deleteBooksByType(TYPE_VIP)
                homeDao.insertBooks(entities)
                
                emit(entities)
                Log.d(TAG, "精品推荐书籍数据更新成功，共${entities.size}本")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取精品推荐书籍失败", e)
            val cachedBooks = homeDao.getBooksByTypeSync(TYPE_VIP)
            if (cachedBooks.isNotEmpty()) {
                emit(cachedBooks)
            }
        }
    }
    
    // endregion
    
    // region 轮播图相关
    
    /**
     * 获取轮播图数据
     */
    fun getBanners(forceRefresh: Boolean = false): Flow<List<HomeBannerEntity>> = flow {
        try {
            val cachedBanners = homeDao.getActiveBannersSync()
            if (cachedBanners.isNotEmpty() && !forceRefresh) {
                emit(cachedBanners)
            }
            
            // 暂时使用模拟数据
            val mockBanners = listOf(
                HomeBannerEntity(1, "精彩推荐1", "", null, 1),
                HomeBannerEntity(2, "精彩推荐2", "", null, 2),
                HomeBannerEntity(3, "精彩推荐3", "", null, 3)
            )
            
            homeDao.clearAllBanners()
            homeDao.insertBanners(mockBanners)
            emit(mockBanners)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取轮播图失败", e)
            val cachedBanners = homeDao.getActiveBannersSync()
            if (cachedBanners.isNotEmpty()) {
                emit(cachedBanners)
            }
        }
    }
    
    // endregion
    
    // region 分类相关
    
    /**
     * 获取分类数据
     */
    fun getCategories(forceRefresh: Boolean = false): Flow<List<HomeCategoryEntity>> = flow {
        try {
            val cachedCategories = homeDao.getCategoriesSync()
            if (cachedCategories.isNotEmpty() && !forceRefresh) {
                emit(cachedCategories)
            }
            
            // 模拟分类数据
            val mockCategories = listOf(
                HomeCategoryEntity(1, "玄幻", null, 1, 1234),
                HomeCategoryEntity(2, "都市", null, 2, 856),
                HomeCategoryEntity(3, "历史", null, 3, 432),
                HomeCategoryEntity(4, "科幻", null, 4, 298),
                HomeCategoryEntity(5, "军事", null, 5, 156),
                HomeCategoryEntity(6, "悬疑", null, 6, 89)
            )
            
            homeDao.clearAllCategories()
            homeDao.insertCategories(mockCategories)
            emit(mockCategories)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取分类失败", e)
            val cachedCategories = homeDao.getCategoriesSync()
            if (cachedCategories.isNotEmpty()) {
                emit(cachedCategories)
            }
        }
    }
    
    // endregion
    
    // region 综合操作
    
    /**
     * 刷新所有首页数据
     */
    suspend fun refreshAllData() {
        try {
            Log.d(TAG, "开始刷新所有首页数据")
            
            // 并发获取所有数据
            getCarouselBooks(true).first()
            getHotBooks(true).first()
            getNewBooks(true).first()
            getVipBooks(true).first()
            getBanners(true).first()
            getCategories(true).first()
            
            Log.d(TAG, "所有首页数据刷新完成")
        } catch (e: Exception) {
            Log.e(TAG, "刷新首页数据失败", e)
            throw e
        }
    }
    
    /**
     * 清空所有缓存数据
     */
    suspend fun clearAllCache() {
        homeDao.clearAllHomeData()
        Log.d(TAG, "首页缓存数据已清空")
    }
    
    // endregion
} 