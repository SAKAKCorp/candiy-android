package com.candiy.candiyhc.data.local.model

data class StepData(
    val count: Int
)

data class HeartRateSample(
    val time: String,
    val beatsPerMinute: Long
)

data class HeartRateData(
    val samples: List<HeartRateSample>
)