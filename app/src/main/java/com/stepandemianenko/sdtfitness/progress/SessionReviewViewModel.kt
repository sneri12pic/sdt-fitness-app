package com.stepandemianenko.sdtfitness.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.CompletedSessionReview
import com.stepandemianenko.sdtfitness.data.repository.ExerciseTrend
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
                val review = repository.getCompletedSessionReview(sessionId)
                val trends = if (review != null) {
                    repository.getExerciseTrends(sessionId)
                } else {
                    emptyList()
                }
                review to trends
            }.onSuccess { (review, trends) ->
                if (review == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Completed session not found."
                        )
                    }
                } else {
                    val trendsByExerciseId = trends.associateBy { it.exerciseId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = resolveSessionTitle(review),
                            dateLabel = formatDate(review.completedAtMillis),
                            exercises = review.exercises.map { exercise ->
                                toExerciseUiModel(exercise, trendsByExerciseId[exercise.exerciseId])
                            },
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

    private fun toExerciseUiModel(
        exercise: SessionExerciseReview,
        trend: ExerciseTrend?
    ): SessionExerciseReviewUiModel {
        val setRows = exercise.sets.map { set ->
            SessionSetRowUiModel(
                setLabel = "Set ${set.setNumber}",
                weightLabel = "${set.actualWeightKg} kg",
                repsLabel = "${set.actualReps} reps"
            )
        }

        // Cross-session trend: one point per completed session that included this exercise,
        // ordered chronologically (the repository already sorts ascending by completion date).
        val trendPoints = trend?.points.orEmpty()
        val timestamps = trendPoints.map { it.completedAtMillis }
        val weightValues = trendPoints.map { it.maxWeightKg.toFloat() }
        val repsValues = trendPoints.map { it.maxReps.toFloat() }

        // Per-set values for the reviewed session, powering the most zoomed-in "Session" view.
        val sessionWeightValues = exercise.sets.map { it.actualWeightKg.toFloat() }
        val sessionRepsValues = exercise.sets.map { it.actualReps.toFloat() }

        return SessionExerciseReviewUiModel(
            exerciseName = exercise.exerciseName,
            sets = setRows,
            weightChart = SetMetricChartUiModel(
                title = "Weight trend",
                actualLabel = "Heaviest set",
                actualValues = weightValues,
                targetValues = List(weightValues.size) { null },
                unitLabel = "kg",
                pointTimestampsMillis = timestamps,
                sessionSetValues = sessionWeightValues
            ),
            repsChart = SetMetricChartUiModel(
                title = "Reps trend",
                actualLabel = "Best set reps",
                actualValues = repsValues,
                targetValues = List(repsValues.size) { null },
                unitLabel = "reps",
                pointTimestampsMillis = timestamps,
                sessionSetValues = sessionRepsValues
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
