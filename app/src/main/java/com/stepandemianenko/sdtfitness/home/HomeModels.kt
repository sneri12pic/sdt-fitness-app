package com.stepandemianenko.sdtfitness.home

import com.stepandemianenko.sdtfitness.progress.SetMetricChartUiModel
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

enum class DailyStepsSourceType {
    MANUAL,
    HEALTH_CONNECT
}

enum class DailyQuestCompletionSource {
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

enum class RoutineCalendarActivity {
    WORKOUT,
    QUICK_LOG,
    REST_DAY,
    ACTIVITY
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

data class WeightInQuestState(
    val isAdded: Boolean = false,
    val isCompleted: Boolean = false,
    val completionSource: DailyQuestCompletionSource? = null,
    val completedAtMillis: Long? = null,
    val weightKg: Double? = null
)

data class CreatineIntakeLog(
    val id: Long,
    val amountGrams: Int,
    val timestampMillis: Long
)

data class CreatineIntakeQuestState(
    val isAdded: Boolean = false,
    val currentGramsToday: Int = 0,
    val targetGrams: Int = 5,
    val portionGrams: Int = 5,
    val todayLogs: List<CreatineIntakeLog> = emptyList()
) {
    val progress: Float
        get() = if (targetGrams <= 0) {
            0f
        } else {
            (currentGramsToday.toFloat() / targetGrams.toFloat()).coerceIn(0f, 1f)
        }
}

data class WaterIntakeLog(
    val id: Long,
    val amountMl: Int,
    val timestampMillis: Long
)

data class WaterIntakeQuestState(
    val isAdded: Boolean = false,
    val currentMlToday: Int = 0,
    val targetMl: Int = 2000,
    val portionMl: Int = 250,
    val todayLogs: List<WaterIntakeLog> = emptyList()
) {
    val progress: Float
        get() = if (targetMl <= 0) {
            0f
        } else {
            (currentMlToday.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)
        }
}

data class DailyGoalSummaryState(
    val stepsCurrent: Int = 0,
    val stepsTarget: Int = 5_000,
    val workoutsCompleted: Int = 0,
    val workoutsTarget: Int = 1,
    val questsCompleted: Int = 0,
    val questsTarget: Int = 1
) {
    val workoutsCompletedCapped: Int
        get() = min(workoutsCompleted, workoutsTarget)

    val questsCompletedCapped: Int
        get() = min(questsCompleted, questsTarget)

    val overallProgress: Float
        get() {
            val stepRatio = if (stepsTarget <= 0) 0f else (stepsCurrent.toFloat() / stepsTarget.toFloat()).coerceIn(0f, 1f)
            val workoutRatio = if (workoutsTarget <= 0) 0f else (workoutsCompletedCapped.toFloat() / workoutsTarget.toFloat()).coerceIn(0f, 1f)
            val questRatio = if (questsTarget <= 0) 0f else (questsCompletedCapped.toFloat() / questsTarget.toFloat()).coerceIn(0f, 1f)
            return ((stepRatio + workoutRatio + questRatio) / 3f).coerceIn(0f, 1f)
        }
}

data class HomeDashboardState(
    val dailyQuest: DailyQuestState = DailyQuestState(),
    val weightInQuest: WeightInQuestState = WeightInQuestState(),
    val creatineIntakeQuest: CreatineIntakeQuestState = CreatineIntakeQuestState(),
    val waterIntakeQuest: WaterIntakeQuestState = WaterIntakeQuestState(),
    val dailyGoalSummary: DailyGoalSummaryState = DailyGoalSummaryState(),
    val routineStreakDates: Set<LocalDate> = emptySet(),
    val routineCalendarActivities: Map<LocalDate, Set<RoutineCalendarActivity>> = emptyMap(),
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
    val isAddCustomQuestDialogOpen: Boolean = false,
    val isWeightInChartDialogOpen: Boolean = false,
    val isCreatineOverlayOpen: Boolean = false,
    val isCreatineTargetEditorOpen: Boolean = false,
    val isCreatinePortionEditorOpen: Boolean = false,
    val isWaterOverlayOpen: Boolean = false,
    val isWaterTargetEditorOpen: Boolean = false,
    val isWaterPortionEditorOpen: Boolean = false,
    val weightInChart: SetMetricChartUiModel = emptyHomeWeightChart(),
    val draftTargetSteps: String = "",
    val draftCurrentSteps: String = "",
    val draftCreatineTargetGrams: String = "",
    val creatineTargetError: String? = null,
    val draftCreatinePortionGrams: String = "",
    val creatinePortionError: String? = null,
    val draftWaterTargetMl: String = "",
    val waterTargetError: String? = null,
    val draftWaterPortionMl: String = "",
    val waterPortionError: String? = null,
    val activeAccountId: String? = null,
    val accounts: List<DebugAccountUiModel> = emptyList(),
    val pendingHealthConnectStepsToAdd: Int? = null
)

private fun emptyHomeWeightChart(): SetMetricChartUiModel {
    return SetMetricChartUiModel(
        title = "Weight Progress",
        actualLabel = "Weight",
        actualValues = emptyList(),
        targetValues = emptyList(),
        unitLabel = "kg"
    )
}

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

fun buildRoutineCalendarActivityMap(
    routineDates: Set<LocalDate>,
    workoutDates: Set<LocalDate>,
    quickLogDates: Set<LocalDate>,
    restDayDates: Set<LocalDate>
): Map<LocalDate, Set<RoutineCalendarActivity>> {
    val activitiesByDate = linkedMapOf<LocalDate, MutableSet<RoutineCalendarActivity>>()

    fun addDates(dates: Set<LocalDate>, activity: RoutineCalendarActivity) {
        dates.sorted().forEach { date ->
            activitiesByDate.getOrPut(date) { linkedSetOf() }.add(activity)
        }
    }

    addDates(workoutDates, RoutineCalendarActivity.WORKOUT)
    addDates(quickLogDates, RoutineCalendarActivity.QUICK_LOG)
    addDates(restDayDates, RoutineCalendarActivity.REST_DAY)

    routineDates.sorted().forEach { date ->
        if (activitiesByDate[date].isNullOrEmpty()) {
            activitiesByDate.getOrPut(date) { linkedSetOf() }
                .add(RoutineCalendarActivity.ACTIVITY)
        }
    }

    return activitiesByDate.mapValues { (_, activities) -> activities.toSet() }
}
