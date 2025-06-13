package com.novel.di

import android.content.Context
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBookCacheManager(
        @ApplicationContext context: Context
    ): BookCacheManager {
        return BookCacheManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkCacheManager(
        @ApplicationContext context: Context,
        gson: Gson
    ): NetworkCacheManager {
        return NetworkCacheManager(context, gson)
    }

    @Provides
    @Singleton
    fun provideBookService(): BookService {
        return BookService()
    }
    
    @Provides
    @Singleton
    fun provideSearchService(): SearchService {
        return SearchService()
    }
    
    @Provides
    @Singleton
    fun provideHomeService(): HomeService {
        return HomeService()
    }
    
    @Provides
    @Singleton
    fun provideNewsService(): NewsService {
        return NewsService()
    }
    
    @Provides
    @Singleton
    fun provideUserService(): UserService {
        return UserService()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
} 