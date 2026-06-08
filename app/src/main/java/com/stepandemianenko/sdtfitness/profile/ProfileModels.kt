package com.stepandemianenko.sdtfitness.profile

data class RoutineSettings(
    val goalId: String = RoutineDefaults.goalId,
    val frequencyId: String = RoutineDefaults.frequencyId,
    val dayIds: Set<String> = RoutineDefaults.dayIds,
    val reminderEnabled: Boolean = true,
    val reminderTimes: List<String> = RoutineDefaults.reminderTimes,
    val customReminderTimes: List<String> = emptyList()
) {
    val allReminderTimes: List<String>
        get() = (reminderTimes + customReminderTimes).distinct().sorted()
}

data class ProfileUiState(
    val routine: RoutineSettings = RoutineSettings(),
    val draftRoutine: RoutineSettings = RoutineSettings(),
    val isTimePickerOpen: Boolean = false,
    val timePickerTarget: ReminderTimeTarget? = null,
    val timePickerInitialHour: Int = 8,
    val timePickerInitialMinute: Int = 0,
    val saveMessage: String? = null
)

sealed interface ReminderTimeTarget {
    data class Preset(val oldTime: String) : ReminderTimeTarget
    data class Custom(val oldTime: String?) : ReminderTimeTarget
}

sealed interface ProfileUiEvent {
    data class SelectGoal(val goalId: String) : ProfileUiEvent
    data class SelectFrequency(val frequencyId: String) : ProfileUiEvent
    data class ToggleDay(val dayId: String) : ProfileUiEvent
    data class TogglePresetReminder(val time: String) : ProfileUiEvent
    data object ToggleRemindersEnabled : ProfileUiEvent
    data class OpenPresetTimePicker(val time: String) : ProfileUiEvent
    data object OpenCustomTimePicker : ProfileUiEvent
    data class SavePickedTime(val hour: Int, val minute: Int) : ProfileUiEvent
    data object DismissTimePicker : ProfileUiEvent
    data class RemoveCustomReminder(val time: String) : ProfileUiEvent
    data object SaveRoutine : ProfileUiEvent
    data object ClearSaveMessage : ProfileUiEvent
}

object RoutineDefaults {
    const val goalId = "strength"
    const val frequencyId = "3_days"
    val dayIds = setOf("wed")
    val reminderTimes = listOf("08:00")
}

fun formatReminderTime(hour: Int, minute: Int): String {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    return safeHour.toString().padStart(2, '0') + ":" + safeMinute.toString().padStart(2, '0')
}

fun parseReminderTime(value: String): Pair<Int, Int>? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}
