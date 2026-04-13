package com.stepandemianenko.sdtfitness.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiEvent {
    data object OpenDailyQuestEditor : HomeUiEvent
    data object DismissDailyQuestEditor : HomeUiEvent
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

    private fun saveRecoveryOption(option: RecoveryOption) {
        repository.logTodayRecovery(option)
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

            repository.recordHealthConnectImport(
                importedSteps = importedSteps,
                latestWeightKg = latestWeightKg
            )

            val normalizedImportedSteps = importedSteps
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val dailyQuest = _uiState.value.dashboard.dailyQuest

            if (dailyQuest.sourceType == DailyStepsSourceType.HEALTH_CONNECT) {
                repository.updateStepsFromHealthConnect(currentSteps = normalizedImportedSteps)
                return@launch
            }

            if (dailyQuest.currentSteps <= 0 || normalizedImportedSteps <= 0) {
                return@launch
            }

            _uiState.update {
                it.copy(pendingHealthConnectStepsToAdd = normalizedImportedSteps)
            }
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
}
