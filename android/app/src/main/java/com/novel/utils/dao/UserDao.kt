package com.novel.utils.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val username: String,
    val nickname: String?,
    val avatar: String?,
)

@Dao
interface UserDao {

    // ――― 写操作 ―――

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")      // demo：清空所有用户
    suspend fun clearAll()

    // 查询所有用户
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}