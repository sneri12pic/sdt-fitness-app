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
    val customExerciseSets: List<CustomExerciseSetUiModel> = emptyList(),
    val selectedCustomSetId: String? = null,
    val selectedExerciseIds: Set<String> = emptySet(),
    val isStartingWorkout: Boolean = false
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
    @DrawableRes val iconRes: Int,
    val targetSets: Int = parseTargetSets(setsText),
    val targetReps: Int = parseTargetReps(prescription),
    val targetWeightKg: Int = parseTargetWeightKg(prescription)
)

@Immutable
data class ExerciseCatalogItemUiModel(
    val id: String,
    val title: String,
    val muscleGroup: String
)

@Immutable
data class CustomExerciseSetUiModel(
    val id: String,
    val name: String,
    val exerciseIds: Set<String>
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

private val estimatedMinutesRegex = Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE)

fun WorkoutPlanUiModel.actualEstimatedDurationText(): String {
    if (exercises.isEmpty()) return "0 min"

    val totalMinutes = exercises.sumOf { exercise ->
        estimatedMinutesRegex
            .find(exercise.estimatedTimeText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0
    }

    return if (totalMinutes > 0) "$totalMinutes min" else estimatedDurationText
}

sealed interface StartWorkoutUiEvent {
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
    data class SaveCustomExerciseSet(
        val setId: String?,
        val name: String,
        val exerciseIds: Set<String>
    ) : StartWorkoutUiEvent
    data class SelectCustomExerciseSet(val setId: String) : StartWorkoutUiEvent
    data class DeleteCustomExerciseSet(val setId: String) : StartWorkoutUiEvent
    data object ConfirmExerciseSelectionClick : StartWorkoutUiEvent
    data class UpdateExerciseWeight(val exerciseId: String, val weightKg: Int) : StartWorkoutUiEvent
    data class UpdateExerciseReps(val exerciseId: String, val reps: Int) : StartWorkoutUiEvent
}

sealed interface StartWorkoutEffect {
    data class NavigateToOngoingWorkout(
        val sessionId: Long,
        val resumedExisting: Boolean
    ) : StartWorkoutEffect
}

private fun parseTargetSets(setsText: String): Int {
    return Regex("(\\d+)").find(setsText)?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 3
}

private fun parseTargetReps(prescription: String): Int {
    return Regex("(\\d+)\\s*reps", RegexOption.IGNORE_CASE)
        .find(prescription)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(1)
        ?: 8
}

private fun parseTargetWeightKg(prescription: String): Int {
    return Regex("(\\d+)\\s*kg", RegexOption.IGNORE_CASE)
        .find(prescription)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 0
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
        selectedExerciseIds = emptySet()
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
                title = "7-day consistency streak",
                subtitle = "Today's goal: 120 XP",
                badge = WorkoutBadgeUiModel(text = "+120 XP")
            ),
            basedOnPlanText = "From your Upper Body plan",
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
