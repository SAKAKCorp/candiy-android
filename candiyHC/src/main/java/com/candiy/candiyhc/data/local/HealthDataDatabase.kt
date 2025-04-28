package com.candiy.candiyhc.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.candiy.candiyhc.data.local.dao.HealthDataDao
import com.candiy.candiyhc.data.local.dao.UserDao
import com.candiy.candiyhc.data.local.entity.HealthDataEntity
import com.candiy.candiyhc.data.local.entity.UserEntity

@Database(entities = [HealthDataEntity::class, UserEntity::class], version = 3, exportSchema = false)
abstract class HealthDataDatabase : RoomDatabase() {

    abstract fun healthDataDao(): HealthDataDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var Instance: HealthDataDatabase? = null

        fun getDatabase(context: Context): HealthDataDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, HealthDataDatabase::class.java, "health_database")
                    .fallbackToDestructiveMigration() // 요기
                    .build()
                    .also { Instance = it }
            }
        }
    }
}