package com.novel.page.login.dao

import android.util.Log
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import com.novel.utils.network.api.front.user.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据仓库 - 统一管理用户数据的缓存和同步
 * ⚠ 安全检查: 用户数据需要确保一致性和安全性
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userDefaults: NovelUserDefaults
) {
    
    /**
     * 缓存用户信息到本地数据库
     * @param data 用户信息数据
     * @return 是否缓存成功
     */
    suspend fun cacheUser(data: UserService.UserInfoData): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = userDefaults.get<Int>(NovelUserDefaultsKey.USER_ID)?.toString()
            
            if (uid.isNullOrEmpty()) {
                Log.e("UserRepository", "缓存用户失败: UID为空")
                return@withContext false
            }
            
            Log.d("UserRepository", "开始缓存用户数据: uid=$uid")
            
            // 先清空旧数据，确保数据一致性
            userDao.clearAll()
            
            // 转换并插入新数据
            val userEntity = data.toEntity(uid)
            userDao.insertUser(userEntity)
            
            Log.d("UserRepository", "用户数据缓存成功: ${userEntity.nickName}")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "缓存用户数据失败", e)
            false
        }
    }
    
    /**
     * 获取当前用户信息
     * @return 用户实体，如果不存在则返回null
     */
    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val uid = userDefaults.get<Int>(NovelUserDefaultsKey.USER_ID)?.toString()
            if (uid.isNullOrEmpty()) {
                Log.w("UserRepository", "获取当前用户失败: UID为空")
                return@withContext null
            }
            
            userDao.getUserByUid(uid)
        } catch (e: Exception) {
            Log.e("UserRepository", "获取当前用户失败", e)
            null
        }
    }
    
    /**
     * 获取所有本地用户（流式）
     * @return 用户列表流
     */
    fun getAllUsersFlow(): Flow<List<UserEntity>> = flow {
        try {
            val users = userDao.getAllUsers()
            emit(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "获取用户列表失败", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 获取所有本地用户（挂起函数）
     * @return 用户列表
     */
    suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        try {
            userDao.getAllUsers()
        } catch (e: Exception) {
            Log.e("UserRepository", "获取用户列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 清理所有本地用户数据
     * @return 是否清理成功
     */
    suspend fun clearAllUsers(): Boolean = withContext(Dispatchers.IO) {
        try {
            userDao.clearAll()
            Log.d("UserRepository", "已清理所有用户数据")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "清理用户数据失败", e)
            false
        }
    }
    
    /**
     * 更新用户信息
     * @param userEntity 用户实体
     * @return 是否更新成功
     */
    suspend fun updateUser(userEntity: UserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            userDao.insertUser(userEntity) // Room的REPLACE策略会自动更新
            Log.d("UserRepository", "用户信息更新成功: ${userEntity.nickName}")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "用户信息更新失败", e)
            false
        }
    }
    
    /**
     * 检查本地是否有用户数据缓存
     * @return 是否有缓存
     */
    suspend fun hasUserCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            userDao.getUserCount() > 0
        } catch (e: Exception) {
            Log.e("UserRepository", "检查用户缓存失败", e)
            false
        }
    }
    
    /**
     * 获取用户缓存数量
     * @return 缓存用户数量
     */
    suspend fun getUserCacheCount(): Int = withContext(Dispatchers.IO) {
        try {
            userDao.getUserCount()
        } catch (e: Exception) {
            Log.e("UserRepository", "获取用户缓存数量失败", e)
            0
        }
    }
}

// ――― DTO ↔ Entity 转换扩展函数 ―――
/**
 * 将网络层DTO转换为本地Entity
 * ⚠ 安全检查: 确保数据转换的完整性
 */
private fun UserService.UserInfoData.toEntity(uid: String) = UserEntity(
    uid = uid,
    nickName = nickName,
    userPhoto = userPhoto,
    userSex = userSex
)

/**
 * 将本地Entity转换为业务层模型
 */
fun UserEntity.toDomainModel() = UserInfo(
    uid = uid,
    nickName = nickName,
    userPhoto = userPhoto,
    userSex = userSex
)

/**
 * 业务层用户信息模型
 */
data class UserInfo(
    val uid: String,
    val nickName: String,
    val userPhoto: String,
    val userSex: Int
)