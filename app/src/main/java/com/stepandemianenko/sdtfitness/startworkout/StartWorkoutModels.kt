package com.stepandemianenko.sdtfitness.startworkout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stepandemianenko.sdtfitness.R

@Immutable
data class StartWorkoutUiState(
    val isLoading: Boolean = false,
    val workoutPlan: WorkoutPlanUiModel? = null,
    val isSessionShortened: Boolean = false,
    val isSelectingExercises: Boolean = false,
    val exerciseCatalog: List<ExerciseCatalogItemUiModel> = emptyList(),
    val selectedExerciseIds: Set<String> = emptySet()
) {
    val isEmpty: Boolean
        get() = !isLoading && workoutPlan == null
}

@Immutable
data class WorkoutPlanUiModel(
    val id: String,
    val headerTitle: String,
    val headerSubtitle: String,
    val streakCard: WorkoutInfoCardUiModel,
    val basedOnPlanText: String,
    val selectedWorkoutsTitle: String,
    val estimatedDurationText: String,
    val selectedWorkoutBadge: WorkoutBadgeUiModel,
    val exercises: List<WorkoutExerciseUiModel>,
    val swapExerciseCard: WorkoutInfoCardUiModel,
    val shortenSessionCard: WorkoutInfoCardUiModel,
    val primaryCtaText: String,
    val miniPlayer: WorkoutMiniPlayerUiModel
)

@Immutable
data class WorkoutExerciseUiModel(
    val id: String,
    val name: String,
    val prescription: String,
    val setsText: String,
    val estimatedTimeText: String,
    @DrawableRes val iconRes: Int
)

@Immutable
data class ExerciseCatalogItemUiModel(
    val id: String,
    val title: String,
    val muscleGroup: String
)

@Immutable
data class WorkoutBadgeUiModel(
    val text: String
)

@Immutable
data class WorkoutInfoCardUiModel(
    val title: String,
    val subtitle: String,
    val badge: WorkoutBadgeUiModel? = null
)

@Immutable
data class WorkoutMiniPlayerUiModel(
    val title: String,
    val subtitle: String
)

sealed interface StartWorkoutUiEvent {
    data object RetryLoad : StartWorkoutUiEvent
    data object BackClick : StartWorkoutUiEvent
    data object StartWorkoutClick : StartWorkoutUiEvent
    data object ShortenSessionClick : StartWorkoutUiEvent
    data object EditWorkoutClick : StartWorkoutUiEvent
    data class ExerciseClick(val exerciseId: String) : StartWorkoutUiEvent
    data class CompleteOrSkipExercise(val exerciseId: String) : StartWorkoutUiEvent
    data class DeleteExercise(val exerciseId: String) : StartWorkoutUiEvent
    data object UndoDeleteExercise : StartWorkoutUiEvent
    data object AddExerciseClick : StartWorkoutUiEvent
    data object CloseExercisePickerClick : StartWorkoutUiEvent
    data class ToggleExerciseSelection(val exerciseId: String) : StartWorkoutUiEvent
    data object ConfirmExerciseSelectionClick : StartWorkoutUiEvent
}

object StartWorkoutFakeStateProvider {
    fun loadingState(): StartWorkoutUiState = StartWorkoutUiState(isLoading = true)

    fun contentState(): StartWorkoutUiState {
        val plan = defaultPlan(isShortened = false)
        return StartWorkoutUiState(
            isLoading = false,
            workoutPlan = plan,
            isSessionShortened = false,
            exerciseCatalog = defaultExerciseCatalog(),
            selectedExerciseIds = plan.exercises.map { it.id }.toSet()
        )
    }

    fun emptyState(): StartWorkoutUiState = StartWorkoutUiState(
        isLoading = false,
        workoutPlan = null,
        isSessionShortened = false,
        exerciseCatalog = defaultExerciseCatalog(),
        selectedExerciseIds = defaultExerciseSelection()
    )

    fun shortenedState(): StartWorkoutUiState {
        val plan = defaultPlan(isShortened = true)
        return StartWorkoutUiState(
            isLoading = false,
            workoutPlan = plan,
            isSessionShortened = true,
            exerciseCatalog = defaultExerciseCatalog(),
            selectedExerciseIds = plan.exercises.map { it.id }.toSet()
        )
    }

    fun defaultPlan(isShortened: Boolean): WorkoutPlanUiModel {
        val exercises = if (isShortened) {
            listOf(
                WorkoutExerciseUiModel(
                    id = "bench_press",
                    name = "Bench Press",
                    prescription = "@ 60 kg / 8 reps",
                    setsText = "3 sets",
                    estimatedTimeText = "(~ 12 min)",
                    iconRes = R.drawable.start_workout_dumbbell_orange
                ),
                WorkoutExerciseUiModel(
                    id = "incline_press",
                    name = "Incline Dumbbell Press",
                    prescription = "@ 20 kg / 10 reps",
                    setsText = "2 sets",
                    estimatedTimeText = "(~ 8 min)",
                    iconRes = R.drawable.start_workout_dumbbell_orange
                )
            )
        } else {
            listOf(
                WorkoutExerciseUiModel(
                    id = "bench_press",
                    name = "Bench Press",
                    prescription = "@ 60 kg / 8 reps",
                    setsText = "3 sets",
                    estimatedTimeText = "(~ 20 min)",
                    iconRes = R.drawable.start_workout_dumbbell_orange
                ),
                WorkoutExerciseUiModel(
                    id = "incline_press",
                    name = "Incline Dumbbell Press",
                    prescription = "@ 20 kg / 10 reps",
                    setsText = "3 sets",
                    estimatedTimeText = "(~ 20 min)",
                    iconRes = R.drawable.start_workout_dumbbell_orange
                ),
                WorkoutExerciseUiModel(
                    id = "triceps_pushdown",
                    name = "Triceps Pushdown",
                    prescription = "@ 30 kg / 12 reps",
                    setsText = "3 sets",
                    estimatedTimeText = "(~ 10 min)",
                    iconRes = R.drawable.start_workout_dumbbell_orange
                )
            )
        }

        return WorkoutPlanUiModel(
            id = "upper_body_day_4",
            headerTitle = "Ready to train?",
            headerSubtitle = "Choose your exercises and start when you're ready",
            streakCard = WorkoutInfoCardUiModel(
                title = "Workout Streak : 7 days",
                subtitle = "Today's XP goal : 120 xp",
                badge = WorkoutBadgeUiModel(text = "+ 120 xp")
            ),
            basedOnPlanText = "Based on your Upper Body plan",
            selectedWorkoutsTitle = "Selected Workouts",
            estimatedDurationText = if (isShortened) "20 min" else "50 min",
            selectedWorkoutBadge = WorkoutBadgeUiModel(text = "+ 90 xp"),
            exercises = exercises,
            swapExerciseCard = WorkoutInfoCardUiModel(
                title = "Swap Exercise",
                subtitle = ""
            ),
            shortenSessionCard = WorkoutInfoCardUiModel(
                title = "Shorten Session",
                subtitle = if (isShortened) "Quick 20 min version selected" else "Quick 20 min version"
            ),
            primaryCtaText = "Start Workout",
            miniPlayer = WorkoutMiniPlayerUiModel(
                title = "Workout",
                subtitle = "10 min\nBench Press (Barbell)"
            )
        )
    }

    fun defaultExerciseCatalog(): List<ExerciseCatalogItemUiModel> = listOf(
        ExerciseCatalogItemUiModel(id = "bench_press_barbell", title = "Bench Press (Barbell)", muscleGroup = "Chest"),
        ExerciseCatalogItemUiModel(id = "bench_press_dumbbell", title = "Bench Press (Dumbbell)", muscleGroup = "Chest"),
        ExerciseCatalogItemUiModel(id = "biceps_curl_barbell", title = "Biceps Curl (Barbell)", muscleGroup = "Biceps"),
        ExerciseCatalogItemUiModel(id = "biceps_curl_dumbbell", title = "Biceps Curl (Dumbbell)", muscleGroup = "Biceps"),
        ExerciseCatalogItemUiModel(id = "deadlift_barbell", title = "Deadlift (Barbell)", muscleGroup = "Back"),
        ExerciseCatalogItemUiModel(id = "hammer_curl_dumbbell", title = "Hammer Curl (Dumbbell)", muscleGroup = "Biceps"),
        ExerciseCatalogItemUiModel(id = "incline_bench_dumbbell", title = "Incline Bench Press (Dumbbell)", muscleGroup = "Chest"),
        ExerciseCatalogItemUiModel(id = "lat_pulldown_cable", title = "Lat Pulldown (Cable)", muscleGroup = "Back"),
        ExerciseCatalogItemUiModel(id = "lateral_raise_dumbbell", title = "Lateral Raise (Dumbbell)", muscleGroup = "Shoulders"),
        ExerciseCatalogItemUiModel(id = "leg_extension", title = "Leg Extension", muscleGroup = "Quads"),
        ExerciseCatalogItemUiModel(id = "leg_press", title = "Leg Press", muscleGroup = "Quads"),
        ExerciseCatalogItemUiModel(id = "lying_leg_curl", title = "Lying Leg Curl", muscleGroup = "Hamstrings")
    )

    fun defaultExerciseSelection(): Set<String> = setOf(
        "bench_press_barbell",
        "bench_press_dumbbell",
        "biceps_curl_barbell"
    )

    fun exerciseTemplate(isShortened: Boolean): WorkoutExerciseUiModel {
        return if (isShortened) {
            WorkoutExerciseUiModel(
                id = "template_short",
                name = "Template",
                prescription = "@ 60 kg / 8 reps",
                setsText = "2 sets",
                estimatedTimeText = "(~ 8 min)",
                iconRes = R.drawable.start_workout_dumbbell_orange
            )
        } else {
            WorkoutExerciseUiModel(
                id = "template_full",
                name = "Template",
                prescription = "@ 60 kg / 8 reps",
                setsText = "3 sets",
                estimatedTimeText = "(~ 20 min)",
                iconRes = R.drawable.start_workout_dumbbell_orange
            )
        }
    }
}
