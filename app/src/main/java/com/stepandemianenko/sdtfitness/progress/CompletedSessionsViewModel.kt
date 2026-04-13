package com.stepandemianenko.sdtfitness.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.data.repository.CompletedSessionHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class CompletedSessionsUiState(
    val isLoading: Boolean = true,
    val sessionsThisWeek: Int = 0,
    val sessionsLastWeek: Int = 0,
    val sessions: List<CompletedSessionItemUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class CompletedSessionItemUiModel(
    val sessionId: Long,
    val title: String,
    val dateLabel: String,
    val exerciseCountLabel: String,
    val summaryLabel: String?
)

class CompletedSessionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AppGraph.progressRepository(application)

    private val _uiState = MutableStateFlow(CompletedSessionsUiState())
    val uiState: StateFlow<CompletedSessionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                repository.getCompletedSessionsHistory()
            }.onSuccess { history ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionsThisWeek = history.sessionsThisWeek,
                        sessionsLastWeek = history.sessionsLastWeek,
                        sessions = history.sessions.map(::toUiModel),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load completed sessions."
                    )
                }
            }
        }
    }

    private fun toUiModel(item: CompletedSessionHistoryItem): CompletedSessionItemUiModel {
        val exercisesLabel = if (item.exerciseCount == 1) {
            "1 exercise"
        } else {
            "${item.exerciseCount} exercises"
        }

        val summary = if (item.totalSets > 0 || item.totalReps > 0 || item.totalVolumeKg > 0.0) {
            val roundedVolume = item.totalVolumeKg.roundToInt()
            "${item.totalSets} sets • ${item.totalReps} reps • ${"%,d".format(roundedVolume)} kg volume"
        } else {
            null
        }

        return CompletedSessionItemUiModel(
            sessionId = item.sessionId,
            title = resolveSessionTitle(templateId = item.templateId, sessionId = item.sessionId),
            dateLabel = formatDate(item.completedAtMillis),
            exerciseCountLabel = exercisesLabel,
            summaryLabel = summary
        )
    }

    private fun resolveSessionTitle(templateId: String?, sessionId: Long): String {
        val cleanedTemplateName = templateId
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

        return cleanedTemplateName ?: "Workout Session #$sessionId"
    }

    private fun formatDate(epochMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }
}
