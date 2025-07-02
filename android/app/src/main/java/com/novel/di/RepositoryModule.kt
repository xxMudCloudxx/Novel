package com.novel.di

import com.novel.utils.TimberLogger
import com.novel.utils.network.repository.CachedBookRepository
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.api.front.SearchService
import com.novel.utils.network.cache.NetworkCacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据仓库层依赖注入模块
 * 
 * 负责配置和提供应用的数据访问层组件：
 * - Repository模式的具体实现
 * - 多数据源的协调和管理
 * - 缓存策略的统一配置
 * 
 * Repository层作为数据访问的抽象层，隔离了上层业务逻辑与底层数据源的具体实现
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    private const val TAG = "RepositoryModule"

    /**
     * 提供带缓存的书籍数据仓库
     * 
     * 整合多个数据源，提供统一的书籍数据访问接口：
     * - 本地缓存优先策略
     * - 网络数据自动同步
     * - 智能缓存更新机制
     * - 离线模式支持
     * 
     * @param bookService 书籍API服务
     * @param searchService 搜索API服务
     * @param cacheManager 网络缓存管理器
     * @return CachedBookRepository实例
     */
    @Provides
    @Singleton
    fun provideCachedBookRepository(
        bookService: BookService,
        searchService: SearchService,
        cacheManager: NetworkCacheManager
    ): CachedBookRepository {
        TimberLogger.d(TAG, "创建带缓存的书籍数据仓库")
        return CachedBookRepository(bookService, searchService, cacheManager)
    }
} 