package com.candiy.candiyhc.data.local.repository

import com.candiy.candiyhc.data.local.entity.HealthDataEntity
import kotlinx.coroutines.flow.Flow


interface HealthDataRepository {
    suspend fun insertHealthData(healthDataEntity: HealthDataEntity)

    suspend fun updateHealthData(healthDataEntity: HealthDataEntity)

    suspend fun deleteHealthData(healthDataEntity: HealthDataEntity)

    suspend fun insertIfNew(metadataId: String, newTimestamp: Long, entity: HealthDataEntity)

    suspend fun getHealthDataByMetadataId(metadataId: String): HealthDataEntity?

    suspend fun getPendingUploadData(lastSyncedAt: String?, userId: Long): List<HealthDataEntity>

    suspend fun markAsUploaded(metadataIds: List<String>, uploadTime: Long, userId: Long)

    suspend fun getLastUploadedAt(userId: Long): Long?

    suspend fun checkIfExists(userId: Long, app: String, metadataId: String, lastModifiedTime: String): Boolean

    fun getUploadedByUserId(userId: Long): Flow<List<HealthDataEntity>>

}
