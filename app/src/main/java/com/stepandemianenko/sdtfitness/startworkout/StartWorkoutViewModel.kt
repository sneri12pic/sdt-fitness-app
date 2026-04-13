package com.stepandemianenko.sdtfitness.startworkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.SessionExerciseDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartWorkoutViewModel(
    application: Application
) : AndroidViewModel(application) {

    private data class DeletedExerciseSnapshot(
        val exercise: WorkoutExerciseUiModel,
        val index: Int
    )

    private val _uiState = MutableStateFlow(StartWorkoutFakeStateProvider.loadingState())
    val uiState: StateFlow<StartWorkoutUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<StartWorkoutEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<StartWorkoutEffect> = _effects.asSharedFlow()
    private var lastDeletedExercise: DeletedExerciseSnapshot? = null
    private val workoutSessionRepository = AppGraph.workoutSessionRepository(application)
    private val homeRepository = AppGraph.homeRepository(application)

    init {
        _uiState.value = StartWorkoutFakeStateProvider.emptyState()

        viewModelScope.launch {
            homeRepository.dashboardState.collect { dashboard ->
                val streakDays = dashboard.currentStreakCount
                _uiState.update { current ->
                    current.copy(
                        workoutPlan = current.workoutPlan?.withConsistencyStreak(streakDays)
                    )
                }
            }
        }
    }

    fun onEvent(event: StartWorkoutUiEvent) {
        when (event) {
            StartWorkoutUiEvent.RetryLoad -> loadDefaultPlan()
            StartWorkoutUiEvent.ShortenSessionClick -> applyShortenedSession()
            StartWorkoutUiEvent.AddExerciseClick -> openExercisePicker()
            is StartWorkoutUiEvent.CompleteOrSkipExercise -> completeOrSkipExercise(event.exerciseId)
            is StartWorkoutUiEvent.DeleteExercise -> deleteExercise(event.exerciseId)
            StartWorkoutUiEvent.UndoDeleteExercise -> undoDeleteExercise()
            StartWorkoutUiEvent.CloseExercisePickerClick -> closeExercisePicker()
            StartWorkoutUiEvent.ConfirmExerciseSelectionClick -> applyExerciseSelection()
            is StartWorkoutUiEvent.ToggleExerciseSelection -> toggleExerciseSelection(event.exerciseId)
            is StartWorkoutUiEvent.SaveCustomExerciseSet -> saveCustomExerciseSet(
                setId = event.setId,
                name = event.name,
                exerciseIds = event.exerciseIds
            )
            is StartWorkoutUiEvent.SelectCustomExerciseSet -> selectCustomExerciseSet(event.setId)
            is StartWorkoutUiEvent.DeleteCustomExerciseSet -> deleteCustomExerciseSet(event.setId)
            is StartWorkoutUiEvent.UpdateExerciseWeight -> updateExerciseTargets(
                exerciseId = event.exerciseId,
                weightKg = event.weightKg
            )
            is StartWorkoutUiEvent.UpdateExerciseReps -> updateExerciseTargets(
                exerciseId = event.exerciseId,
                reps = event.reps
            )
            StartWorkoutUiEvent.StartWorkoutClick -> startWorkout()
            StartWorkoutUiEvent.BackClick,
            StartWorkoutUiEvent.EditWorkoutClick,
            is StartWorkoutUiEvent.ExerciseClick -> Unit
        }
    }

    private fun loadDefaultPlan() {
        val currentStreakDays = homeRepository.dashboardState.value.currentStreakCount
        val contentState = StartWorkoutFakeStateProvider.contentState()
        _uiState.value = contentState.copy(
            workoutPlan = contentState.workoutPlan?.withConsistencyStreak(currentStreakDays)
        )
    }

    private fun applyShortenedSession() {
        _uiState.update { current ->
            val currentPlan = current.workoutPlan ?: return@update current
            if (current.isSessionShortened) return@update current

            val selectedExercises = buildExercisesFromSelection(
                isShortened = true,
                selectedExerciseIds = current.selectedExerciseIds,
                exerciseCatalog = current.exerciseCatalog
            )

            current.copy(
                isSessionShortened = true,
                workoutPlan = StartWorkoutFakeStateProvider.defaultPlan(isShortened = true).copy(
                    id = currentPlan.id,
                    headerTitle = currentPlan.headerTitle,
                    headerSubtitle = currentPlan.headerSubtitle,
                    streakCard = currentPlan.streakCard,
                    basedOnPlanText = currentPlan.basedOnPlanText,
                    selectedWorkoutsTitle = currentPlan.selectedWorkoutsTitle,
                    selectedWorkoutBadge = currentPlan.selectedWorkoutBadge,
                    exercises = if (selectedExercises.isEmpty()) {
                        StartWorkoutFakeStateProvider.defaultPlan(isShortened = true).exercises
                    } else {
                        selectedExercises
                    },
                    swapExerciseCard = currentPlan.swapExerciseCard,
                    primaryCtaText = currentPlan.primaryCtaText,
                    miniPlayer = currentPlan.miniPlayer
                )
            )
        }
    }

    private fun openExercisePicker() {
        _uiState.update { current ->
            val selectedFromPlan = current.workoutPlan
                ?.exercises
                ?.map { it.id }
                ?.toSet()
            val fallbackSelection = current.selectedExerciseIds

            current.copy(
                isSelectingExercises = true,
                selectedExerciseIds = selectedFromPlan ?: fallbackSelection,
                selectedCustomSetId = current.selectedCustomSetId?.takeIf { selectedId ->
                    current.customExerciseSets.any { it.id == selectedId }
                }
            )
        }
    }

    private fun completeOrSkipExercise(exerciseId: String) {
        lastDeletedExercise = null
        _uiState.update { current ->
            val plan = current.workoutPlan ?: return@update current
            current.copy(
                workoutPlan = plan.copy(
                    exercises = plan.exercises.filterNot { it.id == exerciseId }
                )
            )
        }
    }

    private fun deleteExercise(exerciseId: String) {
        _uiState.update { current ->
            val plan = current.workoutPlan ?: return@update current
            val targetIndex = plan.exercises.indexOfFirst { it.id == exerciseId }
            if (targetIndex == -1) return@update current

            val updatedExercises = plan.exercises.toMutableList()
            val removedExercise = updatedExercises.removeAt(targetIndex)
            lastDeletedExercise = DeletedExerciseSnapshot(
                exercise = removedExercise,
                index = targetIndex
            )

            current.copy(workoutPlan = plan.copy(exercises = updatedExercises))
        }
    }

    private fun undoDeleteExercise() {
        val snapshot = lastDeletedExercise ?: return
        _uiState.update { current ->
            val plan = current.workoutPlan ?: return@update current
            if (plan.exercises.any { it.id == snapshot.exercise.id }) return@update current

            val updatedExercises = plan.exercises.toMutableList()
            val insertIndex = snapshot.index.coerceIn(0, updatedExercises.size)
            updatedExercises.add(insertIndex, snapshot.exercise)

            current.copy(workoutPlan = plan.copy(exercises = updatedExercises))
        }
        lastDeletedExercise = null
    }

    private fun closeExercisePicker() {
        _uiState.update { it.copy(isSelectingExercises = false) }
    }

    private fun toggleExerciseSelection(exerciseId: String) {
        _uiState.update { current ->
            val activeSet = current.selectedCustomSetId
                ?.let { selectedSetId -> current.customExerciseSets.find { it.id == selectedSetId } }
            val wasSelected = current.selectedExerciseIds.contains(exerciseId)
            val updatedSelection = current.selectedExerciseIds.toMutableSet().apply {
                if (!add(exerciseId)) remove(exerciseId)
            }
            val shouldClearSelectedSet = activeSet != null &&
                wasSelected &&
                activeSet.exerciseIds.contains(exerciseId)

            current.copy(
                selectedExerciseIds = updatedSelection,
                selectedCustomSetId = if (shouldClearSelectedSet) null else current.selectedCustomSetId
            )
        }
    }

    private fun saveCustomExerciseSet(
        setId: String?,
        name: String,
        exerciseIds: Set<String>
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || exerciseIds.isEmpty()) return

        _uiState.update { current ->
            val normalizedExerciseIds = exerciseIds.toSet()
            val existing = setId?.let { id -> current.customExerciseSets.find { it.id == id } }
            val targetId = existing?.id ?: buildCustomSetId(
                name = normalizedName,
                existingIds = current.customExerciseSets.mapTo(mutableSetOf()) { it.id }
            )
            val updatedSet = CustomExerciseSetUiModel(
                id = targetId,
                name = normalizedName,
                exerciseIds = normalizedExerciseIds
            )

            val updatedSets = if (existing != null) {
                current.customExerciseSets.map { set ->
                    if (set.id == existing.id) updatedSet else set
                }
            } else {
                current.customExerciseSets + updatedSet
            }

            current.copy(
                customExerciseSets = updatedSets,
                selectedCustomSetId = updatedSet.id,
                selectedExerciseIds = normalizedExerciseIds
            )
        }
    }

    private fun selectCustomExerciseSet(setId: String) {
        _uiState.update { current ->
            val set = current.customExerciseSets.find { it.id == setId } ?: return@update current
            val isTappingAlreadySelectedSet = current.selectedCustomSetId == setId
            if (isTappingAlreadySelectedSet) {
                current.copy(
                    selectedCustomSetId = null,
                    selectedExerciseIds = current.selectedExerciseIds - set.exerciseIds
                )
            } else {
                current.copy(
                    selectedCustomSetId = set.id,
                    selectedExerciseIds = set.exerciseIds
                )
            }
        }
    }

    private fun deleteCustomExerciseSet(setId: String) {
        _uiState.update { current ->
            val updatedSets = current.customExerciseSets.filterNot { it.id == setId }
            val fallbackSelectedSetId = current.selectedCustomSetId?.takeIf { id ->
                updatedSets.any { it.id == id }
            }
            val fallbackSelectedExerciseIds = fallbackSelectedSetId
                ?.let { selectedId -> updatedSets.find { it.id == selectedId }?.exerciseIds }
                ?: current.selectedExerciseIds

            current.copy(
                customExerciseSets = updatedSets,
                selectedCustomSetId = fallbackSelectedSetId,
                selectedExerciseIds = fallbackSelectedExerciseIds
            )
        }
    }

    private fun applyExerciseSelection() {
        _uiState.update { current ->
            val selectedExercises = buildExercisesFromSelection(
                isShortened = current.isSessionShortened,
                selectedExerciseIds = current.selectedExerciseIds,
                exerciseCatalog = current.exerciseCatalog
            )

            if (selectedExercises.isEmpty()) {
                return@update current.copy(
                    isSelectingExercises = false,
                    workoutPlan = null
                )
            }

            val basePlan = current.workoutPlan
                ?: StartWorkoutFakeStateProvider.defaultPlan(isShortened = current.isSessionShortened)
            val currentStreakDays = homeRepository.dashboardState.value.currentStreakCount

            current.copy(
                isSelectingExercises = false,
                workoutPlan = basePlan
                    .withConsistencyStreak(currentStreakDays)
                    .copy(exercises = selectedExercises)
            )
        }
    }

    private fun buildExercisesFromSelection(
        isShortened: Boolean,
        selectedExerciseIds: Set<String>,
        exerciseCatalog: List<ExerciseCatalogItemUiModel>
    ): List<WorkoutExerciseUiModel> {
        if (selectedExerciseIds.isEmpty()) return emptyList()

        val template = StartWorkoutFakeStateProvider.exerciseTemplate(isShortened = isShortened)

        return exerciseCatalog
            .filter { selectedExerciseIds.contains(it.id) }
            .map { item ->
                template.copy(
                    id = item.id,
                    name = item.title
                )
            }
    }

    private fun updateExerciseTargets(
        exerciseId: String,
        weightKg: Int? = null,
        reps: Int? = null
    ) {
        _uiState.update { current ->
            val plan = current.workoutPlan ?: return@update current
            val updatedExercises = plan.exercises.map { exercise ->
                if (exercise.id != exerciseId) return@map exercise

                val updatedWeight = weightKg?.coerceAtLeast(0) ?: exercise.targetWeightKg
                val updatedReps = reps?.coerceAtLeast(1) ?: exercise.targetReps

                exercise.copy(
                    prescription = "@ $updatedWeight kg / $updatedReps reps",
                    targetWeightKg = updatedWeight,
                    targetReps = updatedReps
                )
            }

            current.copy(workoutPlan = plan.copy(exercises = updatedExercises))
        }
    }

    private fun startWorkout() {
        val currentPlan = _uiState.value.workoutPlan ?: return
        if (currentPlan.exercises.isEmpty()) return

        _uiState.update { it.copy(isStartingWorkout = true) }

        viewModelScope.launch {
            runCatching {
                val orderedExercises = currentPlan.exercises.map { exercise ->
                    SessionExerciseDraft(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        targetSets = exercise.targetSets,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg
                    )
                }

                workoutSessionRepository.startOrResumeSession(
                    templateId = currentPlan.id,
                    orderedExercises = orderedExercises
                )
            }.onSuccess { result ->
                _effects.emit(
                    StartWorkoutEffect.NavigateToOngoingWorkout(
                        sessionId = result.sessionId,
                        resumedExisting = result.resumedExisting
                    )
                )
            }

            _uiState.update { it.copy(isStartingWorkout = false) }
        }
    }

    private fun buildCustomSetId(
        name: String,
        existingIds: Set<String>
    ): String {
        val slug = name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "custom_set" }
        val baseId = "custom_set_$slug"
        if (!existingIds.contains(baseId)) return baseId

        var index = 2
        var candidate = "${baseId}_$index"
        while (existingIds.contains(candidate)) {
            index += 1
            candidate = "${baseId}_$index"
        }
        return candidate
    }

    private fun WorkoutPlanUiModel.withConsistencyStreak(streakDays: Int): WorkoutPlanUiModel {
        return copy(
            streakCard = streakCard.copy(
                title = formatConsistencyStreakTitle(streakDays)
            )
        )
    }

    private fun formatConsistencyStreakTitle(streakDays: Int): String {
        return when {
            streakDays <= 0 -> "0-day consistency streak"
            streakDays == 1 -> "1-day consistency streak"
            else -> "$streakDays-day consistency streak"
        }
    }
}
