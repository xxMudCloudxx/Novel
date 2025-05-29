package com.novel.page.login.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体 - 本地数据库表结构
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val nickName: String,
    val userPhoto: String,
    val userSex: Int,
    val lastUpdateTime: Long = System.currentTimeMillis() // 添加最后更新时间
)