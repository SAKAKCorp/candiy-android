package com.candiy.candiyhc.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.candiy.candiyhc.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userEntity: UserEntity)

    @Update
    suspend fun update(userEntity: UserEntity)

    @Delete
    suspend fun delete(userEntity: UserEntity)

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM user WHERE end_user_id = :endUserId LIMIT 1")
    suspend fun getUserByEndUserId(endUserId: String): UserEntity?

    @Query("UPDATE user SET last_synced_at = :lastSyncedAt WHERE end_user_id = :endUserId")
    suspend fun updateLastSyncedAt(endUserId: String, lastSyncedAt: String)

    @Query("SELECT * FROM user WHERE end_user_id = :endUserId AND device_model = :deviceModel LIMIT 1")
    suspend fun getUserByEndUserIdAndDeviceModel(endUserId: String, deviceModel: String?): UserEntity?

}
