package com.candiy.candiyhc.network.uploader.strategy

import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.network.SleepListWrapper
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.uploader.HealthDataUploadStrategy
import okhttp3.ResponseBody
import retrofit2.Response

class SleepUploadStrategy(
    private val api: ApiService
) : HealthDataUploadStrategy<SleepListWrapper> {

    override val dataType = DataTypes.SLEEP.toString()

    override fun wrap(data: List<HealthDataUploadRequest>): SleepListWrapper {
        return SleepListWrapper(data)
    }

    override suspend fun upload(authHeader: String, body: SleepListWrapper): Response<ResponseBody> {
        return api.uploadSleepData(authHeader, body)
    }
}