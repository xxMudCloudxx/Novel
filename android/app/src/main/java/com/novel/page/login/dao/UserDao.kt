package com.novel.page.login.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象 - 提供数据库操作接口
 */
@Dao
interface UserDao {

    // ——— 写操作 ———

    /**
     * 插入或更新用户信息
     * @param user 用户实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * 批量插入用户
     * @param users 用户列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    /**
     * 清空所有用户数据
     */
    @Query("DELETE FROM users")
    suspend fun clearAll()

    /**
     * 删除指定用户
     * @param uid 用户ID
     */
    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)

    // ——— 查询操作 ———

    /**
     * 获取所有用户
     * @return 用户列表
     */
    @Query("SELECT * FROM users ORDER BY lastUpdateTime DESC")
    suspend fun getAllUsers(): List<UserEntity>

    /**
     * 获取所有用户（Flow版本，支持响应式查询）
     * @return 用户列表Flow
     */
    @Query("SELECT * FROM users ORDER BY lastUpdateTime DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    /**
     * 根据UID获取用户
     * @param uid 用户ID
     * @return 用户实体，如果不存在则返回null
     */
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserByUid(uid: String): UserEntity?

    /**
     * 根据UID获取用户（Flow版本）
     * @param uid 用户ID
     * @return 用户实体Flow
     */
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserByUidFlow(uid: String): Flow<UserEntity?>

    /**
     * 根据昵称模糊查询用户
     * @param nickName 昵称关键词
     * @return 匹配的用户列表
     */
    @Query("SELECT * FROM users WHERE nickName LIKE '%' || :nickName || '%' ORDER BY lastUpdateTime DESC")
    suspend fun getUsersByNickName(nickName: String): List<UserEntity>

    /**
     * 获取用户总数
     * @return 用户数量
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    /**
     * 获取用户总数（Flow版本）
     * @return 用户数量Flow
     */
    @Query("SELECT COUNT(*) FROM users")
    fun getUserCountFlow(): Flow<Int>

    /**
     * 检查用户是否存在
     * @param uid 用户ID
     * @return 是否存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE uid = :uid)")
    suspend fun isUserExists(uid: String): Boolean

    /**
     * 获取最近更新的用户
     * @param limit 限制数量
     * @return 最近更新的用户列表
     */
    @Query("SELECT * FROM users ORDER BY lastUpdateTime DESC LIMIT :limit")
    suspend fun getRecentUsers(limit: Int = 10): List<UserEntity>

    /**
     * 获取指定性别的用户
     * @param userSex 用户性别
     * @return 指定性别的用户列表
     */
    @Query("SELECT * FROM users WHERE userSex = :userSex ORDER BY lastUpdateTime DESC")
    suspend fun getUsersBySex(userSex: Int): List<UserEntity>

    /**
     * 获取用户更新时间范围
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围内的用户列表
     */
    @Query("SELECT * FROM users WHERE lastUpdateTime BETWEEN :startTime AND :endTime ORDER BY lastUpdateTime DESC")
    suspend fun getUsersByTimeRange(startTime: Long, endTime: Long): List<UserEntity>
}