package com.candiy.candiyhc.data.di

import android.content.Context
import com.candiy.candiyhc.data.local.HealthDataDatabase
import com.candiy.candiyhc.data.local.repository.HealthDataRepository
import com.candiy.candiyhc.data.local.repository.RoomUserRepository
import com.candiy.candiyhc.data.local.repository.UserRepository
import com.candiy.candiyhc.data.local.repository.OfflineHealthDataRepository


class AppContainer(context: Context) {

    // Database 인스턴스 생성
    private val database = HealthDataDatabase.getDatabase(context.applicationContext)

    // Repository 생성
    val itemsRepository: HealthDataRepository by lazy {
        OfflineHealthDataRepository(database.healthDataDao())
    }

    val userRepository: UserRepository =
        RoomUserRepository(database.userDao())
}