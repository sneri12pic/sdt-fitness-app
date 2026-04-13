package com.stepandemianenko.sdtfitness

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.ProgressSummary
import com.stepandemianenko.sdtfitness.home.DailyStepsSourceType
import com.stepandemianenko.sdtfitness.home.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val healthConnectError: String? = null
)

class ProgressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.progressRepository(application)
    private val healthConnectManager = AppGraph.healthConnectManager(application)
    private val homeRepository = HomeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        observeHomeDailyQuest()
        refresh()
        refreshHealthConnectState()
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
                                healthConnectError = error.message ?: "Failed to check Health Connect permissions."
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isHealthConnectAvailable = true,
                            isHealthConnectPermissionGranted = hasPermissions,
                            healthConnectError = null
                        )
                    }

                    if (hasPermissions) {
                        loadHealthConnectData()
                    } else {
                        _uiState.update {
                            it.copy(
                                isHealthConnectLoading = false,
                                importedTodaySteps = null,
                                importedLatestWeightKg = null
                            )
                        }
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
            Pair(steps, latestWeight)
        }.onSuccess { (steps, latestWeight) ->
            val currentQuest = homeRepository.dashboardState.value.dailyQuest
            val shouldApplyImportedSteps = currentQuest.sourceType == DailyStepsSourceType.HEALTH_CONNECT ||
                (currentQuest.isManual && currentQuest.lastUpdatedMillis == null && currentQuest.currentSteps == 0)

            if (shouldApplyImportedSteps) {
                homeRepository.updateStepsFromHealthConnect(
                    currentSteps = steps.toInt().coerceAtLeast(0)
                )
            }

            _uiState.update {
                it.copy(
                    isHealthConnectLoading = false,
                    importedTodaySteps = steps,
                    importedLatestWeightKg = latestWeight,
                    healthConnectError = null
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isHealthConnectLoading = false,
                    importedTodaySteps = null,
                    importedLatestWeightKg = null,
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
                        displayedStepsSourceType = dashboard.dailyQuest.sourceType
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
}
