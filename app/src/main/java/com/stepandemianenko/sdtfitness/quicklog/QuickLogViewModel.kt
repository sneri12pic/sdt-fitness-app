package com.stepandemianenko.sdtfitness.quicklog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.home.HomeRepository
import com.stepandemianenko.sdtfitness.home.QuickLogType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val QuickLogDefaultDurations = listOf(5, 15, 20, 30)

data class QuickLogUiState(
    val selectedType: QuickLogType = QuickLogType.WALK,
    val selectedDurationMinutes: Int = QuickLogDefaultDurations.first(),
    val availableDurations: List<Int> = QuickLogDefaultDurations,
    val isSaving: Boolean = false
)

sealed interface QuickLogEvent {
    data object InitializeDefaults : QuickLogEvent
    data class SelectType(val type: QuickLogType) : QuickLogEvent
    data class SelectDuration(val minutes: Int) : QuickLogEvent
    data object SaveQuickLog : QuickLogEvent
}

sealed interface QuickLogEffect {
    data object Saved : QuickLogEffect
}

class QuickLogViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val homeRepository = HomeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(QuickLogUiState())
    val uiState: StateFlow<QuickLogUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<QuickLogEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<QuickLogEffect> = _effects.asSharedFlow()

    fun onEvent(event: QuickLogEvent) {
        when (event) {
            QuickLogEvent.InitializeDefaults -> resetToDefaults()
            is QuickLogEvent.SelectType -> {
                _uiState.update { it.copy(selectedType = event.type) }
            }

            is QuickLogEvent.SelectDuration -> {
                if (event.minutes !in _uiState.value.availableDurations) return
                _uiState.update { it.copy(selectedDurationMinutes = event.minutes) }
            }

            QuickLogEvent.SaveQuickLog -> saveQuickLog()
        }
    }

    private fun resetToDefaults() {
        _uiState.update {
            it.copy(
                selectedType = QuickLogType.WALK,
                selectedDurationMinutes = QuickLogDefaultDurations.first(),
                isSaving = false
            )
        }
    }

    private fun saveQuickLog() {
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val current = _uiState.value
            homeRepository.logTodayQuickActivity(
                type = current.selectedType,
                durationMinutes = current.selectedDurationMinutes,
                timestampMillis = System.currentTimeMillis(),
                source = MANUAL_QUICK_LOG_SOURCE
            )
            _effects.emit(QuickLogEffect.Saved)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    companion object {
        private const val MANUAL_QUICK_LOG_SOURCE = "manual_quick_log"
    }
}
