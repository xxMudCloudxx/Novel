package com.novel.di

import android.content.Context
import androidx.room.Room
import com.novel.utils.dao.NovelDatabase
import com.novel.utils.dao.UserDao
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
    fun provideDatabase(@ApplicationContext ctx: Context): NovelDatabase =
        Room.databaseBuilder(ctx, NovelDatabase::class.java, "kxq.db")
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideUserDao(db: NovelDatabase): UserDao = db.userDao()
}
