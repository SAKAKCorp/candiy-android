package com.candiy.candiyhc.data.local.repository

import com.candiy.candiyhc.data.local.dao.HealthDataDao
import com.candiy.candiyhc.data.local.entity.HealthDataEntity
import kotlinx.coroutines.flow.Flow

class OfflineHealthDataRepository(private val healthDataDao: HealthDataDao) : HealthDataRepository {
    override suspend fun insertHealthData(healthDataEntity: HealthDataEntity) = healthDataDao.insert(healthDataEntity)

    override suspend fun updateHealthData(healthDataEntity: HealthDataEntity) = healthDataDao.update(healthDataEntity)

    override suspend fun deleteHealthData(healthDataEntity: HealthDataEntity) = healthDataDao.delete(healthDataEntity)

    override suspend fun insertIfNew(metadataId: String, newTimestamp: Long, entity: HealthDataEntity) = healthDataDao.insertIfNew(metadataId, newTimestamp, entity)

    override suspend fun getHealthDataByMetadataId(metadataId: String) = healthDataDao.getByMetadataId(metadataId)


    override suspend fun getPendingUploadData(lastSyncedAt: String?, userId: Long) = healthDataDao.getPendingUploadData(lastSyncedAt, userId)

    override suspend fun markAsUploaded(metadataIds: List<String>, uploadTime: Long, userId: Long) = healthDataDao.markAsUploaded(metadataIds, uploadTime, userId)

    override suspend fun getLastUploadedAt(userId: Long): Long? {
        return healthDataDao.getLastUploadedAt(userId) ?: 0L
    }

    // 중복 데이터 체크
    override suspend fun checkIfExists(
        userId: Long,
        app: String,
        metadataId: String,
        lastModifiedTime: String
    ): Boolean {
        val existingData = healthDataDao.findByUserAndData(
            userId, app, metadataId, lastModifiedTime
        )
        return existingData != null
    }

    override fun getUploadedByUserId(userId: Long): Flow<List<HealthDataEntity>> {
        return healthDataDao.getUploadedByUserId(userId)
    }


}