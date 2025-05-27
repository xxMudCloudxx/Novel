package com.novel.utils.dao

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}