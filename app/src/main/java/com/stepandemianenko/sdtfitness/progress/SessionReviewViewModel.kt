package com.stepandemianenko.sdtfitness.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.CompletedSessionReview
import com.stepandemianenko.sdtfitness.data.repository.SessionExerciseReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionReviewUiState(
    val isLoading: Boolean = true,
    val title: String = "Session Review",
    val dateLabel: String = "",
    val exercises: List<SessionExerciseReviewUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class SessionExerciseReviewUiModel(
    val exerciseName: String,
    val sets: List<SessionSetRowUiModel>,
    val weightChart: SetMetricChartUiModel,
    val repsChart: SetMetricChartUiModel
)

data class SessionSetRowUiModel(
    val setLabel: String,
    val weightLabel: String,
    val repsLabel: String
)

class SessionReviewViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.progressRepository(application)

    private val _uiState = MutableStateFlow(SessionReviewUiState())
    val uiState: StateFlow<SessionReviewUiState> = _uiState.asStateFlow()

    private var loadedSessionId: Long? = null

    fun load(sessionId: Long) {
        if (loadedSessionId == sessionId && !_uiState.value.isLoading) return
        loadedSessionId = sessionId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                repository.getCompletedSessionReview(sessionId)
            }.onSuccess { review ->
                if (review == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Completed session not found."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = resolveSessionTitle(review),
                            dateLabel = formatDate(review.completedAtMillis),
                            exercises = review.exercises.map(::toExerciseUiModel),
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load session review."
                    )
                }
            }
        }
    }

    private fun toExerciseUiModel(exercise: SessionExerciseReview): SessionExerciseReviewUiModel {
        val weightValues = exercise.sets.map { it.actualWeightKg.toFloat() }
        val repsValues = exercise.sets.map { it.actualReps.toFloat() }

        val setRows = exercise.sets.map { set ->
            SessionSetRowUiModel(
                setLabel = "Set ${set.setNumber}",
                weightLabel = "${set.actualWeightKg} kg",
                repsLabel = "${set.actualReps} reps"
            )
        }

        return SessionExerciseReviewUiModel(
            exerciseName = exercise.exerciseName,
            sets = setRows,
            weightChart = SetMetricChartUiModel(
                title = "Weight across sets",
                actualLabel = "Actual weight",
                actualValues = weightValues,
                targetValues = List(weightValues.size) { null },
                unitLabel = "kg"
            ),
            repsChart = SetMetricChartUiModel(
                title = "Reps across sets",
                actualLabel = "Actual reps",
                actualValues = repsValues,
                targetValues = List(repsValues.size) { null },
                unitLabel = "reps"
            )
        )
    }

    private fun resolveSessionTitle(review: CompletedSessionReview): String {
        val cleanedTemplateName = review.templateId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.split(' ')
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ") { token ->
                token.lowercase().replaceFirstChar { first ->
                    if (first.isLowerCase()) first.titlecase(Locale.ENGLISH) else first.toString()
                }
            }

        return cleanedTemplateName ?: "Workout Session #${review.sessionId}"
    }

    private fun formatDate(epochMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }
}
