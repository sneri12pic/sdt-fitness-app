package com.stepandemianenko.sdtfitness.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.health.WeightSample
import com.stepandemianenko.sdtfitness.progress.SetMetricChartUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiEvent {
    data object OpenDailyQuestEditor : HomeUiEvent
    data object DismissDailyQuestEditor : HomeUiEvent
    data object OpenAddCustomQuestDialog : HomeUiEvent
    data object DismissAddCustomQuestDialog : HomeUiEvent
    data object AddWeightInQuest : HomeUiEvent
    data object RemoveWeightInQuest : HomeUiEvent
    data object ToggleWeightInQuestCompletion : HomeUiEvent
    data object AddCreatineIntakeQuest : HomeUiEvent
    data object RemoveCreatineIntakeQuest : HomeUiEvent
    data object OpenCreatineOverlay : HomeUiEvent
    data object DismissCreatineOverlay : HomeUiEvent
    data object AddCreatinePortion : HomeUiEvent
    data object OpenCreatineTargetEditor : HomeUiEvent
    data object DismissCreatineTargetEditor : HomeUiEvent
    data class CreatineTargetInputChanged(val value: String) : HomeUiEvent
    data object SaveCreatineTarget : HomeUiEvent
    data object OpenCreatinePortionEditor : HomeUiEvent
    data object DismissCreatinePortionEditor : HomeUiEvent
    data class CreatinePortionInputChanged(val value: String) : HomeUiEvent
    data object SaveCreatinePortion : HomeUiEvent
    data class DeleteCreatinePortion(val logId: Long) : HomeUiEvent
    data object OpenWeightInChartDialog : HomeUiEvent
    data object DismissWeightInChartDialog : HomeUiEvent
    data class DailyQuestTargetInputChanged(val value: String) : HomeUiEvent
    data class DailyQuestCurrentInputChanged(val value: String) : HomeUiEvent
    data object SaveDailyQuestEditor : HomeUiEvent
    data class SelectRecoveryOption(val option: RecoveryOption) : HomeUiEvent
    data class SaveRecoveryOption(val option: RecoveryOption) : HomeUiEvent
    data object PreviousRoutineMonth : HomeUiEvent
    data object NextRoutineMonth : HomeUiEvent
    data object CreateTestUser : HomeUiEvent
    data class SwitchAccount(val accountId: String) : HomeUiEvent
    data object WipeCurrentAccountData : HomeUiEvent
    data object ConfirmAddImportedSteps : HomeUiEvent
    data object DeclineAddImportedSteps : HomeUiEvent
}

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = AppGraph.homeRepository(application)
    private val accountSessionManager = AppGraph.accountSessionManager(application)
    private val healthConnectManager = AppGraph.healthConnectManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dashboardState.collect { dashboard ->
                _uiState.update { current ->
                    current.copy(dashboard = dashboard)
                }
            }
        }

        viewModelScope.launch {
            accountSessionManager.observeAccounts().collect { accounts ->
                _uiState.update { current ->
                    current.copy(
                        accounts = accounts
                            .sortedBy { it.createdAt }
                            .map { account ->
                                DebugAccountUiModel(
                                    id = account.id,
                                    type = account.type,
                                    createdAt = account.createdAt,
                                    isActive = account.isActive
                                )
                            }
                    )
                }
            }
        }

        viewModelScope.launch {
            accountSessionManager.nonNullActiveAccountId.collect { accountId ->
                _uiState.update { current ->
                    current.copy(activeAccountId = accountId)
                }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OpenDailyQuestEditor -> openDailyQuestEditor()
            HomeUiEvent.DismissDailyQuestEditor -> {
                _uiState.update { it.copy(isDailyQuestEditorOpen = false) }
            }

            HomeUiEvent.OpenAddCustomQuestDialog -> {
                _uiState.update { it.copy(isAddCustomQuestDialogOpen = true) }
            }

            HomeUiEvent.DismissAddCustomQuestDialog -> {
                _uiState.update { it.copy(isAddCustomQuestDialogOpen = false) }
            }

            HomeUiEvent.AddWeightInQuest -> {
                repository.addTodayWeightInQuest()
                _uiState.update { it.copy(isAddCustomQuestDialogOpen = false) }
            }

            HomeUiEvent.RemoveWeightInQuest -> {
                repository.removeWeightInQuest()
                _uiState.update { it.copy(isWeightInChartDialogOpen = false) }
            }

            HomeUiEvent.ToggleWeightInQuestCompletion -> {
                val currentCompleted = _uiState.value.dashboard.weightInQuest.isCompleted
                repository.setTodayWeightInCompleted(completed = !currentCompleted)
            }

            HomeUiEvent.AddCreatineIntakeQuest -> {
                repository.addCreatineIntakeQuest()
                _uiState.update { it.copy(isAddCustomQuestDialogOpen = false) }
            }

            HomeUiEvent.RemoveCreatineIntakeQuest -> {
                repository.removeCreatineIntakeQuest()
                _uiState.update {
                    it.copy(
                        isCreatineOverlayOpen = false,
                        isCreatineTargetEditorOpen = false,
                        isCreatinePortionEditorOpen = false
                    )
                }
            }

            HomeUiEvent.OpenCreatineOverlay -> {
                _uiState.update { it.copy(isCreatineOverlayOpen = true) }
            }

            HomeUiEvent.DismissCreatineOverlay -> {
                _uiState.update {
                    it.copy(
                        isCreatineOverlayOpen = false,
                        isCreatineTargetEditorOpen = false,
                        isCreatinePortionEditorOpen = false,
                        creatineTargetError = null,
                        creatinePortionError = null
                    )
                }
            }

            HomeUiEvent.AddCreatinePortion -> repository.addTodayCreatinePortion()

            HomeUiEvent.OpenCreatineTargetEditor -> {
                val target = _uiState.value.dashboard.creatineIntakeQuest.targetGrams
                _uiState.update {
                    it.copy(
                        isCreatineTargetEditorOpen = true,
                        draftCreatineTargetGrams = target.toString(),
                        creatineTargetError = null
                    )
                }
            }

            HomeUiEvent.DismissCreatineTargetEditor -> {
                _uiState.update {
                    it.copy(
                        isCreatineTargetEditorOpen = false,
                        creatineTargetError = null
                    )
                }
            }

            is HomeUiEvent.CreatineTargetInputChanged -> {
                _uiState.update {
                    it.copy(
                        draftCreatineTargetGrams = sanitizeNumericInput(event.value),
                        creatineTargetError = null
                    )
                }
            }

            HomeUiEvent.SaveCreatineTarget -> saveCreatineTarget()

            HomeUiEvent.OpenCreatinePortionEditor -> {
                val portion = _uiState.value.dashboard.creatineIntakeQuest.portionGrams
                _uiState.update {
                    it.copy(
                        isCreatinePortionEditorOpen = true,
                        draftCreatinePortionGrams = portion.toString(),
                        creatinePortionError = null
                    )
                }
            }

            HomeUiEvent.DismissCreatinePortionEditor -> {
                _uiState.update {
                    it.copy(
                        isCreatinePortionEditorOpen = false,
                        creatinePortionError = null
                    )
                }
            }

            is HomeUiEvent.CreatinePortionInputChanged -> {
                _uiState.update {
                    it.copy(
                        draftCreatinePortionGrams = sanitizeNumericInput(event.value),
                        creatinePortionError = null
                    )
                }
            }

            HomeUiEvent.SaveCreatinePortion -> saveCreatinePortion()

            is HomeUiEvent.DeleteCreatinePortion -> {
                repository.deleteCreatinePortion(logId = event.logId)
            }

            HomeUiEvent.OpenWeightInChartDialog -> openWeightInChartDialog()

            HomeUiEvent.DismissWeightInChartDialog -> {
                _uiState.update { it.copy(isWeightInChartDialogOpen = false) }
            }

            is HomeUiEvent.DailyQuestTargetInputChanged -> {
                _uiState.update {
                    it.copy(draftTargetSteps = sanitizeNumericInput(event.value))
                }
            }

            is HomeUiEvent.DailyQuestCurrentInputChanged -> {
                _uiState.update {
                    it.copy(draftCurrentSteps = sanitizeNumericInput(event.value))
                }
            }

            HomeUiEvent.SaveDailyQuestEditor -> saveDailyQuest()
            is HomeUiEvent.SelectRecoveryOption -> {
                _uiState.update {
                    it.copy(
                        dashboard = it.dashboard.copy(
                            restDay = it.dashboard.restDay.copy(selectedOption = event.option)
                        )
                    )
                }
            }

            is HomeUiEvent.SaveRecoveryOption -> saveRecoveryOption(event.option)
            HomeUiEvent.PreviousRoutineMonth -> {
                _uiState.update { it.copy(visibleRoutineMonth = it.visibleRoutineMonth.minusMonths(1)) }
            }

            HomeUiEvent.NextRoutineMonth -> {
                _uiState.update { it.copy(visibleRoutineMonth = it.visibleRoutineMonth.plusMonths(1)) }
            }

            HomeUiEvent.CreateTestUser -> {
                viewModelScope.launch {
                    accountSessionManager.createTestUserAndSwitch()
                }
            }

            is HomeUiEvent.SwitchAccount -> {
                viewModelScope.launch {
                    accountSessionManager.switchActiveAccount(event.accountId)
                }
            }

            HomeUiEvent.WipeCurrentAccountData -> {
                viewModelScope.launch {
                    val activeAccountId = accountSessionManager.requireActiveAccountId()
                    accountSessionManager.wipeAccountData(activeAccountId)
                }
            }

            HomeUiEvent.ConfirmAddImportedSteps -> {
                confirmAddImportedSteps()
            }

            HomeUiEvent.DeclineAddImportedSteps -> {
                _uiState.update { it.copy(pendingHealthConnectStepsToAdd = null) }
            }
        }
    }

    private fun openDailyQuestEditor() {
        val currentQuest = _uiState.value.dashboard.dailyQuest
        _uiState.update {
            it.copy(
                isDailyQuestEditorOpen = true,
                draftTargetSteps = currentQuest.targetSteps.toString(),
                draftCurrentSteps = currentQuest.currentSteps.toString()
            )
        }
    }

    private fun saveDailyQuest() {
        val currentDashboard = _uiState.value.dashboard
        val target = _uiState.value.draftTargetSteps.toIntOrNull()
            ?: currentDashboard.dailyQuest.targetSteps
        val current = _uiState.value.draftCurrentSteps.toIntOrNull()
            ?: currentDashboard.dailyQuest.currentSteps

        repository.setManualDailyQuest(
            targetSteps = target,
            currentSteps = current
        )

        _uiState.update { it.copy(isDailyQuestEditorOpen = false) }
    }

    private fun saveCreatineTarget() {
        val target = _uiState.value.draftCreatineTargetGrams.toIntOrNull()
        if (target == null || target <= 0) {
            _uiState.update { it.copy(creatineTargetError = "Enter a target greater than 0 g") }
            return
        }

        repository.setCreatineTarget(targetGrams = target)
        _uiState.update {
            it.copy(
                isCreatineTargetEditorOpen = false,
                creatineTargetError = null
            )
        }
    }

    private fun saveCreatinePortion() {
        val portion = _uiState.value.draftCreatinePortionGrams.toIntOrNull()
        if (portion == null || portion <= 0) {
            _uiState.update { it.copy(creatinePortionError = "Enter a portion greater than 0 g") }
            return
        }

        repository.setCreatinePortion(portionGrams = portion)
        _uiState.update {
            it.copy(
                isCreatinePortionEditorOpen = false,
                creatinePortionError = null
            )
        }
    }

    private fun saveRecoveryOption(option: RecoveryOption) {
        repository.logTodayRecovery(option)
    }

    private fun openWeightInChartDialog() {
        _uiState.update { it.copy(isWeightInChartDialogOpen = true) }
        viewModelScope.launch {
            val canReadHealthConnect = runCatching {
                healthConnectManager.isAvailable() && healthConnectManager.hasAllPermissions()
            }.getOrDefault(false)

            val chart = if (canReadHealthConnect) {
                runCatching {
                    healthConnectManager.readWeightHistory(days = 90).toWeightChart()
                }.getOrDefault(emptyWeightChart())
            } else {
                emptyWeightChart()
            }

            _uiState.update { it.copy(weightInChart = chart) }
        }
    }

    fun syncHealthConnectSteps() {
        viewModelScope.launch {
            val canReadHealthConnect = runCatching {
                healthConnectManager.isAvailable() && healthConnectManager.hasAllPermissions()
            }.getOrDefault(false)

            if (!canReadHealthConnect) return@launch

            val importedSteps = runCatching {
                healthConnectManager.readTodaySteps()
            }.getOrNull() ?: return@launch

            val latestWeightKg = runCatching {
                healthConnectManager.readLatestWeightKg()
            }.getOrNull()

            val todayWeightSample = runCatching {
                healthConnectManager.readTodayWeightSample()
            }.getOrNull()

            repository.recordHealthConnectImport(
                importedSteps = importedSteps,
                latestWeightKg = latestWeightKg,
                todayWeightKg = todayWeightSample?.weightKg,
                todayWeightRecordedAt = todayWeightSample?.time
            )

            val normalizedImportedSteps = importedSteps
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            repository.updateStepsFromHealthConnect(currentSteps = normalizedImportedSteps)
            _uiState.update { it.copy(pendingHealthConnectStepsToAdd = null) }
        }
    }

    private fun confirmAddImportedSteps() {
        val imported = _uiState.value.pendingHealthConnectStepsToAdd ?: return
        val current = _uiState.value.dashboard.dailyQuest.currentSteps
        val target = _uiState.value.dashboard.dailyQuest.targetSteps
        val updatedCurrent = (current.toLong() + imported.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        repository.setManualDailyQuest(
            targetSteps = target,
            currentSteps = updatedCurrent
        )
        _uiState.update { it.copy(pendingHealthConnectStepsToAdd = null) }
    }

    private fun sanitizeNumericInput(input: String): String {
        return input.filter { it.isDigit() }
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

    private fun emptyWeightChart(): SetMetricChartUiModel {
        return SetMetricChartUiModel(
            title = "Weight Progress",
            actualLabel = "Weight",
            actualValues = emptyList(),
            targetValues = emptyList(),
            unitLabel = "kg"
        )
    }
}
