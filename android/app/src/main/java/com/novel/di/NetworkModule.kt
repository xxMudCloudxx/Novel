package com.novel.di

import android.content.Context
import android.util.Log
import com.novel.page.read.repository.BookCacheManager
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.network.api.front.HomeService
import com.novel.utils.network.api.front.NewsService
import com.novel.utils.network.api.front.user.UserService
import com.novel.utils.network.cache.NetworkCacheManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 网络模块依赖注入配置
 * 
 * 负责提供小说应用的网络相关服务和工具：
 * - 各功能模块的API服务接口
 * - 网络数据缓存管理器
 * - 书籍内容缓存管理器
 * - JSON序列化工具
 * 
 * 所有网络组件都采用单例模式，提高性能和资源利用率
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    /**
     * 提供书籍缓存管理器
     * 
     * 用于管理书籍内容的本地缓存，支持：
     * - 章节内容离线缓存
     * - 图片资源本地存储
     * - 缓存清理和过期管理
     * 
     * @param context 应用上下文
     * @return BookCacheManager实例
     */
    @Provides
    @Singleton
    fun provideBookCacheManager(
        @ApplicationContext context: Context
    ): BookCacheManager {
        Log.d(TAG, "创建书籍缓存管理器")
        return BookCacheManager(context)
    }

    /**
     * 提供网络缓存管理器
     * 
     * 统一管理API请求的缓存策略：
     * - 内存缓存：提高响应速度
     * - 磁盘缓存：支持离线访问
     * - 缓存策略：智能更新机制
     * 
     * @param context 应用上下文
     * @param gson JSON序列化工具
     * @return NetworkCacheManager实例
     */
    @Provides
    @Singleton
    fun provideNetworkCacheManager(
        @ApplicationContext context: Context,
        gson: Gson
    ): NetworkCacheManager {
        Log.d(TAG, "创建网络缓存管理器")
        return NetworkCacheManager(context, gson)
    }

    /**
     * 提供书籍服务API
     * 
     * @return BookService - 书籍相关API接口
     */
    @Provides
    @Singleton
    fun provideBookService(): BookService {
        Log.d(TAG, "创建书籍服务")
        return BookService()
    }
    
    /**
     * 提供搜索服务API
     * 
     * @return SearchService - 搜索相关API接口
     */
    @Provides
    @Singleton
    fun provideSearchService(): SearchService {
        Log.d(TAG, "创建搜索服务")
        return SearchService()
    }
    
    /**
     * 提供首页服务API
     * 
     * @return HomeService - 首页数据API接口
     */
    @Provides
    @Singleton
    fun provideHomeService(): HomeService {
        Log.d(TAG, "创建首页服务")
        return HomeService()
    }
    
    /**
     * 提供资讯服务API
     * 
     * @return NewsService - 资讯相关API接口
     */
    @Provides
    @Singleton
    fun provideNewsService(): NewsService {
        Log.d(TAG, "创建资讯服务")
        return NewsService()
    }
    
    /**
     * 提供用户服务API
     * 
     * @return UserService - 用户相关API接口
     */
    @Provides
    @Singleton
    fun provideUserService(): UserService {
        Log.d(TAG, "创建用户服务")
        return UserService()
    }
    
    /**
     * 提供JSON序列化工具
     * 
     * 全局统一的Gson实例，用于：
     * - API响应数据解析
     * - 缓存数据序列化
     * - 配置数据持久化
     * 
     * @return Gson实例
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        Log.d(TAG, "创建Gson序列化工具")
        return Gson()
    }
} 