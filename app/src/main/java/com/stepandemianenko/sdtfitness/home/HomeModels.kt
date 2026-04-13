package com.stepandemianenko.sdtfitness.home

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

enum class DailyStepsSourceType {
    MANUAL,
    HEALTH_CONNECT
}

enum class RecoveryOption {
    REST_DAY,
    SHORT_CHECK_IN
}

enum class QuickLogType {
    WALK,
    MOBILITY,
    CUSTOM
}

data class QuickLogEntry(
    val quickLogType: QuickLogType,
    val durationMinutes: Int,
    val timestamp: Long,
    val source: String = "manual_quick_log"
)

data class DailyQuestState(
    val sourceType: DailyStepsSourceType = DailyStepsSourceType.MANUAL,
    val targetSteps: Int = 5_000,
    val currentSteps: Int = 0,
    val isManual: Boolean = true,
    val lastUpdatedMillis: Long? = null
) {
    val progress: Float
        get() = if (targetSteps <= 0) 0f else (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
}

data class DailyGoalSummaryState(
    val stepsCurrent: Int = 0,
    val stepsTarget: Int = 5_000,
    val workoutsCompleted: Int = 0,
    val workoutsTarget: Int = 1,
    val activeMinutesCurrent: Int = 0,
    val activeMinutesTarget: Int = 30
) {
    val workoutsCompletedCapped: Int
        get() = min(workoutsCompleted, workoutsTarget)

    val overallProgress: Float
        get() {
            val stepRatio = if (stepsTarget <= 0) 0f else (stepsCurrent.toFloat() / stepsTarget.toFloat()).coerceIn(0f, 1f)
            val workoutRatio = if (workoutsTarget <= 0) 0f else (workoutsCompletedCapped.toFloat() / workoutsTarget.toFloat()).coerceIn(0f, 1f)
            val activeRatio = if (activeMinutesTarget <= 0) 0f else (activeMinutesCurrent.toFloat() / activeMinutesTarget.toFloat()).coerceIn(0f, 1f)
            return ((stepRatio + workoutRatio + activeRatio) / 3f).coerceIn(0f, 1f)
        }
}

data class HomeDashboardState(
    val dailyQuest: DailyQuestState = DailyQuestState(),
    val dailyGoalSummary: DailyGoalSummaryState = DailyGoalSummaryState(),
    val routineStreakDates: Set<LocalDate> = emptySet(),
    val restDay: RestDayUiState = RestDayUiState(),
    val quickLogToday: QuickLogEntry? = null,
    val healthConnectLastSyncedAtMillis: Long? = null,
    val healthConnectImportedSteps: Long? = null,
    val healthConnectLatestWeightKg: Double? = null
) {
    val currentStreakCount: Int
        get() = calculateCurrentStreak(routineStreakDates, LocalDate.now())
}

data class RestDayUiState(
    val selectedOption: RecoveryOption = RecoveryOption.REST_DAY,
    val savedTodayOption: RecoveryOption? = null,
    val savedTodayAtMillis: Long? = null
)

data class HomeUiState(
    val dashboard: HomeDashboardState = HomeDashboardState(),
    val visibleRoutineMonth: YearMonth = YearMonth.now(),
    val isDailyQuestEditorOpen: Boolean = false,
    val draftTargetSteps: String = "",
    val draftCurrentSteps: String = "",
    val activeAccountId: String? = null,
    val accounts: List<DebugAccountUiModel> = emptyList(),
    val pendingHealthConnectStepsToAdd: Int? = null
)

data class DebugAccountUiModel(
    val id: String,
    val type: String,
    val createdAt: Long,
    val isActive: Boolean
)

fun calculateCurrentStreak(
    streakDates: Set<LocalDate>,
    today: LocalDate
): Int {
    if (streakDates.isEmpty()) return 0
    var streak = 0
    var cursor = today
    while (streakDates.contains(cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}
