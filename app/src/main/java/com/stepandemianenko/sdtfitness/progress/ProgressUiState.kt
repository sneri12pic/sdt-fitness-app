package com.stepandemianenko.sdtfitness.progress

import com.stepandemianenko.sdtfitness.domain.model.ProgressSnapshot
import com.stepandemianenko.sdtfitness.home.DailyStepsSourceType

data class ProgressUiState(
    val data: ProgressSnapshot? = null,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,

    val isHealthConnectAvailable: Boolean = false,
    val isHealthConnectPermissionGranted: Boolean = false,
    val isHealthConnectLoading: Boolean = false,
    val displayedTodaySteps: Int = 0,
    val displayedStepsSourceType: DailyStepsSourceType = DailyStepsSourceType.MANUAL,
    val importedTodaySteps: Long? = null,
    val importedLatestWeightKg: Double? = null,
    val dailyStepsChartPoints: List<DailyStepsBarChartPoint> = emptyList(),
    val weightChart: SetMetricChartUiModel = emptyWeightChart(),
    val healthConnectError: String? = null
)

fun emptyWeightChart(): SetMetricChartUiModel {
    return SetMetricChartUiModel(
        title = "Weight Progress",
        actualLabel = "Weight",
        actualValues = emptyList(),
        targetValues = emptyList(),
        unitLabel = "kg"
    )
}
