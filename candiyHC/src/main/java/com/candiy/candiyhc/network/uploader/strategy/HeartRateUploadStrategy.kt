package com.candiy.candiyhc.network.uploader.strategy

import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.network.HeartRateListWrapper
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.uploader.HealthDataUploadStrategy
import okhttp3.ResponseBody
import retrofit2.Response

class HeartRateUploadStrategy(
    private val api: ApiService
) : HealthDataUploadStrategy<HeartRateListWrapper> {

    override val dataType = DataTypes.HEART_RATE.toString()

    override fun wrap(data: List<HealthDataUploadRequest>): HeartRateListWrapper {
        return HeartRateListWrapper(data)
    }

    override suspend fun upload(authHeader: String, body: HeartRateListWrapper): Response<ResponseBody> {
        return api.uploadHeartRateData(authHeader, body)
    }
}