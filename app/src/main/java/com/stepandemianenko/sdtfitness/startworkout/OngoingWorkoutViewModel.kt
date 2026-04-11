package com.stepandemianenko.sdtfitness.startworkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import com.stepandemianenko.sdtfitness.data.repository.ActiveWorkoutSnapshot
import com.stepandemianenko.sdtfitness.data.repository.LogSetOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class OngoingWorkoutViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.workoutSessionRepository(application)

    private val _uiState = MutableStateFlow(OngoingWorkoutUiState())
    val uiState: StateFlow<OngoingWorkoutUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OngoingWorkoutEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<OngoingWorkoutEffect> = _effects.asSharedFlow()

    private var observeSessionJob: Job? = null
    private var currentSetToken: String? = null
    private var didAttachSession = false

    fun attachSession(initialSessionId: Long?) {
        if (didAttachSession) return
        didAttachSession = true

        viewModelScope.launch {
            val resolvedSessionId = initialSessionId ?: repository.getActiveSessionId()
            if (resolvedSessionId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasActiveSession = false,
                        completionMessage = "No active workout session. Start one from Start Workout."
                    )
                }
                return@launch
            }

            observeSessionJob?.cancel()
            observeSessionJob = launch {
                repository.observeSession(resolvedSessionId).collect { snapshot ->
                    if (snapshot == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasActiveSession = false,
                                completionMessage = "No active workout session found."
                            )
                        }
                        return@collect
                    }
                    applySnapshot(snapshot)
                }
            }
        }
    }

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
            OngoingWorkoutUiEvent.LogSetClick -> logCurrentSet()
        }
    }

    private fun updateWeight(delta: Int) {
        _uiState.update { current ->
            if (!current.hasActiveSession) return@update current
            current.copy(loggedWeightKg = (current.loggedWeightKg + delta).coerceAtLeast(0))
        }
    }

    private fun updateReps(delta: Int) {
        _uiState.update { current ->
            if (!current.hasActiveSession) return@update current
            current.copy(loggedReps = (current.loggedReps + delta).coerceAtLeast(1))
        }
    }

    private fun setWeight(value: Int) {
        _uiState.update { current ->
            if (!current.hasActiveSession) return@update current
            current.copy(loggedWeightKg = value.coerceAtLeast(0))
        }
    }

    private fun setReps(value: Int) {
        _uiState.update { current ->
            if (!current.hasActiveSession) return@update current
            current.copy(loggedReps = value.coerceAtLeast(1))
        }
    }

    private fun selectRpe(index: Int) {
        _uiState.update { current ->
            current.copy(selectedRpeIndex = index.coerceIn(0, current.rpeOptions.lastIndex))
        }
    }

    private fun logCurrentSet() {
        val sessionId = _uiState.value.sessionId ?: return
        if (!_uiState.value.hasActiveSession) return

        viewModelScope.launch {
            val currentState = _uiState.value
            val outcome = repository.logCurrentSet(
                sessionId = sessionId,
                actualWeightKg = currentState.loggedWeightKg,
                actualReps = currentState.loggedReps,
                rpe = currentState.selectedRpeIndex + 1
            )

            when (outcome) {
                is LogSetOutcome.AdvancedSet -> {
                    _uiState.update {
                        it.copy(
                            transitionMessage = "Set logged. Next: Set ${outcome.nextSetNumber} of ${outcome.totalSetsForExercise}",
                            completionMessage = null
                        )
                    }
                }

                is LogSetOutcome.AdvancedExercise -> {
                    _uiState.update {
                        it.copy(
                            transitionMessage = "${outcome.completedExerciseName} complete. Next: ${outcome.nextExerciseName}",
                            completionMessage = null
                        )
                    }
                }

                is LogSetOutcome.SessionCompleted -> {
                    _uiState.update {
                        it.copy(
                            transitionMessage = "${outcome.completedExerciseName} complete.",
                            completionMessage = "Workout complete. Great consistency."
                        )
                    }
                    _effects.emit(OngoingWorkoutEffect.NavigateToProgress(outcome.sessionId))
                }

                LogSetOutcome.NoActiveSession -> {
                    _uiState.update {
                        it.copy(
                            hasActiveSession = false,
                            completionMessage = "No active workout session found."
                        )
                    }
                }
            }
        }
    }

    private fun applySnapshot(snapshot: ActiveWorkoutSnapshot) {
        val currentExercise = snapshot.currentExercise
        val isActiveSession = snapshot.status == WorkoutSessionStatus.ACTIVE || snapshot.status == WorkoutSessionStatus.PLANNED
        val nextSetToken = "${snapshot.sessionId}:${currentExercise?.id}:${snapshot.currentSetNumber}"
        val shouldResetInputs = currentSetToken != nextSetToken

        val targetWeight = currentExercise?.targetWeightKg ?: 0
        val targetReps = currentExercise?.targetReps ?: 0

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                hasActiveSession = isActiveSession && currentExercise != null,
                sessionId = snapshot.sessionId,
                exerciseName = currentExercise?.exerciseName ?: "Workout Complete",
                exercisePositionText = if (snapshot.totalExercises > 0 && currentExercise != null) {
                    "Exercise ${snapshot.currentExerciseIndex + 1} of ${snapshot.totalExercises}"
                } else {
                    "Exercise 0 of 0"
                },
                totalSets = currentExercise?.targetSets ?: 0,
                currentSet = snapshot.currentSetNumber,
                totalSessionSets = snapshot.totalSetsTarget,
                completedSets = snapshot.completedSets,
                targetWeightKg = targetWeight,
                targetReps = targetReps,
                loggedWeightKg = if (shouldResetInputs) targetWeight else current.loggedWeightKg,
                loggedReps = if (shouldResetInputs) targetReps else current.loggedReps,
                weightPresets = buildWeightPresets(targetWeight),
                repsPresets = buildRepPresets(targetReps),
                selectedRpeIndex = if (shouldResetInputs) 2 else current.selectedRpeIndex,
                previousResults = OngoingPreviousResultsUiModel(
                    lastSession = snapshot.previousResult?.let { "${it.weightKg} x ${it.reps} reps" }
                        ?: "No previous completed set yet",
                    personalBest = snapshot.personalBest?.let { "${it.weightKg} x ${it.reps} reps" }
                        ?: "No personal best yet",
                    dateLabel = snapshot.previousResult?.completedAt?.toShortDate() ?: ""
                ),
                remainingSets = snapshot.remainingSets,
                estimatedTimeRemaining = "~${snapshot.estimatedMinutesRemaining} min",
                completionMessage = if (snapshot.status == WorkoutSessionStatus.COMPLETED) {
                    "Workout complete. Great consistency."
                } else {
                    current.completionMessage
                }
            )
        }

        currentSetToken = nextSetToken
    }

    private fun buildWeightPresets(targetWeightKg: Int): List<Int> {
        if (targetWeightKg <= 0) return listOf(0, 5, 10, 15)
        return listOf(targetWeightKg - 10, targetWeightKg - 5, targetWeightKg, targetWeightKg + 5)
            .map { it.coerceAtLeast(0) }
            .distinct()
            .sorted()
    }

    private fun buildRepPresets(targetReps: Int): List<Int> {
        if (targetReps <= 0) return listOf(6, 8, 10, 12)
        return listOf(targetReps - 2, targetReps, targetReps + 2, targetReps + 4)
            .map { it.coerceAtLeast(1) }
            .distinct()
            .sorted()
    }

    private fun Long.toShortDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }
}
