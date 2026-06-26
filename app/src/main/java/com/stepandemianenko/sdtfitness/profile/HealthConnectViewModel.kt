package com.stepandemianenko.sdtfitness.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepandemianenko.sdtfitness.App
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HealthConnectViewModel(
    private val healthConnect: HealthConnectManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as App).container
                HealthConnectViewModel(
                    healthConnect = container.healthConnectManager,
                    profileRepository = container.profileRepository
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    /** Permissions to ask for and the contract to launch the HC permission sheet (used by the screen). */
    val requestedPermissions: Set<String> get() = healthConnect.readPermissions
    fun permissionContract() = healthConnect.requestPermissionsContract()

    init {
        refresh()
    }

    /** Re-read connection status + (when connected) imported data, plus the in-app share summary. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val status = when {
                healthConnect.isProviderUpdateRequired() -> HealthConnectStatus.UPDATE_REQUIRED
                !healthConnect.isAvailable() -> HealthConnectStatus.UNAVAILABLE
                healthConnect.hasAllPermissions() -> HealthConnectStatus.CONNECTED
                else -> HealthConnectStatus.NOT_CONNECTED
            }

            val connected = status == HealthConnectStatus.CONNECTED
            val steps = if (connected) runCatching { healthConnect.readTodaySteps() }.getOrNull() else null
            val weight = if (connected) runCatching { healthConnect.readLatestWeightKg() }.getOrNull() else null
            val inApp = runCatching { profileRepository.inAppShareSummary() }.getOrDefault(InAppShareSummary())

            _uiState.update {
                it.copy(
                    status = status,
                    isLoading = false,
                    todaySteps = steps,
                    latestWeightKg = weight,
                    inApp = inApp
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            runCatching { healthConnect.disconnect() }
            refresh()
        }
    }
}
