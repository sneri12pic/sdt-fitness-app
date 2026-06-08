package com.stepandemianenko.sdtfitness.startworkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.local.ExerciseCatalogListItem
import com.stepandemianenko.sdtfitness.data.repository.SessionExerciseDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartWorkoutViewModel(
    application: Application
) : AndroidViewModel(application) {

    private data class SelectExercisesDerivedState(
        val isLoading: Boolean,
        val searchQuery: String,
        val selectedMuscleGroup: String?,
        val muscleGroups: List<String>,
        val exercises: List<SelectExerciseItemUiModel>,
        val selectedExerciseIds: Set<String>
    )

    private val _uiState = MutableStateFlow(StartWorkoutUiState(isLoading = true))
    val uiState: StateFlow<StartWorkoutUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<StartWorkoutEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<StartWorkoutEffect> = _effects.asSharedFlow()
    private val workoutSessionRepository = AppGraph.workoutSessionRepository(application)
    private val workoutPlanRepository = AppGraph.workoutPlanRepository(application)
    private val exerciseCatalogRepository = AppGraph.exerciseCatalogRepository(application)
    private val selectedExerciseIds = MutableStateFlow<Set<String>>(emptySet())
    private val searchQuery = MutableStateFlow("")
    private val selectedMuscleGroup = MutableStateFlow<String?>(null)
    private var appendToSessionId: Long? = null
    private var appendModeEnabled: Boolean = false

    init {
        observeExerciseSelectionState()
        preloadExerciseCatalog()

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
                        selectExercises = current.selectExercises.copy(
                            customExerciseSets = customSets,
                            selectedCustomSetId = current.selectExercises.selectedCustomSetId
                                ?.takeIf { selectedId -> customSets.any { it.id == selectedId } }
                        )
                    )
                }
            }
        }
    }

    fun onEvent(event: StartWorkoutUiEvent) {
        when (event) {
            StartWorkoutUiEvent.AddExerciseClick -> openExercisePicker(
                title = "Add Exercise",
                actionVerb = if (appendModeEnabled) "Add" else "Start"
            )
            StartWorkoutUiEvent.PlansClick -> openExercisePicker(
                title = "Plans",
                actionVerb = if (appendModeEnabled) "Add" else "Start"
            )
            StartWorkoutUiEvent.CloseExercisePickerClick -> closeExercisePicker()
            StartWorkoutUiEvent.ConfirmExerciseSelectionClick -> applyExerciseSelection()
            is StartWorkoutUiEvent.SearchQueryChanged -> searchQuery.value = event.query
            is StartWorkoutUiEvent.MuscleGroupSelected -> selectedMuscleGroup.value = event.muscleGroup
            is StartWorkoutUiEvent.ToggleExerciseSelection -> toggleExerciseSelection(event.exerciseId)
            is StartWorkoutUiEvent.SaveCustomExerciseSet -> saveCustomExerciseSet(
                setId = event.setId,
                name = event.name,
                exerciseIds = event.exerciseIds
            )
            is StartWorkoutUiEvent.SelectCustomExerciseSet -> selectCustomExerciseSet(event.setId)
            is StartWorkoutUiEvent.DeleteCustomExerciseSet -> deleteCustomExerciseSet(event.setId)
        }
    }

    fun setAppendToSessionId(sessionId: Long?) {
        appendToSessionId = sessionId
    }

    fun setAppendModeEnabled(enabled: Boolean) {
        appendModeEnabled = enabled
    }

    private fun openExercisePicker(
        title: String,
        actionVerb: String
    ) {
        _uiState.update { current ->
            current.copy(
                isSelectingExercises = true,
                selectExercises = current.selectExercises.copy(
                    title = title,
                    primaryActionVerb = actionVerb,
                    selectedCustomSetId = current.selectExercises.selectedCustomSetId?.takeIf { selectedId ->
                        current.selectExercises.customExerciseSets.any { it.id == selectedId }
                    }
                )
            )
        }
    }

    private fun closeExercisePicker() {
        _uiState.update { it.copy(isSelectingExercises = false) }
    }

    private fun toggleExerciseSelection(exerciseId: String) {
        val currentSelection = selectedExerciseIds.value
        val updatedSelection = currentSelection.toMutableSet().apply {
            if (!add(exerciseId)) remove(exerciseId)
        }
        selectedExerciseIds.value = updatedSelection

        _uiState.update { current ->
            val activeSet = current.selectExercises.selectedCustomSetId
                ?.let { selectedSetId ->
                    current.selectExercises.customExerciseSets.find { it.id == selectedSetId }
                }
            val shouldClearSelectedSet = activeSet != null &&
                currentSelection.contains(exerciseId) &&
                activeSet.exerciseIds.contains(exerciseId)
            current.copy(
                selectExercises = current.selectExercises.copy(
                    selectedCustomSetId = if (shouldClearSelectedSet) null else current.selectExercises.selectedCustomSetId
                )
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

        val current = _uiState.value.selectExercises
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

        selectedExerciseIds.value = normalizedExerciseIds
        _uiState.update { state ->
            state.copy(
                selectExercises = state.selectExercises.copy(
                    customExerciseSets = updatedSets,
                    selectedCustomSetId = updatedSet.id
                )
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
        val current = _uiState.value.selectExercises
        val set = current.customExerciseSets.find { it.id == setId } ?: return
        val isTappingAlreadySelectedSet = current.selectedCustomSetId == setId
        val nextSelectedSetId = if (isTappingAlreadySelectedSet) null else set.id
        selectedExerciseIds.value = if (isTappingAlreadySelectedSet) {
            current.selectedExerciseIds - set.exerciseIds
        } else {
            set.exerciseIds
        }

        _uiState.update { state ->
            state.copy(
                selectExercises = state.selectExercises.copy(selectedCustomSetId = nextSelectedSetId)
            )
        }
    }

    private fun deleteCustomExerciseSet(setId: String) {
        val current = _uiState.value.selectExercises
        val updatedSets = current.customExerciseSets.filterNot { it.id == setId }
        val fallbackSelectedSetId = current.selectedCustomSetId?.takeIf { id ->
            updatedSets.any { it.id == id }
        }
        val fallbackSelectedExerciseIds = fallbackSelectedSetId
            ?.let { selectedId -> updatedSets.find { it.id == selectedId }?.exerciseIds }
            ?: current.selectedExerciseIds

        selectedExerciseIds.value = fallbackSelectedExerciseIds
        _uiState.update { state ->
            state.copy(
                selectExercises = state.selectExercises.copy(
                    customExerciseSets = updatedSets,
                    selectedCustomSetId = fallbackSelectedSetId
                )
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

        val current = _uiState.value.selectExercises
        viewModelScope.launch {
            val selectedExercises = buildSessionExerciseDrafts(selectedExerciseIds = current.selectedExerciseIds)
            if (selectedExercises.isEmpty()) return@launch

            _uiState.update { it.copy(isSelectingExercises = false) }
            startNewSessionFromSelectedExercises(
                templateId = current.selectedCustomSetId ?: CUSTOM_WORKOUT_TEMPLATE_ID,
                selectedExercises = selectedExercises
            )
        }
    }

    private fun appendSelectedExercisesToCurrentSession() {
        val current = _uiState.value.selectExercises
        viewModelScope.launch {
            val selectedExercises = buildSessionExerciseDrafts(selectedExerciseIds = current.selectedExerciseIds)
            if (selectedExercises.isEmpty()) return@launch

            _uiState.update { it.copy(isSelectingExercises = false) }
            val sessionId = appendToSessionId ?: workoutSessionRepository.getActiveSessionId()
            if (sessionId == null) {
                startNewSessionFromSelectedExercises(
                    templateId = current.selectedCustomSetId ?: CUSTOM_ONGOING_WORKOUT_TEMPLATE_ID,
                    selectedExercises = selectedExercises
                )
                return@launch
            }

            val appended = workoutSessionRepository.appendExercisesToSession(
                sessionId = sessionId,
                exercisesToAppend = selectedExercises,
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
        templateId: String,
        selectedExercises: List<SessionExerciseDraft>
    ) {
        runCatching {
            workoutSessionRepository.startOrResumeSession(
                templateId = templateId,
                orderedExercises = selectedExercises
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

    private fun observeExerciseSelectionState() {
        viewModelScope.launch {
            combine(
                exerciseCatalogRepository.observeExerciseList(),
                selectedExerciseIds,
                searchQuery,
                selectedMuscleGroup
            ) { exercises, selectedIds, query, muscleGroup ->
                buildSelectExercisesDerivedState(
                    exercises = exercises,
                    selectedIds = selectedIds,
                    query = query,
                    muscleGroup = muscleGroup
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { derived ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            selectExercises = current.selectExercises.copy(
                                isLoading = derived.isLoading,
                                searchQuery = derived.searchQuery,
                                selectedMuscleGroup = derived.selectedMuscleGroup,
                                muscleGroups = derived.muscleGroups,
                                exercises = derived.exercises,
                                selectedExerciseIds = derived.selectedExerciseIds
                            )
                        )
                    }
                }
        }
    }

    private fun preloadExerciseCatalog() {
        viewModelScope.launch {
            exerciseCatalogRepository.ensureInitialSeeded(INITIAL_EXERCISE_CATALOG_LIMIT)
            exerciseCatalogRepository.ensureSeeded()
        }
    }

    private fun buildSelectExercisesDerivedState(
        exercises: List<ExerciseCatalogListItem>,
        selectedIds: Set<String>,
        query: String,
        muscleGroup: String?
    ): SelectExercisesDerivedState {
        val muscleGroups = exercises
            .asSequence()
            .map { it.muscleGroup }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
        val validMuscleGroup = muscleGroup?.takeIf { selected ->
            muscleGroups.any { it.equals(selected, ignoreCase = true) }
        }
        val normalizedQuery = query.trim()
        val visibleExercises = exercises
            .asSequence()
            .filter { item ->
                validMuscleGroup == null || item.muscleGroup.equals(validMuscleGroup, ignoreCase = true)
            }
            .filter { item ->
                normalizedQuery.isBlank() || item.title.contains(normalizedQuery, ignoreCase = true)
            }
            .map { item ->
                SelectExerciseItemUiModel(
                    id = item.id,
                    title = item.title,
                    muscleGroup = item.muscleGroup,
                    isSelected = selectedIds.contains(item.id)
                )
            }
            .toList()

        return SelectExercisesDerivedState(
            isLoading = false,
            searchQuery = query,
            selectedMuscleGroup = validMuscleGroup,
            muscleGroups = muscleGroups,
            exercises = visibleExercises,
            selectedExerciseIds = selectedIds
        )
    }

    private suspend fun buildSessionExerciseDrafts(
        selectedExerciseIds: Set<String>
    ): List<SessionExerciseDraft> {
        if (selectedExerciseIds.isEmpty()) return emptyList()

        return exerciseCatalogRepository.getExerciseListItemsByIds(selectedExerciseIds)
            .map { exercise ->
                SessionExerciseDraft(
                    exerciseId = exercise.id,
                    exerciseName = exercise.title,
                    targetSets = DEFAULT_TARGET_SETS,
                    targetReps = DEFAULT_TARGET_REPS,
                    targetWeightKg = DEFAULT_TARGET_WEIGHT_KG
                )
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

    private companion object {
        const val CUSTOM_WORKOUT_TEMPLATE_ID = "custom_workout"
        const val CUSTOM_ONGOING_WORKOUT_TEMPLATE_ID = "custom_ongoing_workout"
        const val INITIAL_EXERCISE_CATALOG_LIMIT = 30
        const val DEFAULT_TARGET_SETS = 1
        const val DEFAULT_TARGET_REPS = 8
        const val DEFAULT_TARGET_WEIGHT_KG = 0
    }
}
