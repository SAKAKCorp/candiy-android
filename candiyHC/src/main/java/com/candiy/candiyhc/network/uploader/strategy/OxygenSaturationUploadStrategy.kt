package com.candiy.candiyhc.network.uploader.strategy

import com.candiy.candiyhc.data.enums.DataTypes
import com.candiy.candiyhc.network.ApiService
import com.candiy.candiyhc.network.OxygenSaturationListWrapper
import com.candiy.candiyhc.network.model.request.HealthDataUploadRequest
import com.candiy.candiyhc.network.uploader.HealthDataUploadStrategy
import okhttp3.ResponseBody
import retrofit2.Response

class OxygenSaturationUploadStrategy(
    private val api: ApiService
) : HealthDataUploadStrategy<OxygenSaturationListWrapper> {

    override val dataType = DataTypes.OXYGEN_SATURATION.toString()

    override fun wrap(data: List<HealthDataUploadRequest>): OxygenSaturationListWrapper {
        return OxygenSaturationListWrapper(data)
    }

    override suspend fun upload(authHeader: String, body: OxygenSaturationListWrapper): Response<ResponseBody> {
        return api.uploadOxygenSaturationData(authHeader, body)
    }
}