package com.candiy.candiyhc.data.local.model

import com.squareup.moshi.Json

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

data class SleepData(
    @Json(name = "sleep_duration_total")
    val sleepDurationTotal: String,  // ISO-8601 duration format (e.g., "PT4H9M")

    val stages: List<SleepStage>,

    val notes: String,
    val title: String
)

data class SleepStage(
    val startTime: String,  // or Instant if you parse ISO strings
    val endTime: String,
    val stage: Int
)

data class OxygenSaturationData(
    val percentage: Double
)
