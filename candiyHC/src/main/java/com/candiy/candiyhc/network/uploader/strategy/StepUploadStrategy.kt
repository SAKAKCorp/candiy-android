package com.candiy.candiyhc.network.uploader.strategy

import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.network.StepListWrapper
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.uploader.HealthDataUploadStrategy
import okhttp3.ResponseBody
import retrofit2.Response

class StepUploadStrategy(
    private val api: ApiService
) : HealthDataUploadStrategy<StepListWrapper> {

    override val dataType = DataTypes.STEPS.toString()

    override fun wrap(data: List<HealthDataUploadRequest>): StepListWrapper {
        return StepListWrapper(data)
    }

    override suspend fun upload(authHeader: String, body: StepListWrapper): Response<ResponseBody> {
        return api.uploadStepHealthData(authHeader, body)
    }
}