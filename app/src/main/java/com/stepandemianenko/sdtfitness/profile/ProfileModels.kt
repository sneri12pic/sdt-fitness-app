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

data class ProfileStats(
    val workouts: Int = 0,
    val streakDays: Int = 0,
    val questsDone: Int = 0
)

/** Account identity + all-time stats shown on the profile overview. */
data class ProfileOverview(
    val displayName: String = "Guest",
    val isGuest: Boolean = true,
    val stats: ProfileStats = ProfileStats()
)

/** Time window for the quest-completion chart. */
enum class QuestChartRange(val label: String, val days: Long) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90),
    YEAR("Year", 365)
}

/** One column in the quest chart: a quest and how many times it was done in the range. */
data class QuestBarPoint(
    val label: String,
    val count: Int
)

data class ProfileUiState(
    val routine: RoutineSettings = RoutineSettings(),
    val draftRoutine: RoutineSettings = RoutineSettings(),
    val overview: ProfileOverview = ProfileOverview(),
    val isTimePickerOpen: Boolean = false,
    val timePickerTarget: ReminderTimeTarget? = null,
    val timePickerInitialHour: Int = 8,
    val timePickerInitialMinute: Int = 0,
    val saveMessage: String? = null,
    val isQuestChartOpen: Boolean = false,
    val questChartRange: QuestChartRange = QuestChartRange.WEEK,
    val questChart: List<QuestBarPoint> = emptyList()
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
    data object OpenQuestChart : ProfileUiEvent
    data object DismissQuestChart : ProfileUiEvent
    data class SelectQuestChartRange(val range: QuestChartRange) : ProfileUiEvent
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
