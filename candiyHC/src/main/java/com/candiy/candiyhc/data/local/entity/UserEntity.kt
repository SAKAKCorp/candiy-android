package com.candiy.candiyhc.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "end_user_id")
    val endUserId: String,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: String? = null, // 마지막으로 동기화한 시간
)
