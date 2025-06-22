package com.novel.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.novel.page.login.dao.UserDao
import com.novel.page.home.dao.HomeDao
import com.novel.utils.dao.NovelDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 * 
 * 负责配置和提供小说应用的本地数据库相关依赖：
 * - Room数据库实例的创建和配置
 * - 各功能模块DAO对象的提供
 * - 数据库迁移策略的设置
 * 
 * 所有数据库相关组件都采用单例模式，确保数据一致性
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val TAG = "DatabaseModule"
    private const val DATABASE_NAME = "kxq.db"

    /**
     * 提供Room数据库实例
     * 
     * 配置：
     * - 数据库名称：kxq.db
     * - 迁移策略：破坏性迁移（适用于开发阶段）
     * - 实例模式：应用级单例
     * 
     * @param ctx 应用上下文
     * @return NovelDatabase实例
     */
    @Provides 
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NovelDatabase {
        Log.d(TAG, "创建Room数据库实例: $DATABASE_NAME")
        return Room.databaseBuilder(ctx, NovelDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration(true)  // 开发阶段使用破坏性迁移
            .build()
    }

    /**
     * 提供用户数据访问对象
     * 
     * @param db 数据库实例
     * @return UserDao - 用户相关数据操作接口
     */
    @Provides
    fun provideUserDao(db: NovelDatabase): UserDao {
        Log.d(TAG, "提供UserDao实例")
        return db.userDao()
    }

    /**
     * 提供首页数据访问对象
     * 
     * @param db 数据库实例  
     * @return HomeDao - 首页相关数据操作接口
     */
    @Provides
    fun provideHomeDao(db: NovelDatabase): HomeDao {
        Log.d(TAG, "提供HomeDao实例")
        return db.homeDao()
    }
}
