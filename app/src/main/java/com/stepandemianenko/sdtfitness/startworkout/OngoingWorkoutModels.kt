package com.stepandemianenko.sdtfitness.startworkout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stepandemianenko.sdtfitness.R

@Immutable
data class OngoingWorkoutUiState(
    val exerciseName: String,
    val totalSets: Int,
    val currentSet: Int,
    val completedSets: Int,
    val targetWeightKg: Int,
    val targetReps: Int,
    val loggedWeightKg: Int,
    val loggedReps: Int,
    val weightPresets: List<Int>,
    val repsPresets: List<Int>,
    val selectedRpeIndex: Int,
    val rpeOptions: List<OngoingRpeOptionUiModel>,
    val previousResults: OngoingPreviousResultsUiModel,
    val estimatedTimeRemaining: String
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

object OngoingWorkoutFakeStateProvider {
    fun contentState(): OngoingWorkoutUiState = OngoingWorkoutUiState(
        exerciseName = "Bench Press",
        totalSets = 4,
        currentSet = 2,
        completedSets = 2,
        targetWeightKg = 60,
        targetReps = 8,
        loggedWeightKg = 60,
        loggedReps = 8,
        weightPresets = listOf(50, 55, 60, 65),
        repsPresets = listOf(6, 8, 10, 12),
        selectedRpeIndex = 2,
        rpeOptions = listOf(
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
        ),
        previousResults = OngoingPreviousResultsUiModel(
            lastSession = "60 x 8 reps",
            personalBest = "60 x 10 reps",
            dateLabel = "March 12"
        ),
        estimatedTimeRemaining = "~10 min"
    )
}
