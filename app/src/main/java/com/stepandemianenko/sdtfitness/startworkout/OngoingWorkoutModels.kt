package com.stepandemianenko.sdtfitness.startworkout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stepandemianenko.sdtfitness.R

@Immutable
data class OngoingWorkoutUiState(
    val isLoading: Boolean = true,
    val hasActiveSession: Boolean = false,
    val sessionId: Long? = null,
    val exerciseName: String = "Workout",
    val exercisePositionText: String = "Exercise 0 of 0",
    val totalSets: Int = 0,
    val currentSet: Int = 0,
    val totalSessionSets: Int = 0,
    val completedSets: Int = 0,
    val targetWeightKg: Int = 0,
    val targetReps: Int = 0,
    val loggedWeightKg: Int = 0,
    val loggedReps: Int = 0,
    val weightPresets: List<Int> = emptyList(),
    val repsPresets: List<Int> = emptyList(),
    val selectedRpeIndex: Int = 2,
    val rpeOptions: List<OngoingRpeOptionUiModel> = defaultRpeOptions(),
    val previousResults: OngoingPreviousResultsUiModel = OngoingPreviousResultsUiModel(
        lastSession = "No previous completed set yet",
        personalBest = "No personal best yet",
        dateLabel = ""
    ),
    val remainingSets: Int = 0,
    val estimatedTimeRemaining: String = "~0 min",
    val transitionMessage: String? = null,
    val completionMessage: String? = null
)

@Immutable
data class OngoingRpeOptionUiModel(
    val range: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int
)

@Immutable
data class OngoingPreviousResultsUiModel(
    val lastSession: String,
    val personalBest: String,
    val dateLabel: String
)

sealed interface OngoingWorkoutUiEvent {
    data object BackClick : OngoingWorkoutUiEvent
    data object EditClick : OngoingWorkoutUiEvent
    data object WeightMinusClick : OngoingWorkoutUiEvent
    data object WeightPlusClick : OngoingWorkoutUiEvent
    data object RepsMinusClick : OngoingWorkoutUiEvent
    data object RepsPlusClick : OngoingWorkoutUiEvent
    data class WeightPresetClick(val value: Int) : OngoingWorkoutUiEvent
    data class RepsPresetClick(val value: Int) : OngoingWorkoutUiEvent
    data class RpeSelectClick(val index: Int) : OngoingWorkoutUiEvent
    data object LogSetClick : OngoingWorkoutUiEvent
}

sealed interface OngoingWorkoutEffect {
    data class NavigateToProgress(val sessionId: Long) : OngoingWorkoutEffect
}

object OngoingWorkoutFakeStateProvider {
    fun contentState(): OngoingWorkoutUiState = OngoingWorkoutUiState(
        isLoading = false,
        hasActiveSession = true,
        sessionId = 99L,
        exerciseName = "Bench Press",
        exercisePositionText = "Exercise 1 of 3",
        totalSets = 4,
        currentSet = 2,
        totalSessionSets = 12,
        completedSets = 5,
        targetWeightKg = 60,
        targetReps = 8,
        loggedWeightKg = 60,
        loggedReps = 8,
        weightPresets = listOf(50, 55, 60, 65),
        repsPresets = listOf(6, 8, 10, 12),
        selectedRpeIndex = 2,
        rpeOptions = defaultRpeOptions(),
        previousResults = OngoingPreviousResultsUiModel(
            lastSession = "60 x 8 reps",
            personalBest = "65 x 8 reps",
            dateLabel = "Mar 12"
        ),
        remainingSets = 7,
        estimatedTimeRemaining = "~14 min",
        transitionMessage = "Bench Press complete. Next: Lat Pulldown",
        completionMessage = null
    )
}

fun defaultRpeOptions(): List<OngoingRpeOptionUiModel> {
    return listOf(
        OngoingRpeOptionUiModel(
            range = "1",
            label = "Very Easy",
            iconRes = R.drawable.start_workout_rpe_very_easy,
            selectedIconRes = R.drawable.start_workout_rpe_curr_very_easy
        ),
        OngoingRpeOptionUiModel(
            range = "2 - 3",
            label = "Easy",
            iconRes = R.drawable.start_workout_rpe_easy,
            selectedIconRes = R.drawable.start_workout_rpe_curr_easy
        ),
        OngoingRpeOptionUiModel(
            range = "4 - 6",
            label = "Challenging",
            iconRes = R.drawable.start_workout_rpe_challenging,
            selectedIconRes = R.drawable.start_workout_rpe_curr_challenging
        ),
        OngoingRpeOptionUiModel(
            range = "7 - 8",
            label = "Hard",
            iconRes = R.drawable.start_workout_rpe_hard,
            selectedIconRes = R.drawable.start_workout_rpe_curr_hard
        ),
        OngoingRpeOptionUiModel(
            range = "9 - 10",
            label = "Very Hard",
            iconRes = R.drawable.start_workout_rpe_very_hard,
            selectedIconRes = R.drawable.start_workout_rpe_curr_very_hard
        )
    )
}
