package com.candiy.candiyhc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "health_data",
    foreignKeys = [ForeignKey(entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE)])
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "app")
    val app: String,

    @ColumnInfo(name = "data")
    val data: String,

    @ColumnInfo(name = "metadata_id")
    val metadataId: String, // 원본 record의 고유 ID. 중복 방지 위해 반드시 필요

    @ColumnInfo(name = "start")
    val start: String,

    @ColumnInfo(name = "end")
    val end: String? = null,

    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long, // 마지막으로 동기화한 시간 (epoch millis)

    @ColumnInfo(name = "is_uploaded")
    val isUploaded: Boolean = false, // 서버 전송 여부

    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: Long? = null
)