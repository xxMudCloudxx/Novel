package com.novel.utils.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novel.page.home.dao.HomeDao
import com.novel.page.home.dao.HomeBookEntity
import com.novel.page.home.dao.HomeBannerEntity
import com.novel.page.home.dao.HomeCategoryEntity

@Database(
    entities = [
        UserEntity::class,
        HomeBookEntity::class,
        HomeBannerEntity::class,
        HomeCategoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun homeDao(): HomeDao
}