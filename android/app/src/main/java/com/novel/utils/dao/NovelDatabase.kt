package com.novel.utils.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novel.page.home.dao.HomeDao
import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeBannerEntity
import com.novel.page.home.dao.HomeCategoryEntity
import com.novel.page.login.dao.UserDao
import com.novel.page.login.dao.UserEntity

@Database(
    entities = [
        UserEntity::class,
        HomeBookEntity::class,
        HomeBannerEntity::class,
        HomeCategoryEntity::class
    ],
    version = 4,
    exportSchema = false
)

abstract class NovelDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun homeDao(): HomeDao
}