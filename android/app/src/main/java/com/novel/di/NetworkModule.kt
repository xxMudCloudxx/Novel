package com.novel.di

import android.content.Context
import com.novel.page.read.repository.BookCacheManager
import com.novel.utils.network.api.front.BookService
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
    fun provideBookService(): BookService {
        return BookService()
    }
} 