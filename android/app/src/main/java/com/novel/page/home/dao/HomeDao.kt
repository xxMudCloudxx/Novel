package com.novel.page.home.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 首页数据访问对象
 */
@Dao
interface HomeDao {
    
    // region 书籍相关操作
    
    /**
     * 获取指定类型的书籍列表
     */
    @Query("SELECT * FROM home_books WHERE type = :type ORDER BY sortOrder ASC, updateTime DESC")
    fun getBooksByType(type: String): Flow<List<HomeBookEntity>>
    
    /**
     * 获取指定类型的书籍列表（同步）
     */
    @Query("SELECT * FROM home_books WHERE type = :type ORDER BY sortOrder ASC, updateTime DESC")
    suspend fun getBooksByTypeSync(type: String): List<HomeBookEntity>
    
    /**
     * 插入书籍列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<HomeBookEntity>)
    
    /**
     * 删除指定类型的书籍
     */
    @Query("DELETE FROM home_books WHERE type = :type")
    suspend fun deleteBooksByType(type: String)
    
    /**
     * 清空所有书籍
     */
    @Query("DELETE FROM home_books")
    suspend fun clearAllBooks()
    
    // endregion
    
    // region 轮播图相关操作
    
    /**
     * 获取活跃的轮播图列表
     */
    @Query("SELECT * FROM home_banners WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getActiveBanners(): Flow<List<HomeBannerEntity>>
    
    /**
     * 获取活跃的轮播图列表（同步）
     */
    @Query("SELECT * FROM home_banners WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getActiveBannersSync(): List<HomeBannerEntity>
    
    /**
     * 插入轮播图列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<HomeBannerEntity>)
    
    /**
     * 清空所有轮播图
     */
    @Query("DELETE FROM home_banners")
    suspend fun clearAllBanners()
    
    // endregion
    
    // region 分类相关操作
    
    /**
     * 获取分类列表
     */
    @Query("SELECT * FROM home_categories ORDER BY sortOrder ASC")
    fun getCategories(): Flow<List<HomeCategoryEntity>>
    
    /**
     * 获取分类列表（同步）
     */
    @Query("SELECT * FROM home_categories ORDER BY sortOrder ASC")
    suspend fun getCategoriesSync(): List<HomeCategoryEntity>
    
    /**
     * 插入分类列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<HomeCategoryEntity>)
    
    /**
     * 清空所有分类
     */
    @Query("DELETE FROM home_categories")
    suspend fun clearAllCategories()
    
    // endregion
    
    // region 综合操作
    
    /**
     * 清空所有首页数据
     */
    @Transaction
    suspend fun clearAllHomeData() {
        clearAllBooks()
        clearAllBanners()
        clearAllCategories()
    }
    
    // endregion
} 