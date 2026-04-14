package com.stepandemianenko.sdtfitness.startworkout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.stepandemianenko.sdtfitness.R

@Immutable
data class LogWorkoutUiState(
    val isLoading: Boolean = true,
    val hasActiveSession: Boolean = false,
    val session: WorkoutSessionUiModel? = null,
    val hasSeenRestTimerHint: Boolean = false,
    val restTimer: RestTimerUiState = RestTimerUiState(),
    val message: String? = null,
    val showDiscardConfirmation: Boolean = false,
    val isDiscarding: Boolean = false
)

@Immutable
data class RestTimerUiState(
    val state: RestTimerState = RestTimerState.Inactive,
    val remainingSeconds: Int = 0
) {
    val isActive: Boolean
        get() = state == RestTimerState.Running || state == RestTimerState.Finished

    val countdownText: String
        get() {
            val safeSeconds = remainingSeconds.coerceAtLeast(0)
            val minutes = safeSeconds / 60
            val seconds = safeSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

enum class RestTimerState {
    Inactive,
    Running,
    Finished,
    Dismissed
}

@Immutable
data class WorkoutSessionUiModel(
    val id: Long,
    val durationText: String,
    val volumeText: String,
    val setsText: String,
    val progress: Float,
    val exercises: List<ExerciseUiModel>
)

@Immutable
data class ExerciseUiModel(
    val id: Long,
    val name: String,
    val notes: String,
    @DrawableRes val thumbnailRes: Int,
    val isRestTimerOn: Boolean,
    val restTimerStatusText: String,
    val sets: List<WorkoutSetUiModel>
)

@Immutable
data class WorkoutSetUiModel(
    val id: String,
    val setNumber: Int,
    val previousPerformance: String,
    val weight: String,
    val reps: String,
    val isCompleted: Boolean,
    val isCompletionEnabled: Boolean,
    val activeFeedbackVisible: Boolean,
    val feedbackMessage: String?,
    val selectedRpe: Int?,
    val suggestedNextWeight: Int?
)

@Immutable
data class LogWorkoutRpeOptionUiModel(
    val range: String,
    val label: String,
    val rpeValue: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int
)

sealed interface LogWorkoutUiEvent {
    data object OnBackClicked : LogWorkoutUiEvent
    data object OnFinishWorkoutClicked : LogWorkoutUiEvent
    data class OnToggleSetCompleted(val exerciseId: Long, val setId: String) : LogWorkoutUiEvent
    data class OnAddSet(val exerciseId: Long) : LogWorkoutUiEvent
    data object OnAddExercise : LogWorkoutUiEvent
    data class OnUpdateSetWeight(val exerciseId: Long, val setId: String, val weight: String) : LogWorkoutUiEvent
    data class OnUpdateSetReps(val exerciseId: Long, val setId: String, val reps: String) : LogWorkoutUiEvent
    data class OnSelectRpe(val exerciseId: Long, val setId: String, val rpe: Int) : LogWorkoutUiEvent
    data class OnDismissFeedback(val exerciseId: Long, val setId: String) : LogWorkoutUiEvent
    data class OnToggleRestTimer(val exerciseId: Long) : LogWorkoutUiEvent
    data object OnHeaderTimerTapped : LogWorkoutUiEvent
    data class OnStartRestTimer(val durationSeconds: Int) : LogWorkoutUiEvent
    data object OnExtendRestTimerByTenSeconds : LogWorkoutUiEvent
    data object OnDismissRestTimer : LogWorkoutUiEvent
    data object OnDiscardWorkoutClicked : LogWorkoutUiEvent
    data object OnDismissDiscardConfirmation : LogWorkoutUiEvent
    data object OnConfirmDiscardWorkout : LogWorkoutUiEvent
}

sealed interface LogWorkoutEffect {
    data class NavigateToProgress(val sessionId: Long) : LogWorkoutEffect
    data object OpenAddExercise : LogWorkoutEffect
    data object OpenRestTimer : LogWorkoutEffect
    data class ShowSnackbar(val message: String) : LogWorkoutEffect
    data object CloseAfterDiscard : LogWorkoutEffect
}

fun defaultLogWorkoutRpeOptions(): List<LogWorkoutRpeOptionUiModel> {
    return listOf(
        LogWorkoutRpeOptionUiModel(
            range = "1",
            label = "Very Easy",
            rpeValue = 1,
            iconRes = R.drawable.start_workout_rpe_very_easy,
            selectedIconRes = R.drawable.start_workout_rpe_curr_very_easy
        ),
        LogWorkoutRpeOptionUiModel(
            range = "2-3",
            label = "Easy",
            rpeValue = 2,
            iconRes = R.drawable.start_workout_rpe_easy,
            selectedIconRes = R.drawable.start_workout_rpe_curr_easy
        ),
        LogWorkoutRpeOptionUiModel(
            range = "4-6",
            label = "Challenging",
            rpeValue = 5,
            iconRes = R.drawable.start_workout_rpe_challenging,
            selectedIconRes = R.drawable.start_workout_rpe_curr_challenging
        ),
        LogWorkoutRpeOptionUiModel(
            range = "7-8",
            label = "Hard",
            rpeValue = 7,
            iconRes = R.drawable.start_workout_rpe_hard,
            selectedIconRes = R.drawable.start_workout_rpe_curr_hard
        ),
        LogWorkoutRpeOptionUiModel(
            range = "9-10",
            label = "Very Hard",
            rpeValue = 9,
            iconRes = R.drawable.start_workout_rpe_very_hard,
            selectedIconRes = R.drawable.start_workout_rpe_curr_very_hard
        )
    )
}

object LogWorkoutFakeStateProvider {

    fun contentState(): LogWorkoutUiState {
        val exercises = listOf(
            ExerciseUiModel(
                id = 101L,
                name = "Seated Shoulder Press (Machine)",
                notes = "",
                thumbnailRes = R.drawable.home_workout_thumb,
                isRestTimerOn = false,
                restTimerStatusText = "Rest Timer: OFF",
                sets = listOf(
                    WorkoutSetUiModel(
                        id = "101:1",
                        setNumber = 1,
                        previousPerformance = "36kg x 5",
                        weight = "36",
                        reps = "5",
                        isCompleted = true,
                        isCompletionEnabled = false,
                        activeFeedbackVisible = false,
                        feedbackMessage = null,
                        selectedRpe = null,
                        suggestedNextWeight = null
                    ),
                    WorkoutSetUiModel(
                        id = "101:2",
                        setNumber = 2,
                        previousPerformance = "41kg x 5",
                        weight = "41",
                        reps = "5",
                        isCompleted = true,
                        isCompletionEnabled = false,
                        activeFeedbackVisible = false,
                        feedbackMessage = null,
                        selectedRpe = null,
                        suggestedNextWeight = null
                    ),
                    WorkoutSetUiModel(
                        id = "101:3",
                        setNumber = 3,
                        previousPerformance = "45kg x 3",
                        weight = "45",
                        reps = "3",
                        isCompleted = false,
                        isCompletionEnabled = true,
                        activeFeedbackVisible = false,
                        feedbackMessage = null,
                        selectedRpe = null,
                        suggestedNextWeight = null
                    )
                )
            ),
            ExerciseUiModel(
                id = 102L,
                name = "Lat Pulldown (Cable)",
                notes = "",
                thumbnailRes = R.drawable.home_workout_thumb,
                isRestTimerOn = false,
                restTimerStatusText = "Rest Timer: OFF",
                sets = listOf(
                    WorkoutSetUiModel(
                        id = "102:1",
                        setNumber = 1,
                        previousPerformance = "52kg x 4",
                        weight = "52",
                        reps = "4",
                        isCompleted = false,
                        isCompletionEnabled = false,
                        activeFeedbackVisible = false,
                        feedbackMessage = null,
                        selectedRpe = null,
                        suggestedNextWeight = null
                    ),
                    WorkoutSetUiModel(
                        id = "102:2",
                        setNumber = 2,
                        previousPerformance = "52kg x 6",
                        weight = "52",
                        reps = "6",
                        isCompleted = false,
                        isCompletionEnabled = false,
                        activeFeedbackVisible = false,
                        feedbackMessage = null,
                        selectedRpe = null,
                        suggestedNextWeight = null
                    )
                )
            )
        )

        return LogWorkoutUiState(
            isLoading = false,
            hasActiveSession = true,
            session = WorkoutSessionUiModel(
                id = 99L,
                durationText = "24 min",
                volumeText = "645 kg",
                setsText = "2/5",
                progress = 0.4f,
                exercises = exercises
            )
        )
    }

    fun contentStateWithFeedback(): LogWorkoutUiState {
        val state = contentState()
        val updatedExercises = state.session?.exercises?.map { exercise ->
            if (exercise.id != 101L) return@map exercise
            exercise.copy(
                sets = exercise.sets.map { set ->
                    if (set.id != "101:3") return@map set
                    set.copy(
                        isCompleted = true,
                        activeFeedbackVisible = true,
                        feedbackMessage = "Nice - matched previous set",
                        selectedRpe = 5,
                        suggestedNextWeight = 45
                    )
                }
            )
        } ?: emptyList()

        return state.copy(
            session = state.session?.copy(exercises = updatedExercises)
        )
    }
}
