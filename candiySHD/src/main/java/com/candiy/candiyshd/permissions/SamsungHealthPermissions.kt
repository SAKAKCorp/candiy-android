package com.candiy.candiyshd.permissions

import com.candiy.candiyshd.data.enums.SamsungHealthDataType
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes

object SamsungHealthPermissions {
    val defaultDataTypes: Set<SamsungHealthDataType> = setOf(
        SamsungHealthDataType.HEART_RATE,
        SamsungHealthDataType.STEPS,
        SamsungHealthDataType.OXYGEN_SATURATION,
        SamsungHealthDataType.SLEEP
    )

    fun requiredPermissions(
        dataTypes: Set<SamsungHealthDataType> = defaultDataTypes
    ): Set<Permission> = dataTypes.map { dataType ->
        when (dataType) {
            SamsungHealthDataType.HEART_RATE -> Permission.of(DataTypes.HEART_RATE, AccessType.READ)
            SamsungHealthDataType.STEPS -> Permission.of(DataTypes.STEPS, AccessType.READ)
            SamsungHealthDataType.OXYGEN_SATURATION -> Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ)
            SamsungHealthDataType.SLEEP -> Permission.of(DataTypes.SLEEP, AccessType.READ)
        }
    }.toSet()
}
