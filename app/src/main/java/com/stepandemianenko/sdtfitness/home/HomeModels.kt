package com.stepandemianenko.sdtfitness.home

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

enum class DailyStepsSourceType {
    MANUAL,
    HEALTH_CONNECT
}

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
    val routineStreakDates: Set<LocalDate> = emptySet()
) {
    val currentStreakCount: Int
        get() = calculateCurrentStreak(routineStreakDates, LocalDate.now())
}

data class HomeUiState(
    val dashboard: HomeDashboardState = HomeDashboardState(),
    val visibleRoutineMonth: YearMonth = YearMonth.now(),
    val isDailyQuestEditorOpen: Boolean = false,
    val draftTargetSteps: String = "",
    val draftCurrentSteps: String = ""
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
