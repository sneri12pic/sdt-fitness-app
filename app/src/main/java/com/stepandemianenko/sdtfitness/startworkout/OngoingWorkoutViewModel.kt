package com.stepandemianenko.sdtfitness.startworkout

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OngoingWorkoutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OngoingWorkoutFakeStateProvider.contentState())
    val uiState: StateFlow<OngoingWorkoutUiState> = _uiState.asStateFlow()

    fun onEvent(event: OngoingWorkoutUiEvent) {
        when (event) {
            OngoingWorkoutUiEvent.BackClick,
            OngoingWorkoutUiEvent.EditClick -> Unit

            OngoingWorkoutUiEvent.WeightMinusClick -> updateWeight(delta = -1)
            OngoingWorkoutUiEvent.WeightPlusClick -> updateWeight(delta = 1)
            OngoingWorkoutUiEvent.RepsMinusClick -> updateReps(delta = -1)
            OngoingWorkoutUiEvent.RepsPlusClick -> updateReps(delta = 1)
            is OngoingWorkoutUiEvent.WeightPresetClick -> setWeight(event.value)
            is OngoingWorkoutUiEvent.RepsPresetClick -> setReps(event.value)
            is OngoingWorkoutUiEvent.RpeSelectClick -> selectRpe(event.index)
            OngoingWorkoutUiEvent.LogSetClick -> advanceSet()
        }
    }

    private fun updateWeight(delta: Int) {
        _uiState.update { current ->
            current.copy(loggedWeightKg = (current.loggedWeightKg + delta).coerceAtLeast(0))
        }
    }

    private fun updateReps(delta: Int) {
        _uiState.update { current ->
            current.copy(loggedReps = (current.loggedReps + delta).coerceAtLeast(1))
        }
    }

    private fun setWeight(value: Int) {
        _uiState.update { it.copy(loggedWeightKg = value) }
    }

    private fun setReps(value: Int) {
        _uiState.update { it.copy(loggedReps = value) }
    }

    private fun selectRpe(index: Int) {
        _uiState.update { current ->
            current.copy(selectedRpeIndex = index.coerceIn(0, current.rpeOptions.lastIndex))
        }
    }

    private fun advanceSet() {
        _uiState.update { current ->
            val nextCurrentSet = (current.currentSet + 1).coerceAtMost(current.totalSets)
            val nextCompletedSets = (current.completedSets + 1).coerceAtMost(current.totalSets)

            current.copy(
                currentSet = nextCurrentSet,
                completedSets = nextCompletedSets,
                previousResults = current.previousResults.copy(
                    lastSession = "${current.loggedWeightKg} x ${current.loggedReps} reps"
                )
            )
        }
    }
}
