package com.stepandemianenko.sdtfitness.progress

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.health.DailyStepsSample
import com.stepandemianenko.sdtfitness.data.health.WeightSample
import com.stepandemianenko.sdtfitness.home.DailyStepsSourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProgressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val getProgressSnapshot = AppGraph.getProgressSnapshotUseCase(application)
    private val accountSessionManager = AppGraph.accountSessionManager(application)
    private val healthConnectManager = AppGraph.healthConnectManager(application)
    private val homeRepository = AppGraph.homeRepository(application)

    private val _uiState = MutableStateFlow(ProgressUiState(isInitialLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var progressRefreshJob: Job? = null
    private var observedAccountId: String? = null
    private var observedAccountRevision: Long? = null

    init {
        observeAccountScopeChanges()
        observeHomeDailyQuest()
        refresh()
        refreshHealthConnectState()
    }

    private fun observeAccountScopeChanges() {
        viewModelScope.launch {
            accountSessionManager.accountScope.collect { scope ->
                val isAccountChange = observedAccountId != null && observedAccountId != scope.accountId
                val isScopeRevisionChange = observedAccountRevision != null && observedAccountRevision != scope.revision
                observedAccountId = scope.accountId
                observedAccountRevision = scope.revision
                if (isAccountChange || isScopeRevisionChange) {
                    getProgressSnapshot.clearCache()
                    _uiState.update { current ->
                        current.copy(
                            data = null,
                            isInitialLoading = true,
                            isRefreshing = false,
                            errorMessage = null,
                            displayedTodaySteps = 0,
                            displayedStepsSourceType = DailyStepsSourceType.MANUAL,
                            importedTodaySteps = null,
                            importedLatestWeightKg = null,
                            dailyStepsChartPoints = emptyList(),
                            weightChart = emptyWeightChart(),
                            healthConnectError = null
                        )
                    }
                }
                refresh()
            }
        }
    }

    fun refresh() {
        progressRefreshJob?.cancel()
        progressRefreshJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isInitialLoading = current.data == null,
                    isRefreshing = current.data != null,
                    errorMessage = null
                )
            }

            var firstEmission = true
            runCatching {
                getProgressSnapshot().collect { snapshot ->
                    _uiState.update { current ->
                        current.copy(
                            data = snapshot,
                            isInitialLoading = false,
                            isRefreshing = firstEmission && current.data != null && current.data == snapshot,
                            errorMessage = null
                        )
                    }
                    firstEmission = false
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    current.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load progress."
                    )
                }
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
            val todayWeight = healthConnectManager.readTodayWeightSample()
            val dailyStepsHistory = healthConnectManager.readDailyStepsHistory(days = 7)
            val weightHistory = healthConnectManager.readWeightHistory(days = 90)
            HealthConnectImport(
                todaySteps = steps,
                latestWeightKg = latestWeight,
                todayWeightKg = todayWeight?.weightKg,
                todayWeightRecordedAt = todayWeight?.time,
                dailyStepsHistory = dailyStepsHistory,
                weightHistory = weightHistory
            )
        }.onSuccess { import ->
            homeRepository.recordHealthConnectImport(
                importedSteps = import.todaySteps,
                latestWeightKg = import.latestWeightKg,
                todayWeightKg = import.todayWeightKg,
                todayWeightRecordedAt = import.todayWeightRecordedAt
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

    private data class HealthConnectImport(
        val todaySteps: Long,
        val latestWeightKg: Double?,
        val todayWeightKg: Double?,
        val todayWeightRecordedAt: java.time.Instant?,
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
