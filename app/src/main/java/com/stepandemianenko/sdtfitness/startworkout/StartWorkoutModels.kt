package com.stepandemianenko.sdtfitness.startworkout

import androidx.compose.runtime.Immutable

@Immutable
data class StartWorkoutUiState(
    val isLoading: Boolean = false,
    val isSelectingExercises: Boolean = false,
    val selectExercises: SelectExercisesUiState = SelectExercisesUiState()
) {
    val isEmpty: Boolean
        get() = !isLoading && !isSelectingExercises
}

@Immutable
data class SelectExercisesUiState(
    val isLoading: Boolean = true,
    val title: String = "Add Exercise",
    val primaryActionVerb: String = "Start",
    val searchQuery: String = "",
    val selectedMuscleGroup: String? = null,
    val muscleGroups: List<String> = emptyList(),
    val exercises: List<SelectExerciseItemUiModel> = emptyList(),
    val customExerciseSets: List<CustomExerciseSetUiModel> = emptyList(),
    val selectedCustomSetId: String? = null,
    val selectedExerciseIds: Set<String> = emptySet()
)

@Immutable
data class SelectExerciseItemUiModel(
    val id: String,
    val title: String,
    val muscleGroup: String,
    val isSelected: Boolean
)

@Immutable
data class CustomExerciseSetUiModel(
    val id: String,
    val name: String,
    val exerciseIds: Set<String>
)

sealed interface StartWorkoutUiEvent {
    data object AddExerciseClick : StartWorkoutUiEvent
    data object PlansClick : StartWorkoutUiEvent
    data object CloseExercisePickerClick : StartWorkoutUiEvent
    data class SearchQueryChanged(val query: String) : StartWorkoutUiEvent
    data class MuscleGroupSelected(val muscleGroup: String?) : StartWorkoutUiEvent
    data class ToggleExerciseSelection(val exerciseId: String) : StartWorkoutUiEvent
    data class SaveCustomExerciseSet(
        val setId: String?,
        val name: String,
        val exerciseIds: Set<String>
    ) : StartWorkoutUiEvent
    data class SelectCustomExerciseSet(val setId: String) : StartWorkoutUiEvent
    data class DeleteCustomExerciseSet(val setId: String) : StartWorkoutUiEvent
    data object ConfirmExerciseSelectionClick : StartWorkoutUiEvent
}

sealed interface StartWorkoutEffect {
    data class NavigateToOngoingWorkout(
        val sessionId: Long,
        val resumedExisting: Boolean
    ) : StartWorkoutEffect
}

object StartWorkoutPreviewData {
    fun defaultSelectExercisesUiState(): SelectExercisesUiState = SelectExercisesUiState(
        isLoading = false,
        muscleGroups = listOf("Back", "Biceps", "Chest", "Hamstrings", "Quads", "Shoulders"),
        selectedExerciseIds = setOf(
            "bench_press_barbell",
            "bench_press_dumbbell",
            "biceps_curl_barbell"
        ),
        exercises = listOf(
            previewExercise("bench_press_barbell", "Bench Press (Barbell)", "Chest", true),
            previewExercise("bench_press_dumbbell", "Bench Press (Dumbbell)", "Chest", true),
            previewExercise("biceps_curl_barbell", "Biceps Curl (Barbell)", "Biceps", true),
            previewExercise("biceps_curl_dumbbell", "Biceps Curl (Dumbbell)", "Biceps"),
            previewExercise("deadlift_barbell", "Deadlift (Barbell)", "Back"),
            previewExercise("hammer_curl_dumbbell", "Hammer Curl (Dumbbell)", "Biceps"),
            previewExercise("incline_bench_dumbbell", "Incline Bench Press (Dumbbell)", "Chest"),
            previewExercise("lat_pulldown_cable", "Lat Pulldown (Cable)", "Back"),
            previewExercise("lateral_raise_dumbbell", "Lateral Raise (Dumbbell)", "Shoulders"),
            previewExercise("leg_extension", "Leg Extension", "Quads"),
            previewExercise("leg_press", "Leg Press", "Quads"),
            previewExercise("lying_leg_curl", "Lying Leg Curl", "Hamstrings")
        )
    )

    private fun previewExercise(
        id: String,
        title: String,
        muscleGroup: String,
        isSelected: Boolean = false
    ) = SelectExerciseItemUiModel(
        id = id,
        title = title,
        muscleGroup = muscleGroup,
        isSelected = isSelected
    )
}
