package com.candiy.candiyhc.data.local.dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.candiy.candiyhc.data.local.entity.HealthDataEntity

@Dao
interface HealthDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(healthData: HealthDataEntity)

    @Update
    suspend fun update(healthData: HealthDataEntity)

    @Delete
    suspend fun delete(healthData: HealthDataEntity)

    @Query("SELECT * FROM health_data WHERE metadata_id = :metadataId LIMIT 1")
    suspend fun getByMetadataId(metadataId: String): HealthDataEntity?

    @Transaction
    suspend fun insertIfNew(metadataId: String, newTimestamp: Long, entity: HealthDataEntity) {
        val existing = getByMetadataId(metadataId)
        val shouldInsert = existing == null || existing.updatedAt != newTimestamp

        if (shouldInsert) {
            insert(entity)
        }
    }

    @Query("""
SELECT * FROM health_data
WHERE is_uploaded = 0
AND user_id = :userId
AND (
    :lastSyncedAt IS NULL
    OR last_modified_time > :lastSyncedAt
)
""")
    suspend fun getPendingUploadData(lastSyncedAt: String?, userId: Long): List<HealthDataEntity>

    @Query("""
UPDATE health_data 
SET is_uploaded = 1, uploaded_at = :uploadTime 
WHERE metadata_id IN (:metadataIds) 
AND user_id = :userId 
""")
    suspend fun markAsUploaded(metadataIds: List<String>, uploadTime: Long, userId: Long)

    @Query("""
SELECT uploaded_at FROM health_data 
WHERE uploaded_at IS NOT NULL 
AND user_id = :userId
ORDER BY uploaded_at DESC 
LIMIT 1
""")
    suspend fun getLastUploadedAt(userId: Long): Long?

    // unique 필드를 기준으로 데이터 조회 (중복 확인)
    @Query("""
        SELECT * FROM health_data
        WHERE user_id = :userId
        AND app = :app
        AND metadata_id = :metadataId
        AND last_modified_time = :lastModifiedTime
        LIMIT 1
    """)
    suspend fun findByUserAndData(
        userId: Long,
        app: String,
        metadataId: String,
        lastModifiedTime: String
    ): HealthDataEntity?

}