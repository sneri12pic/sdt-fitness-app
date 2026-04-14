package com.stepandemianenko.sdtfitness.startworkout

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import com.stepandemianenko.sdtfitness.data.repository.LogSetOutcome
import com.stepandemianenko.sdtfitness.data.repository.LoggedSetUpdateDraft
import com.stepandemianenko.sdtfitness.data.repository.LogWorkoutExerciseSnapshot
import com.stepandemianenko.sdtfitness.data.repository.LogWorkoutSessionSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LogWorkoutViewModel(
    application: Application
) : AndroidViewModel(application) {

    private data class SetInputDraft(
        val weight: String,
        val reps: String
    )

    private val repository = AppGraph.workoutSessionRepository(application)
    private val homeRepository = AppGraph.homeRepository(application)
    private val preferences = application.getSharedPreferences(
        REST_TIMER_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private var hasSeenRestTimerHint = preferences.getBoolean(REST_TIMER_HINT_KEY, false)
    private var restTimerState: RestTimerState = RestTimerState.Inactive
    private var restTimerRemainingSeconds: Int = 0
    private var restTimerJob: Job? = null

    private val _uiState = MutableStateFlow(
        LogWorkoutUiState(
            hasSeenRestTimerHint = hasSeenRestTimerHint,
            restTimer = toRestTimerUiState()
        )
    )
    val uiState: StateFlow<LogWorkoutUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LogWorkoutEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<LogWorkoutEffect> = _effects.asSharedFlow()

    private var observeSessionJob: Job? = null
    private var didAttachSession = false
    private var latestSnapshot: LogWorkoutSessionSnapshot? = null
    private var completionEffectSessionId: Long? = null

    private var restTimerEnabledExerciseIds: Set<Long> = emptySet()
    private var setInputDrafts: Map<String, SetInputDraft> = emptyMap()
    private var selectedRpeBySet: Map<String, Int> = emptyMap()
    private var pendingSuggestedWeightByExercise: Map<Long, Int> = emptyMap()
    private var activeFeedbackSetKey: String? = null
    private var feedbackMessageBySet: Map<String, String> = emptyMap()
    private var feedbackAutoDismissJob: Job? = null

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
                        message = "No active workout session. Start one from Start Workout."
                    )
                }
                return@launch
            }

            observeSessionJob?.cancel()
            observeSessionJob = launch {
                repository.observeLogWorkoutSession(resolvedSessionId).collect { snapshot ->
                    if (snapshot == null) {
                        latestSnapshot = null
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasActiveSession = false,
                                message = "No active workout session found."
                            )
                        }
                        return@collect
                    }

                    latestSnapshot = snapshot
                    rebuildUiState()

                    if (
                        snapshot.status == WorkoutSessionStatus.COMPLETED &&
                        completionEffectSessionId != snapshot.sessionId
                    ) {
                        completionEffectSessionId = snapshot.sessionId
                        homeRepository.setTodayWorkoutCompleted(completed = true)
                        _effects.emit(LogWorkoutEffect.NavigateToProgress(snapshot.sessionId))
                    }
                }
            }
        }
    }

    fun onEvent(event: LogWorkoutUiEvent) {
        when (event) {
            LogWorkoutUiEvent.OnBackClicked -> Unit
            LogWorkoutUiEvent.OnFinishWorkoutClicked -> finishWorkout()
            is LogWorkoutUiEvent.OnToggleSetCompleted -> toggleSetCompleted(
                exerciseId = event.exerciseId,
                setId = event.setId
            )
            is LogWorkoutUiEvent.OnAddSet -> addSet(exerciseId = event.exerciseId)
            LogWorkoutUiEvent.OnAddExercise -> openAddExercise()
            is LogWorkoutUiEvent.OnUpdateSetWeight -> updateSetWeight(
                exerciseId = event.exerciseId,
                setId = event.setId,
                weight = event.weight
            )
            is LogWorkoutUiEvent.OnUpdateSetReps -> updateSetReps(
                exerciseId = event.exerciseId,
                setId = event.setId,
                reps = event.reps
            )
            is LogWorkoutUiEvent.OnSelectRpe -> selectRpe(
                exerciseId = event.exerciseId,
                setId = event.setId,
                rpe = event.rpe
            )
            is LogWorkoutUiEvent.OnDismissFeedback -> dismissFeedback(
                exerciseId = event.exerciseId,
                setId = event.setId
            )
            is LogWorkoutUiEvent.OnToggleRestTimer -> toggleRestTimer(exerciseId = event.exerciseId)
            LogWorkoutUiEvent.OnHeaderTimerTapped -> handleHeaderTimerTapped()
            is LogWorkoutUiEvent.OnStartRestTimer -> startRestTimer(event.durationSeconds)
            LogWorkoutUiEvent.OnExtendRestTimerByTenSeconds -> extendRestTimerByTenSeconds()
            LogWorkoutUiEvent.OnDismissRestTimer -> dismissRestTimer()
            LogWorkoutUiEvent.OnDiscardWorkoutClicked -> openDiscardConfirmation()
            LogWorkoutUiEvent.OnDismissDiscardConfirmation -> dismissDiscardConfirmation()
            LogWorkoutUiEvent.OnConfirmDiscardWorkout -> discardWorkout()
        }
    }

    private fun finishWorkout() {
        val sessionId = latestSnapshot?.sessionId ?: return
        viewModelScope.launch {
            if (repository.completeSession(sessionId)) {
                completionEffectSessionId = sessionId
                homeRepository.setTodayWorkoutCompleted(completed = true)
                _effects.emit(LogWorkoutEffect.NavigateToProgress(sessionId))
            }
        }
    }

    private fun toggleSetCompleted(exerciseId: Long, setId: String) {
        val snapshot = latestSnapshot ?: return
        val setNumber = parseSetNumber(setId) ?: return
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId } ?: return
        val loggedSet = exercise.loggedSets.firstOrNull { it.setNumber == setNumber }
        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)

        if (loggedSet != null) {
            viewModelScope.launch {
                if (
                    repository.unlogSetForExercise(
                        sessionId = snapshot.sessionId,
                        sessionExerciseId = exercise.id,
                        setNumber = setNumber
                    )
                ) {
                    val exerciseKeyPrefix = "$exerciseId:"
                    selectedRpeBySet = selectedRpeBySet.filterKeys { key ->
                        !key.startsWith(exerciseKeyPrefix)
                    }
                    feedbackMessageBySet = feedbackMessageBySet.filterKeys { key ->
                        !key.startsWith(exerciseKeyPrefix)
                    }
                    pendingSuggestedWeightByExercise = pendingSuggestedWeightByExercise - exerciseId
                    if (activeFeedbackSetKey == setKey) {
                        activeFeedbackSetKey = null
                        feedbackAutoDismissJob?.cancel()
                        feedbackAutoDismissJob = null
                    }
                    rebuildUiState()
                }
            }
            return
        }

        val nextSetNumber = nextPendingSetNumberForExercise(exerciseId = exercise.id, snapshot = snapshot)
        if (setNumber != nextSetNumber) {
            return
        }
        val draft = setInputDrafts[setKey]

        val loggedWeight = draft?.weight?.toIntOrNull() ?: exercise.targetWeightKg
        val loggedReps = draft?.reps?.toIntOrNull() ?: exercise.targetReps
        val selectedRpe = selectedRpeBySet[setKey]
        val feedbackMessage = buildSetFeedbackMessage(
            exercise = exercise,
            loggedWeightKg = loggedWeight,
            loggedReps = loggedReps
        )

        viewModelScope.launch {
            val outcome = repository.logSetForExercise(
                sessionId = snapshot.sessionId,
                sessionExerciseId = exercise.id,
                actualWeightKg = loggedWeight,
                actualReps = loggedReps,
                rpe = selectedRpe
            )

            if (outcome != LogSetOutcome.NoActiveSession) {
                activeFeedbackSetKey = setKey
                feedbackMessageBySet = feedbackMessageBySet + (setKey to feedbackMessage)
                scheduleFeedbackAutoDismiss(setKey)
                rebuildUiState()
            }

            if (outcome is LogSetOutcome.SessionCompleted) {
                completionEffectSessionId = outcome.sessionId
                homeRepository.setTodayWorkoutCompleted(completed = true)
                _effects.emit(LogWorkoutEffect.NavigateToProgress(outcome.sessionId))
            }
        }
    }

    private fun addSet(exerciseId: Long) {
        val snapshot = latestSnapshot ?: return
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId } ?: return
        val pendingSuggestedWeight = pendingSuggestedWeightByExercise[exerciseId]
        val nextSetNumber = exercise.targetSets.coerceAtLeast(1) + 1
        val nextSetKey = buildSetKey(exerciseId = exerciseId, setNumber = nextSetNumber)
        val previousSetNumber = exercise.targetSets.coerceAtLeast(1)
        val previousSetKey = buildSetKey(exerciseId = exerciseId, setNumber = previousSetNumber)
        val loggedBySetNumber = exercise.loggedSets.associateBy { it.setNumber }
        val previousWeight = setInputDrafts[previousSetKey]?.weight
            ?.takeIf { it.isNotBlank() }
            ?: loggedBySetNumber[previousSetNumber]?.actualWeightKg?.toString()
            ?: if (exercise.previousResult != null) exercise.targetWeightKg.toString() else ""
        val previousReps = setInputDrafts[previousSetKey]?.reps
            ?.takeIf { it.isNotBlank() }
            ?: loggedBySetNumber[previousSetNumber]?.actualReps?.toString()
            ?: if (exercise.previousResult != null) exercise.targetReps.toString() else ""
        val nextWeight = pendingSuggestedWeight?.toString() ?: previousWeight

        viewModelScope.launch {
            if (
                repository.increaseExerciseTargetSets(
                    sessionId = snapshot.sessionId,
                    sessionExerciseId = exercise.id,
                    delta = 1
                )
            ) {
                val currentDraft = setInputDrafts[nextSetKey] ?: SetInputDraft(weight = "", reps = "")
                setInputDrafts = setInputDrafts + (
                    nextSetKey to currentDraft.copy(
                        weight = nextWeight,
                        reps = previousReps
                    )
                )
                if (pendingSuggestedWeight != null) {
                    pendingSuggestedWeightByExercise = pendingSuggestedWeightByExercise - exerciseId
                }
                rebuildUiState()
            }
        }
    }

    private fun openAddExercise() {
        viewModelScope.launch {
            _effects.emit(LogWorkoutEffect.OpenAddExercise)
        }
    }

    private fun updateSetWeight(exerciseId: Long, setId: String, weight: String) {
        val setNumber = parseSetNumber(setId) ?: return
        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)
        val currentDraft = setInputDrafts[setKey] ?: SetInputDraft(weight = "", reps = "")
        val sanitized = sanitizeNumericInput(weight)

        setInputDrafts = setInputDrafts + (setKey to currentDraft.copy(weight = sanitized))
        rebuildUiState()
        persistCompletedSetChange(
            exerciseId = exerciseId,
            setNumber = setNumber,
            updatedWeight = sanitized.toIntOrNull(),
            updatedReps = null,
            updatedRpe = null
        )
    }

    private fun updateSetReps(exerciseId: Long, setId: String, reps: String) {
        val setNumber = parseSetNumber(setId) ?: return
        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)
        val currentDraft = setInputDrafts[setKey] ?: SetInputDraft(weight = "", reps = "")
        val sanitized = sanitizeNumericInput(reps)

        setInputDrafts = setInputDrafts + (setKey to currentDraft.copy(reps = sanitized))
        rebuildUiState()
        persistCompletedSetChange(
            exerciseId = exerciseId,
            setNumber = setNumber,
            updatedWeight = null,
            updatedReps = sanitized.toIntOrNull(),
            updatedRpe = null
        )
    }

    private fun selectRpe(exerciseId: Long, setId: String, rpe: Int) {
        val setNumber = parseSetNumber(setId) ?: return
        val snapshot = latestSnapshot ?: return
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId } ?: return
        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)
        val normalizedRpe = rpe.coerceIn(1, 10)
        selectedRpeBySet = selectedRpeBySet + (setKey to normalizedRpe)
        rememberPendingSuggestedWeight(
            exercise = exercise,
            setNumber = setNumber,
            selectedRpe = normalizedRpe
        )
        rebuildUiState()
        persistCompletedSetChange(
            exerciseId = exerciseId,
            setNumber = setNumber,
            updatedWeight = null,
            updatedReps = null,
            updatedRpe = normalizedRpe
        )
    }

    private fun dismissFeedback(exerciseId: Long, setId: String) {
        val setNumber = parseSetNumber(setId) ?: return
        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)
        if (activeFeedbackSetKey == setKey) {
            activeFeedbackSetKey = null
            feedbackMessageBySet = feedbackMessageBySet - setKey
            feedbackAutoDismissJob?.cancel()
            feedbackAutoDismissJob = null
            rebuildUiState()
        }
    }

    private fun toggleRestTimer(exerciseId: Long) {
        restTimerEnabledExerciseIds = if (restTimerEnabledExerciseIds.contains(exerciseId)) {
            restTimerEnabledExerciseIds - exerciseId
        } else {
            restTimerEnabledExerciseIds + exerciseId
        }
        rebuildUiState()
    }

    private fun handleHeaderTimerTapped() {
        viewModelScope.launch {
            if (restTimerState == RestTimerState.Running || restTimerState == RestTimerState.Finished) {
                _effects.emit(LogWorkoutEffect.OpenRestTimer)
                return@launch
            }

            if (!hasSeenRestTimerHint) {
                hasSeenRestTimerHint = true
                preferences.edit().putBoolean(REST_TIMER_HINT_KEY, true).apply()
                synchronizeRestTimerUiState()
                _effects.emit(
                    LogWorkoutEffect.ShowSnackbar(
                        message = "Need a rest timer? Swipe right to open it."
                    )
                )
                return@launch
            }

            _effects.emit(LogWorkoutEffect.OpenRestTimer)
        }
    }

    private fun startRestTimer(durationSeconds: Int) {
        val safeDuration = durationSeconds.coerceAtLeast(1)
        restTimerState = RestTimerState.Running
        restTimerRemainingSeconds = safeDuration
        startRestTimerCountdown()
        synchronizeRestTimerUiState()
    }

    private fun extendRestTimerByTenSeconds() {
        when (restTimerState) {
            RestTimerState.Running -> {
                restTimerRemainingSeconds = (restTimerRemainingSeconds + REST_TIMER_EXTENSION_SECONDS)
                    .coerceAtMost(MAX_REST_TIMER_SECONDS)
            }

            RestTimerState.Finished -> {
                restTimerState = RestTimerState.Running
                restTimerRemainingSeconds = REST_TIMER_EXTENSION_SECONDS
                startRestTimerCountdown()
            }

            RestTimerState.Inactive,
            RestTimerState.Dismissed -> {
                startRestTimer(REST_TIMER_EXTENSION_SECONDS)
                return
            }
        }
        synchronizeRestTimerUiState()
    }

    private fun dismissRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        restTimerState = RestTimerState.Dismissed
        restTimerRemainingSeconds = 0
        synchronizeRestTimerUiState()
    }

    private fun startRestTimerCountdown() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (restTimerState == RestTimerState.Running && restTimerRemainingSeconds > 0) {
                delay(1_000L)
                if (restTimerState != RestTimerState.Running) {
                    return@launch
                }
                restTimerRemainingSeconds = (restTimerRemainingSeconds - 1).coerceAtLeast(0)
                if (restTimerRemainingSeconds == 0) {
                    restTimerState = RestTimerState.Finished
                }
                synchronizeRestTimerUiState()
            }
        }
    }

    private fun openDiscardConfirmation() {
        _uiState.update { current ->
            if (current.isDiscarding || !current.hasActiveSession) {
                current
            } else {
                current.copy(showDiscardConfirmation = true)
            }
        }
    }

    private fun dismissDiscardConfirmation() {
        _uiState.update { current ->
            if (current.isDiscarding) {
                current
            } else {
                current.copy(showDiscardConfirmation = false)
            }
        }
    }

    private fun discardWorkout() {
        val snapshot = latestSnapshot ?: return
        val currentState = _uiState.value
        if (currentState.isDiscarding) return

        _uiState.update { it.copy(isDiscarding = true) }
        viewModelScope.launch {
            val discarded = repository.discardSession(snapshot.sessionId)
            if (discarded) {
                latestSnapshot = null
                completionEffectSessionId = null
                restTimerEnabledExerciseIds = emptySet()
                restTimerJob?.cancel()
                restTimerJob = null
                restTimerState = RestTimerState.Inactive
                restTimerRemainingSeconds = 0
                setInputDrafts = emptyMap()
                selectedRpeBySet = emptyMap()
                pendingSuggestedWeightByExercise = emptyMap()
                activeFeedbackSetKey = null
                feedbackMessageBySet = emptyMap()
                feedbackAutoDismissJob?.cancel()
                feedbackAutoDismissJob = null
                homeRepository.setTodayWorkoutCompleted(completed = false)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasActiveSession = false,
                        session = null,
                        hasSeenRestTimerHint = hasSeenRestTimerHint,
                        restTimer = toRestTimerUiState(),
                        message = "Workout discarded.",
                        showDiscardConfirmation = false,
                        isDiscarding = false
                    )
                }
                _effects.emit(LogWorkoutEffect.CloseAfterDiscard)
            } else {
                _uiState.update {
                    it.copy(
                        showDiscardConfirmation = false,
                        isDiscarding = false,
                        message = "Couldn't discard this workout right now."
                    )
                }
            }
        }
    }

    private fun rebuildUiState() {
        val snapshot = latestSnapshot
        if (snapshot == null) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    hasActiveSession = false,
                    session = null,
                    message = "No active workout session found."
                )
            }
            return
        }

        val orderedExercises = snapshot.exercises.sortedBy { it.exerciseOrder }

        val displayedSetKeys = buildSetKeySpace(orderedExercises)
        selectedRpeBySet = selectedRpeBySet.filterKeys(displayedSetKeys::contains)
        feedbackMessageBySet = feedbackMessageBySet.filterKeys(displayedSetKeys::contains)
        if (activeFeedbackSetKey != null && !displayedSetKeys.contains(activeFeedbackSetKey)) {
            activeFeedbackSetKey = null
            feedbackAutoDismissJob?.cancel()
            feedbackAutoDismissJob = null
        }

        val totalTargetSets = snapshot.totalSetsTarget.coerceAtLeast(1)

        val exercises = orderedExercises.map { exercise ->
            toExerciseUiModel(
                snapshot = snapshot,
                exercise = exercise
            )
        }

        val now = System.currentTimeMillis()
        val durationText = formatDuration(startedAt = snapshot.startedAt, now = now)
        val volumeText = "${snapshot.totalVolumeCompleted.roundToInt()} kg"
        val setsText = "${snapshot.totalSetsCompleted}/$totalTargetSets"
        val progress = (snapshot.totalSetsCompleted.toFloat() / totalTargetSets.toFloat())
            .coerceIn(0f, 1f)

        _uiState.value = LogWorkoutUiState(
            isLoading = false,
            hasActiveSession = snapshot.status != WorkoutSessionStatus.COMPLETED,
            session = WorkoutSessionUiModel(
                id = snapshot.sessionId,
                durationText = durationText,
                volumeText = volumeText,
                setsText = setsText,
                progress = progress,
                exercises = exercises
            ),
            hasSeenRestTimerHint = hasSeenRestTimerHint,
            restTimer = toRestTimerUiState(),
            message = null,
            showDiscardConfirmation = _uiState.value.showDiscardConfirmation,
            isDiscarding = _uiState.value.isDiscarding
        )
    }

    private fun toExerciseUiModel(
        snapshot: LogWorkoutSessionSnapshot,
        exercise: LogWorkoutExerciseSnapshot
    ): ExerciseUiModel {
        val totalSets = exercise.targetSets.coerceAtLeast(1)
        val loggedBySetNumber = exercise.loggedSets.associateBy { it.setNumber }
        val nextSetNumberForExercise = nextPendingSetNumberForExercise(exercise.id, snapshot)
        val previousText = exercise.previousResult?.let { "${it.weightKg}kg x ${it.reps}" }.orEmpty()

        val sets = (1..totalSets).map { setNumber ->
            val setKey = buildSetKey(exerciseId = exercise.id, setNumber = setNumber)
            val logged = loggedBySetNumber[setNumber]
            val draft = setInputDrafts[setKey]
            val fallbackWeight = defaultSetWeight(
                exercise = exercise,
                setNumber = setNumber,
                loggedBySetNumber = loggedBySetNumber
            )
            val fallbackReps = defaultSetReps(
                exercise = exercise,
                setNumber = setNumber,
                loggedBySetNumber = loggedBySetNumber
            )
            val currentWeight = logged?.actualWeightKg?.toString()
                ?: draft?.weight
                ?: fallbackWeight
            val currentReps = logged?.actualReps?.toString()
                ?: draft?.reps
                ?: fallbackReps
            val selectedRpe = selectedRpeBySet[setKey] ?: logged?.rpe

            WorkoutSetUiModel(
                id = setKey,
                setNumber = setNumber,
                previousPerformance = previousText,
                weight = currentWeight,
                reps = currentReps,
                isCompleted = logged != null,
                isCompletionEnabled = logged == null && setNumber == nextSetNumberForExercise,
                activeFeedbackVisible = activeFeedbackSetKey == setKey && logged != null,
                feedbackMessage = feedbackMessageBySet[setKey],
                selectedRpe = selectedRpe,
                suggestedNextWeight = null
            )
        }

        val restTimerOn = restTimerEnabledExerciseIds.contains(exercise.id)

        return ExerciseUiModel(
            id = exercise.id,
            name = exercise.exerciseName,
            notes = "",
            thumbnailRes = com.stepandemianenko.sdtfitness.R.drawable.home_workout_thumb,
            isRestTimerOn = restTimerOn,
            restTimerStatusText = if (restTimerOn) "Rest Timer: ON" else "Rest Timer: OFF",
            sets = sets
        )
    }

    private fun nextPendingSetNumberForExercise(
        exerciseId: Long,
        snapshot: LogWorkoutSessionSnapshot
    ): Int {
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId } ?: return 1
        val totalSets = exercise.targetSets.coerceAtLeast(1)
        return (exercise.loggedSets.size + 1).coerceIn(1, totalSets)
    }

    private fun buildSetKeySpace(exercises: List<LogWorkoutExerciseSnapshot>): Set<String> {
        val keys = mutableSetOf<String>()
        exercises.forEach { exercise ->
            val totalSets = exercise.targetSets.coerceAtLeast(1)
            (1..totalSets).forEach { setNumber ->
                keys += buildSetKey(exercise.id, setNumber)
            }
        }
        return keys
    }

    private fun suggestNextWeight(currentWeightKg: Int, selectedRpe: Int): Int {
        val adjustment = when (selectedRpe) {
            1 -> 2
            2, 3 -> 1
            4, 5, 6 -> 0
            7, 8 -> 0
            else -> -2
        }
        return (currentWeightKg + adjustment).coerceAtLeast(0)
    }

    private fun formatDuration(startedAt: Long, now: Long): String {
        val minutes = ((now - startedAt).coerceAtLeast(0L) / 60_000L).toInt()
        return "$minutes min"
    }

    private fun buildSetKey(exerciseId: Long, setNumber: Int): String {
        return "$exerciseId:$setNumber"
    }

    private fun parseSetNumber(setId: String): Int? {
        return setId.substringAfter(':', missingDelimiterValue = "").toIntOrNull()
    }

    private fun sanitizeNumericInput(raw: String): String {
        return raw.filter { it.isDigit() }.take(3)
    }

    private fun scheduleFeedbackAutoDismiss(setKey: String) {
        feedbackAutoDismissJob?.cancel()
        feedbackAutoDismissJob = viewModelScope.launch {
            delay(FEEDBACK_VISIBLE_DURATION_MS)
            if (activeFeedbackSetKey == setKey) {
                activeFeedbackSetKey = null
                feedbackMessageBySet = feedbackMessageBySet - setKey
                rebuildUiState()
            }
        }
    }

    private fun buildSetFeedbackMessage(
        exercise: LogWorkoutExerciseSnapshot,
        loggedWeightKg: Int,
        loggedReps: Int
    ): String {
        val previous = exercise.previousResult ?: return "Set logged successfully"
        val repDelta = loggedReps - previous.reps
        val weightDelta = loggedWeightKg - previous.weightKg

        return when {
            repDelta > 0 -> "$repDelta rep${if (repDelta == 1) "" else "s"} above last time"
            repDelta == 0 && weightDelta == 0 -> "Nice - matched previous set"
            repDelta == 0 -> "Good consistency"
            else -> "Set logged"
        }
    }

    private fun synchronizeRestTimerUiState() {
        _uiState.update { current ->
            current.copy(
                hasSeenRestTimerHint = hasSeenRestTimerHint,
                restTimer = toRestTimerUiState()
            )
        }
    }

    private fun toRestTimerUiState(): RestTimerUiState {
        return RestTimerUiState(
            state = restTimerState,
            remainingSeconds = restTimerRemainingSeconds.coerceAtLeast(0)
        )
    }

    private fun defaultSetWeight(
        exercise: LogWorkoutExerciseSnapshot,
        setNumber: Int,
        loggedBySetNumber: Map<Int, com.stepandemianenko.sdtfitness.data.repository.LoggedWorkoutSetSnapshot>
    ): String {
        if (exercise.previousResult != null) {
            return exercise.targetWeightKg.toString()
        }
        if (setNumber <= 1) {
            return ""
        }
        val previousSetKey = buildSetKey(exerciseId = exercise.id, setNumber = setNumber - 1)
        return setInputDrafts[previousSetKey]?.weight
            ?.takeIf { it.isNotBlank() }
            ?: loggedBySetNumber[setNumber - 1]?.actualWeightKg?.toString()
            ?: ""
    }

    private fun defaultSetReps(
        exercise: LogWorkoutExerciseSnapshot,
        setNumber: Int,
        loggedBySetNumber: Map<Int, com.stepandemianenko.sdtfitness.data.repository.LoggedWorkoutSetSnapshot>
    ): String {
        if (exercise.previousResult != null) {
            return exercise.targetReps.toString()
        }
        if (setNumber <= 1) {
            return ""
        }
        val previousSetKey = buildSetKey(exerciseId = exercise.id, setNumber = setNumber - 1)
        return setInputDrafts[previousSetKey]?.reps
            ?.takeIf { it.isNotBlank() }
            ?: loggedBySetNumber[setNumber - 1]?.actualReps?.toString()
            ?: ""
    }

    private fun rememberPendingSuggestedWeight(
        exercise: LogWorkoutExerciseSnapshot,
        setNumber: Int,
        selectedRpe: Int
    ) {
        val currentSetKey = buildSetKey(exerciseId = exercise.id, setNumber = setNumber)
        val loggedBySetNumber = exercise.loggedSets.associateBy { it.setNumber }
        val sourceWeight = setInputDrafts[currentSetKey]?.weight?.toIntOrNull()
            ?: loggedBySetNumber[setNumber]?.actualWeightKg
            ?: exercise.targetWeightKg
        val suggestedWeight = suggestNextWeight(
            currentWeightKg = sourceWeight,
            selectedRpe = selectedRpe
        )
        pendingSuggestedWeightByExercise = pendingSuggestedWeightByExercise + (exercise.id to suggestedWeight)
    }

    private fun persistCompletedSetChange(
        exerciseId: Long,
        setNumber: Int,
        updatedWeight: Int?,
        updatedReps: Int?,
        updatedRpe: Int?
    ) {
        val snapshot = latestSnapshot ?: return
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId } ?: return
        val loggedSet = exercise.loggedSets.firstOrNull { it.setNumber == setNumber } ?: return

        val setKey = buildSetKey(exerciseId = exerciseId, setNumber = setNumber)
        val draft = setInputDrafts[setKey]
        val nextWeight = updatedWeight
            ?: draft?.weight?.toIntOrNull()
            ?: loggedSet.actualWeightKg
        val nextReps = updatedReps
            ?: draft?.reps?.toIntOrNull()
            ?: loggedSet.actualReps
        val nextRpe = updatedRpe ?: selectedRpeBySet[setKey] ?: loggedSet.rpe

        viewModelScope.launch {
            repository.updateLoggedSet(
                sessionId = snapshot.sessionId,
                sessionExerciseId = exercise.id,
                setNumber = setNumber,
                update = LoggedSetUpdateDraft(
                    weightKg = nextWeight,
                    reps = nextReps,
                    rpe = nextRpe
                )
            )
        }
    }

    override fun onCleared() {
        feedbackAutoDismissJob?.cancel()
        feedbackAutoDismissJob = null
        restTimerJob?.cancel()
        restTimerJob = null
        observeSessionJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val REST_TIMER_PREFS_NAME = "ongoing_workout_preferences"
        const val REST_TIMER_HINT_KEY = "rest_timer_hint_seen"
        const val REST_TIMER_EXTENSION_SECONDS = 10
        const val MAX_REST_TIMER_SECONDS = 60 * 60
        const val FEEDBACK_VISIBLE_DURATION_MS = 10_000L
    }
}
