package com.novel.di

import com.novel.page.read.service.common.*
import com.novel.page.read.service.settings.*
import com.novel.page.read.viewmodel.ChapterCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Service层依赖注入模块
 * 
 * 提供Service层所需的所有依赖：
 * - 调度器提供者
 * - 日志记录器
 * - 缓存实现
 * - 设置解析和保存器
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    /**
     * 提供协程调度器（性能优化版）
     */
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = OptimizedDispatcherProvider()

    /**
     * 提供服务日志记录器
     */
    @Provides
    @Singleton
    fun provideServiceLogger(): ServiceLogger = AndroidServiceLogger()

    /**
     * 提供章节会话缓存
     */
    @Provides
    @Singleton
    fun provideChapterSessionCache(): SessionCache<String, ChapterCache> = 
        LruSessionCache(ReaderServiceConfig.MAX_SESSION_CACHE_SIZE)

    /**
     * 提供设置解析器
     */
    @Provides
    @Singleton
    fun provideSettingsParser(
        userDefaults: com.novel.utils.Store.UserDefaults.NovelUserDefaults,
        logger: ServiceLogger
    ): SettingsParser = SettingsParser(userDefaults, logger)

    /**
     * 提供设置保存器
     */
    @Provides
    @Singleton
    fun provideSettingsSaver(
        userDefaults: com.novel.utils.Store.UserDefaults.NovelUserDefaults,
        logger: ServiceLogger
    ): SettingsSaver = SettingsSaver(userDefaults, logger)
} 