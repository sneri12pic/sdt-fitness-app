package com.stepandemianenko.sdtfitness.startworkout

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StartWorkoutViewModel : ViewModel() {

    private data class DeletedExerciseSnapshot(
        val exercise: WorkoutExerciseUiModel,
        val index: Int
    )

    private val _uiState = MutableStateFlow(StartWorkoutFakeStateProvider.loadingState())
    val uiState: StateFlow<StartWorkoutUiState> = _uiState.asStateFlow()
    private var lastDeletedExercise: DeletedExerciseSnapshot? = null

    init {
        _uiState.value = StartWorkoutFakeStateProvider.emptyState()
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
            StartWorkoutUiEvent.BackClick,
            StartWorkoutUiEvent.StartWorkoutClick,
            StartWorkoutUiEvent.EditWorkoutClick,
            is StartWorkoutUiEvent.ExerciseClick -> Unit
        }
    }

    private fun loadDefaultPlan() {
        _uiState.value = StartWorkoutFakeStateProvider.contentState()
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
            val fallbackSelection = if (current.selectedExerciseIds.isEmpty()) {
                StartWorkoutFakeStateProvider.defaultExerciseSelection()
            } else {
                current.selectedExerciseIds
            }

            current.copy(
                isSelectingExercises = true,
                selectedExerciseIds = selectedFromPlan ?: fallbackSelection
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
            val updatedSelection = current.selectedExerciseIds.toMutableSet().apply {
                if (!add(exerciseId)) remove(exerciseId)
            }
            current.copy(selectedExerciseIds = updatedSelection)
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

            current.copy(
                isSelectingExercises = false,
                workoutPlan = basePlan.copy(exercises = selectedExercises)
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
}
