package com.stepandemianenko.sdtfitness.startworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepandemianenko.sdtfitness.App
import com.stepandemianenko.sdtfitness.data.repository.ExerciseCatalogRepository
import com.stepandemianenko.sdtfitness.data.repository.SessionExerciseDraft
import com.stepandemianenko.sdtfitness.data.repository.WorkoutPlanRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository
import com.stepandemianenko.sdtfitness.home.HomeRepository
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
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val exerciseCatalogRepository: ExerciseCatalogRepository,
    private val homeRepository: HomeRepository
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as App).container
                StartWorkoutViewModel(
                    workoutSessionRepository = container.workoutSessionRepository,
                    workoutPlanRepository = container.workoutPlanRepository,
                    exerciseCatalogRepository = container.exerciseCatalogRepository,
                    homeRepository = container.homeRepository
                )
            }
        }
    }

    private data class DeletedExerciseSnapshot(
        val exercise: WorkoutExerciseUiModel,
        val index: Int
    )

    private val _uiState = MutableStateFlow(StartWorkoutFakeStateProvider.loadingState())
    val uiState: StateFlow<StartWorkoutUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<StartWorkoutEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<StartWorkoutEffect> = _effects.asSharedFlow()
    private var lastDeletedExercise: DeletedExerciseSnapshot? = null
    private var appendToSessionId: Long? = null
    private var appendModeEnabled: Boolean = false

    init {
        _uiState.value = StartWorkoutFakeStateProvider.emptyState()

        viewModelScope.launch {
            exerciseCatalogRepository.ensureSeeded()
            exerciseCatalogRepository.observeExercises().collect { exercises ->
                _uiState.update { current ->
                    current.copy(
                        exerciseCatalog = exercises.map { exercise ->
                            ExerciseCatalogItemUiModel(
                                id = exercise.id,
                                title = exercise.title,
                                muscleGroup = exercise.muscleGroup
                            )
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            workoutPlanRepository.observePlans().collect { plans ->
                _uiState.update { current ->
                    val customSets = plans.map { plan ->
                        CustomExerciseSetUiModel(
                            id = plan.id,
                            name = plan.name,
                            exerciseIds = plan.exerciseIds
                        )
                    }
                    current.copy(
                        customExerciseSets = customSets,
                        selectedCustomSetId = current.selectedCustomSetId?.takeIf { selectedId ->
                            customSets.any { it.id == selectedId }
                        }
                    )
                }
            }
        }

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
            StartWorkoutUiEvent.ShortenSessionClick -> applyShortenedSession()
            StartWorkoutUiEvent.AddExerciseClick -> openExercisePicker(
                title = "Add Exercise",
                actionVerb = if (appendModeEnabled) "Add" else "Start"
            )
            StartWorkoutUiEvent.PlansClick -> openExercisePicker(
                title = "Plans",
                actionVerb = if (appendModeEnabled) "Add" else "Start"
            )
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

    fun setAppendToSessionId(sessionId: Long?) {
        appendToSessionId = sessionId
    }

    fun setAppendModeEnabled(enabled: Boolean) {
        appendModeEnabled = enabled
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

    private fun openExercisePicker(
        title: String,
        actionVerb: String
    ) {
        _uiState.update { current ->
            val selectedFromPlan = current.workoutPlan
                ?.exercises
                ?.map { it.id }
                ?.toSet()
            val fallbackSelection = current.selectedExerciseIds

            current.copy(
                isSelectingExercises = true,
                exercisePickerTitle = title,
                exercisePickerActionVerb = actionVerb,
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

        val current = _uiState.value
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

        _uiState.update {
            current.copy(
                customExerciseSets = updatedSets,
                selectedCustomSetId = updatedSet.id,
                selectedExerciseIds = normalizedExerciseIds
            )
        }
        viewModelScope.launch {
            workoutPlanRepository.savePlan(
                id = updatedSet.id,
                name = updatedSet.name,
                exerciseIds = normalizedExerciseIds.toList()
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
        val current = _uiState.value
        val updatedSets = current.customExerciseSets.filterNot { it.id == setId }
        val fallbackSelectedSetId = current.selectedCustomSetId?.takeIf { id ->
            updatedSets.any { it.id == id }
        }
        val fallbackSelectedExerciseIds = fallbackSelectedSetId
            ?.let { selectedId -> updatedSets.find { it.id == selectedId }?.exerciseIds }
            ?: current.selectedExerciseIds

        _uiState.update {
            current.copy(
                customExerciseSets = updatedSets,
                selectedCustomSetId = fallbackSelectedSetId,
                selectedExerciseIds = fallbackSelectedExerciseIds
            )
        }
        viewModelScope.launch {
            workoutPlanRepository.deletePlan(setId)
        }
    }

    private fun applyExerciseSelection() {
        if (appendModeEnabled) {
            appendSelectedExercisesToCurrentSession()
            return
        }

        var shouldStartWorkout = false
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
            val selectedPlan = current.selectedCustomSetId
                ?.let { selectedId -> current.customExerciseSets.find { it.id == selectedId } }

            current.copy(
                isSelectingExercises = false,
                workoutPlan = basePlan
                    .withConsistencyStreak(currentStreakDays)
                    .copy(
                        id = selectedPlan?.id ?: basePlan.id,
                        basedOnPlanText = selectedPlan?.let { "From your ${it.name} plan" }
                            ?: basePlan.basedOnPlanText,
                        exercises = selectedExercises
                    )
            )
                .also { shouldStartWorkout = true }
        }
        if (shouldStartWorkout) {
            startWorkout()
        }
    }

    private fun appendSelectedExercisesToCurrentSession() {
        val current = _uiState.value
        val selectedExercises = buildExercisesFromSelection(
            isShortened = false,
            selectedExerciseIds = current.selectedExerciseIds,
            exerciseCatalog = current.exerciseCatalog
        )
        if (selectedExercises.isEmpty()) {
            _uiState.update { it.copy(isSelectingExercises = false) }
            return
        }

        _uiState.update { it.copy(isSelectingExercises = false) }
        viewModelScope.launch {
            val sessionId = appendToSessionId ?: workoutSessionRepository.getActiveSessionId()
            if (sessionId == null) {
                startNewSessionFromSelectedExercises(selectedExercises)
                return@launch
            }

            val appended = workoutSessionRepository.appendExercisesToSession(
                sessionId = sessionId,
                exercisesToAppend = selectedExercises.map { exercise ->
                    SessionExerciseDraft(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        targetSets = 1,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg
                    )
                },
                forceSingleSet = true
            )

            if (appended) {
                _effects.emit(
                    StartWorkoutEffect.NavigateToOngoingWorkout(
                        sessionId = sessionId,
                        resumedExisting = true
                    )
                )
            }
        }
    }

    private suspend fun startNewSessionFromSelectedExercises(
        selectedExercises: List<WorkoutExerciseUiModel>
    ) {
        runCatching {
            workoutSessionRepository.startOrResumeSession(
                templateId = "custom_ongoing_workout",
                orderedExercises = selectedExercises.map { exercise ->
                    SessionExerciseDraft(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        targetSets = 1,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg
                    )
                }
            )
        }.onSuccess { result ->
            _effects.emit(
                StartWorkoutEffect.NavigateToOngoingWorkout(
                    sessionId = result.sessionId,
                    resumedExisting = result.resumedExisting
                )
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
                        targetSets = 1,
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
