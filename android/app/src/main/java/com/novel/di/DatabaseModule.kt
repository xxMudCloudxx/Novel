package com.novel.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): KxqDatabase =
        Room.databaseBuilder(ctx, KxqDatabase::class.java, "kxq.db")
            .fallbackToDestructiveMigration() // 版本迭代后请用正式 Migration
            .build()

    @Provides
    fun provideUserDao(db: KxqDatabase): UserDao = db.userDao()
}
