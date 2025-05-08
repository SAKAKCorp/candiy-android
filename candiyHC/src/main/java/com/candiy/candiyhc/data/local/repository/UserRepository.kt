package com.candiy.candiyhc.data.local.repository

import com.candiy.candiyhc.data.local.entity.UserEntity

interface UserRepository {
    suspend fun insertUser(user: UserEntity)
    suspend fun updateUser(user: UserEntity)
    suspend fun deleteUser(user: UserEntity)
    suspend fun getUserById(id: Long): UserEntity?
    suspend fun getUserByEndUserId(endUserId: String): UserEntity?
    suspend fun updateLastSyncedAt(endUserId: String, lastSyncedAt: String)
    suspend fun getUserByEndUserIdAndDeviceModel(endUserId: String, deviceModel: String?): UserEntity?

}
