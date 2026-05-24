package com.stepandemianenko.sdtfitness

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.health.DailyStepsSample
import com.stepandemianenko.sdtfitness.data.health.WeightSample
import com.stepandemianenko.sdtfitness.data.repository.ProgressSummary
import com.stepandemianenko.sdtfitness.home.DailyStepsSourceType
import com.stepandemianenko.sdtfitness.progress.DailyStepsBarChartPoint
import com.stepandemianenko.sdtfitness.progress.SetMetricChartUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class ProgressUiState(
    val isLoading: Boolean = true,
    val completedSessions: Int = 0,
    val workoutDays: Int = 0,
    val streakDays: Int = 0,
    val consistencyTitle: String = "0 Completed Sessions",
    val consistencySubtitle: String = "No completed sessions yet",
    val streakSubtitle: String = "No streak yet",
    val bestLiftValue: String = "No lift logged",
    val bestLiftSubtitle: String = "Heaviest completed set",
    val volumeValue: String = "0 kg",
    val volumeSubtitle: String = "Across completed sessions",
    val personalBestsValue: String = "0 sets logged",
    val personalBestsSubtitle: String = "0 reps total",
    val topExerciseText: String = "Top volume exercise will appear here",
    val sessionsLast7Days: Int = 0,
    val sessionsPrevious7Days: Int = 0,

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

class ProgressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.progressRepository(application)
    private val accountSessionManager = AppGraph.accountSessionManager(application)
    private val healthConnectManager = AppGraph.healthConnectManager(application)
    private val homeRepository = AppGraph.homeRepository(application)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        observeAccountScopeChanges()
        observeHomeDailyQuest()
        refresh()
        refreshHealthConnectState()
    }

    private fun observeAccountScopeChanges() {
        viewModelScope.launch {
            accountSessionManager.accountScope.collect {
                _uiState.update { current ->
                    current.copy(
                        displayedTodaySteps = 0,
                        displayedStepsSourceType = DailyStepsSourceType.MANUAL,
                        importedTodaySteps = null,
                        importedLatestWeightKg = null,
                        dailyStepsChartPoints = emptyList(),
                        weightChart = emptyWeightChart(),
                        healthConnectError = null
                    )
                }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val summary = repository.getSummary()
            _uiState.update { current ->
                summary.toUiState().copy(
                    isHealthConnectAvailable = current.isHealthConnectAvailable,
                    isHealthConnectPermissionGranted = current.isHealthConnectPermissionGranted,
                    isHealthConnectLoading = current.isHealthConnectLoading,
                    displayedTodaySteps = current.displayedTodaySteps,
                    displayedStepsSourceType = current.displayedStepsSourceType,
                    importedTodaySteps = current.importedTodaySteps,
                    importedLatestWeightKg = current.importedLatestWeightKg,
                    dailyStepsChartPoints = current.dailyStepsChartPoints,
                    weightChart = current.weightChart,
                    healthConnectError = current.healthConnectError
                )
            }
        }
    }

    fun requiredHealthConnectPermissions(): Set<String> {
        return healthConnectManager.readPermissions
    }

    fun refreshHealthConnectState() {
        viewModelScope.launch {
            when (healthConnectManager.getSdkStatus()) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val hasPermissions = runCatching {
                        healthConnectManager.hasAllPermissions()
                    }.getOrElse { error ->
                        _uiState.update {
                            it.copy(
                                isHealthConnectAvailable = true,
                                isHealthConnectPermissionGranted = false,
                                isHealthConnectLoading = false,
                                importedTodaySteps = null,
                                importedLatestWeightKg = null,
                                dailyStepsChartPoints = emptyList(),
                                weightChart = emptyWeightChart(),
                                healthConnectError = error.message ?: "Failed to check Health Connect permissions."
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isHealthConnectAvailable = true,
                            isHealthConnectPermissionGranted = hasPermissions,
                            isHealthConnectLoading = false,
                            healthConnectError = null
                        )
                    }

                    if (!hasPermissions) {
                        _uiState.update {
                            it.copy(
                                isHealthConnectLoading = false,
                                importedTodaySteps = null,
                                importedLatestWeightKg = null,
                                dailyStepsChartPoints = emptyList(),
                                weightChart = emptyWeightChart()
                            )
                        }
                    } else {
                        loadHealthConnectData()
                    }
                }

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    _uiState.update {
                        it.copy(
                            isHealthConnectAvailable = false,
                            isHealthConnectPermissionGranted = false,
                            isHealthConnectLoading = false,
                            importedTodaySteps = null,
                            importedLatestWeightKg = null,
                            dailyStepsChartPoints = emptyList(),
                            weightChart = emptyWeightChart(),
                            healthConnectError = "Health Connect needs an update on this device."
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            isHealthConnectAvailable = false,
                            isHealthConnectPermissionGranted = false,
                            isHealthConnectLoading = false,
                            importedTodaySteps = null,
                            importedLatestWeightKg = null,
                            dailyStepsChartPoints = emptyList(),
                            weightChart = emptyWeightChart(),
                            healthConnectError = null
                        )
                    }
                }
            }
        }
    }

    fun onHealthConnectPermissionsResult(grantedPermissions: Set<String>) {
        val hasAllPermissions = grantedPermissions.containsAll(requiredHealthConnectPermissions())
        _uiState.update {
            it.copy(
                isHealthConnectPermissionGranted = hasAllPermissions,
                isHealthConnectLoading = false,
                importedTodaySteps = if (hasAllPermissions) it.importedTodaySteps else null,
                importedLatestWeightKg = if (hasAllPermissions) it.importedLatestWeightKg else null,
                dailyStepsChartPoints = if (hasAllPermissions) it.dailyStepsChartPoints else emptyList(),
                weightChart = if (hasAllPermissions) it.weightChart else emptyWeightChart(),
                healthConnectError = null
            )
        }

        if (hasAllPermissions) {
            refreshHealthConnectData()
        }
    }

    fun setHealthConnectError(message: String) {
        _uiState.update {
            it.copy(healthConnectError = message)
        }
    }

    fun refreshHealthConnectData() {
        viewModelScope.launch {
            loadHealthConnectData()
        }
    }

    private suspend fun loadHealthConnectData() {
        if (!_uiState.value.isHealthConnectAvailable || !_uiState.value.isHealthConnectPermissionGranted) {
            return
        }

        _uiState.update {
            it.copy(
                isHealthConnectLoading = true,
                healthConnectError = null
            )
        }

        runCatching {
            val steps = healthConnectManager.readTodaySteps()
            val latestWeight = healthConnectManager.readLatestWeightKg()
            val dailyStepsHistory = healthConnectManager.readDailyStepsHistory(days = 7)
            val weightHistory = healthConnectManager.readWeightHistory(days = 90)
            HealthConnectImport(
                todaySteps = steps,
                latestWeightKg = latestWeight,
                dailyStepsHistory = dailyStepsHistory,
                weightHistory = weightHistory
            )
        }.onSuccess { import ->
            homeRepository.recordHealthConnectImport(
                importedSteps = import.todaySteps,
                latestWeightKg = import.latestWeightKg
            )
            homeRepository.updateStepsFromHealthConnect(
                currentSteps = import.todaySteps.toInt().coerceAtLeast(0)
            )

            _uiState.update {
                it.copy(
                    isHealthConnectLoading = false,
                    importedTodaySteps = import.todaySteps,
                    importedLatestWeightKg = import.latestWeightKg,
                    dailyStepsChartPoints = import.dailyStepsHistory.toBarChartPoints(),
                    weightChart = import.weightHistory.toWeightChart(),
                    healthConnectError = null
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isHealthConnectLoading = false,
                    importedTodaySteps = null,
                    importedLatestWeightKg = null,
                    dailyStepsChartPoints = emptyList(),
                    weightChart = emptyWeightChart(),
                    healthConnectError = error.message ?: "Failed to import from Health Connect."
                )
            }
        }
    }

    private fun observeHomeDailyQuest() {
        viewModelScope.launch {
            homeRepository.dashboardState.collect { dashboard ->
                _uiState.update {
                    it.copy(
                        displayedTodaySteps = dashboard.dailyQuest.currentSteps,
                        displayedStepsSourceType = dashboard.dailyQuest.sourceType,
                        importedTodaySteps = dashboard.healthConnectImportedSteps,
                        importedLatestWeightKg = dashboard.healthConnectLatestWeightKg
                    )
                }
            }
        }
    }

    private fun ProgressSummary.toUiState(): ProgressUiState {
        val weeklyDelta = last7DaySessions - previous7DaySessions
        val weeklyDeltaText = when {
            weeklyDelta > 0 -> "+$weeklyDelta vs previous 7 days"
            weeklyDelta < 0 -> "$weeklyDelta vs previous 7 days"
            else -> "Same as previous 7 days"
        }

        return ProgressUiState(
            isLoading = false,
            completedSessions = completedSessions,
            workoutDays = workoutDays,
            streakDays = streakDays,
            consistencyTitle = "$completedSessions Sessions",
            consistencySubtitle = weeklyDeltaText,
            streakSubtitle = if (streakDays > 0) "$streakDays-day continuity" else "No streak yet",
            bestLiftValue = if (bestLiftKg > 0) "$bestLiftKg kg" else "No lift logged",
            bestLiftSubtitle = "Heaviest completed set",
            volumeValue = "${formatWhole(totalVolumeKg)} kg",
            volumeSubtitle = if (latestSessionCompletedToday) {
                "+${formatWhole(latestSessionVolumeKg)} kg today"
            } else {
                "Across completed sessions"
            },
            personalBestsValue = "$totalSets sets logged",
            personalBestsSubtitle = if (latestSessionCompletedToday) {
                formatSessionLoadDeltaText(latestSessionSets)
            } else {
                "$totalReps reps total"
            },
            topExerciseText = topExerciseName?.let {
                val topVolume = topExerciseVolumeKg?.let(::formatWhole) ?: "0"
                "$it ($topVolume kg)"
            } ?: "Top volume exercise will appear here",
            sessionsLast7Days = last7DaySessions,
            sessionsPrevious7Days = previous7DaySessions
        )
    }

    private fun formatSessionLoadDeltaText(sessionSets: Int): String {
        return if (sessionSets > 0) "+$sessionSets sets today" else "Completed today"
    }

    private fun formatWhole(value: Double): String {
        return "%,d".format(value.roundToInt())
    }

    private data class HealthConnectImport(
        val todaySteps: Long,
        val latestWeightKg: Double?,
        val dailyStepsHistory: List<DailyStepsSample>,
        val weightHistory: List<WeightSample>
    )

    private fun List<DailyStepsSample>.toBarChartPoints(): List<DailyStepsBarChartPoint> {
        return map { sample ->
            DailyStepsBarChartPoint(
                dateLabel = sample.date.format(dayFormatter),
                steps = sample.steps
                    .coerceAtLeast(0L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            )
        }
    }

    private fun List<WeightSample>.toWeightChart(): SetMetricChartUiModel {
        return SetMetricChartUiModel(
            title = "Weight Progress",
            actualLabel = "Weight",
            actualValues = map { it.weightKg.toFloat().coerceAtLeast(0f) },
            targetValues = List(size) { null },
            unitLabel = "kg"
        )
    }

    companion object {
        private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    }
}

private fun emptyWeightChart(): SetMetricChartUiModel {
    return SetMetricChartUiModel(
        title = "Weight Progress",
        actualLabel = "Weight",
        actualValues = emptyList(),
        targetValues = emptyList(),
        unitLabel = "kg"
    )
}
