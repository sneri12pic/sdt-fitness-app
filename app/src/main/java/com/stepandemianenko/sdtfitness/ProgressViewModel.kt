package com.stepandemianenko.sdtfitness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.ProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ProgressUiState(
    val isLoading: Boolean = true,
    val completedSessions: Int = 0,
    val workoutDays: Int = 0,
    val streakDays: Int = 0,
    val consistencyTitle: String = "0 Completed Sessions",
    val consistencySubtitle: String = "No completed sessions yet",
    val streakSubtitle: String = "No streak yet",
    val bestLiftValue: String = "No lift logged",
    val bestLiftSubtitle: String = "Heaviest completed set",
    val volumeValue: String = "0 kg",
    val volumeSubtitle: String = "Across completed sessions",
    val personalBestsValue: String = "0 sets logged",
    val personalBestsSubtitle: String = "0 reps total",
    val topExerciseText: String = "Top volume exercise will appear here",
    val sessionsLast7Days: Int = 0,
    val sessionsPrevious7Days: Int = 0
)

class ProgressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.progressRepository(application)

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val summary = repository.getSummary()
            _uiState.value = summary.toUiState()
        }
    }

    private fun ProgressSummary.toUiState(): ProgressUiState {
        val weeklyDelta = last7DaySessions - previous7DaySessions
        val weeklyDeltaText = when {
            weeklyDelta > 0 -> "+$weeklyDelta vs previous 7 days"
            weeklyDelta < 0 -> "$weeklyDelta vs previous 7 days"
            else -> "Same as previous 7 days"
        }

        return ProgressUiState(
            isLoading = false,
            completedSessions = completedSessions,
            workoutDays = workoutDays,
            streakDays = streakDays,
            consistencyTitle = "$completedSessions Completed Sessions",
            consistencySubtitle = weeklyDeltaText,
            streakSubtitle = if (streakDays > 0) "$streakDays-day continuity" else "No streak yet",
            bestLiftValue = if (bestLiftKg > 0) "$bestLiftKg kg" else "No lift logged",
            bestLiftSubtitle = "Heaviest completed set",
            volumeValue = "${formatWhole(totalVolumeKg)} kg",
            volumeSubtitle = "Across completed sessions",
            personalBestsValue = "$totalSets sets logged",
            personalBestsSubtitle = "$totalReps reps total",
            topExerciseText = topExerciseName?.let {
                val topVolume = topExerciseVolumeKg?.let(::formatWhole) ?: "0"
                "$it ($topVolume kg)"
            } ?: "Top volume exercise will appear here",
            sessionsLast7Days = last7DaySessions,
            sessionsPrevious7Days = previous7DaySessions
        )
    }

    private fun formatWhole(value: Double): String {
        return "%,d".format(value.roundToInt())
    }
}
