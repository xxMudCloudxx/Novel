package com.novel.di

import android.content.Context
import android.content.SharedPreferences
import com.novel.utils.Store.NovelKeyChain.NovelKeyChain
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.SharedPrefsUserDefaults
import com.novel.utils.network.TokenProvider
import com.novel.utils.StringProvider
import com.novel.utils.AndroidStringProvider
import java.time.Clock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module：提供 SharedPreferences & NovelUserDefaults & Clock & StringProvider
 */
@Module
@InstallIn(SingletonComponent::class)
object NovelUserDefaultsModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext ctx: Context
    ): SharedPreferences =
        ctx.getSharedPreferences("Novel_user_defaults", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideNovelUserDefaults(
        impl: SharedPrefsUserDefaults
    ): NovelUserDefaults = impl

    @Provides
    @Singleton
    fun provideTokenProvider(keyChain: NovelKeyChain): TokenProvider = TokenProvider(keyChain)

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    // 添加Clock依赖
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    // 添加StringProvider依赖
    @Provides
    @Singleton
    fun provideStringProvider(
        androidStringProvider: AndroidStringProvider
    ): StringProvider = androidStringProvider
}