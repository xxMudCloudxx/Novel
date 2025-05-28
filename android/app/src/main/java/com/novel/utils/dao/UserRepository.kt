package com.novel.utils.dao

import android.util.Log
import com.novel.utils.Store.UserDefaults.NovelUserDefaults
import com.novel.utils.Store.UserDefaults.NovelUserDefaultsKey
import com.novel.utils.network.api.front.user.UserService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userDefaults: NovelUserDefaults
) {
    /** 将网络层 DTO 转为 Entity 并持久化 */
    suspend fun cacheUser(data: UserService.UserInfoData) {
        val uid = userDefaults.get<Int>(NovelUserDefaultsKey.USER_ID).toString()
        Log.d("UserRepository", "📤 cacheUser: 清空旧数据")
        userDao.clearAll()

        val user = data.toEntity(uid)
        Log.d("UserRepository", "📥 cacheUser start: user=${user.uid}")
        userDao.insertUser(user)
    }

    /** 获取当前所有本地 UserEntity */
    suspend fun fetchAllUsers(): List<UserEntity> =
        userDao.getAllUsers()
}

// ――― DTO ↔ Entity 转换扩展函数 ―――
private fun UserService.UserInfoData.toEntity(uid: String) = UserEntity(
    uid = uid,
    nickName = nickName,
    userPhoto = userPhoto,
    userSex = userSex
)