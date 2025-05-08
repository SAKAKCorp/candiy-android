package com.candiy.candiyhc.data.local.repository

import com.candiy.candiyhc.data.local.entity.UserEntity
import com.candiy.candiyhc.data.local.dao.UserDao

class RoomUserRepository(private val userDao: UserDao) : UserRepository {

    override suspend fun getUserById(id: Long): UserEntity? {
        return userDao.getUserById(id)
    }

    override suspend fun insertUser(user: UserEntity) {
        userDao.insert(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.update(user)
    }

    override suspend fun deleteUser(user: UserEntity) {
        userDao.delete(user)
    }

    override suspend fun getUserByEndUserId(endUserId: String): UserEntity? {
        return userDao.getUserByEndUserId(endUserId)
    }

    override suspend fun updateLastSyncedAt(endUserId: String, lastSyncedAt: String) {
        userDao.updateLastSyncedAt(endUserId, lastSyncedAt)
    }

    override suspend fun getUserByEndUserIdAndDeviceModel(
        endUserId: String,
        deviceModel: String?
    ): UserEntity? {
        return userDao.getUserByEndUserIdAndDeviceModel(endUserId, deviceModel)
    }
}
