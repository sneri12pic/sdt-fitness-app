package com.stepandemianenko.sdtfitness.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepandemianenko.sdtfitness.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    repository = (this[APPLICATION_KEY] as App).container.profileRepository
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var hasReceivedRoutine = false

    init {
        viewModelScope.launch {
            repository.routineSettings.collect { settings ->
                _uiState.update { current ->
                    val keepEditing = hasReceivedRoutine && current.draftRoutine != current.routine
                    hasReceivedRoutine = true
                    current.copy(
                        routine = settings,
                        draftRoutine = if (keepEditing) current.draftRoutine else settings
                    )
                }
            }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.SelectGoal -> updateDraft { it.copy(goalId = event.goalId) }
            is ProfileUiEvent.SelectFrequency -> updateDraft { it.copy(frequencyId = event.frequencyId) }
            is ProfileUiEvent.ToggleDay -> updateDraft { draft ->
                val days = if (draft.dayIds.contains(event.dayId)) {
                    draft.dayIds - event.dayId
                } else {
                    draft.dayIds + event.dayId
                }
                draft.copy(dayIds = days)
            }

            is ProfileUiEvent.TogglePresetReminder -> updateDraft { draft ->
                val times = if (draft.reminderTimes.contains(event.time)) {
                    draft.reminderTimes - event.time
                } else {
                    draft.reminderTimes + event.time
                }
                draft.copy(reminderTimes = times.distinct().sorted())
            }

            ProfileUiEvent.ToggleRemindersEnabled -> updateDraft { it.copy(reminderEnabled = !it.reminderEnabled) }

            is ProfileUiEvent.OpenPresetTimePicker -> openTimePicker(
                target = ReminderTimeTarget.Preset(oldTime = event.time),
                existingTime = event.time
            )

            ProfileUiEvent.OpenCustomTimePicker -> openTimePicker(
                target = ReminderTimeTarget.Custom(oldTime = null),
                existingTime = null
            )

            is ProfileUiEvent.SavePickedTime -> applyPickedTime(hour = event.hour, minute = event.minute)

            ProfileUiEvent.DismissTimePicker -> _uiState.update {
                it.copy(isTimePickerOpen = false, timePickerTarget = null)
            }

            is ProfileUiEvent.RemoveCustomReminder -> updateDraft { draft ->
                draft.copy(customReminderTimes = draft.customReminderTimes - event.time)
            }

            ProfileUiEvent.SaveRoutine -> saveRoutine()

            ProfileUiEvent.ClearSaveMessage -> _uiState.update { it.copy(saveMessage = null) }
        }
    }

    private fun updateDraft(transform: (RoutineSettings) -> RoutineSettings) {
        _uiState.update { it.copy(draftRoutine = transform(it.draftRoutine)) }
    }

    private fun openTimePicker(target: ReminderTimeTarget, existingTime: String?) {
        val (initialHour, initialMinute) = existingTime?.let(::parseReminderTime) ?: (8 to 0)
        _uiState.update {
            it.copy(
                isTimePickerOpen = true,
                timePickerTarget = target,
                timePickerInitialHour = initialHour,
                timePickerInitialMinute = initialMinute
            )
        }
    }

    private fun applyPickedTime(hour: Int, minute: Int) {
        val newTime = formatReminderTime(hour, minute)
        when (val target = _uiState.value.timePickerTarget) {
            is ReminderTimeTarget.Preset -> updateDraft { draft ->
                val times = (draft.reminderTimes - target.oldTime + newTime).distinct().sorted()
                draft.copy(reminderTimes = times)
            }

            is ReminderTimeTarget.Custom -> updateDraft { draft ->
                val withoutOld = if (target.oldTime != null) {
                    draft.customReminderTimes - target.oldTime
                } else {
                    draft.customReminderTimes
                }
                draft.copy(customReminderTimes = (withoutOld + newTime).distinct().sorted())
            }

            null -> Unit
        }
        _uiState.update { it.copy(isTimePickerOpen = false, timePickerTarget = null) }
    }

    private fun saveRoutine() {
        val draft = _uiState.value.draftRoutine
        viewModelScope.launch {
            val saved = repository.saveRoutine(draft)
            _uiState.update {
                it.copy(
                    routine = saved,
                    draftRoutine = saved,
                    saveMessage = "Saved — your routine, your call. You can adjust it anytime."
                )
            }
        }
    }
}
