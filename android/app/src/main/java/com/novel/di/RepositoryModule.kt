package com.novel.di

import com.novel.repository.CachedBookRepository
import com.novel.utils.network.api.front.BookService
import com.novel.utils.network.cache.NetworkCacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCachedBookRepository(
        bookService: BookService,
        cacheManager: NetworkCacheManager
    ): CachedBookRepository {
        return CachedBookRepository(bookService, cacheManager)
    }
} 